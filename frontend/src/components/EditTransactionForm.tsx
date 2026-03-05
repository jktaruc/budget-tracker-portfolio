import { useState } from "react";
import api from "../api/api";
import "../styles/EditForm.css";
import type { TransactionEntry, TransactionFormData, CategoryType } from "../types";
import CategorySelect from "./CategorySelect";

interface EditTransactionFormProps {
    transaction: TransactionEntry;
    /** API resource path, e.g. "/expenses" or "/incomes" */
    endpoint: string;
    /** Category type used to filter the category picker */
    categoryType: CategoryType;
    onSave: (updated: TransactionEntry) => void;
    onCancel: () => void;
}

/**
 * Generic edit form for both expenses and incomes.
 * Replaces the previously duplicated EditExpenseForm / EditIncomeForm.
 */
export default function EditTransactionForm({
    transaction,
    endpoint,
    categoryType,
    onSave,
    onCancel,
}: EditTransactionFormProps) {
    const [formData, setFormData] = useState<TransactionFormData>({
        title: transaction.title ?? "",
        category: transaction.category ?? "",
        amount: String(transaction.amount ?? ""),
        date: transaction.date ?? "",
    });
    const [loading, setLoading] = useState(false);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setLoading(true);
        try {
            const { data } = await api.put<TransactionEntry>(`${endpoint}/${transaction.id}`, {
                ...formData,
                amount: parseFloat(formData.amount),
            });
            onSave(data);
        } catch (err) {
            console.error(`Failed to update ${endpoint}`, err);
            alert("Failed to save changes. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="edit-form">
            <div className="form-group">
                <label htmlFor="category">Category</label>
                <CategorySelect
                    type={categoryType}
                    value={formData.category}
                    onChange={v => setFormData(prev => ({ ...prev, category: v }))}
                    className="form-input"
                />
            </div>

            <div className="form-group">
                <label htmlFor="title">Description</label>
                <input
                    id="title"
                    type="text"
                    name="title"
                    placeholder="Description"
                    value={formData.title}
                    onChange={handleChange}
                    className="form-input"
                    required
                />
            </div>

            <div className="form-group">
                <label htmlFor="amount">Amount</label>
                <input
                    id="amount"
                    type="number"
                    name="amount"
                    placeholder="Amount"
                    value={formData.amount}
                    onChange={handleChange}
                    step="0.01"
                    min="0"
                    className="form-input"
                    required
                />
            </div>

            <div className="form-group">
                <label htmlFor="date">Date</label>
                <input
                    id="date"
                    type="date"
                    name="date"
                    value={formData.date}
                    onChange={handleChange}
                    className="form-input"
                    required
                />
            </div>

            <div className="form-actions">
                <button type="button" onClick={onCancel} className="btn-cancel" disabled={loading}>
                    Cancel
                </button>
                <button type="submit" className="btn-save" disabled={loading}>
                    {loading ? "Saving..." : "Save Changes"}
                </button>
            </div>
        </form>
    );
}
