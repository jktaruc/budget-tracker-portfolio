package com.budgettracker.backend.controller;

import com.budgettracker.backend.dto.ImportResultDTO;
import com.budgettracker.backend.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private static final String TEMPLATE_CSV =
            "Date,Title,Category,Amount,Type\n" +
            "2025-01-15,Woolworths Groceries,Food,85.40,EXPENSE\n" +
            "2025-01-15,Monthly Salary,Salary,5500.00,INCOME\n" +
            "2025-01-20,Netflix,Entertainment,22.99,EXPENSE\n" +
            "2025-01-28,Freelance Project,Freelance,1200.00,INCOME\n";

    private static final Set<String> ACCEPTED_MIME_TYPES = Set.of(
            "text/csv",
            "text/plain",
            "application/csv",
            "application/vnd.ms-excel"
    );

    private final ImportService importService;

    /**
     * POST /api/import/csv
     * Accepts a multipart CSV file. Returns a summary of imported rows and any skipped rows with reasons.
     */
    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultDTO> importCsv(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are accepted");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ACCEPTED_MIME_TYPES.contains(contentType.toLowerCase().split(";")[0].trim())) {
            throw new IllegalArgumentException(
                    "Invalid content type '" + contentType + "'. Expected a CSV file.");
        }

        ImportResultDTO result = importService.importCsv(file, userDetails.getUsername());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/import/template
     * Public endpoint — returns a sample CSV that users can fill in and import.
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = TEMPLATE_CSV.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
