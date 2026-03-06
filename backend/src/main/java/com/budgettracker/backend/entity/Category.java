package com.budgettracker.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "categories",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "type", "user_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Category name is required")
    @Size(max = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type; // EXPENSE or INCOME

    // null = global default visible to all users; non-null = user-scoped custom category
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public enum Type { EXPENSE, INCOME }
}
