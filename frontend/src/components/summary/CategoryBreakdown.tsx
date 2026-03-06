import { useState } from "react";
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from "recharts";
import type { CategorySummary, ChartDataPoint } from "../../types";

interface CategoryBreakdownProps {
    expenses: CategorySummary;
    income: CategorySummary;
}

type ViewMode = "expenses" | "income";

const COLORS = [
    "#0088FE", "#00C49F", "#FFBB28", "#FF8042", "#8884D8",
    "#82CA9D", "#FFC658", "#FF6B9D", "#C77DFF", "#48CAE4",
];

const renderCustomLabel = ({ name, percent }: { name?: string; percent?: number }) =>
    `${name ?? ''} ${((percent ?? 0) * 100).toFixed(0)}%`

export default function CategoryBreakdown({ expenses, income }: CategoryBreakdownProps) {
    const [view, setView] = useState<ViewMode>("expenses");

    const expenseData: ChartDataPoint[] = Object.entries(expenses ?? {}).map(
        ([category, amount]) => ({ name: category, value: amount })
    );
    const incomeData: ChartDataPoint[] = Object.entries(income ?? {}).map(
        ([category, amount]) => ({ name: category, value: amount })
    );

    const currentData = view === "expenses" ? expenseData : incomeData;

    return (
        <div>
            <div style={styles.toggleContainer}>
                <button
                    onClick={() => setView("expenses")}
                    style={{ ...styles.toggleBtn, ...(view === "expenses" ? styles.activeToggle : {}) }}
                >
                    Expenses
                </button>
                <button
                    onClick={() => setView("income")}
                    style={{ ...styles.toggleBtn, ...(view === "income" ? styles.activeToggle : {}) }}
                >
                    Income
                </button>
            </div>

            {currentData.length === 0 ? (
                <div className="no-chart-data">No category data available</div>
            ) : (
                <ResponsiveContainer width="100%" height={350}>
                    <PieChart>
                        <Pie
                            data={currentData}
                            cx="50%"
                            cy="50%"
                            labelLine={false}
                            label={renderCustomLabel}
                            outerRadius={100}
                            dataKey="value"
                        >
                            {currentData.map((_entry, index) => (
                                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                            ))}
                        </Pie>
                        <Tooltip formatter={(value) => `$${Number(value).toFixed(2)}`} />
                        <Legend />
                    </PieChart>
                </ResponsiveContainer>
            )}
        </div>
    );
}

const styles: Record<string, React.CSSProperties> = {
    toggleContainer: { display: "flex", gap: "10px", marginBottom: "20px", justifyContent: "center" },
    toggleBtn: {
        padding: "8px 20px", border: "2px solid #ddd", background: "white",
        borderRadius: "6px", cursor: "pointer", fontSize: "14px", fontWeight: "500",
        transition: "all 0.3s ease",
    },
    activeToggle: { background: "#667eea", color: "white", borderColor: "#667eea" },
};
