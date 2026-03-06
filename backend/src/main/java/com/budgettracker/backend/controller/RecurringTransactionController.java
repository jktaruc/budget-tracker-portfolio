package com.budgettracker.backend.controller;

import com.budgettracker.backend.entity.RecurringTransaction;
import com.budgettracker.backend.service.RecurringTransactionService;
import com.budgettracker.backend.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringService;
    private final SubscriptionService         subscriptionService;

    @GetMapping
    public ResponseEntity<List<RecurringTransaction>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.requirePro(userDetails.getUsername(), "Recurring Transactions");
        return ResponseEntity.ok(recurringService.getByUser(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<RecurringTransaction> create(
            @RequestBody RecurringTransaction rt,
            @AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.requirePro(userDetails.getUsername(), "Recurring Transactions");
        return ResponseEntity.ok(recurringService.create(rt, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransaction> update(
            @PathVariable String id,
            @RequestBody RecurringTransaction rt,
            @AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.requirePro(userDetails.getUsername(), "Recurring Transactions");
        return ResponseEntity.ok(recurringService.update(id, rt, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.requirePro(userDetails.getUsername(), "Recurring Transactions");
        recurringService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
