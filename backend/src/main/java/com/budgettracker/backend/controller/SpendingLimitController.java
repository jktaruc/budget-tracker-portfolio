package com.budgettracker.backend.controller;

import com.budgettracker.backend.dto.SpendingLimitDTO;
import com.budgettracker.backend.entity.SpendingLimit;
import com.budgettracker.backend.service.SpendingLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @GetMapping
    public List<SpendingLimitDTO> getAll(@AuthenticationPrincipal UserDetails userDetails) {
        return spendingLimitService.getLimitsWithProgress(userDetails.getUsername());
    }

    @PostMapping
    public ResponseEntity<SpendingLimitDTO> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SpendingLimit limit) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spendingLimitService.createLimit(limit, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpendingLimitDTO> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @Valid @RequestBody SpendingLimit limit) {
        return ResponseEntity.ok(spendingLimitService.updateLimit(id, limit, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id) {
        spendingLimitService.deleteLimit(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
