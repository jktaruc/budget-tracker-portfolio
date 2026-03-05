import TransactionList from "./TransactionList";
import "../styles/IncomeList.css";

interface IncomeListProps {
    refresh: boolean;
    month?: string;
    category?: string;
}

export default function IncomeList({ refresh, month, category }: IncomeListProps) {
    return (
        <TransactionList
            endpoint="/incomes"
            categoryType="INCOME"
            refresh={refresh}
            month={month}
            category={category}
            emptyMessage="No income found."
            editModalTitle="Edit Income"
            cardClassName="income-card"
        />
    );
}
