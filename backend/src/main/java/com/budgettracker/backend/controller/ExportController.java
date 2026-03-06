package com.budgettracker.backend.controller;

import com.budgettracker.backend.entity.Expense;
import com.budgettracker.backend.entity.Income;
import com.budgettracker.backend.service.ExpenseService;
import com.budgettracker.backend.service.IncomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExpenseService expenseService;
    private final IncomeService incomeService;

    @GetMapping("/expenses/csv")
    public ResponseEntity<byte[]> exportExpensesCsv(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Expense> expenses = expenseService.getExpensesByUserAndDateRange(
                userDetails.getUsername(), defaultFrom(startDate), defaultTo(endDate));

        StringBuilder csv = new StringBuilder("Date,Title,Category,Amount\n");
        for (Expense e : expenses) {
            csv.append(String.format("%s,%s,%s,%.2f\n",
                    e.getDate(), escapeCsv(e.getTitle()), escapeCsv(e.getCategory()), e.getAmount()));
        }
        return csvResponse(csv.toString(), "expenses.csv");
    }

    @GetMapping("/incomes/csv")
    public ResponseEntity<byte[]> exportIncomesCsv(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Income> incomes = incomeService.getIncomesByUserAndDateRange(
                userDetails.getUsername(), defaultFrom(startDate), defaultTo(endDate));

        StringBuilder csv = new StringBuilder("Date,Title,Category,Amount\n");
        for (Income i : incomes) {
            csv.append(String.format("%s,%s,%s,%.2f\n",
                    i.getDate(), escapeCsv(i.getTitle()), escapeCsv(i.getCategory()), i.getAmount()));
        }
        return csvResponse(csv.toString(), "incomes.csv");
    }

    @GetMapping("/all/csv")
    public ResponseEntity<byte[]> exportAllCsv(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate from = defaultFrom(startDate);
        LocalDate to   = defaultTo(endDate);

        List<Expense> expenses = expenseService.getExpensesByUserAndDateRange(userDetails.getUsername(), from, to);
        List<Income>  incomes  = incomeService.getIncomesByUserAndDateRange(userDetails.getUsername(), from, to);

        StringBuilder csv = new StringBuilder("Date,Title,Category,Amount,Type\n");
        for (Expense e : expenses) {
            csv.append(String.format("%s,%s,%s,%.2f,EXPENSE\n",
                    e.getDate(), escapeCsv(e.getTitle()), escapeCsv(e.getCategory()), e.getAmount()));
        }
        for (Income i : incomes) {
            csv.append(String.format("%s,%s,%s,%.2f,INCOME\n",
                    i.getDate(), escapeCsv(i.getTitle()), escapeCsv(i.getCategory()), i.getAmount()));
        }
        return csvResponse(csv.toString(), "transactions.csv");
    }

    private static ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(bytes.length)
                .body(bytes);
    }

    private static LocalDate defaultFrom(LocalDate date) {
        return date != null ? date : LocalDate.now().minusMonths(6);
    }

    private static LocalDate defaultTo(LocalDate date) {
        return date != null ? date : LocalDate.now();
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
