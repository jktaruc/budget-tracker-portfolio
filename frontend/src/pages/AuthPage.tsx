import { useState } from "react";
import { useNavigate, Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "../styles/Auth.css";

type Mode = "login" | "register";

export default function AuthPage() {
    const [mode, setMode] = useState<Mode>("login");
    const [email, setEmail] = useState("");
    const [name, setName] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [isLoading, setIsLoading] = useState(false);

    const { user, isLoading: authLoading, login, register } = useAuth();
    const navigate = useNavigate();

    // Already authenticated — go straight to the app
    if (!authLoading && user) return <Navigate to="/" replace />;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");
        setIsLoading(true);

        try {
            if (mode === "login") {
                await login(email, password);
            } else {
                await register(email, name, password);
            }
            navigate("/");
        } catch (err: unknown) {
            const msg = (err as { response?: { data?: { message?: string } } })
                ?.response?.data?.message;
            setError(msg || "Something went wrong. Please try again.");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <div className="auth-header">
                    <h1>💰 Budget Tracker</h1>
                    <p>{mode === "login" ? "Sign in to your account" : "Create your account"}</p>
                </div>

                <div className="auth-tabs">
                    <button
                        className={`auth-tab ${mode === "login" ? "active" : ""}`}
                        onClick={() => { setMode("login"); setError(""); }}
                    >
                        Login
                    </button>
                    <button
                        className={`auth-tab ${mode === "register" ? "active" : ""}`}
                        onClick={() => { setMode("register"); setError(""); }}
                    >
                        Register
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="auth-form">
                    {mode === "register" && (
                        <div className="auth-field">
                            <label>Name</label>
                            <input
                                type="text"
                                placeholder="Your full name"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                required
                            />
                        </div>
                    )}

                    <div className="auth-field">
                        <label>Email</label>
                        <input
                            type="email"
                            placeholder="you@example.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    </div>

                    <div className="auth-field">
                        <label>Password</label>
                        <input
                            type="password"
                            placeholder={mode === "register" ? "At least 6 characters" : "Your password"}
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            minLength={mode === "register" ? 6 : undefined}
                        />
                    </div>

                    {error && <div className="auth-error">{error}</div>}

                    <button type="submit" className="auth-submit" disabled={isLoading}>
                        {isLoading ? "Please wait..." : mode === "login" ? "Sign In" : "Create Account"}
                    </button>
                </form>
            </div>
        </div>
    );
}
