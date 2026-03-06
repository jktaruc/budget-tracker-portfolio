import { lazy, Suspense, useState, useEffect } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import ErrorBoundary from "./components/ErrorBoundary";
import Navbar from "./components/NavBar";
import ProtectedRoute from "./components/ProtectedRoute";
import PaywallModal from "./components/PaywallModal";
import { AuthProvider } from "./context/AuthContext";
import { SubscriptionProvider } from "./context/SubscriptionContext";
import AuthPage from "./pages/AuthPage";
import DemoPage from "./pages/DemoPage";
import "./styles/App.css";

const Dashboard      = lazy(() => import("./pages/Dashboard"));
const Summary        = lazy(() => import("./pages/Summary"));
const SpendingLimits = lazy(() => import("./pages/SpendingLimitsPage"));
const Recurring      = lazy(() => import("./pages/RecurringPage"));
const BillingPage    = lazy(() => import("./pages/BillingPage"));

function PageLoader() {
    return (
        <div className="page-loader">
            <div className="loader-spinner"></div>
            <p>Loading...</p>
        </div>
    );
}

function LazyLoadError() {
    return (
        <div className="lazy-load-error">
            <h2>Failed to load page</h2>
            <p>Please check your internet connection and try again.</p>
            <button onClick={() => window.location.reload()}>Reload Page</button>
        </div>
    );
}

/** Listens for global "paywall" events fired by the Axios 402 interceptor. */
function PaywallListener() {
    const [paywallFeature, setPaywallFeature] = useState<string | null>(null);

    useEffect(() => {
        const handler = (e: Event) => {
            const feature = (e as CustomEvent<{ feature: string }>).detail.feature;
            setPaywallFeature(feature);
        };
        window.addEventListener("paywall", handler);
        return () => window.removeEventListener("paywall", handler);
    }, []);

    if (!paywallFeature) return null;
    return <PaywallModal feature={paywallFeature} onClose={() => setPaywallFeature(null)} />;
}

export default function App() {
    return (
        <AuthProvider>
            <SubscriptionProvider>
                <Router>
                    <ErrorBoundary>
                        {/* Global paywall modal — triggered by 402 responses anywhere in the app */}
                        <PaywallListener />

                        <Routes>
                            <Route path="/demo" element={<DemoPage />} />
                            <Route path="/login" element={<AuthPage />} />
                            <Route
                                path="/*"
                                element={
                                    <ProtectedRoute>
                                        <Navbar />
                                        <Suspense fallback={<PageLoader />}>
                                            <ErrorBoundary fallback={<LazyLoadError />}>
                                                <Routes>
                                                    <Route path="/"                element={<Dashboard />} />
                                                    <Route path="/summary"         element={<Summary />} />
                                                    <Route path="/spending-limits" element={<SpendingLimits />} />
                                                    <Route path="/recurring"       element={<Recurring />} />
                                                    <Route path="/billing"         element={<BillingPage />} />
                                                    <Route path="/goals"           element={<Navigate to="/spending-limits" replace />} />
                                                    <Route path="*"               element={<Navigate to="/" replace />} />
                                                </Routes>
                                            </ErrorBoundary>
                                        </Suspense>
                                    </ProtectedRoute>
                                }
                            />
                        </Routes>
                    </ErrorBoundary>
                </Router>
            </SubscriptionProvider>
        </AuthProvider>
    );
}
