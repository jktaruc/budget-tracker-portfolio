import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProtectedRoute from '../components/ProtectedRoute';
import * as AuthContextModule from '../context/AuthContext';

function renderWithRouter(ui: React.ReactElement, initialPath = '/') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      {ui}
    </MemoryRouter>
  );
}

describe('ProtectedRoute', () => {
  it('renders children when user is authenticated', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: { email: 'alice@example.com', name: 'Alice' },
      isLoading: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      storeSession: vi.fn(),
    });

    renderWithRouter(
      <ProtectedRoute>
        <p>Protected content</p>
      </ProtectedRoute>
    );

    expect(screen.getByText('Protected content')).toBeInTheDocument();
  });

  it('redirects to /demo when user is null', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: null,
      isLoading: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      storeSession: vi.fn(),
    });

    renderWithRouter(
      <ProtectedRoute>
        <p>Protected content</p>
      </ProtectedRoute>
    );

    expect(screen.queryByText('Protected content')).toBeNull();
  });

  it('shows a loading spinner while auth state is resolving', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: null,
      isLoading: true,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      storeSession: vi.fn(),
    });

    renderWithRouter(
      <ProtectedRoute>
        <p>Protected content</p>
      </ProtectedRoute>
    );

    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.queryByText('Protected content')).toBeNull();
  });
});
