package com.budgettracker.backend.repository;

import com.budgettracker.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, String> {

    // Returns global defaults (user IS NULL) + the user's own custom categories, sorted by name
    @Query("SELECT c FROM Category c WHERE c.type = :type AND (c.user IS NULL OR c.user.id = :userId) ORDER BY c.name")
    List<Category> findByTypeForUser(Category.Type type, String userId);

    List<Category> findByUserId(String userId);

    long countByUserIsNull();

    // Checks for a name collision in the full visible set (globals + user's own),
    // so we never show two identical entries in the dropdown
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND c.type = :type AND (c.user IS NULL OR c.user.id = :userId)")
    boolean existsByNameAndTypeForUser(String name, Category.Type type, String userId);
}
