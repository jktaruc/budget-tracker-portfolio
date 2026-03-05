package com.budgettracker.backend.dto;

import com.budgettracker.backend.entity.SpendingLimit;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SpendingLimitDTO {
    private String id;
    private String category;
    private Double limitAmount;
    private SpendingLimit.Period period;
    private LocalDate startDate;
    private Double spent;
    private Double remaining;
    private Double percentageUsed;
    private boolean exceeded;
    private LocalDate windowStart;
    private LocalDate windowEnd;
}
