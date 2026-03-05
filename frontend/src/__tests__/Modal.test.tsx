import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Modal from '../components/Modal';

function renderModal(props: Partial<React.ComponentProps<typeof Modal>> = {}) {
  const defaults = {
    isOpen: true,
    onClose: vi.fn(),
    title: 'Test Modal',
    children: <p>Modal content</p>,
    ...props,
  };
  return { ...render(<Modal {...defaults} />), onClose: defaults.onClose };
}

describe('Modal', () => {
  beforeEach(() => {
    // Modal renders into #modal-root via a portal
    const modalRoot = document.createElement('div');
    modalRoot.setAttribute('id', 'modal-root');
    document.body.appendChild(modalRoot);
  });

  describe('rendering', () => {
    it('renders title and children when open', () => {
      renderModal();
      expect(screen.getByText('Test Modal')).toBeInTheDocument();
      expect(screen.getByText('Modal content')).toBeInTheDocument();
    });

    it('renders nothing when isOpen is false', () => {
      renderModal({ isOpen: false });
      expect(screen.queryByText('Test Modal')).toBeNull();
      expect(screen.queryByText('Modal content')).toBeNull();
    });

    it('renders a close button with aria-label', () => {
      renderModal();
      expect(screen.getByRole('button', { name: /close modal/i })).toBeInTheDocument();
    });
  });

  describe('close interactions', () => {
    it('calls onClose when the close button is clicked', async () => {
      const user = userEvent.setup();
      const { onClose } = renderModal();
      await user.click(screen.getByRole('button', { name: /close modal/i }));
      expect(onClose).toHaveBeenCalledOnce();
    });

    it('calls onClose when the overlay backdrop is clicked', async () => {
      const user = userEvent.setup();
      const { onClose } = renderModal();
      // The overlay is the outermost div rendered by the portal
      const overlay = document.querySelector('.modal-overlay') as HTMLElement;
      await user.click(overlay);
      expect(onClose).toHaveBeenCalledOnce();
    });

    it('does not call onClose when clicking inside the modal content', async () => {
      const user = userEvent.setup();
      const { onClose } = renderModal();
      await user.click(screen.getByText('Modal content'));
      expect(onClose).not.toHaveBeenCalled();
    });

    it('calls onClose when Escape key is pressed', async () => {
      const user = userEvent.setup();
      const { onClose } = renderModal();
      await user.keyboard('{Escape}');
      expect(onClose).toHaveBeenCalledOnce();
    });

    it('does not call onClose for other keys', async () => {
      const user = userEvent.setup();
      const { onClose } = renderModal();
      await user.keyboard('{Enter}');
      expect(onClose).not.toHaveBeenCalled();
    });
  });

  describe('body overflow', () => {
    it('sets body overflow to hidden when open', () => {
      renderModal({ isOpen: true });
      expect(document.body.style.overflow).toBe('hidden');
    });

    it('restores body overflow on unmount', () => {
      const { unmount } = renderModal({ isOpen: true });
      unmount();
      expect(document.body.style.overflow).toBe('unset');
    });
  });
});
