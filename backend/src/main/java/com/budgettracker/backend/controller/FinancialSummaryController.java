package com.budgettracker.backend.controller;

import com.budgettracker.backend.dto.FinancialSummaryDTO;
import com.budgettracker.backend.service.FinancialSummaryService;
import com.budgettracker.backend.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class FinancialSummaryController {

    private final FinancialSummaryService financialSummaryService;
    private final SubscriptionService     subscriptionService;

    @GetMapping
    public ResponseEntity<FinancialSummaryDTO> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean projected,
            @AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.requirePro(userDetails.getUsername(), "Financial Summary");
        return ResponseEntity.ok(
                financialSummaryService.getFinancialSummary(startDate, endDate, userDetails.getUsername(), projected)
        );
    }
}
