import "../../styles/Pagination.css";

interface PaginationProps {
    currentPage: number;        // 0-based (Spring Data style)
    totalPages: number;
    totalElements: number;
    pageSize: number;
    onPageChange: (page: number) => void;
}

export default function Pagination({
    currentPage, totalPages, totalElements, pageSize, onPageChange
}: PaginationProps) {
    if (totalPages <= 1) return null;

    const from = currentPage * pageSize + 1;
    const to   = Math.min((currentPage + 1) * pageSize, totalElements);

    const pages = buildPageRange(currentPage, totalPages);

    return (
        <div className="pagination">
            <span className="pagination-info">
                Showing {from}–{to} of {totalElements}
            </span>
            <div className="pagination-controls">
                <button
                    className="page-btn"
                    disabled={currentPage === 0}
                    onClick={() => onPageChange(currentPage - 1)}
                >
                    ‹ Prev
                </button>

                {pages.map((p, i) =>
                    p === "..." ? (
                        <span key={`ellipsis-${i}`} className="page-ellipsis">…</span>
                    ) : (
                        <button
                            key={p}
                            className={`page-btn ${p === currentPage ? "active" : ""}`}
                            onClick={() => onPageChange(p as number)}
                        >
                            {(p as number) + 1}
                        </button>
                    )
                )}

                <button
                    className="page-btn"
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => onPageChange(currentPage + 1)}
                >
                    Next ›
                </button>
            </div>
        </div>
    );
}

function buildPageRange(current: number, total: number): (number | "...")[] {
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    const pages: (number | "...")[] = [];
    if (current <= 3) {
        pages.push(0, 1, 2, 3, 4, "...", total - 1);
    } else if (current >= total - 4) {
        pages.push(0, "...", total - 5, total - 4, total - 3, total - 2, total - 1);
    } else {
        pages.push(0, "...", current - 1, current, current + 1, "...", total - 1);
    }
    return pages;
}
