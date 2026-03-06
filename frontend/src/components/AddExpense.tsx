import AddTransactionForm from "./AddTransactionForm";
import "../styles/AddExpense.css";

interface AddExpenseProps {
    onAdd?: () => void;
}

export default function AddExpense({ onAdd }: AddExpenseProps) {
    return (
        <AddTransactionForm
            endpoint="/expenses"
            categoryType="EXPENSE"
            defaultCategory="Food"
            submitLabel="Add Expense"
            formClassName="add-expense-form"
            inputClassName="add-expense-input"
            buttonClassName="add-expense-button"
            onAdd={onAdd}
        />
    );
}
