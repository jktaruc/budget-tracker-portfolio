import TransactionList from "./TransactionList";
import "../styles/ExpenseList.css";

interface ExpenseListProps {
    refresh: boolean;
    month?: string;
    category?: string;
}

export default function ExpenseList({ refresh, month, category }: ExpenseListProps) {
    return (
        <TransactionList
            endpoint="/expenses"
            categoryType="EXPENSE"
            refresh={refresh}
            month={month}
            category={category}
            emptyMessage="No expenses found."
            editModalTitle="Edit Expense"
        />
    );
}
