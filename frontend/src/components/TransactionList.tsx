import { useState } from "react";
import Modal from "./Modal";
import EditTransactionForm from "./EditTransactionForm";
import { formatDate } from "../utils/date-utils";
import { useTransactionList } from "../hooks/useTransactionList";
import type { TransactionEntry, CategoryType } from "../types";

interface TransactionListProps {
    endpoint: string;
    categoryType: CategoryType;
    refresh: boolean;
    month?: string;
    category?: string;
    emptyMessage: string;
    editModalTitle: string;
    /** Extra CSS class applied to each card (e.g. "income-card") */
    cardClassName?: string;
}

export default function TransactionList({
    endpoint,
    categoryType,
    refresh,
    month,
    category,
    emptyMessage,
    editModalTitle,
    cardClassName = "",
}: TransactionListProps) {
    const { items, isLoading, error, deletingId, deleteItem, updateItem } = useTransactionList({
        endpoint, refresh, month, category,
    });
    const [editing, setEditing] = useState<TransactionEntry | null>(null);

    const handleSave = (updated: TransactionEntry) => {
        updateItem(updated);
        setEditing(null);
    };

    if (isLoading) return <p className="loading-text">Loading...</p>;
    if (error)     return <p className="error-text">{error}</p>;

    return (
        <>
            {items.length === 0 && <p className="no-expenses">{emptyMessage}</p>}
            {items.length > 0 && (
                <div className="expense-grid">
                    {items.map(item => (
                        <div
                            key={item.id}
                            className={`expense-card ${cardClassName} ${deletingId === item.id ? "fade-out" : ""}`.trim()}
                        >
                            <h3>{item.title || "Untitled"}</h3>
                            <p><strong>Category:</strong> {item.category}</p>
                            <p><strong>Amount:</strong> ${item.amount.toFixed(2)}</p>
                            <p><strong>Date:</strong> {formatDate(item.date)}</p>
                            <div className="card-actions">
                                <button className="edit-btn" onClick={() => setEditing(item)} title="Edit">✏️</button>
                                <button className="expense-close-btn" onClick={() => deleteItem(item)} title="Delete">✕</button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
            <Modal isOpen={!!editing} onClose={() => setEditing(null)} title={editModalTitle}>
                {editing && (
                    <EditTransactionForm
                        transaction={editing}
                        endpoint={endpoint}
                        categoryType={categoryType}
                        onSave={handleSave}
                        onCancel={() => setEditing(null)}
                    />
                )}
            </Modal>
        </>
    );
}
