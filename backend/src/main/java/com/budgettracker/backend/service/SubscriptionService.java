package com.budgettracker.backend.service;

import com.budgettracker.backend.dto.SubscriptionDTO;
import com.budgettracker.backend.entity.Subscription;
import com.budgettracker.backend.entity.User;
import com.budgettracker.backend.exception.PlanGateException;
import com.budgettracker.backend.repository.SubscriptionRepository;
import com.budgettracker.backend.repository.UserRepository;
import com.budgettracker.backend.service.base.UserScopedServiceBase;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.time.LocalDateTime;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages subscription state and Stripe Checkout integration.
 *
 * <p><b>Checkout flow:</b>
 * <ol>
 *   <li>Frontend calls POST /api/subscription/checkout</li>
 *   <li>We create (or reuse) a Stripe Customer for the user</li>
 *   <li>We create a Stripe Checkout Session with the PRO price</li>
 *   <li>We return the session URL — frontend redirects to it</li>
 *   <li>Stripe redirects back to /billing?success=true on payment</li>
 *   <li>Stripe sends checkout.session.completed webhook → we upgrade the plan</li>
 * </ol>
 *
 * <p><b>Webhook events handled:</b>
 * <ul>
 *   <li>checkout.session.completed → upgrade to PRO</li>
 *   <li>customer.subscription.deleted → downgrade to FREE</li>
 *   <li>invoice.payment_failed → set status PAST_DUE</li>
 * </ul>
 */
@Slf4j
@Service
public class SubscriptionService extends UserScopedServiceBase {

    private final SubscriptionRepository subscriptionRepository;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.price-id}")
    private String priceId;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               UserRepository userRepository) {
        super(userRepository);
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostConstruct
    void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    // ── Plan gate ─────────────────────────────────────────────────────────────

    /**
     * Returns the subscription for a user, creating a FREE one if none exists yet.
     * This is the single source of truth for plan checks.
     */
    @Transactional
    public Subscription getOrCreate(String userEmail) {
        User user = getUser(userEmail);
        return subscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Subscription s = new Subscription();
                    s.setUser(user);
                    s.setPlan(Subscription.Plan.FREE);
                    s.setStatus(Subscription.Status.ACTIVE);
                    return subscriptionRepository.save(s);
                });
    }

    /** Throws PlanGateException if the user is not on PRO. */
    public void requirePro(String userEmail, String featureName) {
        if (!getOrCreate(userEmail).isPro()) {
            throw new PlanGateException(featureName);
        }
    }

    public boolean isPro(String userEmail) {
        return getOrCreate(userEmail).isPro();
    }

    public SubscriptionDTO getStatus(String userEmail) {
        Subscription sub = getOrCreate(userEmail);
        return new SubscriptionDTO(
                sub.getPlan().name(),
                sub.getStatus().name(),
                sub.isPro(),
                sub.getStatus() == Subscription.Status.CANCELLING,
                sub.getCurrentPeriodEnd()
        );
    }

    // ── Stripe checkout ───────────────────────────────────────────────────────

    @Transactional
    public String createCheckoutSession(String userEmail) throws StripeException {
        User user = getUser(userEmail);
        Subscription sub = getOrCreate(userEmail);

        // Create or reuse Stripe customer
        String customerId = sub.getStripeCustomerId();
        if (customerId == null) {
            Customer customer = Customer.create(
                    CustomerCreateParams.builder()
                            .setEmail(user.getEmail())
                            .setName(user.getName())
                            .build()
            );
            customerId = customer.getId();
            sub.setStripeCustomerId(customerId);
            subscriptionRepository.save(sub);
        }

        // Create Checkout Session for a recurring subscription
        Session session = Session.create(
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                        .setCustomer(customerId)
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPrice(priceId)
                                        .setQuantity(1L)
                                        .build()
                        )
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .build()
        );

        log.info("Created Stripe checkout session {} for user {}", session.getId(), userEmail);
        return session.getUrl();
    }

    @Transactional
    public void cancelSubscription(String userEmail) throws StripeException {
        Subscription sub = getOrCreate(userEmail);
        if (sub.getStripeSubscriptionId() == null) return;

        // Cancel at period end — user stays PRO until billing cycle ends
        com.stripe.model.Subscription stripeSub =
                com.stripe.model.Subscription.retrieve(sub.getStripeSubscriptionId());

        com.stripe.param.SubscriptionUpdateParams params =
                com.stripe.param.SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build();
        stripeSub.update(params);

        sub.setStatus(Subscription.Status.CANCELLING);
        subscriptionRepository.save(sub);
        log.info("User {} scheduled cancellation at period end ({})",
                userEmail, sub.getCurrentPeriodEnd());
    }

    // ── Webhook handling ──────────────────────────────────────────────────────

    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        log.info("Received Stripe webhook: {}", event.getType());

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "customer.subscription.deleted" -> handleSubscriptionCancelled(event);
            case "invoice.payment_failed" -> handlePaymentFailed(event);
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Event event) {
        Session session;
        try {
            session = (Session) event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (Exception e) {
            log.error("Could not deserialize checkout.session.completed", e);
            return;
        }
        String customerId = session.getCustomer();
        String subscriptionId = session.getSubscription();

        subscriptionRepository.findByStripeCustomerId(customerId).ifPresent(sub -> {
            sub.setPlan(Subscription.Plan.PRO);
            sub.setStatus(Subscription.Status.ACTIVE);
            sub.setStripeSubscriptionId(subscriptionId);

            // Store billing period end so frontend can show it
            try {
                com.stripe.model.Subscription stripeSub =
                        com.stripe.model.Subscription.retrieve(subscriptionId);
                sub.setCurrentPeriodEnd(
                        LocalDateTime.ofEpochSecond(stripeSub.getCurrentPeriodEnd(), 0, java.time.ZoneOffset.UTC)
                );
            } catch (Exception e) {
                log.warn("Could not fetch period end for sub {}", subscriptionId);
            }

            subscriptionRepository.save(sub);
            log.info("Upgraded user {} to PRO (Stripe sub: {})",
                    sub.getUser().getEmail(), subscriptionId);
        });
    }

    private void handleSubscriptionCancelled(Event event) {
        com.stripe.model.Subscription stripeSub;
        try {
            stripeSub = (com.stripe.model.Subscription) event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (Exception e) {
            log.error("Could not deserialize customer.subscription.deleted", e);
            return;
        }

        subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId()).ifPresent(sub -> {
            sub.setPlan(Subscription.Plan.FREE);
            sub.setStatus(Subscription.Status.CANCELLED);
            subscriptionRepository.save(sub);
            log.info("Downgraded user {} to FREE (subscription cancelled)",
                    sub.getUser().getEmail());
        });
    }

    private void handlePaymentFailed(Event event) {
        com.stripe.model.Invoice invoice;
        try {
            invoice = (com.stripe.model.Invoice) event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (Exception e) {
            log.error("Could not deserialize invoice.payment_failed", e);
            return;
        }
        String customerId = invoice.getCustomer();

        subscriptionRepository.findByStripeCustomerId(customerId).ifPresent(sub -> {
            sub.setStatus(Subscription.Status.PAST_DUE);
            subscriptionRepository.save(sub);
            log.warn("Payment failed for user {} — status set to PAST_DUE",
                    sub.getUser().getEmail());
        });
    }
}