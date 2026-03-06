package com.budgettracker.backend.dto;

import lombok.Getter;

import java.util.List;

/** Returned by POST /api/import/csv. Summarises what was imported and what was skipped. */
@Getter
public class ImportResultDTO {

    private final int importedExpenses;
    private final int importedIncome;
    private final int skippedRows;
    private final List<String> errors;

    public ImportResultDTO(int importedExpenses, int importedIncome, List<String> errors) {
        this.importedExpenses = importedExpenses;
        this.importedIncome   = importedIncome;
        this.skippedRows      = errors.size();
        this.errors           = errors;
    }

    public int totalImported() {
        return importedExpenses + importedIncome;
    }
}
