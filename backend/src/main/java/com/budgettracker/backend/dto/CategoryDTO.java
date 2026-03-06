package com.budgettracker.backend.dto;

import com.budgettracker.backend.entity.Category;

/**
 * API response shape for a category.
 * Replaces returning the raw Category entity, which would include the full User relation.
 * isCustom=true means this category belongs to the requesting user (can be deleted).
 * isCustom=false means it is a global default (cannot be deleted).
 */
public record CategoryDTO(String id, String name, String type, boolean isCustom) {

    public static CategoryDTO from(Category c) {
        return new CategoryDTO(
                c.getId(),
                c.getName(),
                c.getType().name(),
                c.getUser() != null
        );
    }
}
