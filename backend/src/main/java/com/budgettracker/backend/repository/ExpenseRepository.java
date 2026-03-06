package com.budgettracker.backend.repository;

import com.budgettracker.backend.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, String> {

    List<Expense> findByUserIdOrderByDateDesc(String userId);

    Page<Expense> findByUserId(String userId, Pageable pageable);

    List<Expense> findByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);

    List<Expense> findByUserIdAndDateBetweenOrderByDateDesc(String userId, LocalDate startDate, LocalDate endDate);

    List<Expense> findByUserIdAndCategoryIgnoreCaseContainingAndDateBetweenOrderByDateDesc(
            String userId, String category, LocalDate startDate, LocalDate endDate);

    boolean existsByUserIdAndCategory(String userId, String category);

    void deleteByUserId(String userId);

    /**
     * Aggregates total spending for a specific category within a date range directly in SQL,
     * avoiding the need to load and filter all expenses in Java (used by SpendingLimitService).
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.user.id = :userId AND e.category = :category " +
           "AND e.date BETWEEN :startDate AND :endDate")
    double sumAmountByUserIdAndCategoryAndDateBetween(
            @Param("userId") String userId,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
