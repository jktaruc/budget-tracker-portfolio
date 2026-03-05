package com.budgettracker.backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "spending_limits",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category", "period"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SpendingLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull @Positive(message = "Limit must be positive")
    private Double limitAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Period period = Period.MONTHLY;

    // Start date anchors the period window.
    // e.g. MONTHLY + startDate=March 15 → window is Mar 15 to Apr 14
    // If null, defaults to calendar start (1st of month, Monday of week, etc.)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    public enum Period { WEEKLY, BI_WEEKLY, MONTHLY, YEARLY }
}
