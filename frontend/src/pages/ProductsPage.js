import React, { useEffect } from 'react';
import useStore from '../store/useStore';
import api from '../services/api';
import demoProducts from '../data/demoProducts';
import { showErrorToast, showSuccessToast } from '../utils/toast';

function ProductsPage() {
  const { products, productsLoading, productsError, fetchProducts, addToCart } = useStore();

  useEffect(() => { fetchProducts(); }, [fetchProducts]);

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
    for (const product of demoProducts) {
      try {
        await api.post('/api/products', product);
        created += 1;
      } catch (err) {
        // Ignore duplicates/conflicts for idempotent demo seeding.
      }
    }
    await fetchProducts();
    if (created > 0) {
      showSuccessToast(`Added ${created} demo products`);
    } else {
      showErrorToast('Demo products already exist or could not be created');
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
          <button className="btn btn-success" onClick={seedDemoProducts} disabled={productsLoading}>
            Load Demo Products
          </button>
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
              Rs {Number(p.price || 0).toLocaleString('en-IN')}
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