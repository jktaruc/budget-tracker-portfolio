package com.budgettracker.backend.service;

import com.budgettracker.backend.dto.SpendingLimitDTO;
import com.budgettracker.backend.entity.SpendingLimit;
import com.budgettracker.backend.entity.User;
import com.budgettracker.backend.exception.ResourceNotFoundException;
import com.budgettracker.backend.repository.ExpenseRepository;
import com.budgettracker.backend.repository.SpendingLimitRepository;
import com.budgettracker.backend.repository.UserRepository;
import com.budgettracker.backend.service.base.UserScopedServiceBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SpendingLimitService extends UserScopedServiceBase {

    private final SpendingLimitRepository spendingLimitRepository;
    private final ExpenseRepository expenseRepository;

    public SpendingLimitService(
            SpendingLimitRepository spendingLimitRepository,
            ExpenseRepository expenseRepository,
            UserRepository userRepository) {
        super(userRepository);
        this.spendingLimitRepository = spendingLimitRepository;
        this.expenseRepository = expenseRepository;
    }

    public List<SpendingLimitDTO> getLimitsWithProgress(String userEmail) {
        User user = getUser(userEmail);
        return spendingLimitRepository.findByUserId(user.getId()).stream()
                .map(limit -> enrichWithProgress(limit, user.getId()))
                .collect(Collectors.toList());
    }

    public SpendingLimitDTO createLimit(SpendingLimit limit, String userEmail) {
        User user = getUser(userEmail);
        limit.setUser(user);
        SpendingLimit saved = spendingLimitRepository.save(limit);
        log.info("Created spending limit for category: {} user: {}", saved.getCategory(), userEmail);
        return enrichWithProgress(saved, user.getId());
    }

    public SpendingLimitDTO updateLimit(String id, SpendingLimit updated, String userEmail) {
        SpendingLimit existing = spendingLimitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SpendingLimit", id));
        ensureOwnership(existing.getUser(), userEmail);
        existing.setCategory(updated.getCategory());
        existing.setLimitAmount(updated.getLimitAmount());
        existing.setPeriod(updated.getPeriod());
        existing.setStartDate(updated.getStartDate());
        SpendingLimit saved = spendingLimitRepository.save(existing);
        return enrichWithProgress(saved, existing.getUser().getId());
    }

    public void deleteLimit(String id, String userEmail) {
        SpendingLimit existing = spendingLimitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SpendingLimit", id));
        ensureOwnership(existing.getUser(), userEmail);
        spendingLimitRepository.deleteById(id);
    }

    /**
     * Enriches a SpendingLimit entity with live spending progress for the current period.
     * Uses a single SQL aggregation (SUM) rather than loading all expenses into memory.
     */
    private SpendingLimitDTO enrichWithProgress(SpendingLimit limit, String userId) {
        LocalDate[] window = getPeriodWindow(limit);
        LocalDate windowStart = window[0];
        LocalDate windowEnd   = window[1];

        double spent = expenseRepository.sumAmountByUserIdAndCategoryAndDateBetween(
                userId, limit.getCategory(), windowStart, windowEnd);

        double remaining = limit.getLimitAmount() - spent;
        double pct = limit.getLimitAmount() > 0 ? (spent / limit.getLimitAmount() * 100) : 0;

        return new SpendingLimitDTO(
                limit.getId(), limit.getCategory(), limit.getLimitAmount(),
                limit.getPeriod(), limit.getStartDate(),
                spent, remaining, pct, spent > limit.getLimitAmount(),
                windowStart, windowEnd
        );
    }

    /**
     * Resolves the start and end of the current active period window for a spending limit.
     *
     * <p><b>Anchor-based (startDate set):</b> the window is fixed-width and rolls forward
     * from the anchor date in period-sized steps until today falls inside the window.
     * Example: MONTHLY + startDate=Mar 15 → Mar 15–Apr 14 → Apr 15–May 14, etc.
     * This lets users who are paid on the 15th track spending against their actual pay cycle
     * rather than the calendar month.
     *
     * <p><b>Calendar fallback (startDate null):</b> aligns to the current calendar period
     * (start of week, start of month, start of year).
     */
    private LocalDate[] getPeriodWindow(SpendingLimit limit) {
        LocalDate today  = LocalDate.now();
        LocalDate anchor = limit.getStartDate();

        if (anchor == null) {
            return switch (limit.getPeriod()) {
                case WEEKLY    -> new LocalDate[]{today.minusDays(today.getDayOfWeek().getValue() - 1), today};
                case BI_WEEKLY -> new LocalDate[]{today.minusDays(today.getDayOfWeek().getValue() - 1), today.plusDays(7 - today.getDayOfWeek().getValue() + 7)};
                case MONTHLY   -> new LocalDate[]{today.withDayOfMonth(1), today};
                case YEARLY    -> new LocalDate[]{today.withDayOfYear(1), today};
            };
        }

        // Roll anchor forward in period-sized steps until today is inside the window
        LocalDate windowStart = anchor;
        LocalDate windowEnd;

        while (true) {
            windowEnd = switch (limit.getPeriod()) {
                case WEEKLY    -> windowStart.plusWeeks(1).minusDays(1);
                case BI_WEEKLY -> windowStart.plusWeeks(2).minusDays(1);
                case MONTHLY   -> windowStart.plusMonths(1).minusDays(1);
                case YEARLY    -> windowStart.plusYears(1).minusDays(1);
            };

            if (!today.isAfter(windowEnd)) break;
            windowStart = windowEnd.plusDays(1);
        }

        return new LocalDate[]{windowStart, windowEnd};
    }
}
