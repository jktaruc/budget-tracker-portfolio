import "../../styles/ExpensesList.css";
import { formatCurrency, formatDisplayDate } from "../../utils/date-utils";
import type { Transaction } from "../../types";

interface RecentTransactionsProps {
    transactions: Transaction[];
}

export default function RecentTransactions({ transactions }: RecentTransactionsProps) {
    if (!transactions || transactions.length === 0) {
        return <p className="no-expenses">No transactions found</p>;
    }

    return (
        <div className="expenses-list">
            <ul>
                {transactions.map((tx) => {
                    const isIncome = tx.type === "INCOME";
                    return (
                        <li key={tx.id} className="expense-item">
                            <div
                                className="transaction-type-badge"
                                style={{ backgroundColor: isIncome ? "#4caf50" : "#f44336", color: "white" }}
                            >
                                {isIncome ? "↑" : "↓"}
                            </div>
                            <div className="expense-details">
                                <div className="expense-description">
                                    {tx.title}
                                    <span
                                        style={{
                                            marginLeft: "10px",
                                            fontSize: "0.85em",
                                            color: isIncome ? "#4caf50" : "#f44336",
                                            fontWeight: "bold",
                                        }}
                                    >
                                        {isIncome ? "+" : "-"}{formatCurrency(tx.amount)}
                                    </span>
                                </div>
                                <div className="expense-meta">
                                    <span className="category">{tx.category}</span>
                                    <span className="date">{formatDisplayDate(tx.date)}</span>
                                </div>
                            </div>
                        </li>
                    );
                })}
            </ul>
        </div>
    );
}
