import { useState } from "react";
import { useCategories } from "../hooks/useCategories";
import type { CategoryType } from "../types";

interface CategorySelectProps {
    type: CategoryType;
    value: string;
    onChange: (value: string) => void;
    className?: string;
}

export default function CategorySelect({ type, value, onChange, className }: CategorySelectProps) {
    const { categories, rawCategories, loading, addCategory, deleteCategory } = useCategories(type);
    const [showAdd, setShowAdd] = useState(false);
    const [newName, setNewName] = useState("");
    const [adding, setAdding] = useState(false);
    const [addError, setAddError] = useState<string | null>(null);
    const [deletingId, setDeletingId] = useState<string | null>(null);
    const [deleteError, setDeleteError] = useState<string | null>(null);

    const handleAdd = async () => {
        if (!newName.trim()) return;
        setAdding(true);
        setAddError(null);
        try {
            await addCategory(newName);
            onChange(newName.trim());
            setNewName("");
            setShowAdd(false);
        } catch (err: any) {
            setAddError(err?.response?.data?.message ?? "Failed to add category");
        } finally {
            setAdding(false);
        }
    };

    const handleDelete = async (id: string, name: string) => {
        setDeletingId(id);
        setDeleteError(null);
        try {
            await deleteCategory(id);
            // If the deleted category was selected, fall back to the first remaining one
            if (value === name) {
                const remaining = rawCategories.filter(c => c.id !== id);
                if (remaining.length > 0) onChange(remaining[0].name);
            }
        } catch (err: any) {
            setDeleteError(err?.response?.data?.message ?? "Failed to delete category");
        } finally {
            setDeletingId(null);
        }
    };

    if (loading) return <select className={className} disabled><option>Loading...</option></select>;

    const customCategories = rawCategories.filter(c => c.isCustom);

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
            <div style={{ display: "flex", gap: "6px" }}>
                <select
                    value={value}
                    onChange={e => onChange(e.target.value)}
                    className={className}
                    style={{ flex: 1 }}
                >
                    {categories.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
                <button
                    type="button"
                    onClick={() => { setShowAdd(s => !s); setAddError(null); setDeleteError(null); }}
                    title="Manage custom categories"
                    style={{
                        padding: "0 10px", border: "1px solid #d1d5db",
                        borderRadius: "6px", background: "white",
                        cursor: "pointer", fontSize: "16px", color: "#667eea"
                    }}
                >
                    {showAdd ? "✕" : "+"}
                </button>
            </div>

            {showAdd && (
                <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>

                    {/* Custom categories list with delete buttons */}
                    {customCategories.length > 0 && (
                        <div style={{
                            border: "1px solid #e5e7eb", borderRadius: "6px",
                            overflow: "hidden", background: "white"
                        }}>
                            {customCategories.map(cat => (
                                <div key={cat.id} style={{
                                    display: "flex", alignItems: "center", justifyContent: "space-between",
                                    padding: "6px 10px", borderBottom: "1px solid #f3f4f6",
                                    fontSize: "13px", color: "#374151"
                                }}>
                                    <span>{cat.name}</span>
                                    <button
                                        type="button"
                                        onClick={() => handleDelete(cat.id, cat.name)}
                                        disabled={deletingId === cat.id}
                                        title={`Delete "${cat.name}"`}
                                        style={{
                                            background: "none", border: "none", cursor: "pointer",
                                            color: "#ef4444", fontSize: "12px", padding: "2px 4px",
                                            borderRadius: "4px", lineHeight: 1,
                                            opacity: deletingId === cat.id ? 0.5 : 1
                                        }}
                                    >
                                        ✕
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* Delete error */}
                    {deleteError && (
                        <p style={{
                            margin: 0, fontSize: "12px", color: "#b91c1c",
                            background: "#fff5f5", border: "1px solid #fca5a5",
                            borderRadius: "6px", padding: "6px 10px"
                        }}>
                            {deleteError}
                        </p>
                    )}

                    {/* Add new category row */}
                    <div style={{ display: "flex", gap: "6px" }}>
                        <input
                            type="text"
                            placeholder="New category name"
                            value={newName}
                            onChange={e => { setNewName(e.target.value); setAddError(null); }}
                            onKeyDown={e => { if (e.key === "Enter") { e.preventDefault(); handleAdd(); } }}
                            maxLength={50}
                            autoFocus
                            style={{
                                flex: 1, padding: "6px 10px", border: "1px solid #d1d5db",
                                borderRadius: "6px", fontSize: "14px"
                            }}
                        />
                        <button
                            type="button"
                            onClick={handleAdd}
                            disabled={adding || !newName.trim()}
                            style={{
                                padding: "6px 12px", background: "#667eea", color: "white",
                                border: "none", borderRadius: "6px", cursor: "pointer", fontSize: "13px",
                                opacity: adding || !newName.trim() ? 0.6 : 1
                            }}
                        >
                            {adding ? "…" : "Add"}
                        </button>
                    </div>

                    {/* Add error */}
                    {addError && (
                        <p style={{
                            margin: 0, fontSize: "12px", color: "#b91c1c",
                            background: "#fff5f5", border: "1px solid #fca5a5",
                            borderRadius: "6px", padding: "6px 10px"
                        }}>
                            {addError}
                        </p>
                    )}
                </div>
            )}
        </div>
    );
}
