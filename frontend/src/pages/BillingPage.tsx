import { useEffect, useState } from "react";
import { useSearchParams, Link } from "react-router-dom";
import { useSubscription } from "../context/SubscriptionContext";
import "../styles/BillingPage.css";

export default function BillingPage() {
    const [params] = useSearchParams();
    const { isPro, plan, cancelling, currentPeriodEnd, refresh, cancelSubscription } = useSubscription();
    const [cancelling_req, setCancellingReq] = useState(false);
    const [showConfirm, setShowConfirm]      = useState(false);

    const success   = params.get("success") === "true";
    const cancelled = params.get("cancelled") === "true";

    useEffect(() => {
        if (success) refresh();
    }, [success, refresh]);

    const handleCancel = async () => {
        setCancellingReq(true);
        try {
            await cancelSubscription();
            setShowConfirm(false);
        } finally {
            setCancellingReq(false);
        }
    };

    const formatDate = (iso: string | null) => {
        if (!iso) return "";
        return new Date(iso).toLocaleDateString(undefined, { year: "numeric", month: "long", day: "numeric" });
    };

    return (
        <div className="billing-page">
            <div className="billing-card">
                {success && (
                    <>
                        <div className="billing-icon">🎉</div>
                        <h1>You're on Pro!</h1>
                        <p>Your payment was successful. All Pro features are now unlocked.</p>
                    </>
                )}
                {cancelled && (
                    <>
                        <div className="billing-icon">↩️</div>
                        <h1>Checkout cancelled</h1>
                        <p>No charge was made. You're still on the Free plan.</p>
                    </>
                )}
                {!success && !cancelled && (
                    <>
                        <div className="billing-icon">💳</div>
                        <h1>Billing</h1>
                    </>
                )}

                <div className="billing-status">
                    <span className="billing-label">Current plan:</span>
                    <span className={`billing-plan billing-plan--${plan.toLowerCase()}`}>
                        {isPro ? "⭐ Pro" : "Free"}
                    </span>
                </div>

                {cancelling && currentPeriodEnd && (
                    <div className="billing-cancelling-notice">
                        ⚠️ Your Pro access will end on <strong>{formatDate(currentPeriodEnd)}</strong>. You won't be charged again.
                    </div>
                )}

                {isPro && !cancelling && (
                    <>
                        {currentPeriodEnd && (
                            <p className="billing-renews">
                                Next renewal: <strong>{formatDate(currentPeriodEnd)}</strong>
                            </p>
                        )}
                        {!showConfirm ? (
                            <button className="billing-btn-cancel" onClick={() => setShowConfirm(true)}>
                                Cancel subscription
                            </button>
                        ) : (
                            <div className="billing-confirm">
                                <p>Are you sure? You'll keep Pro access until <strong>{formatDate(currentPeriodEnd)}</strong>.</p>
                                <div className="billing-confirm-actions">
                                    <button onClick={handleCancel} disabled={cancelling_req} className="billing-btn-confirm-cancel">
                                        {cancelling_req ? "Cancelling…" : "Yes, cancel"}
                                    </button>
                                    <button onClick={() => setShowConfirm(false)} className="billing-btn-keep">
                                        Keep Pro
                                    </button>
                                </div>
                            </div>
                        )}
                    </>
                )}

                <Link to="/" className="billing-btn-home">
                    ← Back to Dashboard
                </Link>
            </div>
        </div>
    );
}
