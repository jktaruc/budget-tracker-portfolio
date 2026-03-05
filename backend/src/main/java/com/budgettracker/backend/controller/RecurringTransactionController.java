package com.budgettracker.backend.controller;

import com.budgettracker.backend.entity.RecurringTransaction;
import com.budgettracker.backend.service.RecurringTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    @GetMapping
    public List<RecurringTransaction> getAll(@AuthenticationPrincipal UserDetails userDetails) {
        return recurringTransactionService.getByUser(userDetails.getUsername());
    }

    @PostMapping
    public ResponseEntity<RecurringTransaction> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RecurringTransaction rt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recurringTransactionService.create(rt, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransaction> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @Valid @RequestBody RecurringTransaction rt) {
        return ResponseEntity.ok(recurringTransactionService.update(id, rt, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id) {
        recurringTransactionService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
