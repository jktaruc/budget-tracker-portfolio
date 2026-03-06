package com.budgettracker.backend.service;

import com.budgettracker.backend.dto.ImportResultDTO;
import com.budgettracker.backend.entity.Expense;
import com.budgettracker.backend.entity.Income;
import com.budgettracker.backend.entity.User;
import com.budgettracker.backend.repository.ExpenseRepository;
import com.budgettracker.backend.repository.IncomeRepository;
import com.budgettracker.backend.repository.UserRepository;
import com.budgettracker.backend.service.base.UserScopedServiceBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ImportService extends UserScopedServiceBase {

    private static final int MAX_ROWS = 1_000;

    // Expected CSV column indices — must match the template at ImportController.TEMPLATE_CSV
    private static final int COL_DATE     = 0;
    private static final int COL_TITLE    = 1;
    private static final int COL_CATEGORY = 2;
    private static final int COL_AMOUNT   = 3;
    private static final int COL_TYPE     = 4;

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository  incomeRepository;

    public ImportService(
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository,
            UserRepository userRepository) {
        super(userRepository);
        this.expenseRepository = expenseRepository;
        this.incomeRepository  = incomeRepository;
    }

    /**
     * Parses a CSV file with the header: Date,Title,Category,Amount,Type
     * and saves each valid row as an Expense or Income for the given user.
     *
     * Invalid rows are collected and returned in the result instead of
     * aborting the entire import. The entire save is wrapped in a transaction
     * so a DB failure does not leave partial data.
     */
    @Transactional
    public ImportResultDTO importCsv(MultipartFile file, String userEmail) {
        User user = getUser(userEmail);

        List<Expense> expenses = new ArrayList<>();
        List<Income>  incomes  = new ArrayList<>();
        List<String>  errors   = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("File is empty");
            }

            int rowNumber = 1; // header was row 1
            String line;

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                if (line.isBlank()) continue;

                if (expenses.size() + incomes.size() >= MAX_ROWS) {
                    errors.add("Row " + rowNumber + ": import limit of " + MAX_ROWS + " rows reached — remaining rows skipped");
                    break;
                }

                String[] cols = parseCsvLine(line);
                String rowError = validateRow(cols, rowNumber);
                if (rowError != null) {
                    errors.add(rowError);
                    continue;
                }

                LocalDate date     = LocalDate.parse(cols[COL_DATE].trim());
                String    title    = cols[COL_TITLE].trim();
                String    category = cols[COL_CATEGORY].trim();
                double    amount   = Double.parseDouble(cols[COL_AMOUNT].trim());
                String    type     = cols[COL_TYPE].trim().toUpperCase();

                if ("EXPENSE".equals(type)) {
                    Expense e = new Expense();
                    e.setDate(date);
                    e.setTitle(title);
                    e.setCategory(category);
                    e.setAmount(amount);
                    e.setUser(user);
                    expenses.add(e);
                } else {
                    Income i = new Income();
                    i.setDate(date);
                    i.setTitle(title);
                    i.setCategory(category);
                    i.setAmount(amount);
                    i.setUser(user);
                    incomes.add(i);
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("CSV import failed for user {}: {}", userEmail, e.getMessage());
            throw new IllegalArgumentException("Could not read file: " + e.getMessage());
        }

        try {
            expenseRepository.saveAll(expenses);
            incomeRepository.saveAll(incomes);
        } catch (Exception e) {
            log.error("CSV import DB save failed for user {}: {}", userEmail, e.getMessage());
            throw new IllegalArgumentException("Failed to save imported records: " + e.getMessage());
        }

        log.info("CSV import for user {}: {} expenses, {} income, {} skipped",
                userEmail, expenses.size(), incomes.size(), errors.size());

        return new ImportResultDTO(expenses.size(), incomes.size(), errors);
    }

    /** Returns a user-facing error string if the row is invalid, or null if it is valid. */
    private String validateRow(String[] cols, int rowNumber) {
        String prefix = "Row " + rowNumber + ": ";

        if (cols.length < 5) {
            return prefix + "expected 5 columns (Date,Title,Category,Amount,Type), found " + cols.length;
        }

        String dateStr   = cols[COL_DATE].trim();
        String title     = cols[COL_TITLE].trim();
        String category  = cols[COL_CATEGORY].trim();
        String amountStr = cols[COL_AMOUNT].trim();
        String type      = cols[COL_TYPE].trim().toUpperCase();

        if (dateStr.isBlank()) return prefix + "Date is required";
        try {
            LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return prefix + "invalid date \"" + dateStr + "\" (expected YYYY-MM-DD)";
        }

        if (title.isBlank())    return prefix + "Title is required";
        if (title.length() > 100) return prefix + "Title must not exceed 100 characters";
        if (category.isBlank()) return prefix + "Category is required";

        if (amountStr.isBlank()) return prefix + "Amount is required";
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) return prefix + "Amount must be greater than zero";
        } catch (NumberFormatException e) {
            return prefix + "invalid amount \"" + amountStr + "\"";
        }

        if (!"EXPENSE".equals(type) && !"INCOME".equals(type)) {
            return prefix + "Type must be EXPENSE or INCOME (got \"" + cols[COL_TYPE].trim() + "\")";
        }

        return null;
    }

    /**
     * Minimal CSV line parser that handles quoted fields containing commas or quotes.
     * Supports the RFC 4180 subset produced by the export endpoint.
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
