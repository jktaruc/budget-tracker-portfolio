import {
    LineChart, Line, XAxis, YAxis, CartesianGrid,
    Tooltip, Legend, ResponsiveContainer,
} from "recharts";
import { formatMonthYear } from "../../utils/date-utils";
import type { MonthlyData, FormattedMonthlyData } from "../../types";

interface IncomeVsExpenseChartProps {
    data: MonthlyData[];
}

export default function IncomeVsExpenseChart({ data }: IncomeVsExpenseChartProps) {
    const formattedData: FormattedMonthlyData[] = data.map((item) => ({
        monthLabel: formatMonthYear(item.month),
        income: item.totalIncome,
        expenses: item.totalExpenses,
        net: item.netBalance,
    }));

    if (formattedData.length === 0) {
        return <div className="no-chart-data">No monthly data available</div>;
    }

    return (
        <ResponsiveContainer width="100%" height={400}>
            <LineChart data={formattedData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="monthLabel" angle={-45} textAnchor="end" height={80} />
                <YAxis tickFormatter={(v: number) => `$${v}`} />
                <Tooltip
                    formatter={(value) => `$${Number(value).toFixed(2)}`}
                    labelFormatter={(label) => `Month: ${label}`}
                />
                <Legend />
                <Line type="monotone" dataKey="income"   stroke="#4caf50" strokeWidth={3} name="Income"      activeDot={{ r: 8 }} />
                <Line type="monotone" dataKey="expenses" stroke="#f44336" strokeWidth={3} name="Expenses"    activeDot={{ r: 8 }} />
                <Line type="monotone" dataKey="net"      stroke="#2196f3" strokeWidth={2} name="Net Balance" strokeDasharray="5 5" />
            </LineChart>
        </ResponsiveContainer>
    );
}
