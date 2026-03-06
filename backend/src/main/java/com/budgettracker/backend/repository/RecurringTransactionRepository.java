package com.budgettracker.backend.repository;

import com.budgettracker.backend.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, String> {
    List<RecurringTransaction> findByUserId(String userId);
    List<RecurringTransaction> findByActiveTrue();
}
