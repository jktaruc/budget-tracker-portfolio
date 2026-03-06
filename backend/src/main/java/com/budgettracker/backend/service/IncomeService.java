package com.budgettracker.backend.service;

import com.budgettracker.backend.entity.Income;
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
public class IncomeService extends UserScopedServiceBase {

    private final TransactionService transactionService;

    public IncomeService(TransactionService transactionService, UserRepository userRepository) {
        super(userRepository);
        this.transactionService = transactionService;
    }

    public List<Income> getAllIncomesByUser(String userEmail, String month, String category) {
        return transactionService.getIncome(userEmail, month, category);
    }

    public Page<Income> getIncomesByUserPaged(String userEmail, Pageable pageable) {
        return transactionService.getIncomePaged(userEmail, pageable);
    }

    public List<Income> getIncomesByUserAndDateRange(String userEmail, LocalDate start, LocalDate end) {
        return transactionService.getIncomeByDateRange(userEmail, start, end);
    }

    public Income createIncome(Income income, String userEmail) {
        return transactionService.createIncome(income, userEmail);
    }

    public Income updateIncome(String id, Income updated, String userEmail) {
        return transactionService.updateIncome(id, updated, userEmail);
    }

    public void deleteIncome(String id, String userEmail) {
        transactionService.deleteIncome(id, userEmail);
    }
}
