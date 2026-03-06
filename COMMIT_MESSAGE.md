feat: add Stripe subscription billing with Pro plan gating

Introduces a full subscription system using Stripe Checkout. Users can
upgrade to Pro, cancel at period end (retaining access until the billing
cycle expires), and are automatically downgraded when Stripe fires the
final webhook. Pro features are enforced on both the backend (402 gate)
and the frontend (paywall modal + page-level upgrade prompts).

## Backend

- Add `Subscription` entity with `Plan` (FREE/PRO) and `Status`
  (ACTIVE/CANCELLING/CANCELLED/PAST_DUE) and `currentPeriodEnd`
- Add `SubscriptionRepository` with lookups by userId, stripeCustomerId,
  and stripeSubscriptionId
- Add `SubscriptionService` — plan gate (`requirePro`), Stripe Checkout
  session creation, cancel-at-period-end, and webhook handling
- Add `SubscriptionController` — `/api/subscription/status`, `/checkout`,
  `/cancel`, and `/api/stripe/webhook` (unauthenticated, signature-verified)
- Add `PlanGateException` → mapped to HTTP 402 in `GlobalExceptionHandler`
- Add `SubscriptionDTO` exposing `plan`, `status`, `pro`, `cancelling`,
  `currentPeriodEnd`
- Gate `SpendingLimitController`, `RecurringTransactionController`,
  `FinancialSummaryController`, `ExportController`, `ImportController`
  behind `requirePro`
- Use `deserializeUnsafe()` on all webhook handlers to bypass Stripe SDK
  version mismatch with newer API fields
- Permit `/api/stripe/webhook` in `SecurityConfig`
- Split `DemoDataService` into `resetFreeDemo` / `resetProDemo` with
  `ensureSubscription` to set the correct plan on each reset
- Add `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_PRICE_ID`,
  `FRONTEND_URL` env vars to `application.yml` and `docker-compose.yml`
- Fix `management.endpoint.health.show-details` from `always` to
  `when-authorized`

## Frontend

- Add `SubscriptionContext` — fetches `/subscription/status`, exposes
  `isPro`, `plan`, `cancelling`, `currentPeriodEnd`, `cancelSubscription`
- Wrap app in `SubscriptionProvider` inside `App.tsx`
- Add `PaywallModal` — triggered globally by 402 responses via a
  `window.dispatchEvent("paywall")` custom event in the Axios interceptor;
  shows Free vs Pro plan comparison and redirects to Stripe Checkout
- Add `PlanBadge` component — clickable link to `/billing`, turns amber
  with "Pro (cancelling)" label when cancellation is scheduled
- Add `BillingPage` — shows current plan, next renewal date, cancel
  button with confirmation step, and cancelling notice with period-end date
- Add paywall screens to `Summary`, `SpendingLimitsPage`, `RecurringPage`
  — detect 402 on load and replace page content with an upgrade prompt
- Hide Import/Export buttons in `NavBar` for Free users
- Update `DemoPage` with side-by-side Free and Pro demo cards, each with
  their own "Try Demo" button
- Register `/billing` route in `App.tsx`
