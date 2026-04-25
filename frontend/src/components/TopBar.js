import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Icon from './Icon';

const ROUTE_TITLES = {
  '/': { title: 'Product Catalogue', crumb: 'Storefront' },
  '/cart': { title: 'Shopping Cart', crumb: 'Storefront' },
  '/orders': { title: 'My Orders', crumb: 'Storefront' },
  '/inventory': { title: 'Inventory', crumb: 'Operations' },
  '/products/new': { title: 'Create Product', crumb: 'Operations' },
  '/login': { title: 'Sign In', crumb: 'Account' },
};

function TopBar({ isAuthenticated, isAdmin, user, onMenuClick }) {
  const location = useLocation();
  const navigate = useNavigate();
  const meta = ROUTE_TITLES[location.pathname] || { title: 'ShopScale Fabric', crumb: 'Console' };
  const initials = (user?.name || user?.username || 'GU').slice(0, 2).toUpperCase();

  return (
    <header className="topbar" role="banner">
      <button
        type="button"
        className="topbar__icon-btn"
        aria-label="Toggle navigation"
        onClick={onMenuClick}
        style={{ display: 'none' }}
        data-mobile-only
      >
        <Icon name="menu" size={18} />
      </button>

      <div>
        <div className="topbar__breadcrumb">
          <span>{meta.crumb}</span>
          <span className="topbar__breadcrumb-sep">/</span>
          <span>{meta.title}</span>
        </div>
      </div>

      <div className="topbar__spacer" />

      <div className="topbar__search" role="search">
        <span className="topbar__search-icon">
          <Icon name="search" size={16} />
        </span>
        <input
          type="search"
          aria-label="Quick search"
          placeholder="Search products, orders, SKUs..."
          onKeyDown={(e) => {
            if (e.key === 'Enter') navigate('/');
          }}
        />
        <kbd className="topbar__kbd">⌘K</kbd>
      </div>

      <button type="button" className="topbar__icon-btn" aria-label="Notifications">
        <Icon name="bell" size={18} />
      </button>

      {isAuthenticated ? (
        <div className="topbar__profile" aria-label="User profile">
          <span className="topbar__profile-avatar" aria-hidden="true">{initials}</span>
          <span style={{ display: 'flex', flexDirection: 'column' }}>
            <span className="topbar__profile-name">{user?.name || user?.username || 'Signed in'}</span>
            <span className="topbar__profile-role">{isAdmin ? 'Administrator' : 'Customer'}</span>
          </span>
        </div>
      ) : (
        <button type="button" className="btn btn-primary btn-sm" onClick={() => navigate('/login')}>
          <Icon name="login" size={14} /> Sign in
        </button>
      )}

      <style>{`
        @media (max-width: 960px) {
          [data-mobile-only] { display: inline-flex !important; }
        }
      `}</style>
    </header>
  );
}

export default TopBar;
