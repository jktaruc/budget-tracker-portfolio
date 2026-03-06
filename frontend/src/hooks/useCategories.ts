import { useState, useEffect, useCallback } from "react";
import api from "../api/api";
import type { Category, CategoryType } from "../types";

interface UseCategoriesResult {
    categories: string[];
    loading: boolean;
    addCategory: (name: string) => Promise<void>;
    deleteCategory: (id: string) => Promise<void>;
    rawCategories: Category[];
}

// Simple in-memory cache so we don't refetch on every component mount
const cache: Partial<Record<CategoryType, { data: Category[]; ts: number }>> = {};
const CACHE_TTL = 60_000; // 1 minute

export function useCategories(type: CategoryType): UseCategoriesResult {
    const [rawCategories, setRawCategories] = useState<Category[]>([]);
    const [loading, setLoading] = useState(true);

    const fetchCategories = useCallback(async (force = false) => {
        const cached = cache[type];
        if (!force && cached && Date.now() - cached.ts < CACHE_TTL) {
            setRawCategories(cached.data);
            setLoading(false);
            return;
        }
        try {
            const { data } = await api.get<Category[]>("/categories", { params: { type } });
            cache[type] = { data, ts: Date.now() };
            setRawCategories(data);
        } catch (err) {
            console.error("Failed to fetch categories", err);
        } finally {
            setLoading(false);
        }
    }, [type]);

    useEffect(() => { fetchCategories(); }, [fetchCategories]);

    const addCategory = useCallback(async (name: string) => {
        const trimmed = name.trim();
        if (!trimmed) return;
        await api.post("/categories", { name: trimmed, type });
        delete cache[type]; // properly remove cache entry so next fetch hits the API
        await fetchCategories(true);
    }, [type, fetchCategories]);

    const deleteCategory = useCallback(async (id: string) => {
        await api.delete(`/categories/${id}`);
        delete cache[type];
        await fetchCategories(true);
    }, [type, fetchCategories]);

    return {
        categories: rawCategories.map(c => c.name),
        rawCategories,
        loading,
        addCategory,
        deleteCategory,
    };
}