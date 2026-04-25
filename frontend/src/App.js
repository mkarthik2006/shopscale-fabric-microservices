import React from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import ProductsPage from './pages/ProductsPage';
import CartPage from './pages/CartPage';
import OrdersPage from './pages/OrdersPage';
import LoginPage from './pages/LoginPage';
import CreateProductPage from './pages/CreateProductPage';
import InventoryPage from './pages/InventoryPage';
import ToastHost from './components/ToastHost';
import Layout from './components/Layout';
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
  const location = useLocation();
  const claims = getTokenClaims();
  const roles = claims?.realm_access?.roles || [];
  const isAdmin = roles.includes('ADMIN') || roles.includes('ROLE_ADMIN');
  const isAuthenticated = hasValidSession();
  const user = claims
    ? {
        username: claims.preferred_username || claims.sub,
        name: claims.name || claims.preferred_username || claims.sub,
      }
    : null;

  const isAuthRoute = location.pathname === '/login';

  return (
    <>
      <Layout
        isAuthenticated={isAuthenticated}
        isAdmin={isAdmin}
        user={user}
        isAuthRoute={isAuthRoute}
      >
        <Routes>
          <Route path="/" element={<ProductsPage />} />
          <Route
            path="/products/new"
            element={
              <RoleRoute allowedRoles={['ADMIN', 'ROLE_ADMIN']}>
                <CreateProductPage />
              </RoleRoute>
            }
          />
          <Route
            path="/cart"
            element={
              <ProtectedRoute>
                <CartPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders"
            element={
              <ProtectedRoute>
                <OrdersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/inventory"
            element={
              <ProtectedRoute>
                <InventoryPage />
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </Layout>
      <ToastHost />
    </>
  );
}

export default App;
