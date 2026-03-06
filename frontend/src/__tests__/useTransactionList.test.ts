import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { useTransactionList } from '../hooks/useTransactionList';
import api from '../api/api';

vi.mock('../api/api', () => ({
  default: {
    get: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockGet = vi.mocked(api.get);
const mockDelete = vi.mocked(api.delete);

const sampleItems = [
  { id: '1', title: 'Groceries', category: 'Food', amount: 85.0, date: '2025-03-10' },
  { id: '2', title: 'Netflix', category: 'Entertainment', amount: 22.99, date: '2025-03-01' },
];

describe('useTransactionList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Restore window.confirm after each test
    vi.restoreAllMocks();
  });

  describe('initial fetch', () => {
    it('starts with empty items and isLoading true, then resolves', async () => {
      mockGet.mockResolvedValueOnce({ data: sampleItems });

      const { result } = renderHook(() =>
          useTransactionList({ endpoint: '/expenses', refresh: false })
      );

      expect(result.current.isLoading).toBe(true);

      await waitFor(() => expect(result.current.isLoading).toBe(false));

      expect(result.current.items).toHaveLength(2);
      expect(result.current.items[0].title).toBe('Groceries');
    });

    it('passes month and category as query params when provided', async () => {
      mockGet.mockResolvedValueOnce({ data: [] });

      renderHook(() =>
          useTransactionList({
            endpoint: '/expenses',
            refresh: false,
            month: '2025-03',
            category: 'Food',
          })
      );

      await waitFor(() => {
        expect(mockGet).toHaveBeenCalledWith('/expenses', {
          params: { month: '2025-03', category: 'Food' },
        });
      });
    });

    it('omits params when month and category are not provided', async () => {
      mockGet.mockResolvedValueOnce({ data: [] });

      renderHook(() =>
          useTransactionList({ endpoint: '/expenses', refresh: false })
      );

      await waitFor(() => {
        expect(mockGet).toHaveBeenCalledWith('/expenses', { params: {} });
      });
    });

    it('sets an error message when the fetch fails', async () => {
      mockGet.mockRejectedValueOnce(new Error('Network error'));

      const { result } = renderHook(() =>
          useTransactionList({ endpoint: '/expenses', refresh: false })
      );

      await waitFor(() => expect(result.current.isLoading).toBe(false));

      expect(result.current.error).toMatch(/failed to load/i);
      expect(result.current.items).toHaveLength(0);
    });
  });

  describe('refetching', () => {
    it('refetches when the refresh flag changes', async () => {
      mockGet.mockResolvedValue({ data: sampleItems });

      const { rerender } = renderHook(
          ({ refresh }) => useTransactionList({ endpoint: '/expenses', refresh }),
          { initialProps: { refresh: false } }
      );

      await waitFor(() => expect(mockGet).toHaveBeenCalledTimes(1));

      rerender({ refresh: true });

      await waitFor(() => expect(mockGet).toHaveBeenCalledTimes(2));
    });
  });

  describe('updateItem', () => {
    it('replaces the updated item in the list by id', async () => {
      mockGet.mockResolvedValueOnce({ data: sampleItems });

      const { result } = renderHook(() =>
          useTransactionList({ endpoint: '/expenses', refresh: false })
      );

      await waitFor(() => expect(result.current.items).toHaveLength(2));

      act(() => {
        result.current.updateItem({ id: '1', title: 'Updated', category: 'Food', amount: 90, date: '2025-03-10' });
      });

      expect(result.current.items[0].title).toBe('Updated');
      expect(result.current.items[1].title).toBe('Netflix');
    });
  });

  describe('deleteItem', () => {
    it('removes the item from the list after a confirmed delete', async () => {
      vi.spyOn(window, 'confirm').mockReturnValue(true);

      mockGet.mockResolvedValueOnce({ data: sampleItems });
      mockDelete.mockResolvedValueOnce({});

      const { result } = renderHook(() =>
          useTransactionList({ endpoint: '/expenses', refresh: false })
      );

      await waitFor(() => expect(result.current.items).toHaveLength(2));

      vi.useFakeTimers();

      await act(async () => {
        await result.current.deleteItem(sampleItems[0]);
        vi.runAllTimers();
      });

      vi.useRealTimers();

      expect(result.current.items).toHaveLength(1);
      expect(result.current.items[0].id).toBe('2');
    });

    it('does not call the API when the user cancels the confirm dialog', async () => {
      vi.spyOn(window, 'confirm').mockReturnValue(false);
      mockGet.mockResolvedValueOnce({ data: sampleItems });

      const { result } = renderHook(() =>
          useTransactionList({ endpoint: '/expenses', refresh: false })
      );

      await waitFor(() => expect(result.current.items).toHaveLength(2));

      await act(async () => {
        await result.current.deleteItem(sampleItems[0]);
      });

      expect(mockDelete).not.toHaveBeenCalled();
      expect(result.current.items).toHaveLength(2);
    });
  });
});