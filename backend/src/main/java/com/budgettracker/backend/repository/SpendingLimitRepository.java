package com.budgettracker.backend.repository;

import com.budgettracker.backend.entity.SpendingLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpendingLimitRepository extends JpaRepository<SpendingLimit, String> {

    List<SpendingLimit> findByUserId(String userId);
}
