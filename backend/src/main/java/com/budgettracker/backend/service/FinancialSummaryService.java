package com.budgettracker.backend.service;

import com.budgettracker.backend.dto.FinancialSummaryDTO;
import com.budgettracker.backend.dto.MonthlyFinancialDTO;
import com.budgettracker.backend.dto.TransactionItemDTO;
import com.budgettracker.backend.entity.*;
import com.budgettracker.backend.repository.*;
import com.budgettracker.backend.service.base.UserScopedServiceBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds the financial summary shown on the Summary page.
 *
 * <p>Supports two modes:
 * <ul>
 *   <li><b>Actual</b> — aggregates only transactions that have already been recorded.</li>
 *   <li><b>Projected</b> — overlays synthetic future entries from active recurring rules
 *       onto the actual data so users can see estimated income and spending for the
 *       selected date range without recording anything in the database.</li>
 * </ul>
 */
@Slf4j
@Service
public class FinancialSummaryService extends UserScopedServiceBase {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int TOP_ITEMS_LIMIT = 5;
    private static final int RECENT_TRANSACTIONS_LIMIT = 10;

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final RecurringTransactionRepository recurringRepo;

    public FinancialSummaryService(
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository,
            RecurringTransactionRepository recurringRepo,
            UserRepository userRepository) {
        super(userRepository);
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.recurringRepo = recurringRepo;
    }

    /**
     * @param projected if true, overlays projected recurring transactions on top of actual data.
     *                  Recurring transactions that are active and would fire within the date range
     *                  are projected as synthetic transactions (not saved to DB).
     */
    public FinancialSummaryDTO getFinancialSummary(
            LocalDate startDate, LocalDate endDate, String userEmail, boolean projected) {

        log.info("Generating {} summary from {} to {} for user: {}",
                projected ? "projected" : "actual", startDate, endDate, userEmail);

        User user = getUser(userEmail);

        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(user.getId(), startDate, endDate);
        List<Income>  incomes  = incomeRepository.findByUserIdAndDateBetween(user.getId(), startDate, endDate);

        // In projected mode, add synthetic transactions from recurring rules
        if (projected) {
            List<RecurringTransaction> actives = recurringRepo.findByUserId(user.getId())
                    .stream().filter(RecurringTransaction::isActive).collect(Collectors.toList());
            expenses = new ArrayList<>(expenses);
            incomes  = new ArrayList<>(incomes);
            projectRecurring(actives, startDate, endDate, expenses, incomes, user);
        }

        return buildSummary(expenses, incomes, startDate, endDate);
    }

    /**
     * Projects all future occurrences of active recurring transactions within the date range.
     * Only adds dates AFTER today (or after lastProcessed) to avoid double-counting actuals.
     */
    private void projectRecurring(
            List<RecurringTransaction> actives,
            LocalDate startDate, LocalDate endDate,
            List<Expense> expenses, List<Income> incomes,
            User user) {

        LocalDate today = LocalDate.now();

        for (RecurringTransaction rt : actives) {
            if (rt.getEndDate() != null && rt.getEndDate().isBefore(startDate)) continue;

            // Start projecting from the next due date after today
            LocalDate cursor = getNextDueDate(rt, today);

            while (!cursor.isAfter(endDate)) {
                if (!cursor.isBefore(startDate) && cursor.isAfter(today)) {
                    if (rt.getType() == RecurringTransaction.Type.EXPENSE) {
                        Expense projected = new Expense();
                        projected.setTitle(rt.getTitle() + " (Projected)");
                        projected.setCategory(rt.getCategory());
                        projected.setAmount(rt.getAmount());
                        projected.setDate(cursor);
                        projected.setUser(user);
                        expenses.add(projected);
                    } else {
                        Income projected = new Income();
                        projected.setTitle(rt.getTitle() + " (Projected)");
                        projected.setCategory(rt.getCategory());
                        projected.setAmount(rt.getAmount());
                        projected.setDate(cursor);
                        projected.setUser(user);
                        incomes.add(projected);
                    }
                }
                cursor = advanceByFrequency(cursor, rt.getFrequency());
            }
        }
    }

    private LocalDate getNextDueDate(RecurringTransaction rt, LocalDate after) {
        LocalDate base = rt.getLastProcessed() != null ? rt.getLastProcessed() : rt.getStartDate().minusDays(1);
        LocalDate next = advanceByFrequency(base, rt.getFrequency());
        while (!next.isAfter(after)) {
            next = advanceByFrequency(next, rt.getFrequency());
        }
        return next;
    }

    private LocalDate advanceByFrequency(LocalDate date, RecurringTransaction.Frequency freq) {
        return switch (freq) {
            case DAILY     -> date.plusDays(1);
            case WEEKLY    -> date.plusWeeks(1);
            case BI_WEEKLY -> date.plusWeeks(2);
            case MONTHLY   -> date.plusMonths(1);
            case YEARLY    -> date.plusYears(1);
        };
    }

