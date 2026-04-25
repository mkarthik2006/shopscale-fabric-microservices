import React from 'react';
import { NavLink } from 'react-router-dom';
import Icon from './Icon';
import Logo from './Logo';

function SidebarLink({ to, end, icon, label, badge }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) => `sidebar__link${isActive ? ' sidebar__link--active' : ''}`}
    >
      <Icon name={icon} size={18} />
      <span>{label}</span>
      {badge && <span className="sidebar__link-badge">{badge}</span>}
    </NavLink>
  );
}

function Sidebar({ isAdmin, isAuthenticated, user, isOpen, onClose }) {
  const initials = (user?.name || user?.username || 'U').slice(0, 2).toUpperCase();

  return (
    <>
      {isOpen && <div className="sidebar__backdrop" onClick={onClose} aria-hidden="true" />}
      <aside className={`sidebar${isOpen ? ' sidebar--open' : ''}`} aria-label="Primary">
        <div className="sidebar__brand">
          <Logo size={32} withText />
        </div>

        <div className="sidebar__section-label">Storefront</div>
        <nav className="sidebar__nav" onClick={onClose}>
          <SidebarLink to="/" end icon="products" label="Products" />
          <SidebarLink to="/cart" icon="cart" label="Cart" />
          <SidebarLink to="/orders" icon="orders" label="My Orders" />
        </nav>

        {isAuthenticated && (
          <>
            <div className="sidebar__section-label">Operations</div>
            <nav className="sidebar__nav" onClick={onClose}>
              <SidebarLink to="/inventory" icon="inventory" label="Inventory" />
              {isAdmin && (
                <SidebarLink to="/products/new" icon="add" label="Create Product" badge="Admin" />
              )}
            </nav>
          </>
        )}

        <div className="sidebar__footer">
          {isAuthenticated ? (
            <div className="sidebar__user-card">
              <span className="sidebar__user-avatar">{initials}</span>
              <span className="sidebar__user-meta">
                <span className="sidebar__user-name">{user?.name || user?.username || 'Signed in'}</span>
                <span className="sidebar__user-role">{isAdmin ? 'Administrator' : 'Customer'}</span>
              </span>
            </div>
          ) : (
            <NavLink to="/login" className="sidebar__link" onClick={onClose}>
              <Icon name="login" size={18} />
              <span>Sign in</span>
            </NavLink>
          )}
        </div>
      </aside>
    </>
  );
}

export default Sidebar;
