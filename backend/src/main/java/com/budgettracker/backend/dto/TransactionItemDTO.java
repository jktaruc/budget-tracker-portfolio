package com.budgettracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionItemDTO {
    private String id;
    private String title;
    private String category;
    private Double amount;
    private String date;
    private String type; // "INCOME" or "EXPENSE"
}
