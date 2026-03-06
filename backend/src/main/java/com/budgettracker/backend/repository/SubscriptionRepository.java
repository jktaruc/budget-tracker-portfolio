package com.budgettracker.backend.repository;

import com.budgettracker.backend.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    Optional<Subscription> findByUserId(String userId);
    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
