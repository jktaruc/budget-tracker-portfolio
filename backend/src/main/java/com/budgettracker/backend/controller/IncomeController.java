package com.budgettracker.backend.controller;

import com.budgettracker.backend.entity.Income;
import com.budgettracker.backend.service.IncomeService;
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
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
public class IncomeController {

    private final IncomeService incomeService;

    /**
     * GET /api/incomes
     * Optional params:
     *   ?month=2025-03          → filter by month (YYYY-MM)
     *   ?category=Salary        → filter by category
     */
    @GetMapping
    public List<Income> getAllIncomes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String category) {
        return incomeService.getAllIncomesByUser(userDetails.getUsername(), month, category);
    }

    @GetMapping("/paged")
    public Page<Income> getAllIncomesPaged(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        return incomeService.getIncomesByUserPaged(userDetails.getUsername(), pageable);
    }

    @PostMapping
    public ResponseEntity<Income> createIncome(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody Income income) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incomeService.createIncome(income, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Income> updateIncome(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @Valid @RequestBody Income income) {
        return ResponseEntity.ok(incomeService.updateIncome(id, income, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncome(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id) {
        incomeService.deleteIncome(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
