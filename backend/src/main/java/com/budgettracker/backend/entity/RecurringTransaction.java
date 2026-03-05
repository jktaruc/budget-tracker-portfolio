package com.budgettracker.backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "recurring_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Title is required")
    @Size(max = 100)
    private String title;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull @Positive
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    /**
     * Defaults to today if not supplied by the client.
     * The field-level default ensures @NotNull is satisfied before any
     * persistence validation runs, so the service no longer needs a null-guard.
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate = LocalDate.now();

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastProcessed;

    private boolean active = true;

    public enum Type { EXPENSE, INCOME }
    public enum Frequency { DAILY, WEEKLY, BI_WEEKLY, MONTHLY, YEARLY }
}
