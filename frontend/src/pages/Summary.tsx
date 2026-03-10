import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import api from "../api/api";
import FinancialStatsCards from "../components/summary/FinancialStatsCards";
import IncomeVsExpenseChart from "../components/summary/IncomeVsExpenseChart";
import CategoryBreakdown from "../components/summary/CategoryBreakdown";
import RecentTransactions from "../components/summary/RecentTransactions";
import DateRangePicker from "../components/summary/DateRangePicker";
import type { DateRange } from "../components/summary/DateRangePicker";
import { useSubscription } from "../context/SubscriptionContext";
import "../styles/Summary.css";
import "../styles/Layout.css";
import type { FinancialSummary, MonthlyData, Transaction, CategorySummary } from "../types";
import { getTodayDate, getDateMonthsAgo } from "../utils/date-utils";

interface FullSummaryResponse extends FinancialSummary {
    monthlyTrend: MonthlyData[];
    expensesByCategory: CategorySummary;
    incomeByCategory: CategorySummary;
    recentTransactions: Transaction[];
}

export default function Summary() {
    const { isPro } = useSubscription();

    const [summary, setSummary] = useState<FullSummaryResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // projected is always false for FREE users — the toggle is hidden from them
    const [projected, setProjected] = useState(false);

    const [dateRange, setDateRange] = useState<DateRange>({
        startDate: getDateMonthsAgo(6),
        endDate: getTodayDate(),
    });

    useEffect(() => {
        const fetchSummary = async () => {
            try {
                setLoading(true);
                // FREE users never send projected=true — the param is omitted entirely
                const params: Record<string, string | boolean> = {
                    startDate: dateRange.startDate,
                    endDate: dateRange.endDate,
                };
                if (isPro && projected) {
                    params.projected = true;
                }
                const { data } = await api.get<FullSummaryResponse>("/summary", { params });
                setSummary(data);
                setError(null);
            } catch (err: any) {
                setError("Failed to load financial summary");
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchSummary();
    }, [dateRange, projected, isPro]);

    // Reset projected when user loses PRO (e.g. subscription cancelled)
    useEffect(() => {
        if (!isPro) setProjected(false);
    }, [isPro]);

    if (loading) return <div className="loading">Loading financial summary...</div>;
    if (error)   return <div className="error">{error}</div>;
    if (!summary) return <div className="no-data">No data available</div>;

    return (
        <div className="page-container">
            <div className="summary-page">
                <div className="summary-header">
                    <h1>Financial Summary</h1>
                    <div className="summary-controls">
                        <DateRangePicker dateRange={dateRange} onChange={setDateRange} />

                        {/* Projected toggle is only shown to PRO users — it depends on recurring transactions */}
                        {isPro && (
                            <div className={`projected-toggle ${projected ? "active" : ""}`}>
                                <span className="projected-toggle-label">
                                    {projected ? "📈 Projected" : "📊 Actual"}
                                </span>
                                <button
                                    className={`projected-toggle-btn ${projected ? "active" : ""}`}
                                    onClick={() => setProjected(p => !p)}
                                    title={projected
                                        ? "Showing actual + projected recurring transactions. Click to show actual only."
                                        : "Click to overlay projected recurring transactions"}
                                >
                                    <span className={`projected-toggle-knob ${projected ? "active" : ""}`} />
                                </button>
                            </div>
                        )}

                        {/* Subtle PRO upsell for FREE users — not a hard block */}
                        {!isPro && (
                            <Link to="/billing" className="projected-upsell">
                                📈 <span>Projected cash flow</span>
                                <span className="pro-badge">PRO</span>
                            </Link>
                        )}
                    </div>
                </div>

                {projected && (
                    <div className="projected-banner">
                        📈 <strong>Projected mode:</strong> Figures include actual transactions plus estimated
                        future occurrences of your active recurring transactions within the selected date range.
                        Items marked "(Projected)" are estimates only.
                    </div>
                )}

                <FinancialStatsCards summary={summary} />

                <div className="charts-grid">
                    <div className="chart-container">
                        <h2>
                            Income vs Expenses{" "}
                            {projected && <span className="projected-label">(incl. projected)</span>}
                        </h2>
                        <IncomeVsExpenseChart data={summary.monthlyTrend} />
                    </div>
                    <div className="chart-container">
                        <h2>Category Breakdown</h2>
                        <CategoryBreakdown expenses={summary.expensesByCategory} income={summary.incomeByCategory} />
                    </div>
                </div>

                <div className="expenses-lists">
                    <div className="list-container">
                        <h2>
                            Recent Transactions{" "}
                            {projected && <span className="projected-label">(incl. projected)</span>}
                        </h2>
                        <RecentTransactions transactions={summary.recentTransactions} />
                    </div>
                </div>
            </div>
        </div>
    );
}
