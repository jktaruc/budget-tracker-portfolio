package com.budgettracker.backend.controller;

import com.budgettracker.backend.dto.ImportResultDTO;
import com.budgettracker.backend.service.ImportService;
import com.budgettracker.backend.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private static final String TEMPLATE_CSV =
            "Date,Title,Category,Amount,Type\n" +
            "2024-01-15,Woolworths,Food,120.50,EXPENSE\n" +
            "2024-01-15,Monthly Salary,Salary,5000.00,INCOME\n";

    private final ImportService       importService;
    private final SubscriptionService subscriptionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultDTO> importCsv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.requirePro(userDetails.getUsername(), "CSV Import");
        return ResponseEntity.ok(importService.importCsv(file, userDetails.getUsername()));
    }

    /** Template download — free for everyone so they can see the format. */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(TEMPLATE_CSV.getBytes());
    }
}
