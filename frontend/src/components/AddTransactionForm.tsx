import { useState } from "react";
import api from "../api/api";
import CategorySelect from "./CategorySelect";
import { getTodayDate } from "../utils/date-utils";
import type { TransactionFormData, CategoryType } from "../types";

interface AddTransactionFormProps {
    /** API resource path, e.g. "/expenses" or "/incomes" */
    endpoint: string;
    categoryType: CategoryType;
    defaultCategory: string;
    submitLabel: string;
    formClassName: string;
    inputClassName: string;
    buttonClassName: string;
    onAdd?: () => void;
}

/**
 * Generic add-transaction form used by both AddExpense and AddIncome.
 */
export default function AddTransactionForm({
    endpoint,
    categoryType,
    defaultCategory,
    submitLabel,
    formClassName,
    inputClassName,
    buttonClassName,
    onAdd,
}: AddTransactionFormProps) {
    const [formData, setFormData] = useState<TransactionFormData>({
        title: "",
        category: defaultCategory,
        amount: "",
        date: getTodayDate(),
    });

    const set = (field: keyof TransactionFormData) =>
        (e: React.ChangeEvent<HTMLInputElement>) =>
            setFormData(prev => ({ ...prev, [field]: e.target.value }));

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        try {
            await api.post(endpoint, { ...formData, amount: parseFloat(formData.amount) });
            setFormData({ title: "", category: defaultCategory, amount: "", date: getTodayDate() });
            onAdd?.();
        } catch (err) {
            console.error(`Failed to add to ${endpoint}`, err);
        }
    };

    return (
        <form onSubmit={handleSubmit} className={formClassName}>
            <CategorySelect
                type={categoryType}
                value={formData.category}
                onChange={v => setFormData(prev => ({ ...prev, category: v }))}
                className={inputClassName}
            />
            <input
                type="text"
                name="title"
                placeholder="Description"
                value={formData.title}
                onChange={set("title")}
                required
                className={inputClassName}
            />
            <input
                type="number"
                name="amount"
                placeholder="Amount"
                value={formData.amount}
                onChange={set("amount")}
                step="0.01"
                min="0"
                required
                className={inputClassName}
            />
            <input
                type="date"
                name="date"
                value={formData.date}
                onChange={set("date")}
                required
                className={inputClassName}
            />
            <button type="submit" className={buttonClassName}>{submitLabel}</button>
        </form>
    );
}
