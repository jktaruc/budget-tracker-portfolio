package com.budgettracker.backend.dto;

import java.time.LocalDateTime;

public record SubscriptionDTO(
        String plan,
        String status,
        boolean pro,
        boolean cancelling,
        LocalDateTime currentPeriodEnd
) {}