import { useState, useEffect, useCallback } from "react";
import api from "../api/api";
import CategorySelect from "../components/CategorySelect";
import { useCategories } from "../hooks/useCategories";
import { getTodayDate, formatShortDate } from "../utils/date-utils";
import type { SpendingLimit, SpendingLimitPeriod } from "../types";
import "../styles/BudgetGoals.css";

const PERIODS: SpendingLimitPeriod[] = ["WEEKLY", "BI_WEEKLY", "MONTHLY", "YEARLY"];
const PERIOD_LABELS: Record<SpendingLimitPeriod, string> = {
    WEEKLY: "Weekly", BI_WEEKLY: "Bi-Weekly", MONTHLY: "Monthly", YEARLY: "Yearly"
};

interface FormState {
    category: string;
    limitAmount: string;
    period: SpendingLimitPeriod;
    startDate: string;
}

export default function SpendingLimitsPage() {
    const [limits, setLimits] = useState<SpendingLimit[]>([]);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);
    const [form, setForm] = useState<FormState>({
        category: "Food", limitAmount: "", period: "MONTHLY",
        startDate: getTodayDate(),
    });
    const [saving, setSaving] = useState(false);

    const { categories } = useCategories("EXPENSE");

    const fetchLimits = useCallback(async () => {
        try {
            const { data } = await api.get<SpendingLimit[]>("/spending-limits");
            setLimits(data);
        } catch (err) {
            console.error("Failed to fetch spending limits", err);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { fetchLimits(); }, [fetchLimits]);

    // Keep category in sync if categories load after form init
    useEffect(() => {
        if (categories.length > 0 && !categories.includes(form.category)) {
            setForm(p => ({ ...p, category: categories[0] }));
        }
    }, [categories, form.category]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);
        try {
            await api.post("/spending-limits", {
                category: form.category,
                limitAmount: parseFloat(form.limitAmount),
                period: form.period,
                startDate: form.startDate || null,
            });
            setForm({ category: categories[0] || "Food", limitAmount: "", period: "MONTHLY", startDate: getTodayDate() });
            setShowForm(false);
            fetchLimits();
        } catch (err) {
            console.error("Failed to create spending limit", err);
            alert("Failed to save. This category/period combination may already exist.");
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (id: string) => {
        if (!window.confirm("Delete this spending limit?")) return;
        await api.delete(`/spending-limits/${id}`);
        setLimits(prev => prev.filter(l => l.id !== id));
    };

    const getBarColor = (pct: number, exceeded: boolean) => {
        if (exceeded) return "#ef4444";
        if (pct >= 80) return "#f97316";
        return "#22c55e";
    };

    

    if (loading) return <div className="loading">Loading spending limits...</div>;

    return (
        <div className="page-container">
            <div className="goals-header">
                <h1>💳 Spending Limits</h1>
                <button className="goals-add-btn" onClick={() => setShowForm(!showForm)}>
                    {showForm ? "Cancel" : "+ Add Limit"}
                </button>
            </div>

            {showForm && (
                <form onSubmit={handleSubmit} className="goals-form">
                    <h3>Set a spending limit</h3>
                    <div className="goals-form-row">
                        <div className="goals-form-field">
                            <label>Category</label>
                            <CategorySelect
                                type="EXPENSE"
                                value={form.category}
                                onChange={v => setForm(p => ({ ...p, category: v }))}
                                className="goals-select"
                            />
                        </div>
                        <div className="goals-form-field">
                            <label>Period</label>
                            <select
                                value={form.period}
                                onChange={e => setForm(p => ({ ...p, period: e.target.value as SpendingLimitPeriod }))}
                            >
                                {PERIODS.map(p => <option key={p} value={p}>{PERIOD_LABELS[p]}</option>)}
                            </select>
                        </div>
                        <div className="goals-form-field">
                            <label>Start Date</label>
                            <input
                                type="date"
                                value={form.startDate}
                                onChange={e => setForm(p => ({ ...p, startDate: e.target.value }))}
                            />
                        </div>
                        <div className="goals-form-field">
                            <label>Limit ($)</label>
                            <input
                                type="number" step="0.01" min="1" required
                                placeholder="e.g. 400"
                                value={form.limitAmount}
                                onChange={e => setForm(p => ({ ...p, limitAmount: e.target.value }))}
                            />
                        </div>
                        <button type="submit" className="goals-save-btn" disabled={saving}>
                            {saving ? "Saving..." : "Save Limit"}
                        </button>
                    </div>
                </form>
            )}

            {limits.length === 0 && !showForm && (
                <div className="goals-empty">
                    <p>No spending limits yet. Add one to track your spending against a budget.</p>
                </div>
            )}

            <div className="goals-grid">
                {limits.map(limit => (
                    <div key={limit.id} className={`goal-card ${limit.exceeded ? "exceeded" : ""}`}>
                        <div className="goal-card-header">
                            <div>
                                <h3>{limit.category}</h3>
                                <span className="goal-period">{PERIOD_LABELS[limit.period]}</span>
                                {limit.windowStart && limit.windowEnd && (
                                    <span className="goal-window">
                                        {formatShortDate(limit.windowStart)} – {formatShortDate(limit.windowEnd)}
                                    </span>
                                )}
                            </div>
                            <div className="goal-amounts">
                                <span className="goal-spent">${limit.spent.toFixed(2)}</span>
                                <span className="goal-limit"> / ${limit.limitAmount.toFixed(2)}</span>
                            </div>
                        </div>

                        <div className="goal-bar-bg">
                            <div
                                className="goal-bar-fill"
                                style={{
                                    width: `${Math.min(limit.percentageUsed, 100)}%`,
                                    background: getBarColor(limit.percentageUsed, limit.exceeded)
                                }}
                            />
                        </div>

                        <div className="goal-card-footer">
                            <span className={`goal-status ${limit.exceeded ? "over" : limit.percentageUsed >= 80 ? "warning" : "ok"}`}>
                                {limit.exceeded
                                    ? `⚠️ Over by $${Math.abs(limit.remaining).toFixed(2)}`
                                    : limit.percentageUsed >= 80
                                    ? `⚠️ $${limit.remaining.toFixed(2)} remaining`
                                    : `✅ $${limit.remaining.toFixed(2)} remaining`}
                            </span>
                            <span className="goal-pct">{limit.percentageUsed.toFixed(0)}%</span>
                            <button className="goal-delete-btn" onClick={() => handleDelete(limit.id)}>✕</button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
