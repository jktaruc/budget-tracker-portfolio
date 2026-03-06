import { useMemo, useState } from "react";
import AddExpense from "../components/AddExpense";
import ExpenseList from "../components/ExpenseList";
import AddIncome from "../components/AddIncome";
import IncomeList from "../components/IncomeList";
import "../styles/Dashboard.css";
import "../styles/Layout.css";

type ActiveTab = "expenses" | "income";

function buildMonthOptions() {
    const options = [];
    const now = new Date();
    for (let i = 0; i < 12; i++) {
        const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
        const value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
        const label = d.toLocaleDateString("en-AU", { month: "long", year: "numeric" });
        options.push({ value, label });
    }
    return options;
}

export default function Dashboard() {
    const [refreshFlag, setRefreshFlag] = useState(false);
    const [activeTab, setActiveTab] = useState<ActiveTab>("expenses");
    const [selectedMonth, setSelectedMonth] = useState<string>("");
    const [selectedCategory, setSelectedCategory] = useState<string>("");

    const triggerRefresh = () => setRefreshFlag(prev => !prev);

    // Memoised so the 12-item array isn't recreated on every render
    const monthOptions = useMemo(() => buildMonthOptions(), []);

    return (
        <div className="page-container">
            <h1>Financial Dashboard</h1>

            <div className="tab-container">
                <button
                    onClick={() => setActiveTab("expenses")}
                    className={`tab ${activeTab === "expenses" ? "active" : ""}`}
                >
                    💸 Expenses
                </button>
                <button
                    onClick={() => setActiveTab("income")}
                    className={`tab ${activeTab === "income" ? "active" : ""}`}
                >
                    💰 Income
                </button>
            </div>

            <div className="filters-row">
                <div className="filter-group">
                    <label className="filter-label">Month</label>
                    <select
                        value={selectedMonth}
                        onChange={e => setSelectedMonth(e.target.value)}
                        className="filter-select"
                    >
                        <option value="">All time</option>
                        {monthOptions.map(o => (
                            <option key={o.value} value={o.value}>{o.label}</option>
                        ))}
                    </select>
                </div>
                <div className="filter-group">
                    <label className="filter-label">Category</label>
                    <input
                        type="text"
                        placeholder="Filter by category..."
                        value={selectedCategory}
                        onChange={e => setSelectedCategory(e.target.value)}
                        className="filter-input"
                    />
                </div>
                {(selectedMonth || selectedCategory) && (
                    <button
                        onClick={() => { setSelectedMonth(""); setSelectedCategory(""); }}
                        className="filter-clear-btn"
                    >
                        ✕ Clear filters
                    </button>
                )}
            </div>

            {activeTab === "expenses" && (
                <div>
                    <h2>Track Your Expenses</h2>
                    <AddExpense onAdd={triggerRefresh} />
                    <ExpenseList
                        refresh={refreshFlag}
                        month={selectedMonth || undefined}
                        category={selectedCategory || undefined}
                    />
                </div>
            )}

            {activeTab === "income" && (
                <div>
                    <h2>Track Your Income</h2>
                    <AddIncome onAdd={triggerRefresh} />
                    <IncomeList
                        refresh={refreshFlag}
                        month={selectedMonth || undefined}
                        category={selectedCategory || undefined}
                    />
                </div>
            )}
        </div>
    );
}
