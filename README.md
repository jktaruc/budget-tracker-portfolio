# Budget Tracker

A full-stack personal finance application for tracking income and expenses, setting spending limits, automating recurring transactions, and visualising financial trends. Includes a Stripe-powered subscription system with Free and Pro plans.

**Live demo:** https://budget-tracker-frontend-5o9w.onrender.com

> The demo runs on Render's free tier — the backend may take 30–60 seconds to wake up on first load.

---

## Tech Stack

| Layer     | Technology                                       |
|-----------|--------------------------------------------------|
| Frontend  | React 19, TypeScript, Vite, Recharts             |
| Backend   | Spring Boot 3.3, Java 17                         |
| Database  | PostgreSQL 16                                    |
| Auth      | JWT (access + refresh tokens), BCrypt            |
| Payments  | Stripe Checkout, Stripe Webhooks                 |
| API Docs  | SpringDoc OpenAPI (Swagger UI)                   |
| Testing   | JUnit 5, Mockito, MockMvc, H2 (in-memory)        |
| Deploy    | Render (Docker backend, static frontend + Nginx) |

---

## Features

### Subscription & Billing
- Free plan with core expense and income tracking
- Pro plan ($5/month) unlocks Spending Limits, Financial Summary, Recurring Transactions, and CSV Import/Export
- Stripe Checkout for secure payment — users are never redirected to a custom payment form
- Auto-renewing monthly subscription handled entirely by Stripe
- Cancel at period end — users stay on Pro until their billing cycle expires, then downgrade automatically
- Webhook-driven plan upgrades and downgrades (`checkout.session.completed`, `customer.subscription.deleted`, `invoice.payment_failed`)
- Per-endpoint plan gate (`requirePro`) returns HTTP 402, triggering a paywall modal on the frontend
- Plan badge in the navbar links to the billing page; turns amber when cancellation is scheduled

### Transactions
- Add, edit, and delete expenses and income
- Filter by month (`YYYY-MM`), category, or both simultaneously
- Paginated list endpoints for large datasets

### Custom Categories
- Global default categories seeded on startup (Food, Transport, Bills, etc.)
- Each user can create and delete their own custom categories for both expenses and income
- Category dropdowns across the app are always fetched live from the API
- Deletion is blocked if any transactions already use that category

### Spending Limits
- Set per-category spending limits with Weekly, Bi-Weekly, Monthly, or Yearly periods
- Choose a custom start date — the period window anchors to that date and rolls forward automatically (e.g. Monthly from the 15th tracks the 15th–14th window, not the calendar month)
- Live progress per limit: amount spent, amount remaining, percentage used
- Visual warnings at 80% usage and over-limit alerts

### Recurring Transactions
- Define recurring expenses or income at Daily, Weekly, Bi-Weekly, Monthly, or Yearly frequencies
- Optional end dates — rules auto-deactivate once the end date passes
- Pause or resume any rule without deleting it
- A daily scheduled job processes all due transactions automatically and stamps each with `(Recurring)` in the title

### Financial Summary
- Date range picker with a 6-month default view
- Key stats: net balance, savings rate, monthly average income and expenses
- Income vs Expenses chart by month
- Category breakdown charts for both income and expenses
- Recent transactions feed
- **Projected mode** — overlays estimated future recurring transactions on actual data for the selected date range, so you can see where you're headed without recording anything

### Export & Import
- Download all transactions, expenses only, or income only as CSV
- Import transactions from a single CSV file (`Date,Title,Category,Amount,Type`) — supports both EXPENSE and INCOME rows
- Invalid rows are skipped with per-row error messages; all valid rows are always saved
- Download a pre-filled CSV template from the Import modal (public endpoint — no login required)

### Authentication
- JWT access tokens (short-lived) + refresh tokens (long-lived)
- Refresh token versioning: incrementing the version on logout invalidates all outstanding refresh tokens server-side without a blocklist
- Silent token refresh on the frontend — the user is only redirected to login when the refresh itself fails

