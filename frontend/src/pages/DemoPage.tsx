import { useState } from "react";
import { useNavigate, Navigate } from "react-router-dom";
import api from "../api/api";
import { useAuth } from "../context/AuthContext";
import "../styles/Demo.css";

export default function DemoPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const navigate = useNavigate();
    const { user, isLoading, storeSession } = useAuth();

    // Already authenticated — go straight to the app
    if (!isLoading && user) return <Navigate to="/" replace />;

    const handleTryDemo = async () => {
        setLoading(true);
        setError("");
        try {
            const { data } = await api.post("/demo/reset");
            storeSession(data);
            navigate("/");
        } catch {
            setError("Failed to load demo. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="demo-landing">
            <div className="demo-hero">
                <div className="demo-hero-content">
                    <h1>💰 Budget Tracker</h1>
                    <p className="demo-tagline">
                        Track expenses, set budget goals, and understand your finances — all in one place.
                    </p>

                    <div className="demo-features">
                        <div className="demo-feature">
                            <span>📊</span>
                            <div>
                                <strong>Dashboard</strong>
                                <p>Track expenses and income with a clean, intuitive interface</p>
                            </div>
                        </div>
                        <div className="demo-feature">
                            <span>🎯</span>
                            <div>
                                <strong>Spending Limits</strong>
                                <p>Set spending limits per category and get alerted when you're close</p>
                            </div>
                        </div>
                        <div className="demo-feature">
                            <span>🔁</span>
                            <div>
                                <strong>Recurring Transactions</strong>
                                <p>Automate regular income and expenses — never miss an entry</p>
                            </div>
                        </div>
                        <div className="demo-feature">
                            <span>📈</span>
                            <div>
                                <strong>Financial Summary</strong>
                                <p>Visual charts showing your income vs expenses and savings rate</p>
                            </div>
                        </div>
                        <div className="demo-feature">
                            <span>📥</span>
                            <div>
                                <strong>Export CSV</strong>
                                <p>Download your transaction history anytime</p>
                            </div>
                        </div>
                    </div>

                    <div className="demo-cta">
                        <button
                            className="demo-btn-primary"
                            onClick={handleTryDemo}
                            disabled={loading}
                        >
                            {loading ? "Loading demo..." : "🚀 Try the Live Demo"}
                        </button>
                        <button
                            className="demo-btn-secondary"
                            onClick={() => navigate("/login")}
                        >
                            Sign In / Register
                        </button>
                    </div>

                    {error && <p className="demo-error">{error}</p>}

                    <p className="demo-note">
                        Demo uses a shared account with prepopulated data. Your changes will be reset when anyone clicks "Reset Demo".
                    </p>
                </div>
            </div>

            <div className="demo-stack">
                <h3>Built with</h3>
                <div className="demo-badges">
                    {["React 19", "TypeScript", "Spring Boot 3", "PostgreSQL", "Docker", "JWT Auth", "GitHub Actions"].map(t => (
                        <span key={t} className="demo-badge">{t}</span>
                    ))}
                </div>
            </div>
        </div>
    );
}
