import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react";
import api from "../api/api";
import { useAuth } from "./AuthContext";

interface SubscriptionContextType {
  isPro: boolean;
  plan: "FREE" | "PRO";
  cancelling: boolean;
  currentPeriodEnd: string | null;
  isLoading: boolean;
  refresh: () => void;
  cancelSubscription: () => Promise<void>;
}

const SubscriptionContext = createContext<SubscriptionContextType | null>(null);

export function SubscriptionProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [isPro, setIsPro]                     = useState(false);
  const [plan, setPlan]                       = useState<"FREE" | "PRO">("FREE");
  const [cancelling, setCancelling]           = useState(false);
  const [currentPeriodEnd, setPeriodEnd]      = useState<string | null>(null);
  const [isLoading, setLoading]               = useState(true);

  const fetchStatus = useCallback(async () => {
    if (!user) { setLoading(false); return; }
    try {
      const { data } = await api.get("/subscription/status");
      setIsPro(data.pro);
      setPlan(data.plan);
      setCancelling(data.cancelling ?? false);
      setPeriodEnd(data.currentPeriodEnd ?? null);
    } catch {
      setIsPro(false);
      setPlan("FREE");
      setCancelling(false);
      setPeriodEnd(null);
    } finally {
      setLoading(false);
    }
  }, [user]);

  const cancelSubscription = useCallback(async () => {
    await api.post("/subscription/cancel");
    await fetchStatus();
  }, [fetchStatus]);

  useEffect(() => { fetchStatus(); }, [fetchStatus]);

  return (
    <SubscriptionContext.Provider value={{
      isPro, plan, cancelling, currentPeriodEnd, isLoading, refresh: fetchStatus, cancelSubscription
    }}>
      {children}
    </SubscriptionContext.Provider>
  );
}

export function useSubscription() {
  const ctx = useContext(SubscriptionContext);
  if (!ctx) throw new Error("useSubscription must be used within SubscriptionProvider");
  return ctx;
}
