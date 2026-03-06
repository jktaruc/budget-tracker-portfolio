package com.budgettracker.backend.config;

import com.budgettracker.backend.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {

    private final RecurringTransactionService recurringTransactionService;

    @Scheduled(cron = "0 0 0 * * *")
    public void processRecurringTransactions() {
        log.info("Running recurring transaction scheduler...");
        recurringTransactionService.processAll();
    }
}
