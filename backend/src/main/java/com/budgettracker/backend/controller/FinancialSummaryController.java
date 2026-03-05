package com.budgettracker.backend.controller;

import com.budgettracker.backend.dto.FinancialSummaryDTO;
import com.budgettracker.backend.service.FinancialSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class FinancialSummaryController {

    private final FinancialSummaryService financialSummaryService;

    /**
     * Returns the full financial summary for the authenticated user.
     *
     * @param startDate  range start (defaults to 6 months ago)
     * @param endDate    range end   (defaults to today)
     * @param projected  if true, overlays estimated future recurring transactions
     */
    @GetMapping("/financial")
    public ResponseEntity<FinancialSummaryDTO> getFinancialSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean projected,
            @AuthenticationPrincipal UserDetails userDetails) {

        LocalDate end   = endDate   != null ? endDate   : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusMonths(6);

        return ResponseEntity.ok(
                financialSummaryService.getFinancialSummary(start, end, userDetails.getUsername(), projected)
        );
    }
}
