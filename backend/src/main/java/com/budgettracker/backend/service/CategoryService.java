package com.budgettracker.backend.service;

import com.budgettracker.backend.dto.CategoryCreateRequest;
import com.budgettracker.backend.entity.Category;
import com.budgettracker.backend.entity.User;
import com.budgettracker.backend.exception.ResourceNotFoundException;
import com.budgettracker.backend.repository.CategoryRepository;
import com.budgettracker.backend.repository.ExpenseRepository;
import com.budgettracker.backend.repository.IncomeRepository;
import com.budgettracker.backend.repository.UserRepository;
import com.budgettracker.backend.service.base.UserScopedServiceBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Manages the two-tier category system:
 * <ul>
 *   <li><b>Global defaults</b> — seeded by {@link com.budgettracker.backend.config.DataSeeder}
 *       at startup; user field is null; visible to everyone; cannot be deleted.</li>
 *   <li><b>Custom categories</b> — created per-user; user field is non-null;
 *       can only be deleted by the owning user and only when no transactions reference them.</li>
 * </ul>
 */
@Slf4j
@Service
public class CategoryService extends UserScopedServiceBase {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository   expenseRepository;
    private final IncomeRepository    incomeRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository,
            UserRepository userRepository) {
        super(userRepository);
        this.categoryRepository = categoryRepository;
        this.expenseRepository  = expenseRepository;
        this.incomeRepository   = incomeRepository;
    }

    /**
     * Returns all categories visible to the user for the given type:
     * global defaults (user IS NULL) plus the user's own custom categories,
     * ordered by name. This is the full set shown in every category dropdown.
     */
    public List<Category> getCategoriesForUser(Category.Type type, String userEmail) {
        User user = getUser(userEmail);
        return categoryRepository.findByTypeForUser(type, user.getId());
    }

    /**
     * Creates a user-scoped custom category.
     * Accepts a DTO (not the entity) so the client can never supply a user field
     * and accidentally create a globally-visible category.
     */
    public Category createCategory(CategoryCreateRequest request, String userEmail) {
        User user = getUser(userEmail);

        // Block duplicates in the full visible set (globals + user's own)
        boolean alreadyVisible = categoryRepository.existsByNameAndTypeForUser(
                request.name(), request.type(), user.getId());
        if (alreadyVisible) {
            throw new IllegalArgumentException(
                    "Category '" + request.name() + "' already exists");
        }

        Category category = new Category();
        category.setName(request.name().trim());
        category.setType(request.type());
        category.setUser(user);   // always user-scoped — never null

        Category saved = categoryRepository.save(category);
        log.info("Created category: {} ({}) for user: {}", saved.getName(), saved.getType(), userEmail);
        return saved;
    }

    public void deleteCategory(String id, String userEmail) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        if (existing.getUser() == null) {
            throw new AccessDeniedException("Cannot delete a global default category");
        }

        ensureOwnership(existing.getUser(), userEmail);

        User user = getUser(userEmail);
        String name = existing.getName();

        boolean usedInExpenses = expenseRepository.existsByUserIdAndCategory(user.getId(), name);
        boolean usedInIncome   = incomeRepository.existsByUserIdAndCategory(user.getId(), name);

        if (usedInExpenses || usedInIncome) {
            List<String> usedIn = new java.util.ArrayList<>();
            if (usedInExpenses) usedIn.add("expenses");
            if (usedInIncome)   usedIn.add("income");
            throw new IllegalArgumentException(
                    "Cannot delete '" + name + "' — it is used in existing " +
                            String.join(" and ", usedIn) + ". Remove those transactions first.");
        }

        categoryRepository.deleteById(id);
        log.info("Deleted category: {} for user: {}", name, userEmail);
    }
}