package com.budgettracker.backend.service;

import com.budgettracker.backend.entity.*;
import com.budgettracker.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataService {

    static final String DEMO_FREE_EMAIL = "demo@budgettracker.com";
    static final String DEMO_PRO_EMAIL  = "demo-pro@budgettracker.com";
    private static final String DEMO_PASSWORD   = "demo1234";

    private final UserRepository                  userRepository;
    private final ExpenseRepository               expenseRepository;
    private final IncomeRepository                incomeRepository;
    private final SpendingLimitRepository         spendingLimitRepository;
    private final RecurringTransactionRepository  recurringRepo;
    private final CategoryRepository              categoryRepository;
    private final SubscriptionRepository          subscriptionRepository;
    private final PasswordEncoder                 passwordEncoder;

    // ── Public reset methods ──────────────────────────────────────────────────

    @Transactional
    public String resetFreeDemo() {
        User demo = getOrCreateUser(DEMO_FREE_EMAIL, "Demo User (Free)");
        clearUserData(demo);
        seedTransactions(demo);
        ensureSubscription(demo, Subscription.Plan.FREE);
        log.info("Free demo reset for: {}", DEMO_FREE_EMAIL);
        return DEMO_FREE_EMAIL;
    }

    @Transactional
    public String resetProDemo() {
        User demo = getOrCreateUser(DEMO_PRO_EMAIL, "Demo User (Pro)");
        clearUserData(demo);
        seedTransactions(demo);
        seedBudgetGoals(demo);
        seedRecurring(demo, LocalDate.now());
        ensureSubscription(demo, Subscription.Plan.PRO);
        log.info("Pro demo reset for: {}", DEMO_PRO_EMAIL);
        return DEMO_PRO_EMAIL;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User getOrCreateUser(String email, String name) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setName(name);
            u.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
            u.setRole(User.Role.USER);
            return userRepository.save(u);
        });
    }

    private void clearUserData(User user) {
        spendingLimitRepository.deleteAllInBatch(spendingLimitRepository.findByUserId(user.getId()));
        recurringRepo.deleteAllInBatch(recurringRepo.findByUserId(user.getId()));
        expenseRepository.deleteByUserId(user.getId());
        incomeRepository.deleteByUserId(user.getId());
        categoryRepository.deleteAllInBatch(categoryRepository.findByUserId(user.getId()));
    }

    private void ensureSubscription(User user, Subscription.Plan plan) {
        Subscription sub = subscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Subscription s = new Subscription();
                    s.setUser(user);
                    return s;
                });
        sub.setPlan(plan);
        sub.setStatus(Subscription.Status.ACTIVE);
        subscriptionRepository.save(sub);
    }

    // ── Seed data (shared between free and pro) ────────────────────────────────

    private void seedTransactions(User user) {
        seedIncomes(user, LocalDate.now());
        seedExpenses(user, LocalDate.now());
    }

    private void seedIncomes(User user, LocalDate today) {
        List<Income> incomes = new ArrayList<>();
        List<Object[]> rows = List.of(
                new Object[]{"Monthly Salary",    "Salary",      5500.00, 0},
                new Object[]{"Monthly Salary",    "Salary",      5500.00, 1},
                new Object[]{"Monthly Salary",    "Salary",      5500.00, 2},
                new Object[]{"Monthly Salary",    "Salary",      5500.00, 3},
                new Object[]{"Monthly Salary",    "Salary",      5500.00, 4},
                new Object[]{"Monthly Salary",    "Salary",      5500.00, 5},
                new Object[]{"Freelance Project", "Freelance",   1200.00, 0},
                new Object[]{"Freelance Project", "Freelance",    800.00, 2},
                new Object[]{"Stock Dividends",   "Investment",   350.00, 1},
                new Object[]{"Stock Dividends",   "Investment",   350.00, 4},
                new Object[]{"Tax Refund",        "Other",        920.00, 3}
        );
        for (Object[] row : rows) {
            Income i = new Income();
            i.setTitle((String) row[0]);
            i.setCategory((String) row[1]);
            i.setAmount((Double) row[2]);
            i.setDate(today.minusMonths((int) row[3]).withDayOfMonth(1));
            i.setUser(user);
            incomes.add(i);
        }
        incomeRepository.saveAll(incomes);
    }

    private void seedExpenses(User user, LocalDate today) {
        List<Expense> expenses = new ArrayList<>();
        List<Object[]> rows = List.of(
                new Object[]{"Woolworths Groceries",    "Food",          180.50, 5, 5},
                new Object[]{"Coles Weekly Shop",       "Food",          145.20, 4, 8},
                new Object[]{"Woolworths Groceries",    "Food",          162.80, 3, 6},
                new Object[]{"Restaurant Dinner",       "Food",           95.00, 3, 20},
                new Object[]{"Coles Weekly Shop",       "Food",          138.40, 2, 9},
                new Object[]{"Woolworths Groceries",    "Food",          175.60, 1, 7},
                new Object[]{"Cafe Lunches",            "Food",           72.00, 0, 15},
                new Object[]{"Woolworths Groceries",    "Food",          155.30, 0, 22},
                new Object[]{"Opal Card Top-up",        "Transport",      80.00, 5, 3},
                new Object[]{"Opal Card Top-up",        "Transport",      80.00, 4, 3},
                new Object[]{"Fuel - BP Station",       "Transport",      95.00, 3, 12},
                new Object[]{"Opal Card Top-up",        "Transport",      80.00, 2, 3},
                new Object[]{"Uber Rides",              "Transport",      45.00, 1, 18},
                new Object[]{"Opal Card Top-up",        "Transport",      80.00, 0, 3},
                new Object[]{"AGL Electricity",         "Bills",         220.00, 5, 14},
                new Object[]{"Sydney Water",            "Bills",          85.00, 4, 14},
                new Object[]{"Internet - Aussie BB",    "Bills",          79.00, 3, 14},
                new Object[]{"AGL Electricity",         "Bills",         195.00, 2, 14},
                new Object[]{"Mobile Phone - Telstra",  "Bills",          65.00, 1, 14},
                new Object[]{"Internet - Aussie BB",    "Bills",          79.00, 0, 14},
                new Object[]{"Netflix Subscription",    "Entertainment",  22.99, 5, 1},
                new Object[]{"Spotify Premium",         "Entertainment",  11.99, 4, 1},
                new Object[]{"Cinema Tickets",          "Entertainment",  45.00, 3, 16},
                new Object[]{"Netflix Subscription",    "Entertainment",  22.99, 2, 1},
                new Object[]{"Gym Membership",          "Entertainment",  55.00, 1, 1},
                new Object[]{"Netflix Subscription",    "Entertainment",  22.99, 0, 1},
                new Object[]{"Zara Clothing",           "Shopping",      189.00, 4, 11},
                new Object[]{"Amazon Purchase",         "Shopping",       67.50, 3, 19},
                new Object[]{"JB Hi-Fi Electronics",    "Shopping",      349.00, 2, 8},
                new Object[]{"Kmart Household",         "Shopping",       85.00, 0, 17}
        );
        for (Object[] row : rows) {
            Expense e = new Expense();
            e.setTitle((String) row[0]);
            e.setCategory((String) row[1]);
            e.setAmount((Double) row[2]);
            e.setDate(today.minusMonths((int) row[3]).withDayOfMonth((int) row[4]));
            e.setUser(user);
            expenses.add(e);
        }
        expenseRepository.saveAll(expenses);
    }

    private void seedBudgetGoals(User user) {
        List<SpendingLimit> goals = new ArrayList<>();
        List<Object[]> rows = List.of(
                new Object[]{"Food",          400.00, SpendingLimit.Period.MONTHLY},
                new Object[]{"Transport",     200.00, SpendingLimit.Period.MONTHLY},
                new Object[]{"Entertainment", 100.00, SpendingLimit.Period.MONTHLY},
                new Object[]{"Shopping",      300.00, SpendingLimit.Period.MONTHLY}
        );
        for (Object[] row : rows) {
            SpendingLimit g = new SpendingLimit();
            g.setUser(user);
            g.setCategory((String) row[0]);
            g.setLimitAmount((Double) row[1]);
            g.setPeriod((SpendingLimit.Period) row[2]);
            g.setStartDate(LocalDate.now());
            goals.add(g);
        }
        spendingLimitRepository.saveAll(goals);
    }

    private void seedRecurring(User user, LocalDate today) {
        List<RecurringTransaction> recurrings = new ArrayList<>();
        List<Object[]> rows = List.of(
                new Object[]{"Monthly Salary", "Salary",        5500.00, RecurringTransaction.Type.INCOME,  RecurringTransaction.Frequency.MONTHLY},
                new Object[]{"Netflix",        "Entertainment",   22.99, RecurringTransaction.Type.EXPENSE, RecurringTransaction.Frequency.MONTHLY},
                new Object[]{"Opal Card",      "Transport",       80.00, RecurringTransaction.Type.EXPENSE, RecurringTransaction.Frequency.MONTHLY},
                new Object[]{"Internet Bill",  "Bills",           79.00, RecurringTransaction.Type.EXPENSE, RecurringTransaction.Frequency.MONTHLY}
        );
        for (Object[] row : rows) {
            RecurringTransaction rt = new RecurringTransaction();
            rt.setUser(user);
            rt.setTitle((String) row[0]);
            rt.setCategory((String) row[1]);
            rt.setAmount((Double) row[2]);
            rt.setType((RecurringTransaction.Type) row[3]);
            rt.setFrequency((RecurringTransaction.Frequency) row[4]);
            rt.setStartDate(today.minusMonths(5));
            rt.setActive(true);
            recurrings.add(rt);
        }
        recurringRepo.saveAll(recurrings);
    }
}
