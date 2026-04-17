import React from 'react';
import { Routes, Route, Link } from 'react-router-dom';
import ProductsPage from './pages/ProductsPage';
import CartPage from './pages/CartPage';
import OrdersPage from './pages/OrdersPage';
import LoginPage from './pages/LoginPage';
import CreateProductPage from './pages/CreateProductPage';
import InventoryPage from './pages/InventoryPage';
import ToastHost from './components/ToastHost';

function App() {
  return (
    <div className="app-shell">
      <nav className="top-nav">
        <strong className="brand">ShopScale Fabric</strong>
        <Link to="/" className="nav-link">Products</Link>
        <Link to="/products/new" className="nav-link">Create Product</Link>
        <Link to="/cart" className="nav-link">Cart</Link>
        <Link to="/orders" className="nav-link">Orders</Link>
        <Link to="/inventory" className="nav-link">Inventory</Link>
        <Link to="/login" className="nav-link">Login</Link>
      </nav>
      <main className="container">
        <Routes>
          <Route path="/" element={<ProductsPage />} />
          <Route path="/products/new" element={<CreateProductPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/orders" element={<OrdersPage />} />
          <Route path="/inventory" element={<InventoryPage />} />
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </main>
      <ToastHost />
    </div>
  );
}

export default App;