package com.budgettracker.backend.service;

import com.budgettracker.backend.entity.Expense;
import com.budgettracker.backend.entity.Income;
import com.budgettracker.backend.entity.User;
import com.budgettracker.backend.exception.ResourceNotFoundException;
import com.budgettracker.backend.repository.ExpenseRepository;
import com.budgettracker.backend.repository.IncomeRepository;
import com.budgettracker.backend.repository.UserRepository;
import com.budgettracker.backend.service.base.UserScopedServiceBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Central service holding all CRUD logic for both expenses and income.
 *
 * <p>{@link ExpenseService} and {@link IncomeService} are thin delegators to this class,
 * kept only so {@code ExportController} and {@code FinancialSummaryService} don't need
 * to be changed when the service layer is refactored in the future.
 *
 * <p>All list results are returned in descending date order.
 */
@Slf4j
@Service
public class TransactionService extends UserScopedServiceBase {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository  incomeRepository;

    public TransactionService(
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository,
            UserRepository userRepository) {
        super(userRepository);
        this.expenseRepository = expenseRepository;
        this.incomeRepository  = incomeRepository;
    }

    // ── Expenses ──────────────────────────────────────────────────────────────

    /**
     * Returns expenses filtered by the supplied criteria:
     * <ul>
     *   <li>month + category → scoped to that month and category</li>
     *   <li>month only       → all categories for that month</li>
     *   <li>category only    → all time (year 2000 onward) to avoid an unbounded query</li>
     *   <li>neither          → all expenses for the user</li>
     * </ul>
     */
    public List<Expense> getExpenses(String userEmail, String month, String category) {
        String userId = getUser(userEmail).getId();
        if (month != null && category != null) {
            YearMonth ym = YearMonth.parse(month);
            return expenseRepository.findByUserIdAndCategoryIgnoreCaseContainingAndDateBetweenOrderByDateDesc(
                    userId, category, ym.atDay(1), ym.atEndOfMonth());
        }
        if (month != null) {
            YearMonth ym = YearMonth.parse(month);
            return expenseRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                    userId, ym.atDay(1), ym.atEndOfMonth());
        }
        if (category != null) {
            // No month filter — search across all records to find every matching entry
            return expenseRepository.findByUserIdAndCategoryIgnoreCaseContainingAndDateBetweenOrderByDateDesc(
                    userId, category, LocalDate.of(2000, 1, 1), LocalDate.now());
        }
        return expenseRepository.findByUserIdOrderByDateDesc(userId);
    }

    public Page<Expense> getExpensesPaged(String userEmail, Pageable pageable) {
        return expenseRepository.findByUserId(getUser(userEmail).getId(), pageable);
    }

    public List<Expense> getExpensesByDateRange(String userEmail, LocalDate start, LocalDate end) {
        return expenseRepository.findByUserIdAndDateBetween(getUser(userEmail).getId(), start, end);
    }

    public Expense createExpense(Expense expense, String userEmail) {
        User user = getUser(userEmail);
        expense.setUser(user);
        Expense saved = expenseRepository.save(expense);
        log.info("Created expense: {} for user: {}", saved.getId(), userEmail);
        return saved;
    }

    public Expense updateExpense(String id, Expense updated, String userEmail) {
        Expense existing = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        ensureOwnership(existing.getUser(), userEmail);
        existing.setTitle(updated.getTitle());
        existing.setCategory(updated.getCategory());
        existing.setAmount(updated.getAmount());
        existing.setDate(updated.getDate());
        log.info("Updated expense: {}", id);
        return expenseRepository.save(existing);
    }

    public void deleteExpense(String id, String userEmail) {
        Expense existing = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        ensureOwnership(existing.getUser(), userEmail);
        expenseRepository.deleteById(id);
        log.info("Deleted expense: {}", id);
    }

    // ── Income ────────────────────────────────────────────────────────────────

    /** Same filtering logic as {@link #getExpenses} — see that method for details. */
    public List<Income> getIncome(String userEmail, String month, String category) {
        String userId = getUser(userEmail).getId();
        if (month != null && category != null) {
            YearMonth ym = YearMonth.parse(month);
            return incomeRepository.findByUserIdAndCategoryIgnoreCaseContainingAndDateBetweenOrderByDateDesc(
                    userId, category, ym.atDay(1), ym.atEndOfMonth());
        }
        if (month != null) {
            YearMonth ym = YearMonth.parse(month);
            return incomeRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                    userId, ym.atDay(1), ym.atEndOfMonth());
        }
        if (category != null) {
            return incomeRepository.findByUserIdAndCategoryIgnoreCaseContainingAndDateBetweenOrderByDateDesc(
                    userId, category, LocalDate.of(2000, 1, 1), LocalDate.now());
        }
        return incomeRepository.findByUserIdOrderByDateDesc(userId);
    }

    public Page<Income> getIncomePaged(String userEmail, Pageable pageable) {
        return incomeRepository.findByUserId(getUser(userEmail).getId(), pageable);
    }

    public List<Income> getIncomeByDateRange(String userEmail, LocalDate start, LocalDate end) {
        return incomeRepository.findByUserIdAndDateBetween(getUser(userEmail).getId(), start, end);
    }

    public Income createIncome(Income income, String userEmail) {
        User user = getUser(userEmail);
        income.setUser(user);
        Income saved = incomeRepository.save(income);
        log.info("Created income: {} for user: {}", saved.getId(), userEmail);
        return saved;
    }

    public Income updateIncome(String id, Income updated, String userEmail) {
        Income existing = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income", id));
        ensureOwnership(existing.getUser(), userEmail);
        existing.setTitle(updated.getTitle());
        existing.setCategory(updated.getCategory());
        existing.setAmount(updated.getAmount());
        existing.setDate(updated.getDate());
        log.info("Updated income: {}", id);
        return incomeRepository.save(existing);
    }

    public void deleteIncome(String id, String userEmail) {
        Income existing = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income", id));
        ensureOwnership(existing.getUser(), userEmail);
        incomeRepository.deleteById(id);
        log.info("Deleted income: {}", id);
    }
}
