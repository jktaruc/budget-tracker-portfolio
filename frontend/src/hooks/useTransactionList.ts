import { useEffect, useState } from "react";
import api from "../api/api";
import type { TransactionEntry } from "../types";

interface UseTransactionListOptions {
    /** API resource path, e.g. "/expenses" or "/incomes" */
    endpoint: string;
    refresh: boolean;
    month?: string;
    category?: string;
}

interface UseTransactionListResult {
    items: TransactionEntry[];
    isLoading: boolean;
    error: string | null;
    deletingId: string | null;
    deleteItem: (item: TransactionEntry) => Promise<void>;
    updateItem: (updated: TransactionEntry) => void;
}

export function useTransactionList({
    endpoint,
    refresh,
    month,
    category,
}: UseTransactionListOptions): UseTransactionListResult {
    const [items, setItems] = useState<TransactionEntry[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [deletingId, setDeletingId] = useState<string | null>(null);

    useEffect(() => {
        const params: Record<string, string> = {};
        if (month) params.month = month;
        if (category) params.category = category;

        setIsLoading(true);
        setError(null);

        api.get<TransactionEntry[]>(endpoint, { params })
            .then(res => setItems(res.data))
            .catch(err => {
                console.error(`Failed to fetch ${endpoint}`, err);
                setError("Failed to load transactions. Please try again.");
            })
            .finally(() => setIsLoading(false));
    }, [endpoint, refresh, month, category]);

    const deleteItem = async (item: TransactionEntry) => {
        if (!window.confirm(`Delete "${item.title}"?`)) return;
        setDeletingId(item.id);
        try {
            await api.delete(`${endpoint}/${item.id}`);
            // Brief delay to allow fade-out animation
            setTimeout(() => {
                setItems(prev => prev.filter(i => i.id !== item.id));
                setDeletingId(null);
            }, 300);
        } catch (err) {
            console.error(`Failed to delete from ${endpoint}`, err);
            alert("Failed to delete. Please try again.");
            setDeletingId(null);
        }
    };

    const updateItem = (updated: TransactionEntry) => {
        setItems(prev => prev.map(i => i.id === updated.id ? updated : i));
    };

    return { items, isLoading, error, deletingId, deleteItem, updateItem };
}
