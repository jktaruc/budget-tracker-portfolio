import { useEffect, useState } from "react";
import api from "../api/api";
import { useSubscription } from "../context/SubscriptionContext";
import "../styles/PaywallModal.css";

interface Props {
  feature: string;
  onClose: () => void;
}

export default function PaywallModal({ feature, onClose }: Props) {
  useSubscription();
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState("");

  // Lock body scroll while open
  useEffect(() => {
    document.body.style.overflow = "hidden";
    return () => { document.body.style.overflow = ""; };
  }, []);

  const handleUpgrade = async () => {
    setLoading(true);
    setError("");
    try {
      const { data } = await api.post("/subscription/checkout");
      // Redirect to Stripe Checkout
      window.location.href = data.url;
    } catch {
      setError("Failed to start checkout. Please try again.");
      setLoading(false);
    }
  };

  const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) onClose();
  };

  return (
    <div className="paywall-overlay" onClick={handleOverlayClick}>
      <div className="paywall-modal">
        <button className="paywall-close" onClick={onClose} aria-label="Close">×</button>

        <div className="paywall-icon">⭐</div>

        <h2 className="paywall-title">Upgrade to Pro</h2>
        <p className="paywall-subtitle">
          <strong>{feature}</strong> is a Pro feature.
        </p>

        <div className="paywall-features">
          <div className="paywall-plan">
            <div className="paywall-plan-header free">
              <span>Free</span>
              <strong>$0 / month</strong>
            </div>
            <ul>
              <li>✅ Track expenses &amp; income</li>
              <li>✅ Add categories</li>
              <li>❌ Spending Limits</li>
              <li>❌ Financial Summary</li>
              <li>❌ Recurring Transactions</li>
              <li>❌ CSV Import / Export</li>
            </ul>
          </div>

          <div className="paywall-plan highlighted">
            <div className="paywall-plan-header pro">
              <span>Pro</span>
              <strong>$5 / month</strong>
            </div>
            <ul>
              <li>✅ Everything in Free</li>
              <li>✅ Spending Limits</li>
              <li>✅ Financial Summary + Projections</li>
              <li>✅ Recurring Transactions</li>
              <li>✅ CSV Import &amp; Export</li>
              <li>✅ Priority support</li>
            </ul>
          </div>
        </div>

        {error && <p className="paywall-error">{error}</p>}

        <button
          className="paywall-btn-upgrade"
          onClick={handleUpgrade}
          disabled={loading}
        >
          {loading ? "Redirecting to Stripe..." : "Upgrade to Pro — $5/month"}
        </button>

        <p className="paywall-note">
          Test card: <code>4242 4242 4242 4242</code> · Any future date · Any CVC
        </p>
      </div>
    </div>
  );
}
