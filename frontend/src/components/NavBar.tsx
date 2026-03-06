import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import api from "../api/api";
import ImportModal from "./ImportModal";
import PlanBadge from "./PlanBadge";
import { useSubscription } from "../context/SubscriptionContext";
import "../styles/NavBar.css";

const DEMO_FREE_EMAIL = "demo@budgettracker.com";
const DEMO_PRO_EMAIL  = "demo-pro@budgettracker.com";

export default function Navbar() {
    const location = useLocation();
    const { user, logout, storeSession } = useAuth();
    const { isPro } = useSubscription();
    const navigate = useNavigate();
    const isDemo = user?.email === DEMO_FREE_EMAIL || user?.email === DEMO_PRO_EMAIL;

    const [importOpen, setImportOpen] = useState(false);
    const [menuOpen,   setMenuOpen]   = useState(false);

    const closeMenu = () => setMenuOpen(false);

    const handleLogout = () => { closeMenu(); logout(); navigate("/demo"); };

    const handleResetDemo = async () => {
        closeMenu();
        if (!window.confirm("Reset demo data? All changes will be lost.")) return;
        const endpoint = user?.email === DEMO_PRO_EMAIL ? "/demo/reset-pro" : "/demo/reset";
        try {
            const { data } = await api.post(endpoint);
            storeSession(data);
            window.location.href = "/";
        } catch (err) {
            console.error("Failed to reset demo", err);
            alert("Failed to reset demo. Please try again.");
        }
    };

    const handleExport = async () => {
        closeMenu();
        try {
            const res = await api.get("/export/all/csv", { responseType: "blob" });
            const url = URL.createObjectURL(res.data);
            const a = document.createElement("a");
            a.href = url;
            a.download = "transactions.csv";
            a.click();
            URL.revokeObjectURL(url);
        } catch (err) {
            // 402 handled globally by Axios interceptor — no extra handling needed here
            console.error("Export failed", err);
        }
    };

    const navLink = (to: string, label: string) => (
        <Link
            to={to}
            className={location.pathname === to ? "nav-link active" : "nav-link"}
            onClick={closeMenu}
        >
            {label}
        </Link>
    );

    return (
        <>
            <nav className="navbar">
                <div className="navbar-container">
                    <div className="navbar-brand">
                        <h1>💰 Budget Tracker</h1>
                    </div>

                    <div className="navbar-links">
                        {navLink("/", "📊 Dashboard")}
                        {navLink("/summary", "📈 Summary")}
                        {navLink("/spending-limits", "💳 Spending Limits")}
                        {navLink("/recurring", "🔁 Recurring")}
                    </div>

                    <div className="navbar-user">
                        {isPro && (
                            <>
                                <button className="navbar-import" onClick={() => setImportOpen(true)} title="Import CSV">
                                    📤 Import
                                </button>
                                <button className="navbar-export" onClick={handleExport} title="Export CSV">
                                    📥 Export
                                </button>
                            </>
                        )}
                        <PlanBadge />
                        {isDemo && (
                            <button className="navbar-reset-demo" onClick={handleResetDemo}>
                                🔄 Reset Demo
                            </button>
                        )}
                        {user && <span className="navbar-name">👤 {user.name}</span>}
                        <button className="navbar-logout" onClick={handleLogout}>
                            {isDemo ? "Exit Demo" : "Logout"}
                        </button>
                    </div>

                    <button
                        className={`navbar-hamburger ${menuOpen ? "open" : ""}`}
                        onClick={() => setMenuOpen(o => !o)}
                        aria-label="Toggle menu"
                        aria-expanded={menuOpen}
                    >
                        <span /><span /><span />
                    </button>
                </div>

                <div className={`navbar-drawer ${menuOpen ? "open" : ""}`}>
                    {navLink("/", "📊 Dashboard")}
                    {navLink("/summary", "📈 Summary")}
                    {navLink("/spending-limits", "💳 Spending Limits")}
                    {navLink("/recurring", "🔁 Recurring")}

                    <div className="navbar-drawer-actions">
                        {isPro && (
                            <>
                                <button className="navbar-import" onClick={() => { closeMenu(); setImportOpen(true); }}>
                                    📤 Import CSV
                                </button>
                                <button className="navbar-export" onClick={handleExport}>
                                    📥 Export CSV
                                </button>
                            </>
                        )}
                        {isDemo && (
                            <button className="navbar-reset-demo" onClick={handleResetDemo}>
                                🔄 Reset Demo
                            </button>
                        )}
                        <button className="navbar-logout" onClick={handleLogout}>
                            {isDemo ? "Exit Demo" : "Logout"}
                        </button>
                    </div>
                </div>
            </nav>

            <ImportModal
                isOpen={importOpen}
                onClose={() => setImportOpen(false)}
                onImported={() => {}}
            />
        </>
    );
}
