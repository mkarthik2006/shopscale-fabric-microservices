import React from 'react';
import { Routes, Route, Link, Navigate } from 'react-router-dom';
import ProductsPage from './pages/ProductsPage';
import CartPage from './pages/CartPage';
import OrdersPage from './pages/OrdersPage';
import LoginPage from './pages/LoginPage';
import CreateProductPage from './pages/CreateProductPage';
import InventoryPage from './pages/InventoryPage';
import ToastHost from './components/ToastHost';
import { getTokenClaims, hasValidSession } from './services/api';

function ProtectedRoute({ children }) {
  if (!hasValidSession()) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

function RoleRoute({ children, allowedRoles }) {
  if (!hasValidSession()) {
    return <Navigate to="/login" replace />;
  }
  const roles = getTokenClaims()?.realm_access?.roles || [];
  const allowed = allowedRoles.some((role) => roles.includes(role));
  if (!allowed) {
    return <Navigate to="/" replace />;
  }
  return children;
}

function App() {
  const roles = getTokenClaims()?.realm_access?.roles || [];
  const isAdmin = roles.includes('ADMIN') || roles.includes('ROLE_ADMIN');

  return (
    <div className="app-shell">
      <nav className="top-nav">
        <strong className="brand">ShopScale Fabric</strong>
        <Link to="/" className="nav-link">Products</Link>
        {isAdmin && <Link to="/products/new" className="nav-link">Create Product</Link>}
        <Link to="/cart" className="nav-link">Cart</Link>
        <Link to="/orders" className="nav-link">Orders</Link>
        {hasValidSession() && <Link to="/inventory" className="nav-link">Inventory</Link>}
        <Link to="/login" className="nav-link">Login</Link>
      </nav>
      <main className="container">
        <Routes>
          <Route path="/" element={<ProductsPage />} />
          <Route path="/products/new" element={<RoleRoute allowedRoles={['ADMIN', 'ROLE_ADMIN']}><CreateProductPage /></RoleRoute>} />
          <Route path="/cart" element={<ProtectedRoute><CartPage /></ProtectedRoute>} />
          <Route path="/orders" element={<ProtectedRoute><OrdersPage /></ProtectedRoute>} />
          <Route path="/inventory" element={<ProtectedRoute><InventoryPage /></ProtectedRoute>} />
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </main>
      <ToastHost />
    </div>
  );
}

export default App;