import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Badge from './Badge';
import EmptyState from './EmptyState';
import StatsCard from './StatsCard';
import Icon from './Icon';
import Logo from './Logo';
import Footer from './Footer';
import { SkeletonProductCard, SkeletonRow, SkeletonOrderCard } from './Skeleton';
import Sidebar from './Sidebar';
import TopBar from './TopBar';

describe('UI primitive components', () => {
  test('Badge renders with default neutral tone and label', () => {
    render(<Badge>Active</Badge>);
    expect(screen.getByText('Active')).toBeInTheDocument();
  });

  test('Badge supports tone variants', () => {
    const { rerender } = render(<Badge tone="success">In stock</Badge>);
    expect(screen.getByText('In stock').className).toMatch(/badge--success/);

    rerender(<Badge tone="danger" plain>Out</Badge>);
    expect(screen.getByText('Out').className).toMatch(/badge--plain/);
  });

  test('EmptyState renders title, description and optional action', () => {
    render(
      <EmptyState
        icon="package"
        title="Nothing here yet"
        description="Add some items to get started"
        action={<button type="button">Add</button>}
      />
    );
    expect(screen.getByText(/nothing here yet/i)).toBeInTheDocument();
    expect(screen.getByText(/add some items/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add/i })).toBeInTheDocument();
  });

  test('StatsCard renders label, value and delta', () => {
    render(
      <StatsCard
        icon="package"
        tone="brand"
        label="Total products"
        value={42}
        delta="+5 this week"
        deltaTone="up"
      />
    );
    expect(screen.getByText(/total products/i)).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText(/\+5 this week/i)).toBeInTheDocument();
  });

  test('Icon returns null for unknown icon names and renders SVG for known ones', () => {
    const { container, rerender } = render(<Icon name="this-is-not-a-real-icon" />);
    expect(container.firstChild).toBeNull();

    rerender(<Icon name="cart" />);
    expect(container.querySelector('svg')).not.toBeNull();
  });

  test('Logo renders brand mark and optional text', () => {
    const { rerender, container } = render(<Logo />);
    expect(container.querySelector('svg')).not.toBeNull();

    rerender(<Logo withText />);
    expect(screen.getByText(/shopscale/i)).toBeInTheDocument();
    expect(screen.getByText(/fabric/i)).toBeInTheDocument();
  });

  test('Footer renders the brand and copyright', () => {
    render(<Footer />);
    const matches = screen.getAllByText(/shopscale fabric/i);
    expect(matches.length).toBeGreaterThan(0);
    expect(screen.getByText(new RegExp(`© ${new Date().getFullYear()}`))).toBeInTheDocument();
  });

  test('Skeleton placeholders render shimmer surfaces', () => {
    const { container: c1 } = render(<SkeletonProductCard />);
    expect(c1.querySelectorAll('.skeleton').length).toBeGreaterThan(0);

    const { container: c2 } = render(
      <table>
        <tbody>
          <SkeletonRow columns={3} />
        </tbody>
      </table>
    );
    expect(c2.querySelectorAll('.skeleton').length).toBe(3);

    const { container: c3 } = render(<SkeletonOrderCard />);
    expect(c3.querySelectorAll('.skeleton').length).toBeGreaterThan(0);
  });

  test('Sidebar renders authenticated user card and admin link', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <Sidebar
          isAuthenticated
          isAdmin
          user={{ name: 'Admin User', username: 'admin' }}
          isOpen={false}
          onClose={() => {}}
        />
      </MemoryRouter>
    );
    expect(screen.getByText(/products/i)).toBeInTheDocument();
    expect(screen.getByText(/cart/i)).toBeInTheDocument();
    expect(screen.getByText(/inventory/i)).toBeInTheDocument();
    expect(screen.getByText(/create product/i)).toBeInTheDocument();
    expect(screen.getByText(/administrator/i)).toBeInTheDocument();
  });

  test('Sidebar shows sign-in link when unauthenticated', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <Sidebar
          isAuthenticated={false}
          isAdmin={false}
          user={null}
          isOpen={false}
          onClose={() => {}}
        />
      </MemoryRouter>
    );
    expect(screen.getAllByText(/sign in/i).length).toBeGreaterThan(0);
  });

  test('TopBar renders breadcrumb and search field', () => {
    render(
      <MemoryRouter initialEntries={['/cart']}>
        <TopBar
          isAuthenticated
          isAdmin={false}
          user={{ username: 'testuser', name: 'Test User' }}
          onMenuClick={() => {}}
        />
      </MemoryRouter>
    );
    expect(screen.getByPlaceholderText(/search products/i)).toBeInTheDocument();
    expect(screen.getByText(/shopping cart/i)).toBeInTheDocument();
  });

  test('TopBar shows sign-in button when unauthenticated', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <TopBar
          isAuthenticated={false}
          isAdmin={false}
          user={null}
          onMenuClick={() => {}}
        />
      </MemoryRouter>
    );
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });
});
