// ── Category types ───────────────────────────────────────────────────────────

export type CategoryType = "EXPENSE" | "INCOME";

export interface Category {
  id: string;
  name: string;
  type: CategoryType;
  isCustom: boolean; // true = user-created (can be deleted), false = global default
}

// ── Core domain types ────────────────────────────────────────────────────────

/** A single financial entry (expense or income) returned from the API. */
export interface TransactionEntry {
  id: string;
  title: string;
  category: string;
  amount: number;
  date: string;
}


/** Form state for creating/editing a transaction entry (amount kept as string for input binding). */
export interface TransactionFormData {
  title: string;
  category: string;
  amount: string;
  date: string;
}


// ── Spending Limits (formerly Budget Goals) ──────────────────────────────────

export type SpendingLimitPeriod = "WEEKLY" | "BI_WEEKLY" | "MONTHLY" | "YEARLY";

export interface SpendingLimit {
  id: string;
  category: string;
  limitAmount: number;
  period: SpendingLimitPeriod;
  startDate: string | null;
  spent: number;
  remaining: number;
  percentageUsed: number;
  exceeded: boolean;
  windowStart: string;
  windowEnd: string;
}

// ── Recurring ────────────────────────────────────────────────────────────────

export type RecurringFrequency = "DAILY" | "WEEKLY" | "BI_WEEKLY" | "MONTHLY" | "YEARLY";

export interface RecurringTransaction {
  id: string;
  title: string;
  category: string;
  amount: number;
  type: "EXPENSE" | "INCOME";
  frequency: RecurringFrequency;
  startDate: string;
  endDate?: string;
  lastProcessed?: string;
  active: boolean;
}

// ── Summary / chart types ────────────────────────────────────────────────────

export interface FinancialSummary {
  totalIncome: number;
  totalExpenses: number;
  netBalance: number;
  savingsRate: number;
  totalIncomeTransactions: number;
  totalExpenseTransactions: number;
}

export interface MonthlyData {
  month: string;
  totalIncome: number;
  totalExpenses: number;
  netBalance: number;
}

export type TransactionType = "INCOME" | "EXPENSE";

export interface Transaction {
  id: string;
  title: string;
  category: string;
  amount: number;
  date: string;
  type: TransactionType;
}

export interface CategorySummary {
  [category: string]: number;
}

export interface ChartDataPoint {
  name: string;
  value: number;
}

export interface FormattedMonthlyData {
  monthLabel: string;
  income: number;
  expenses: number;
  net: number;
}
