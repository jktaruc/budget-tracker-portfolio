package com.budgettracker.backend.controller;

import com.budgettracker.backend.dto.SubscriptionDTO;
import com.budgettracker.backend.service.SubscriptionService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /** Returns the current user's plan (FREE / PRO) and status. */
    @GetMapping("/api/subscription/status")
    public ResponseEntity<SubscriptionDTO> getStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(subscriptionService.getStatus(userDetails.getUsername()));
    }

    /**
     * Creates a Stripe Checkout Session and returns the redirect URL.
     * The frontend redirects the browser to this URL to complete payment.
     */
    @PostMapping("/api/subscription/checkout")
    public ResponseEntity<Map<String, String>> createCheckout(
            @AuthenticationPrincipal UserDetails userDetails) throws StripeException {
        String url = subscriptionService.createCheckoutSession(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Cancels the subscription at period end.
     * User stays PRO until the current billing cycle ends, then downgrades to FREE.
     */
    @PostMapping("/api/subscription/cancel")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal UserDetails userDetails) throws StripeException {
        subscriptionService.cancelSubscription(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * Must be unauthenticated (Stripe calls this, not the user).
     * Signature is verified inside SubscriptionService.handleWebhook().
     */
    @PostMapping("/api/stripe/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        subscriptionService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}