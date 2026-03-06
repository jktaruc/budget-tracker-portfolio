import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Pagination from '../components/pagination/Pagination';

// CSS import is a no-op in test environment (css: false in vitest config)

function renderPagination(overrides: Partial<React.ComponentProps<typeof Pagination>> = {}) {
  const defaults = {
    currentPage: 0,
    totalPages: 5,
    totalElements: 100,
    pageSize: 20,
    onPageChange: vi.fn(),
    ...overrides,
  };
  return { ...render(<Pagination {...defaults} />), onPageChange: defaults.onPageChange };
}

describe('Pagination', () => {
  describe('visibility', () => {
    it('renders nothing when totalPages is 1', () => {
      const { container } = renderPagination({ totalPages: 1 });
      expect(container.firstChild).toBeNull();
    });

    it('renders nothing when totalPages is 0', () => {
      const { container } = renderPagination({ totalPages: 0 });
      expect(container.firstChild).toBeNull();
    });

    it('renders when totalPages is greater than 1', () => {
      renderPagination({ totalPages: 3 });
      expect(screen.getByText(/Prev/i)).toBeInTheDocument();
    });
  });

  describe('info text', () => {
    it('shows correct range for first page', () => {
      renderPagination({ currentPage: 0, pageSize: 20, totalElements: 100 });
      expect(screen.getByText(/1.+20 of 100/)).toBeInTheDocument();
    });

    it('shows correct range for second page', () => {
      renderPagination({ currentPage: 1, pageSize: 20, totalElements: 100 });
      expect(screen.getByText(/21.+40 of 100/)).toBeInTheDocument();
    });

    it('clamps the "to" value at totalElements on the last page', () => {
      renderPagination({ currentPage: 4, pageSize: 20, totalElements: 95 });
      expect(screen.getByText(/81.+95 of 95/)).toBeInTheDocument();
    });
  });

  describe('button states', () => {
    it('disables Prev button on the first page', () => {
      renderPagination({ currentPage: 0 });
      expect(screen.getByText(/Prev/i)).toBeDisabled();
    });

    it('enables Prev button when not on the first page', () => {
      renderPagination({ currentPage: 2 });
      expect(screen.getByText(/Prev/i)).not.toBeDisabled();
    });

    it('disables Next button on the last page', () => {
      renderPagination({ currentPage: 4, totalPages: 5 });
      expect(screen.getByText(/Next/i)).toBeDisabled();
    });

    it('enables Next button when not on the last page', () => {
      renderPagination({ currentPage: 0, totalPages: 5 });
      expect(screen.getByText(/Next/i)).not.toBeDisabled();
    });
  });

  describe('navigation callbacks', () => {
    it('calls onPageChange with currentPage - 1 when Prev is clicked', async () => {
      const user = userEvent.setup();
      const { onPageChange } = renderPagination({ currentPage: 2 });
      await user.click(screen.getByText(/Prev/i));
      expect(onPageChange).toHaveBeenCalledWith(1);
    });

    it('calls onPageChange with currentPage + 1 when Next is clicked', async () => {
      const user = userEvent.setup();
      const { onPageChange } = renderPagination({ currentPage: 1 });
      await user.click(screen.getByText(/Next/i));
      expect(onPageChange).toHaveBeenCalledWith(2);
    });

    it('calls onPageChange with correct page when a numbered button is clicked', async () => {
      const user = userEvent.setup();
      // totalPages: 3 → pages 0,1,2 shown as buttons labelled 1,2,3
      const { onPageChange } = renderPagination({ currentPage: 0, totalPages: 3, totalElements: 60 });
      // Button "2" maps to page index 1
      await user.click(screen.getByRole('button', { name: '2' }));
      expect(onPageChange).toHaveBeenCalledWith(1);
    });
  });

  describe('page range (ellipsis)', () => {
    it('shows ellipsis when total pages exceed 7 and cursor is in the middle', () => {
      renderPagination({ currentPage: 5, totalPages: 12, totalElements: 240 });
      const ellipses = screen.getAllByText('…');
      expect(ellipses.length).toBeGreaterThanOrEqual(1);
    });

    it('shows all pages without ellipsis when totalPages is 7 or fewer', () => {
      renderPagination({ currentPage: 0, totalPages: 7, totalElements: 140 });
      // Buttons 1-7 should all be present, no ellipsis
      for (let i = 1; i <= 7; i++) {
        expect(screen.getByRole('button', { name: String(i) })).toBeInTheDocument();
      }
      expect(screen.queryByText('…')).toBeNull();
    });
  });
});
