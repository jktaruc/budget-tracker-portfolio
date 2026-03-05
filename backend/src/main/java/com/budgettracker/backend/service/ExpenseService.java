package com.budgettracker.backend.service;

import com.budgettracker.backend.entity.Expense;
import com.budgettracker.backend.repository.UserRepository;
import com.budgettracker.backend.service.base.UserScopedServiceBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Thin delegator to TransactionService.
 * Kept for backward compatibility with ExportController and FinancialSummaryService.
 */
@Slf4j
@Service
public class ExpenseService extends UserScopedServiceBase {

    private final TransactionService transactionService;

    public ExpenseService(TransactionService transactionService, UserRepository userRepository) {
        super(userRepository);
        this.transactionService = transactionService;
    }

    public List<Expense> getAllExpensesByUser(String userEmail, String month, String category) {
        return transactionService.getExpenses(userEmail, month, category);
    }

    public Page<Expense> getExpensesByUserPaged(String userEmail, Pageable pageable) {
        return transactionService.getExpensesPaged(userEmail, pageable);
    }

    public List<Expense> getExpensesByUserAndDateRange(String userEmail, LocalDate start, LocalDate end) {
        return transactionService.getExpensesByDateRange(userEmail, start, end);
    }

    public Expense createExpense(Expense expense, String userEmail) {
        return transactionService.createExpense(expense, userEmail);
    }

    public Expense updateExpense(String id, Expense updated, String userEmail) {
        return transactionService.updateExpense(id, updated, userEmail);
    }

    public void deleteExpense(String id, String userEmail) {
        transactionService.deleteExpense(id, userEmail);
    }
}
