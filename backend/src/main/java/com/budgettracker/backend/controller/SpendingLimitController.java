package com.budgettracker.backend.controller;

import com.budgettracker.backend.dto.SpendingLimitDTO;
import com.budgettracker.backend.entity.SpendingLimit;
import com.budgettracker.backend.service.SpendingLimitService;
import com.budgettracker.backend.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spending-limits")
@RequiredArgsConstructor
public class SpendingLimitController {

    private final SpendingLimitService spendingLimitService;
    private final SubscriptionService  subscriptionService;

    @GetMapping
    public ResponseEntity<List<SpendingLimitDTO>> getLimits(
            @AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.requirePro(userDetails.getUsername(), "Spending Limits");
        return ResponseEntity.ok(spendingLimitService.getLimitsWithProgress(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<SpendingLimitDTO> createLimit(
            @Valid @RequestBody SpendingLimit limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.requirePro(userDetails.getUsername(), "Spending Limits");
        return ResponseEntity.ok(spendingLimitService.createLimit(limit, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpendingLimitDTO> updateLimit(
            @PathVariable String id,
            @Valid @RequestBody SpendingLimit limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.requirePro(userDetails.getUsername(), "Spending Limits");
        return ResponseEntity.ok(spendingLimitService.updateLimit(id, limit, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLimit(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.requirePro(userDetails.getUsername(), "Spending Limits");
        spendingLimitService.deleteLimit(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
