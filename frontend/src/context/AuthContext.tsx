import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react";
import api from "../api/api";

interface User { email: string; name: string; }

interface SessionData {
    accessToken: string;
    refreshToken: string;
    email: string;
    name: string;
}

interface AuthContextType {
    user: User | null;
    isLoading: boolean;
    login: (email: string, password: string) => Promise<void>;
    register: (email: string, name: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
    /** Store a session received from an external flow (e.g. demo login). */
    storeSession: (data: SessionData) => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const token = localStorage.getItem("accessToken");
        const savedUser = localStorage.getItem("user");

        if (!token || !savedUser) {
            setIsLoading(false);
            return;
        }

        // Validate the stored token is not expired before trusting it
        try {
            const payload = JSON.parse(atob(token.split(".")[1]));
            const isExpired = payload.exp * 1000 < Date.now();
            if (!isExpired) {
                setUser(JSON.parse(savedUser));
                api.defaults.headers.common["Authorization"] = `Bearer ${token}`;
            } else {
                // Token expired — clear stale session; the refresh interceptor in api.ts
                // will attempt a silent refresh on the first API call if a refreshToken exists.
                localStorage.removeItem("accessToken");
                localStorage.removeItem("user");
            }
        } catch {
            // Malformed token — clear everything
            clearLocalSession();
        }

        setIsLoading(false);
    }, []);

    const storeSession = useCallback((data: SessionData) => {
        localStorage.setItem("accessToken", data.accessToken);
        localStorage.setItem("refreshToken", data.refreshToken);
        localStorage.setItem("user", JSON.stringify({ email: data.email, name: data.name }));
        api.defaults.headers.common["Authorization"] = `Bearer ${data.accessToken}`;
        setUser({ email: data.email, name: data.name });
    }, []);

    const login = async (email: string, password: string) => {
        const { data } = await api.post("/auth/login", { email, password });
        storeSession(data);
    };

    const register = async (email: string, name: string, password: string) => {
        const { data } = await api.post("/auth/register", { email, name, password });
        storeSession(data);
    };

    const logout = async () => {
        // Tell the server to invalidate all refresh tokens for this user
        try {
            await api.post("/auth/logout");
        } catch {
            // Best-effort — clear client session regardless
        }
        clearLocalSession();
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, isLoading, login, register, logout, storeSession }}>
            {children}
        </AuthContext.Provider>
    );
}

function clearLocalSession() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
    delete api.defaults.headers.common["Authorization"];
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used within AuthProvider");
    return ctx;
}