    private FinancialSummaryDTO buildSummary(
            List<Expense> expenses, List<Income> incomes,
            LocalDate startDate, LocalDate endDate) {

        double totalExpenses = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double totalIncome   = incomes.stream().mapToDouble(Income::getAmount).sum();
        double netBalance    = totalIncome - totalExpenses;
        double savingsRate   = totalIncome > 0 ? ((totalIncome - totalExpenses) / totalIncome * 100) : 0.0;
        long months = ChronoUnit.MONTHS.between(startDate, endDate) + 1;

        FinancialSummaryDTO summary = new FinancialSummaryDTO();
        summary.setTotalExpenses(totalExpenses);
        summary.setTotalExpenseTransactions(expenses.size());
        summary.setTotalIncome(totalIncome);
        summary.setTotalIncomeTransactions(incomes.size());
        summary.setNetBalance(netBalance);
        summary.setSavingsRate(savingsRate);
        summary.setMonthlyAverageExpenses(months > 0 ? totalExpenses / months : 0.0);
        summary.setMonthlyAverageIncome(months > 0 ? totalIncome / months : 0.0);
        summary.setExpensesByCategory(groupExpensesByCategory(expenses));
        summary.setIncomeByCategory(groupIncomeByCategory(incomes));
        summary.setMonthlyTrend(calculateMonthlyTrend(expenses, incomes));
        summary.setTopExpenses(topExpenses(expenses));
        summary.setTopIncomes(topIncomes(incomes));
        summary.setRecentTransactions(recentTransactions(expenses, incomes));
        return summary;
    }

    private Map<String, Double> groupExpensesByCategory(List<Expense> expenses) {
        return expenses.stream().collect(
                Collectors.groupingBy(Expense::getCategory, Collectors.summingDouble(Expense::getAmount)));
    }

    private Map<String, Double> groupIncomeByCategory(List<Income> incomes) {
        return incomes.stream().collect(
                Collectors.groupingBy(Income::getCategory, Collectors.summingDouble(Income::getAmount)));
    }

    private List<TransactionItemDTO> topExpenses(List<Expense> expenses) {
        return expenses.stream()
                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                .limit(TOP_ITEMS_LIMIT)
                .map(e -> new TransactionItemDTO(e.getId(), e.getTitle(), e.getCategory(), e.getAmount(), e.getDate().toString(), "EXPENSE"))
                .collect(Collectors.toList());
    }

    private List<TransactionItemDTO> topIncomes(List<Income> incomes) {
        return incomes.stream()
                .sorted(Comparator.comparing(Income::getAmount).reversed())
                .limit(TOP_ITEMS_LIMIT)
                .map(i -> new TransactionItemDTO(i.getId(), i.getTitle(), i.getCategory(), i.getAmount(), i.getDate().toString(), "INCOME"))
                .collect(Collectors.toList());
    }

    private List<TransactionItemDTO> recentTransactions(List<Expense> expenses, List<Income> incomes) {
        return Stream.concat(
                        expenses.stream().map(e -> new TransactionItemDTO(e.getId(), e.getTitle(), e.getCategory(), e.getAmount(), e.getDate().toString(), "EXPENSE")),
                        incomes.stream().map(i -> new TransactionItemDTO(i.getId(), i.getTitle(), i.getCategory(), i.getAmount(), i.getDate().toString(), "INCOME"))
                )
                .sorted(Comparator.comparing(TransactionItemDTO::getDate).reversed())
                .limit(RECENT_TRANSACTIONS_LIMIT)
                .collect(Collectors.toList());
    }

    private List<MonthlyFinancialDTO> calculateMonthlyTrend(List<Expense> expenses, List<Income> incomes) {
        Map<String, List<Expense>> expByMonth = expenses.stream()
                .collect(Collectors.groupingBy(e -> e.getDate().format(MONTH_FORMATTER)));
        Map<String, List<Income>> incByMonth = incomes.stream()
                .collect(Collectors.groupingBy(i -> i.getDate().format(MONTH_FORMATTER)));

        Set<String> allMonths = new TreeSet<>();
        allMonths.addAll(expByMonth.keySet());
        allMonths.addAll(incByMonth.keySet());

        return allMonths.stream().map(month -> {
            double me = expByMonth.getOrDefault(month, List.of()).stream().mapToDouble(Expense::getAmount).sum();
            double mi = incByMonth.getOrDefault(month, List.of()).stream().mapToDouble(Income::getAmount).sum();
            return new MonthlyFinancialDTO(month, me, mi, mi - me,
                    expByMonth.getOrDefault(month, List.of()).size(),
                    incByMonth.getOrDefault(month, List.of()).size());
        }).sorted(Comparator.comparing(MonthlyFinancialDTO::getMonth)).collect(Collectors.toList());
    }
}
