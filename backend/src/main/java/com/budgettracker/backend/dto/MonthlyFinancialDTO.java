package com.budgettracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyFinancialDTO {
    private String month;
    private Double totalExpenses;
    private Double totalIncome;
    private Double netBalance;
    private Integer expenseCount;
    private Integer incomeCount;
}
