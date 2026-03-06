import "../../styles/DateRangePicker.css";
import { getTodayDate } from "../../utils/date-utils";

export interface DateRange {
    startDate: string;
    endDate: string;
}

type Preset = "thisMonth" | "lastMonth" | "last3Months" | "last6Months" | "thisYear";

interface DateRangePickerProps {
    dateRange: DateRange;
    onChange: (range: DateRange) => void;
}

function toISODate(date: Date): string {
    return date.toISOString().split("T")[0];
}

const PRESETS: { label: string; value: Preset }[] = [
    { label: "This Month",   value: "thisMonth" },
    { label: "Last Month",   value: "lastMonth" },
    { label: "Last 3 Months", value: "last3Months" },
    { label: "Last 6 Months", value: "last6Months" },
    { label: "This Year",    value: "thisYear" },
];

function resolvePreset(preset: Preset): DateRange {
    const today = new Date();
    const endDate = getTodayDate();

    switch (preset) {
        case "thisMonth":
            return { startDate: toISODate(new Date(today.getFullYear(), today.getMonth(), 1)), endDate };
        case "lastMonth":
            return {
                startDate: toISODate(new Date(today.getFullYear(), today.getMonth() - 1, 1)),
                endDate:   toISODate(new Date(today.getFullYear(), today.getMonth(), 0)),
            };
        case "last3Months":
            return { startDate: toISODate(new Date(today.getFullYear(), today.getMonth() - 3, today.getDate())), endDate };
        case "last6Months":
            return { startDate: toISODate(new Date(today.getFullYear(), today.getMonth() - 6, today.getDate())), endDate };
        case "thisYear":
            return { startDate: toISODate(new Date(today.getFullYear(), 0, 1)), endDate };
    }
}

export default function DateRangePicker({ dateRange, onChange }: DateRangePickerProps) {
    const handleCustomChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        onChange({ ...dateRange, [name]: value });
    };

    return (
        <div className="date-range-picker">
            <div className="preset-buttons">
                {PRESETS.map(({ label, value }) => (
                    <button key={value} onClick={() => onChange(resolvePreset(value))}>{label}</button>
                ))}
            </div>
            <div className="custom-dates">
                <label>
                    From:
                    <input type="date" name="startDate" value={dateRange.startDate} onChange={handleCustomChange} />
                </label>
                <label>
                    To:
                    <input type="date" name="endDate" value={dateRange.endDate} onChange={handleCustomChange} />
                </label>
            </div>
        </div>
    );
}
