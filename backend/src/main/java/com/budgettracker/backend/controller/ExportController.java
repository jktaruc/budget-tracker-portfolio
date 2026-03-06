package com.budgettracker.backend.controller;

import com.budgettracker.backend.entity.Expense;
import com.budgettracker.backend.entity.Income;
import com.budgettracker.backend.service.SubscriptionService;
import com.budgettracker.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final TransactionService  transactionService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/all/csv")
    public ResponseEntity<byte[]> exportAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.requirePro(userDetails.getUsername(), "CSV Export");
        String email = userDetails.getUsername();
        String csv = buildCsv(
                transactionService.getExpenses(email, null, null),
                transactionService.getIncome(email, null, null)
        );
        return csvResponse(csv, "transactions-all.csv");
    }

    @GetMapping("/expenses/csv")
    public ResponseEntity<byte[]> exportExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.requirePro(userDetails.getUsername(), "CSV Export");
        String email = userDetails.getUsername();
        List<Expense> expenses = (start != null && end != null)
                ? transactionService.getExpensesByDateRange(email, start, end)
                : transactionService.getExpenses(email, null, null);
        String csv = buildExpenseCsv(expenses);
        return csvResponse(csv, "expenses.csv");
    }

    @GetMapping("/income/csv")
    public ResponseEntity<byte[]> exportIncome(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.requirePro(userDetails.getUsername(), "CSV Export");
        String email = userDetails.getUsername();
        List<Income> incomes = (start != null && end != null)
                ? transactionService.getIncomeByDateRange(email, start, end)
                : transactionService.getIncome(email, null, null);
        String csv = buildIncomeCsv(incomes);
        return csvResponse(csv, "income.csv");
    }

    private String buildCsv(List<Expense> expenses, List<Income> incomes) {
        StringBuilder sb = new StringBuilder("Date,Title,Category,Amount,Type\n");
        for (Expense e : expenses)
            sb.append(row(e.getDate(), e.getTitle(), e.getCategory(), e.getAmount(), "EXPENSE"));
        for (Income i : incomes)
            sb.append(row(i.getDate(), i.getTitle(), i.getCategory(), i.getAmount(), "INCOME"));
        return sb.toString();
    }

    private String buildExpenseCsv(List<Expense> expenses) {
        StringBuilder sb = new StringBuilder("Date,Title,Category,Amount,Type\n");
        for (Expense e : expenses)
            sb.append(row(e.getDate(), e.getTitle(), e.getCategory(), e.getAmount(), "EXPENSE"));
        return sb.toString();
    }

    private String buildIncomeCsv(List<Income> incomes) {
        StringBuilder sb = new StringBuilder("Date,Title,Category,Amount,Type\n");
        for (Income i : incomes)
            sb.append(row(i.getDate(), i.getTitle(), i.getCategory(), i.getAmount(), "INCOME"));
        return sb.toString();
    }

    private String row(Object date, String title, String category, double amount, String type) {
        return String.format("%s,%s,%s,%.2f,%s\n", date, quote(title), quote(category), amount, type);
    }

    private String quote(String value) {
        if (value == null) return "";
        return value.contains(",") ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }
}
