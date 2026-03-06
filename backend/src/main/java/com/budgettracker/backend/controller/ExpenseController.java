package com.budgettracker.backend.controller;

import com.budgettracker.backend.entity.Expense;
import com.budgettracker.backend.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    /**
     * GET /api/expenses
     * Optional params:
     *   ?month=2025-03          → filter by month (YYYY-MM)
     *   ?category=Food          → filter by category (searches all history)
     *   ?month=2025-03&category=Food → both
     */
    @GetMapping
    public List<Expense> getAllExpenses(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String category) {
        return expenseService.getAllExpensesByUser(userDetails.getUsername(), month, category);
    }

    @GetMapping("/paged")
    public Page<Expense> getAllExpensesPaged(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        return expenseService.getExpensesByUserPaged(userDetails.getUsername(), pageable);
    }

    @PostMapping
    public ResponseEntity<Expense> createExpense(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody Expense expense) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.createExpense(expense, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @Valid @RequestBody Expense expense) {
        return ResponseEntity.ok(expenseService.updateExpense(id, expense, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id) {
        expenseService.deleteExpense(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
