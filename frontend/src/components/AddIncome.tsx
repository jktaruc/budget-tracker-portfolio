import AddTransactionForm from "./AddTransactionForm";
import "../styles/AddIncome.css";

interface AddIncomeProps {
    onAdd?: () => void;
}

export default function AddIncome({ onAdd }: AddIncomeProps) {
    return (
        <AddTransactionForm
            endpoint="/incomes"
            categoryType="INCOME"
            defaultCategory="Salary"
            submitLabel="Add Income"
            formClassName="add-income-form"
            inputClassName="add-income-input"
            buttonClassName="add-income-button"
            onAdd={onAdd}
        />
    );
}
