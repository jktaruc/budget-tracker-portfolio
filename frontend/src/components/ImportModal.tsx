import { useState, useRef } from "react";
import api from "../api/api";
import Modal from "./Modal";
import "../styles/ImportModal.css";

interface ImportResult {
    importedExpenses: number;
    importedIncome: number;
    skippedRows: number;
    errors: string[];
}

interface PreviewRow {
    date: string;
    title: string;
    category: string;
    amount: string;
    type: string;
    valid: boolean;
    error?: string;
}

interface ImportModalProps {
    isOpen: boolean;
    onClose: () => void;
    /** Called after a successful import so the dashboard can refresh */
    onImported: () => void;
}

const MAX_PREVIEW_ROWS = 5;

export default function ImportModal({ isOpen, onClose, onImported }: ImportModalProps) {
    const [file, setFile] = useState<File | null>(null);
    const [preview, setPreview] = useState<PreviewRow[]>([]);
    const [totalRows, setTotalRows] = useState(0);
    const [uploading, setUploading] = useState(false);
    const [result, setResult] = useState<ImportResult | null>(null);
    const [error, setError] = useState<string | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const reset = () => {
        setFile(null);
        setPreview([]);
        setTotalRows(0);
        setResult(null);
        setError(null);
        if (fileInputRef.current) fileInputRef.current.value = "";
    };

    const handleClose = () => {
        reset();
        onClose();
    };

    /** Parse the selected file locally to show a preview before uploading. */
    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const selected = e.target.files?.[0];
        if (!selected) return;

        setFile(selected);
        setResult(null);
        setError(null);

        const reader = new FileReader();
        reader.onload = (ev) => {
            const text = ev.target?.result as string;
            const lines = text.split("\n").filter(l => l.trim());
            if (lines.length < 2) {
                setPreview([]);
                setTotalRows(0);
                return;
            }

            const dataLines = lines.slice(1); // skip header
            setTotalRows(dataLines.length);

            const parsed: PreviewRow[] = dataLines.slice(0, MAX_PREVIEW_ROWS).map(line => {
                const cols = parseCSVLine(line);
                if (cols.length < 5) {
                    return { date: "", title: line, category: "", amount: "", type: "", valid: false, error: "Expected 5 columns" };
                }
                const [date, title, category, amount, type] = cols.map(c => c.trim());
                const typeUpper = type.toUpperCase();
                const valid =
                    !!date && !!title && !!category &&
                    !isNaN(parseFloat(amount)) && parseFloat(amount) > 0 &&
                    (typeUpper === "EXPENSE" || typeUpper === "INCOME");
                return { date, title, category, amount, type: typeUpper, valid };
            });

            setPreview(parsed);
        };
        reader.readAsText(selected);
    };

    const handleUpload = async () => {
        if (!file) return;
        setUploading(true);
        setError(null);
        try {
            const formData = new FormData();
            formData.append("file", file);
            const { data } = await api.post<ImportResult>("/import/csv", formData, {
                headers: { "Content-Type": "multipart/form-data" },
            });
            setResult(data);
            if (data.importedExpenses + data.importedIncome > 0) {
                onImported();
            }
        } catch (err: any) {
            setError(err?.response?.data?.message ?? "Import failed. Please check your file and try again.");
        } finally {
            setUploading(false);
        }
    };

    const handleDownloadTemplate = () => {
        // Template endpoint is public — no auth needed
        const base = import.meta.env.VITE_API_BASE_URL || "/api";
        window.open(`${base}/import/template`, "_blank");
    };

    return (
        <Modal isOpen={isOpen} onClose={handleClose} title="📥 Import Transactions">
            <div className="import-modal">

                {/* Template download */}
                <div className="import-template-row">
                    <span className="import-template-hint">
                        Need a starting point? Download the CSV template:
                    </span>
                    <button className="import-template-btn" onClick={handleDownloadTemplate}>
                        ⬇ Download Template
                    </button>
                </div>

                <div className="import-format-note">
                    File must be a <strong>.csv</strong> with columns:{" "}
                    <code>Date, Title, Category, Amount, Type</code><br />
                    <span className="import-format-sub">
                        Date: YYYY-MM-DD &nbsp;·&nbsp; Amount: positive number &nbsp;·&nbsp; Type: EXPENSE or INCOME
                    </span>
                </div>

                {/* File picker */}
                {!result && (
                    <div className="import-pick-row">
                        <input
                            ref={fileInputRef}
                            type="file"
                            accept=".csv"
                            onChange={handleFileChange}
                            className="import-file-input"
                            id="import-file"
                        />
                        <label htmlFor="import-file" className="import-file-label">
                            {file ? `📄 ${file.name}` : "Choose CSV file…"}
                        </label>
                        {file && (
                            <button className="import-clear-btn" onClick={reset} title="Clear">✕</button>
                        )}
                    </div>
                )}

                {/* Preview */}
                {preview.length > 0 && !result && (
                    <div className="import-preview">
                        <p className="import-preview-label">
                            Preview — first {Math.min(MAX_PREVIEW_ROWS, totalRows)} of {totalRows} data row{totalRows !== 1 ? "s" : ""}
                        </p>
                        <div className="import-preview-table-wrap">
                            <table className="import-preview-table">
                                <thead>
                                    <tr>
                                        <th>Date</th>
                                        <th>Title</th>
                                        <th>Category</th>
                                        <th>Amount</th>
                                        <th>Type</th>
                                        <th></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {preview.map((row, i) => (
                                        <tr key={i} className={row.valid ? "" : "import-row-invalid"}>
                                            <td>{row.date}</td>
                                            <td>{row.title}</td>
                                            <td>{row.category}</td>
                                            <td>{row.amount}</td>
                                            <td>
                                                <span className={`import-type-badge ${row.type === "INCOME" ? "income" : "expense"}`}>
                                                    {row.type || "?"}
                                                </span>
                                            </td>
                                            <td className="import-row-status">
                                                {row.valid ? "✅" : <span title={row.error}>⚠️</span>}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        {totalRows > MAX_PREVIEW_ROWS && (
                            <p className="import-more-rows">
                                + {totalRows - MAX_PREVIEW_ROWS} more row{totalRows - MAX_PREVIEW_ROWS !== 1 ? "s" : ""} not shown
                            </p>
                        )}
                    </div>
                )}

                {/* Error */}
                {error && <p className="import-error">{error}</p>}

                {/* Result */}
                {result && (
                    <div className="import-result">
                        <div className="import-result-stats">
                            <div className="import-stat">
                                <span className="import-stat-value">{result.importedExpenses}</span>
                                <span className="import-stat-label">Expenses imported</span>
                            </div>
                            <div className="import-stat">
                                <span className="import-stat-value">{result.importedIncome}</span>
                                <span className="import-stat-label">Income imported</span>
                            </div>
                            {result.skippedRows > 0 && (
                                <div className="import-stat skipped">
                                    <span className="import-stat-value">{result.skippedRows}</span>
                                    <span className="import-stat-label">Rows skipped</span>
                                </div>
                            )}
                        </div>

                        {result.errors.length > 0 && (
                            <details className="import-errors-details">
                                <summary>Show skipped rows ({result.errors.length})</summary>
                                <ul className="import-error-list">
                                    {result.errors.map((e, i) => <li key={i}>{e}</li>)}
                                </ul>
                            </details>
                        )}

                        <div className="import-result-actions">
                            <button className="import-again-btn" onClick={reset}>Import another file</button>
                            <button className="import-done-btn" onClick={handleClose}>Done</button>
                        </div>
                    </div>
                )}

                {/* Upload button */}
                {file && !result && (
                    <div className="import-actions">
                        <button className="import-cancel-btn" onClick={handleClose} disabled={uploading}>
                            Cancel
                        </button>
                        <button className="import-submit-btn" onClick={handleUpload} disabled={uploading || !file}>
                            {uploading ? "Importing…" : `Import ${totalRows} row${totalRows !== 1 ? "s" : ""}`}
                        </button>
                    </div>
                )}
            </div>
        </Modal>
    );
}

/** Minimal RFC 4180 CSV line parser (matches backend parseCsvLine logic). */
function parseCSVLine(line: string): string[] {
    const fields: string[] = [];
    let current = "";
    let inQuotes = false;

    for (let i = 0; i < line.length; i++) {
        const c = line[i];
        if (inQuotes) {
            if (c === '"' && line[i + 1] === '"') { current += '"'; i++; }
            else if (c === '"') { inQuotes = false; }
            else { current += c; }
        } else {
            if (c === '"') { inQuotes = true; }
            else if (c === ',') { fields.push(current); current = ""; }
            else { current += c; }
        }
    }
    fields.push(current);
    return fields;
}
