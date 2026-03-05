package com.budgettracker.backend.config;

import com.budgettracker.backend.entity.Category;
import com.budgettracker.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds global default categories on startup if none exist yet.
 * Safe to run on every deploy — skips if already seeded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    private record CategorySeed(String name, Category.Type type) {}

    private static final List<CategorySeed> DEFAULT_CATEGORIES = List.of(
            new CategorySeed("Food",          Category.Type.EXPENSE),
            new CategorySeed("Transport",     Category.Type.EXPENSE),
            new CategorySeed("Bills",         Category.Type.EXPENSE),
            new CategorySeed("Shopping",      Category.Type.EXPENSE),
            new CategorySeed("Entertainment", Category.Type.EXPENSE),
            new CategorySeed("Health",        Category.Type.EXPENSE),
            new CategorySeed("Other",         Category.Type.EXPENSE),
            new CategorySeed("Salary",        Category.Type.INCOME),
            new CategorySeed("Freelance",     Category.Type.INCOME),
            new CategorySeed("Investment",    Category.Type.INCOME),
            new CategorySeed("Business",      Category.Type.INCOME),
            new CategorySeed("Gift",          Category.Type.INCOME),
            new CategorySeed("Other",         Category.Type.INCOME)
    );

    @Override
    public void run(ApplicationArguments args) {
        // Use a count query to avoid loading the entire table into memory
        long existing = categoryRepository.countByUserIsNull();
        if (existing > 0) {
            log.info("Global categories already seeded ({} found), skipping", existing);
            return;
        }

        DEFAULT_CATEGORIES.forEach(seed -> {
            Category c = new Category();
            c.setName(seed.name());
            c.setType(seed.type());
            c.setUser(null); // null = global default
            categoryRepository.save(c);
        });

        log.info("Seeded {} global default categories", DEFAULT_CATEGORIES.size());
    }
}
