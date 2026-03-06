import { useState } from "react";
import { useNavigate, Navigate } from "react-router-dom";
import api from "../api/api";
import { useAuth } from "../context/AuthContext";
import "../styles/Demo.css";

export default function DemoPage() {
    const [loadingFree, setLoadingFree] = useState(false);
    const [loadingPro,  setLoadingPro]  = useState(false);
    const [error, setError] = useState("");
    const navigate = useNavigate();
    const { user, isLoading, storeSession } = useAuth();

    if (!isLoading && user) return <Navigate to="/" replace />;

    const handleTryDemo = async (plan: "free" | "pro") => {
        const setLoading = plan === "free" ? setLoadingFree : setLoadingPro;
        setLoading(true);
        setError("");
        try {
            const endpoint = plan === "pro" ? "/demo/reset-pro" : "/demo/reset";
            const { data } = await api.post(endpoint);
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
                                <strong>Spending Limits <span className="pro-tag">Pro</span></strong>
                                <p>Set spending limits per category and monitor your budget in real time</p>
                            </div>
                        </div>
                        <div className="demo-feature">
                            <span>🔁</span>
                            <div>
                                <strong>Recurring Transactions <span className="pro-tag">Pro</span></strong>
                                <p>Automate regular income and expenses — never miss an entry</p>
                            </div>
                        </div>
                        <div className="demo-feature">
                            <span>📈</span>
                            <div>
                                <strong>Financial Summary <span className="pro-tag">Pro</span></strong>
                                <p>Visual charts, projections, and savings rate analysis</p>
                            </div>
                        </div>
                        <div className="demo-feature">
                            <span>📥</span>
                            <div>
                                <strong>Import / Export CSV <span className="pro-tag">Pro</span></strong>
                                <p>Bulk import transactions and download your full history</p>
                            </div>
                        </div>
                    </div>

                    {/* Two demo buttons */}
                    <div className="demo-plans">
                        <div className="demo-plan-card">
                            <div className="demo-plan-header">
                                <h3>Free Plan</h3>
                                <span className="demo-plan-price">$0 / month</span>
                            </div>
                            <ul className="demo-plan-features">
                                <li>✅ Expense &amp; income tracking</li>
                                <li>✅ Categories</li>
                                <li>❌ Spending Limits</li>
                                <li>❌ Financial Summary</li>
                                <li>❌ Recurring Transactions</li>
                                <li>❌ CSV Import / Export</li>
                            </ul>
                            <button
                                className="demo-btn-secondary"
                                onClick={() => handleTryDemo("free")}
                                disabled={loadingFree || loadingPro}
                            >
                                {loadingFree ? "Loading..." : "Try Free Demo"}
                            </button>
                        </div>

                        <div className="demo-plan-card demo-plan-card--pro">
                            <div className="demo-plan-header">
                                <h3>⭐ Pro Plan</h3>
                                <span className="demo-plan-price">$5 / month</span>
                            </div>
                            <ul className="demo-plan-features">
                                <li>✅ Everything in Free</li>
                                <li>✅ Spending Limits</li>
                                <li>✅ Financial Summary + Projections</li>
                                <li>✅ Recurring Transactions</li>
                                <li>✅ CSV Import &amp; Export</li>
                            </ul>
                            <button
                                className="demo-btn-primary"
                                onClick={() => handleTryDemo("pro")}
                                disabled={loadingFree || loadingPro}
                            >
                                {loadingPro ? "Loading..." : "🚀 Try Pro Demo"}
                            </button>
                        </div>
                    </div>

                    {error && <p className="demo-error">{error}</p>}

                    <p className="demo-note">
                        Demo accounts use shared pre-populated data. Changes reset when the demo is restarted.
                    </p>

                    <button
                        className="demo-btn-link"
                        onClick={() => navigate("/login")}
                    >
                        Sign In / Register →
                    </button>
                </div>
            </div>

            <div className="demo-stack">
                <h3>Built with</h3>
                <div className="demo-badges">
                    {["React 19", "TypeScript", "Spring Boot 3", "PostgreSQL", "Stripe", "Docker", "JWT Auth", "GitHub Actions"].map(t => (
                        <span key={t} className="demo-badge">{t}</span>
                    ))}
                </div>
            </div>
        </div>
    );
}
