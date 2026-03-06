package com.budgettracker.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    /**
     * Retained for DB compatibility — this column exists in the production schema.
     * It is not actively used for token invalidation; JWT expiry is handled stateless in JwtUtil.
     * Incrementing this value can be used in a future token revocation implementation.
     */
    @Column(nullable = true)
    private Integer refreshTokenVersion = 0;

    /** Never returns null — coerces legacy null rows to 0. Overrides Lombok @Getter. */
    public int getRefreshTokenVersion() {
        return this.refreshTokenVersion == null ? 0 : this.refreshTokenVersion;
    }

    /** Increments the refresh token version, invalidating all existing refresh tokens for this user. */
    public void incrementRefreshTokenVersion() {
        this.refreshTokenVersion = getRefreshTokenVersion() + 1;
    }

    public enum Role { USER, ADMIN }
}