package com.budgettracker.backend.service;

import com.budgettracker.backend.entity.*;
import com.budgettracker.backend.exception.ResourceNotFoundException;
import com.budgettracker.backend.repository.*;
import com.budgettracker.backend.service.base.UserScopedServiceBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class RecurringTransactionService extends UserScopedServiceBase {

    private final RecurringTransactionRepository recurringRepo;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;

    public RecurringTransactionService(
            RecurringTransactionRepository recurringRepo,
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository,
            UserRepository userRepository) {
        super(userRepository);
        this.recurringRepo = recurringRepo;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
    }

    public List<RecurringTransaction> getByUser(String userEmail) {
        User user = getUser(userEmail);
        return recurringRepo.findByUserId(user.getId());
    }

    public RecurringTransaction create(RecurringTransaction rt, String userEmail) {
        User user = getUser(userEmail);
        rt.setUser(user);
        // startDate defaults to LocalDate.now() at the entity level if not supplied by the client
        RecurringTransaction saved = recurringRepo.save(rt);
        log.info("Created recurring transaction: {} for user: {}", rt.getTitle(), userEmail);
        return saved;
    }

    public RecurringTransaction update(String id, RecurringTransaction updated, String userEmail) {
        RecurringTransaction existing = recurringRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RecurringTransaction", id));
        ensureOwnership(existing.getUser(), userEmail);
        existing.setTitle(updated.getTitle());
        existing.setCategory(updated.getCategory());
        existing.setAmount(updated.getAmount());
        existing.setFrequency(updated.getFrequency());
        existing.setType(updated.getType());
        existing.setEndDate(updated.getEndDate());
        existing.setActive(updated.isActive());
        return recurringRepo.save(existing);
    }

    public void delete(String id, String userEmail) {
        RecurringTransaction existing = recurringRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RecurringTransaction", id));
        ensureOwnership(existing.getUser(), userEmail);
        recurringRepo.deleteById(id);
    }

    /**
     * Called daily by {@link com.budgettracker.backend.config.SchedulerConfig}.
     * Iterates all active recurring rules and creates actual expense/income records
     * for any that are due on or before today.
     *
     * <p><b>Past-end-date deactivation:</b> rules that have passed their end date
     * are flipped to {@code active=false} automatically, so they no longer appear
     * as pending in the UI.
     *
     * <p><b>Catch-up behaviour:</b> if the server was offline for multiple periods
     * (e.g. 3 missed MONTHLY transactions), only <em>one</em> catch-up entry is
     * created per rule on resume. The scheduler advances {@code lastProcessed} to
     * today, so subsequent runs within the same calendar day are idempotent. If full
     * catch-up is needed in the future, replace the single {@code createTransaction}
     * call with a loop over all missed dates.
     */
    public void processAll() {
        List<RecurringTransaction> actives = recurringRepo.findByActiveTrue();
        LocalDate today = LocalDate.now();
        int processed = 0;

        for (RecurringTransaction rt : actives) {
            if (rt.getEndDate() != null && today.isAfter(rt.getEndDate())) {
                rt.setActive(false);
                recurringRepo.save(rt);
                continue;
            }
            LocalDate nextDue = getNextDueDate(rt);
            if (!today.isBefore(nextDue)) {
                createTransaction(rt, today);
                rt.setLastProcessed(today);
                recurringRepo.save(rt);
                processed++;
            }
        }
        log.info("Recurring scheduler: processed {} transactions", processed);
    }

    /**
     * Computes the next due date for a recurring rule.
     * Uses {@code lastProcessed} as the base when available; falls back to
     * {@code startDate - 1 day} so the very first run fires on startDate itself.
     */
    private LocalDate getNextDueDate(RecurringTransaction rt) {
        LocalDate base = rt.getLastProcessed() != null
                ? rt.getLastProcessed()
                : rt.getStartDate().minusDays(1);
        return switch (rt.getFrequency()) {
            case DAILY     -> base.plusDays(1);
            case WEEKLY    -> base.plusWeeks(1);
            case BI_WEEKLY -> base.plusWeeks(2);
            case MONTHLY   -> base.plusMonths(1);
            case YEARLY    -> base.plusYears(1);
        };
    }

    private void createTransaction(RecurringTransaction rt, LocalDate date) {
        if (rt.getType() == RecurringTransaction.Type.EXPENSE) {
            Expense e = new Expense();
            e.setTitle(rt.getTitle() + " (Recurring)");
            e.setCategory(rt.getCategory());
            e.setAmount(rt.getAmount());
            e.setDate(date);
            e.setUser(rt.getUser());
            expenseRepository.save(e);
        } else {
            Income i = new Income();
            i.setTitle(rt.getTitle() + " (Recurring)");
            i.setCategory(rt.getCategory());
            i.setAmount(rt.getAmount());
            i.setDate(date);
            i.setUser(rt.getUser());
            incomeRepository.save(i);
        }
    }
}
