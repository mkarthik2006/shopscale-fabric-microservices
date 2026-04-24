import React, { useEffect } from 'react';
import useStore from '../store/useStore';
import api from '../services/api';
import demoProducts from '../data/demoProducts';
import { showErrorToast, showSuccessToast } from '../utils/toast';
import { getAccessToken, getApiErrorMessage, getTokenClaims, isTokenExpired } from '../services/api';

function ProductsPage() {
  const { products, productsLoading, productsError, fetchProducts, addToCart } = useStore();

  useEffect(() => { fetchProducts(); }, [fetchProducts]);
  const hasValidSession = Boolean(getAccessToken()) && !isTokenExpired();
  const tokenClaims = getTokenClaims();
  const realmRoles = tokenClaims?.realm_access?.roles || [];
  const isAdmin = realmRoles.includes('ADMIN') || realmRoles.includes('ROLE_ADMIN');
  const formatCurrency = (amount) => new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(Number(amount || 0));

  const stockClass = (stock) => {
    if (!stock || stock <= 0) return 'stock-badge stock-out';
    if (stock < 10) return 'stock-badge stock-low';
    return 'stock-badge stock-in';
  };

  const stockLabel = (stock) => {
    if (!stock || stock <= 0) return 'Out of stock';
    if (stock < 10) return `Low stock (${stock})`;
    return `In stock (${stock})`;
  };

  const seedDemoProducts = async () => {
    let created = 0;
    let failures = 0;
    let lastFailure = '';
    for (const product of demoProducts) {
      try {
        await api.post('/api/products', product);
        created += 1;
      } catch (err) {
        if (err?.response?.status !== 409) {
          failures += 1;
          lastFailure = getApiErrorMessage(err);
        }
      }
    }
    await fetchProducts();
    if (created > 0) {
      showSuccessToast(`Added ${created} demo products`);
    }
    if (failures > 0) {
      showErrorToast(`Failed to add ${failures} products: ${lastFailure}`);
    } else {
      if (created === 0) {
        showErrorToast('Demo products already exist.');
      }
    }
  };

  return (
    <div>
      <div className="page-header">
        <h2 style={{ margin: 0 }}>Product Catalogue</h2>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-primary" onClick={fetchProducts} disabled={productsLoading}>
            {productsLoading ? 'Refreshing...' : 'Refresh'}
          </button>
          {hasValidSession && isAdmin && (
            <button className="btn btn-success" onClick={seedDemoProducts} disabled={productsLoading}>
              Load Demo Products
            </button>
          )}
        </div>
      </div>
      {productsLoading && (
        <div className="grid">
          {Array.from({ length: 6 }).map((_, idx) => (
            <div key={idx} className="card">
              <div className="skeleton skeleton-title" />
              <div className="skeleton skeleton-line" />
              <div className="skeleton skeleton-line" />
              <div className="skeleton skeleton-line" style={{ width: '45%' }} />
            </div>
          ))}
        </div>
      )}
      {productsError && (
        <div className="status status-error">
          {productsError}
          <div style={{ marginTop: 8 }}>
            <button className="btn btn-primary" onClick={fetchProducts}>Retry</button>
          </div>
        </div>
      )}
      {!productsLoading && !productsError && products.length === 0 && (
        <div className="card muted">
          No products available. Use <strong>Load Demo Products</strong> to seed the catalog.
        </div>
      )}
      <div className="grid">
        {products.map((p) => (
          <div key={p.id} className="card">
            <h3 className="product-title">{p.name}</h3>
            <p className="muted">SKU: {p.sku}</p>
            <p className="price-text">
              {formatCurrency(p.price)}
            </p>
            <p><span className={stockClass(p.stock)}>{stockLabel(p.stock)}</span></p>
            <button className="btn btn-success" onClick={() => addToCart(p)} disabled={!p.active || !p.stock}>
              Add to Cart
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

export default ProductsPage;