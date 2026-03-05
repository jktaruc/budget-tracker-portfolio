package com.budgettracker.backend.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinancialSummaryDTO {
    private Double totalExpenses;
    private Integer totalExpenseTransactions;
    private Map<String, Double> expensesByCategory;

    private Double totalIncome;
    private Integer totalIncomeTransactions;
    private Map<String, Double> incomeByCategory;

    private Double netBalance;
    private Double savingsRate;
    private Double monthlyAverageExpenses;
    private Double monthlyAverageIncome;

    private List<MonthlyFinancialDTO> monthlyTrend;

    private List<TransactionItemDTO> topExpenses;
    private List<TransactionItemDTO> topIncomes;
    private List<TransactionItemDTO> recentTransactions;
}