### Demo Mode
- Two demo accounts — Free and Pro — each with pre-seeded Australian transaction data
- One-click reset per plan restores the original data and returns a fresh JWT
- Pro demo includes spending limits, recurring transactions, and financial summary data pre-populated
- IP-based rate limiting on the reset endpoints to prevent abuse

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│  Browser                                                         │
│  React 19 + TypeScript (Vite, Recharts)                          │
│  Axios interceptors → silent JWT refresh on 401, paywall on 402  │
└─────────────────────┬────────────────────────────────────────────┘
                      │ HTTPS  /api/**
┌─────────────────────▼────────────────────────────────────────────┐
│  Spring Boot 3.3  (Docker on Render)                             │
│                                                                  │
│  JwtAuthFilter → SecurityConfig → Controllers → Services         │
│                                                                  │
│  SubscriptionService ← plan gate (requirePro → 402)              │
│  UserScopedServiceBase  ←  shared ownership + user-lookup        │
│  GlobalExceptionHandler ←  unified error responses               │
└──────────┬──────────────────────────────────────┬────────────────┘
           │ JPA / Hibernate                       │ Stripe SDK
┌──────────▼──────────────────────┐   ┌───────────▼────────────────┐
│  PostgreSQL 16                  │   │  Stripe                    │
│  users · expenses · incomes     │   │  Checkout Sessions         │
│  categories · spending_limits   │   │  Subscriptions             │
│  recurring_transactions         │   │  Webhooks → /stripe/webhook│
│  subscriptions                  │   └────────────────────────────┘
└─────────────────────────────────┘
```

---

## Technical Decisions

A few non-obvious choices made during development and why:

**Stripe webhook deserialization with `deserializeUnsafe()`**
The Stripe Java SDK validates the event payload against its own API version. When Stripe's API version is newer than the SDK version, fields like `branding_settings` or `wallet_options` cause `getObject()` to return empty rather than throwing. Using `deserializeUnsafe()` bypasses the version check and deserializes directly from the raw JSON — safe here because the payload signature is already verified by `Webhook.constructEvent` before deserialization is attempted.

**Cancel at period end instead of immediate cancellation**
Immediate cancellation (`stripe.cancel()`) terminates the subscription right away and would require prorating refunds. Setting `cancelAtPeriodEnd = true` instead tells Stripe to stop charging at the next renewal date while keeping the subscription active. The backend stores a `CANCELLING` status so the frontend can show "Pro until [date]" without polling Stripe on every request. When the period ends, Stripe fires `customer.subscription.deleted` and the backend downgrades the user automatically.

**Refresh token versioning instead of a blocklist**
Logout needs to invalidate all outstanding refresh tokens without storing them. Adding a `refresh_token_version` integer to the `users` table and embedding it as a JWT claim (`rtv`) means any token issued before a logout is rejected on next use — zero extra storage and no cache dependency.

**`UserScopedServiceBase` for ownership checks**
Every service that touches user-owned data needs the same two operations: load a user by email, and verify ownership before mutating. Extracting these into an abstract base class eliminated repeated boilerplate and made ownership enforcement a single auditable path rather than something that could be accidentally omitted in a new service.

**Spending limit windows anchored to a start date**
Calendar-based periods (1st of the month, Monday of the week) are intuitive but wrong for anyone paid mid-month or on a non-standard cycle. Anchoring the window to a user-supplied start date and computing `windowStart = startDate + N * periodLength` means the limit always tracks the right 30 days, not the calendar month.

**Projected mode without touching the database**
The projected view overlays synthetic future transactions from active recurring rules on top of actual data — but only in memory. Projected `Expense` and `Income` objects are constructed in the service layer and discarded after the response is built. The database never sees them, so there is no risk of accidentally persisting projections as real transactions.

**`CategoryCreateRequest` DTO instead of accepting the entity**
The `Category` entity has a `user` JPA relation. Accepting the raw entity as a `@RequestBody` means a client-supplied `"user": null` in the JSON body could save a globally-visible category (accessible to all users). Using a dedicated DTO with only `name` and `type` makes it structurally impossible for the client to influence the user field — it is always set server-side from `@AuthenticationPrincipal`.

**N+1 query fix on spending limits**
The naive `getLimitsWithProgress()` implementation loaded all expenses into memory and filtered in Java — one full-table scan per spending limit. Replaced with a single indexed `SUM` aggregation query per limit scoped to the computed period window, turning an O(n × rows) scan into O(n) indexed lookups.

**Demo rate limiting without Redis**
The demo reset endpoint is public and unauthenticated. A `ConcurrentHashMap`-based sliding window filter (5 requests per 60 seconds per IP) covers the single-instance deployment without adding a Redis dependency. The filter is documented to be swapped for a Redis-backed solution if the app ever runs multiple instances.

---

## Project Structure

```
budget-tracker/
├── backend/                        # Spring Boot API
│   └── src/
│       ├── main/java/com/budgettracker/backend/
│       │   ├── config/             # Security, CORS, daily scheduler, data seeder
│       │   ├── controller/         # REST endpoints
│       │   ├── dto/                # API response shapes (raw entities are never returned)
│       │   ├── entity/             # JPA entities mapped to DB tables
│       │   ├── exception/          # GlobalExceptionHandler + custom exceptions (incl. PlanGateException)
│       │   ├── filter/             # JwtAuthFilter, DemoRateLimitFilter
│       │   ├── repository/         # Spring Data JPA interfaces (includes custom @Query methods)
│       │   ├── security/           # JwtUtil, UserDetailsServiceImpl
│       │   └── service/            # All business logic
│       │       └── base/           # UserScopedServiceBase — shared user-lookup & ownership helpers
│       └── test/                   # JUnit + Mockito + MockMvc + H2
└── frontend/                       # React + TypeScript (Vite)
    └── src/
        ├── api/                    # Axios instance with request/response interceptors
        ├── components/             # Reusable UI components
        │   ├── summary/            # Chart and stats sub-components
        │   ├── pagination/         # Pagination control
        │   ├── PaywallModal.tsx    # Global 402 upgrade modal
        │   └── PlanBadge.tsx       # Navbar plan indicator linking to /billing
        ├── context/                # AuthContext, SubscriptionContext
        ├── hooks/                  # useCategories, useTransactionList
        ├── pages/                  # Route-level views (Dashboard, Summary, Billing, etc.)
        ├── styles/                 # Component CSS files
        ├── types/                  # TypeScript interfaces
        └── utils/                  # date-utils and other helpers
```

---

## Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL 14+  *(not needed for Docker)*

---

### Option A — Docker (recommended)

Runs the full stack (database, backend, frontend) with a single command.

```bash
docker compose up --build
```

| Service  | URL                                    |
|----------|----------------------------------------|
| Frontend | http://localhost                       |
| Backend  | http://localhost:8080                  |
| Swagger  | http://localhost:8080/swagger-ui.html  |

To stop and remove volumes:
```bash
docker compose down -v
```

---

### Option B — Local development

**1. Database**

```sql
CREATE DATABASE budget_tracker;
```

**2. Backend**

```bash
export DB_URL=jdbc:postgresql://localhost:5432/budget_tracker
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
export JWT_SECRET=any-long-base64-random-string
export STRIPE_SECRET_KEY=sk_test_...
export STRIPE_WEBHOOK_SECRET=whsec_...   # from: stripe listen --forward-to localhost:8080/api/stripe/webhook
export STRIPE_PRICE_ID=price_...
```

```bash
cd backend
./gradlew bootRun
```

API: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

**3. Frontend**

```bash
cd frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`

The Vite dev server proxies all `/api` requests to `http://localhost:8080` automatically — no separate config needed.

---

## Environment Variables

| Variable               | Required     | Description                                                         |
|------------------------|--------------|---------------------------------------------------------------------|
| `DB_URL`               | Local only   | Full JDBC URL (`jdbc:postgresql://host:port/db`)                    |
| `DB_HOST`              | Production   | Database host (injected by Render from a linked database)           |
| `DB_PORT`              | Production   | Database port                                                       |
| `DB_NAME`              | Production   | Database name                                                       |
| `DB_USERNAME`          | Yes          | Database user                                                       |
| `DB_PASSWORD`          | Yes          | Database password                                                   |
| `JWT_SECRET`           | Yes          | Base64-encoded secret for signing JWTs — use a long random string   |
| `STRIPE_SECRET_KEY`    | Yes          | Stripe secret key (`sk_live_...` or `sk_test_...`)                  |
| `STRIPE_WEBHOOK_SECRET`| Yes          | Stripe webhook signing secret (`whsec_...`) — use CLI secret locally|
| `STRIPE_PRICE_ID`      | Yes          | Stripe Price ID for the Pro monthly plan (`price_...`)              |
| `FRONTEND_URL`         | Production   | Frontend origin used for Stripe success/cancel redirect URLs        |
| `CORS_ALLOWED_ORIGINS` | Production   | Comma-separated list of allowed frontend origins                    |
| `PORT`                 | Production   | Server port (defaults to `8080`)                                    |
| `SELF_URL`             | Production   | Backend's own public URL — enables the keep-alive ping              |
| `VITE_API_BASE_URL`    | Production   | Full API base URL baked into the frontend build                     |

---

## Running Tests

```bash
cd backend
./gradlew test
```

HTML report: `backend/build/reports/tests/test/index.html`

The test suite covers:
- **Service unit tests** — business logic with Mockito-mocked repositories (Expense, Income, RecurringTransaction, SpendingLimit)
- **Controller tests** — MockMvc slice tests for REST endpoints (auth, expenses)
- **Edge cases** — boundary dates, empty states, ownership checks, pagination

---

## API Reference

All endpoints except `/api/auth/**`, `/api/demo/**`, `/api/import/template`, and `/actuator/health` require a JWT.
Pass it as: `Authorization: Bearer <accessToken>`

### Auth

| Method | Endpoint              | Auth | Description                                                                   |
|--------|-----------------------|------|-------------------------------------------------------------------------------|
| POST   | `/api/auth/register`  | No   | Register a new account, returns JWT pair                                      |
| POST   | `/api/auth/login`     | No   | Login, returns JWT pair                                                       |
| POST   | `/api/auth/refresh`   | No   | Exchange a refresh token for a new JWT pair                                   |
| POST   | `/api/auth/logout`    | Yes  | Increments refresh token version, invalidating all outstanding refresh tokens |

### Expenses

| Method | Endpoint              | Description                                        |
|--------|-----------------------|----------------------------------------------------|
| GET    | `/api/expenses`       | List expenses (`?month=YYYY-MM`, `?category=Food`) |
| GET    | `/api/expenses/paged` | Paginated expenses (Spring Data Pageable)          |
| POST   | `/api/expenses`       | Create expense                                     |
| PUT    | `/api/expenses/{id}`  | Update expense                                     |
| DELETE | `/api/expenses/{id}`  | Delete expense                                     |

### Income

| Method | Endpoint              | Description                                          |
|--------|-----------------------|------------------------------------------------------|
| GET    | `/api/incomes`        | List income (`?month=YYYY-MM`, `?category=Salary`)   |
| GET    | `/api/incomes/paged`  | Paginated income                                     |
| POST   | `/api/incomes`        | Create income entry                                  |
| PUT    | `/api/incomes/{id}`   | Update income entry                                  |
| DELETE | `/api/incomes/{id}`   | Delete income entry                                  |

### Financial Summary

| Method | Endpoint        | Description                                                          |
|--------|-----------------|----------------------------------------------------------------------|
| GET    | `/api/summary`  | Full summary (`?startDate`, `?endDate`, `?projected=true`) — **Pro** |

### Subscription & Billing

| Method | Endpoint                       | Auth | Description                                        |
|--------|--------------------------------|------|----------------------------------------------------|
| GET    | `/api/subscription/status`     | Yes  | Returns current plan, status, and period end date  |
| POST   | `/api/subscription/checkout`   | Yes  | Creates a Stripe Checkout Session, returns `url`   |
| POST   | `/api/subscription/cancel`     | Yes  | Schedules cancellation at period end               |
| POST   | `/api/stripe/webhook`          | No   | Stripe webhook receiver (signature-verified)       |

### Spending Limits

| Method | Endpoint                    | Description                        |
|--------|-----------------------------|-------------------------------------|
| GET    | `/api/spending-limits`      | List all limits with live progress  |
| POST   | `/api/spending-limits`      | Create a spending limit             |
| PUT    | `/api/spending-limits/{id}` | Update a spending limit             |
| DELETE | `/api/spending-limits/{id}` | Delete a spending limit             |

### Recurring Transactions

| Method | Endpoint              | Description                           |
|--------|-----------------------|---------------------------------------|
| GET    | `/api/recurring`      | List all recurring rules              |
| POST   | `/api/recurring`      | Create a recurring rule               |
| PUT    | `/api/recurring/{id}` | Update or pause/resume a rule         |
| DELETE | `/api/recurring/{id}` | Delete a recurring rule               |

### Categories

| Method | Endpoint               | Description                                                |
|--------|------------------------|------------------------------------------------------------|
| GET    | `/api/categories`      | List categories (`?type=EXPENSE` or `?type=INCOME`)        |
| POST   | `/api/categories`      | Create a custom category                                   |
| DELETE | `/api/categories/{id}` | Delete a custom category (blocked if used in transactions) |

### Export

| Method | Endpoint                   | Description                     |
|--------|----------------------------|---------------------------------|
| GET    | `/api/export/all/csv`      | Export all transactions as CSV  |
| GET    | `/api/export/expenses/csv` | Export expenses as CSV          |
| GET    | `/api/export/incomes/csv`  | Export income as CSV            |

All export endpoints accept optional `?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` (default: last 6 months).

### Import

| Method | Endpoint               | Auth | Description                                         |
|--------|------------------------|------|-----------------------------------------------------|
| POST   | `/api/import/csv`      | Yes  | Import transactions from a `.csv` file (multipart)  |
| GET    | `/api/import/template` | No   | Download a sample CSV template                      |

CSV format: `Date,Title,Category,Amount,Type` — `Type` must be `EXPENSE` or `INCOME`.
Maximum file size: 5 MB. Invalid rows are skipped and reported individually in the response.

### Demo

| Method | Endpoint              | Auth | Description                                      |
|--------|-----------------------|------|--------------------------------------------------|
| POST   | `/api/demo/reset`     | No   | Reset Free demo account to seed data, return JWT |
| POST   | `/api/demo/reset-pro` | No   | Reset Pro demo account to seed data, return JWT  |

Rate limited to 5 requests per 60 seconds per IP.

---

## License

Copyright (c) 2025 John Kenneth Taruc

This project is published under the [Business Source License 1.1](LICENSE).
It may be freely used for personal, educational, and non-commercial purposes.
Commercial use and hosting as a competing service require the author's written permission.
The license converts automatically to MIT on 2029-03-05.