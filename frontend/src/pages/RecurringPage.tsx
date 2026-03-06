import { useState, useEffect, useCallback } from "react";
import api from "../api/api";
import CategorySelect from "../components/CategorySelect";
import { getTodayDate } from "../utils/date-utils";
import type { RecurringTransaction, RecurringFrequency } from "../types";
import "../styles/Recurring.css";

interface FormState {
    title: string;
    category: string;
    amount: string;
    type: "EXPENSE" | "INCOME";
    frequency: RecurringFrequency;
    startDate: string;
    endDate: string;
}

const FREQUENCIES: { value: RecurringFrequency; label: string }[] = [
    { value: "DAILY",     label: "Daily" },
    { value: "WEEKLY",    label: "Weekly" },
    { value: "BI_WEEKLY", label: "Bi-Weekly" },
    { value: "MONTHLY",   label: "Monthly" },
    { value: "YEARLY",    label: "Yearly" },
];

const emptyForm: FormState = {
    title: "", category: "Food", amount: "", type: "EXPENSE",
    frequency: "MONTHLY", startDate: getTodayDate(), endDate: "",
};

export default function RecurringPage() {
    const [items, setItems] = useState<RecurringTransaction[]>([]);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);
    const [form, setForm] = useState<FormState>(emptyForm);
    const [saving, setSaving] = useState(false);

    const fetchItems = useCallback(async () => {
        try {
            const { data } = await api.get<RecurringTransaction[]>("/recurring");
            setItems(data);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { fetchItems(); }, [fetchItems]);

    const handleTypeChange = (type: "EXPENSE" | "INCOME") => {
        setForm(p => ({ ...p, type, category: type === "EXPENSE" ? "Food" : "Salary" }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);
        try {
            await api.post("/recurring", {
                ...form,
                amount: parseFloat(form.amount),
                endDate: form.endDate || null,
            });
            setForm(emptyForm);
            setShowForm(false);
            fetchItems();
        } catch (err) {
            console.error(err);
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (id: string) => {
        if (!window.confirm("Delete this recurring transaction?")) return;
        await api.delete(`/recurring/${id}`);
        setItems(prev => prev.filter(i => i.id !== id));
    };

    const handleToggleActive = async (item: RecurringTransaction) => {
        await api.put(`/recurring/${item.id}`, { ...item, active: !item.active });
        fetchItems();
    };

    const freqLabel = (f: RecurringFrequency) =>
        FREQUENCIES.find(x => x.value === f)?.label ?? f;

    if (loading) return <div className="loading">Loading recurring transactions...</div>;

    return (
        <div className="page-container">
            <div className="recurring-header">
                <h1>🔁 Recurring Transactions</h1>
                <button className="recurring-add-btn" onClick={() => setShowForm(!showForm)}>
                    {showForm ? "Cancel" : "+ Add Recurring"}
                </button>
            </div>

            {showForm && (
                <form onSubmit={handleSubmit} className="recurring-form">
                    <h3>New Recurring Transaction</h3>
                    <div className="recurring-type-toggle">
                        <button type="button"
                            className={form.type === "EXPENSE" ? "active expense" : ""}
                            onClick={() => handleTypeChange("EXPENSE")}>💸 Expense</button>
                        <button type="button"
                            className={form.type === "INCOME" ? "active income" : ""}
                            onClick={() => handleTypeChange("INCOME")}>💰 Income</button>
                    </div>
                    <div className="recurring-form-grid">
                        <div className="recurring-field">
                            <label>Title</label>
                            <input required placeholder="e.g. Netflix" value={form.title}
                                onChange={e => setForm(p => ({ ...p, title: e.target.value }))} />
                        </div>
                        <div className="recurring-field">
                            <label>Category</label>
                            <CategorySelect
                                type={form.type}
                                value={form.category}
                                onChange={v => setForm(p => ({ ...p, category: v }))}
                            />
                        </div>
                        <div className="recurring-field">
                            <label>Amount ($)</label>
                            <input type="number" step="0.01" min="0.01" required
                                placeholder="e.g. 22.99" value={form.amount}
                                onChange={e => setForm(p => ({ ...p, amount: e.target.value }))} />
                        </div>
                        <div className="recurring-field">
                            <label>Frequency</label>
                            <select value={form.frequency}
                                onChange={e => setForm(p => ({ ...p, frequency: e.target.value as RecurringFrequency }))}>
                                {FREQUENCIES.map(f => <option key={f.value} value={f.value}>{f.label}</option>)}
                            </select>
                        </div>
                        <div className="recurring-field">
                            <label>Start Date</label>
                            <input type="date" required value={form.startDate}
                                onChange={e => setForm(p => ({ ...p, startDate: e.target.value }))} />
                        </div>
                        <div className="recurring-field">
                            <label>End Date (optional)</label>
                            <input type="date" value={form.endDate}
                                onChange={e => setForm(p => ({ ...p, endDate: e.target.value }))} />
                        </div>
                    </div>
                    <button type="submit" className="recurring-save-btn" disabled={saving}>
                        {saving ? "Saving..." : "Save Recurring Transaction"}
                    </button>
                </form>
            )}

            {items.length === 0 && !showForm && (
                <div className="recurring-empty">
                    <p>No recurring transactions yet.</p>
                </div>
            )}

            <div className="recurring-list">
                {items.map(item => (
                    <div key={item.id} className={`recurring-item ${!item.active ? "inactive" : ""}`}>
                        <div className={`recurring-type-badge ${item.type.toLowerCase()}`}>
                            {item.type === "EXPENSE" ? "💸" : "💰"}
                        </div>
                        <div className="recurring-info">
                            <h3>{item.title}</h3>
                            <span className="recurring-meta">{item.category} · {freqLabel(item.frequency)}</span>
                            {item.lastProcessed && (
                                <span className="recurring-last">Last: {item.lastProcessed}</span>
                            )}
                        </div>
                        <div className="recurring-amount">
                            <span className={item.type === "INCOME" ? "income-amount" : "expense-amount"}>
                                {item.type === "INCOME" ? "+" : "-"}${item.amount.toFixed(2)}
                            </span>
                            <span className="recurring-freq">{freqLabel(item.frequency).toLowerCase()}</span>
                        </div>
                        <div className="recurring-actions">
                            <button
                                className={`recurring-toggle ${item.active ? "active" : "paused"}`}
                                onClick={() => handleToggleActive(item)}
                                title={item.active ? "Pause" : "Resume"}
                            >
                                {item.active ? "⏸" : "▶️"}
                            </button>
                            <button className="recurring-delete" onClick={() => handleDelete(item.id)}>✕</button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
