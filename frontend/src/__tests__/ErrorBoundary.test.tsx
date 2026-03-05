import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ErrorBoundary from '../components/ErrorBoundary';

// A component that throws on first render
function Bomb({ shouldThrow }: { shouldThrow: boolean }) {
  if (shouldThrow) throw new Error('Test error from Bomb');
  return <p>No explosion</p>;
}

describe('ErrorBoundary', () => {
  // Suppress React's console.error output for expected errors
  let consoleErrorSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  describe('normal rendering', () => {
    it('renders children when no error is thrown', () => {
      render(
        <ErrorBoundary>
          <p>All good</p>
        </ErrorBoundary>
      );
      expect(screen.getByText('All good')).toBeInTheDocument();
    });
  });

  describe('error handling', () => {
    it('renders the default error UI when a child throws', () => {
      render(
        <ErrorBoundary>
          <Bomb shouldThrow />
        </ErrorBoundary>
      );
      expect(screen.getByText(/something went wrong/i)).toBeInTheDocument();
    });

    it('shows the thrown error message in the default error UI', () => {
      render(
        <ErrorBoundary>
          <Bomb shouldThrow />
        </ErrorBoundary>
      );
      expect(screen.getByText(/Test error from Bomb/i)).toBeInTheDocument();
    });

    it('renders a custom fallback prop instead of the default UI', () => {
      render(
        <ErrorBoundary fallback={<p>Custom fallback</p>}>
          <Bomb shouldThrow />
        </ErrorBoundary>
      );
      expect(screen.getByText('Custom fallback')).toBeInTheDocument();
      expect(screen.queryByText(/something went wrong/i)).toBeNull();
    });

    it('does not render children after an error', () => {
      render(
        <ErrorBoundary>
          <Bomb shouldThrow />
        </ErrorBoundary>
      );
      expect(screen.queryByText('No explosion')).toBeNull();
    });
  });

  describe('reload button', () => {
    it('renders a reload button in the error state', () => {
      render(
        <ErrorBoundary>
          <Bomb shouldThrow />
        </ErrorBoundary>
      );
      expect(screen.getByRole('button', { name: /reload/i })).toBeInTheDocument();
    });

    it('calls window.location.reload when the reload button is clicked', async () => {
      const user = userEvent.setup();
      const reloadSpy = vi.fn();
      Object.defineProperty(window, 'location', {
        value: { ...window.location, reload: reloadSpy },
        writable: true,
      });

      render(
        <ErrorBoundary>
          <Bomb shouldThrow />
        </ErrorBoundary>
      );

      await user.click(screen.getByRole('button', { name: /reload/i }));
      expect(reloadSpy).toHaveBeenCalledOnce();
    });
  });
});
