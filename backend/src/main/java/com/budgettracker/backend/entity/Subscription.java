package com.budgettracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Tracks the subscription plan for each user.
 *
 * <p>Every user gets a FREE subscription row on registration (or on first access).
 * When a Stripe payment succeeds, the plan is upgraded to PRO and the Stripe IDs
 * are stored here so we can verify webhooks and manage the subscription lifecycle.
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan = Plan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    /** Stripe customer ID (cus_xxx). Populated when checkout session is created. */
    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    /** Stripe subscription ID (sub_xxx). Populated after payment succeeds via webhook. */
    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    /**
     * End of the current billing period (epoch seconds from Stripe).
     * When status is CANCELLING, the user remains PRO until this date,
     * then Stripe fires customer.subscription.deleted and we downgrade.
     */
    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    /** PRO and active, or PRO but scheduled to cancel at period end. */
    public boolean isPro() {
        return plan == Plan.PRO && (status == Status.ACTIVE || status == Status.CANCELLING);
    }

    public enum Plan   { FREE, PRO }
    public enum Status { ACTIVE, CANCELLING, CANCELLED, PAST_DUE }
}