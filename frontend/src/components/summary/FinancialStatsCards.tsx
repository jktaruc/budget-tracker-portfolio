import "../../styles/StatsCards.css";
import { formatCurrency } from "../../utils/date-utils";
import type { FinancialSummary } from "../../types";

interface StatItem {
    label: string;
    value: string;
    icon: string;
    color: string;
    subtext: string;
}

interface FinancialStatsCardsProps {
    summary: FinancialSummary;
}

export default function FinancialStatsCards({ summary }: FinancialStatsCardsProps) {
    const stats: StatItem[] = [
        {
            label: "Total Income",
            value: formatCurrency(summary.totalIncome),
            icon: "💰",
            color: "green",
            subtext: `${summary.totalIncomeTransactions} transactions`,
        },
        {
            label: "Total Expenses",
            value: formatCurrency(summary.totalExpenses),
            icon: "💸",
            color: "red",
            subtext: `${summary.totalExpenseTransactions} transactions`,
        },
        {
            label: "Net Balance",
            value: formatCurrency(summary.netBalance),
            icon: summary.netBalance >= 0 ? "✅" : "⚠️",
            color: summary.netBalance >= 0 ? "blue" : "orange",
            subtext: summary.netBalance >= 0 ? "Surplus" : "Deficit",
        },
        {
            label: "Savings Rate",
            value: `${summary.savingsRate.toFixed(1)}%`,
            icon: "📊",
            color: "purple",
            subtext: "of income saved",
        },
    ];

    return (
        <div className="stats-cards">
            {stats.map((stat, index) => (
                <div key={index} className={`stat-card ${stat.color}`}>
                    <div className="stat-icon">{stat.icon}</div>
                    <div className="stat-content">
                        <div className="stat-label">{stat.label}</div>
                        <div className="stat-value">{stat.value}</div>
                        {stat.subtext && <div className="stat-subtext">{stat.subtext}</div>}
                    </div>
                </div>
            ))}
        </div>
    );
}
