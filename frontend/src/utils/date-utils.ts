const MONTH_LABELS = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];

/** "2025-03-15" → "03-15-2025" (used in transaction cards) */
export const formatDate = (isoDate: string | null | undefined): string => {
    if (!isoDate) return "No date";
    const [year, month, day] = isoDate.split("-");
    return `${month}-${day}-${year}`;
};

/** "2025-03-15" → "Mar 15, 2025" (used in summary lists) */
export const formatDisplayDate = (isoDate: string): string => {
    const date = new Date(isoDate);
    return `${MONTH_LABELS[date.getMonth()]} ${date.getDate()}, ${date.getFullYear()}`;
};

/** "2025-03" → "Mar 2025" (used in charts) */
export const formatMonthYear = (yearMonth: string): string => {
    const [year, month] = yearMonth.split("-");
    return `${MONTH_LABELS[parseInt(month) - 1]} ${year}`;
};

/** Today's date in YYYY-MM-DD format */
export const getTodayDate = (): string =>
    new Date().toISOString().split("T")[0];

/** Date N months ago in YYYY-MM-DD format */
export const getDateMonthsAgo = (months: number): string => {
    const date = new Date();
    date.setMonth(date.getMonth() - months);
    return date.toISOString().split("T")[0];
};

/** Format number as USD currency */
export const formatCurrency = (amount: number | null | undefined): string =>
    new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(amount ?? 0);

/** "2025-03-15" → "15 Mar" (used in spending limit window labels) */
export const formatShortDate = (isoDate: string): string =>
    new Date(isoDate).toLocaleDateString("en-AU", { day: "numeric", month: "short" });
