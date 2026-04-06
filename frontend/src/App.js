import React from 'react';
import { Routes, Route, Link } from 'react-router-dom';
import ProductsPage from './pages/ProductsPage';
import CartPage from './pages/CartPage';
import OrdersPage from './pages/OrdersPage';
import LoginPage from './pages/LoginPage';

const navStyle = {
  display: 'flex', gap: '20px', padding: '16px 24px',
  background: '#1a1a2e', color: '#fff', alignItems: 'center',
};
const linkStyle = { color: '#e94560', textDecoration: 'none', fontWeight: 'bold' };

function App() {
  return (
    <div>
      <nav style={navStyle}>
        <strong style={{ fontSize: '1.2rem', marginRight: 'auto' }}>🛒 ShopScale Fabric</strong>
        <Link to="/" style={linkStyle}>Products</Link>
        <Link to="/cart" style={linkStyle}>Cart</Link>
        <Link to="/orders" style={linkStyle}>Orders</Link>
        <Link to="/login" style={linkStyle}>Login</Link>
      </nav>
      <div style={{ maxWidth: '1200px', margin: '24px auto', padding: '0 16px' }}>
        <Routes>
          <Route path="/" element={<ProductsPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/orders" element={<OrdersPage />} />
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </div>
    </div>
  );
}

export default App;