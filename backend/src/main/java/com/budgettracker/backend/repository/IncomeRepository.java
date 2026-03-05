package com.budgettracker.backend.repository;

import com.budgettracker.backend.entity.Income;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, String> {

    List<Income> findByUserIdOrderByDateDesc(String userId);

    Page<Income> findByUserId(String userId, Pageable pageable);

    List<Income> findByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);

    List<Income> findByUserIdAndDateBetweenOrderByDateDesc(String userId, LocalDate startDate, LocalDate endDate);

    List<Income> findByUserIdAndCategoryIgnoreCaseContainingAndDateBetweenOrderByDateDesc(
            String userId, String category, LocalDate startDate, LocalDate endDate);

    boolean existsByUserIdAndCategory(String userId, String category);

    void deleteByUserId(String id);
}
