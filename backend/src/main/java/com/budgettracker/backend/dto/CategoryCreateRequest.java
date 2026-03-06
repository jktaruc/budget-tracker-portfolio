package com.budgettracker.backend.dto;

import com.budgettracker.backend.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/categories.
 * Using a dedicated DTO (rather than the Category entity) ensures the client
 * can never supply a user field and accidentally create a globally-visible category.
 */
public record CategoryCreateRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 50)
        String name,

        @NotNull(message = "Category type is required")
        Category.Type type
) {}