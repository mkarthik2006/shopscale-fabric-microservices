import React, { useEffect, useMemo, useState } from 'react';
import useStore from '../store/useStore';
import api from '../services/api';
import demoProducts from '../data/demoProducts';
import { showErrorToast, showSuccessToast } from '../utils/toast';
import { getAccessToken, getApiErrorMessage, getTokenClaims, isTokenExpired } from '../services/api';
import Icon from '../components/Icon';
import Badge from '../components/Badge';
import EmptyState from '../components/EmptyState';
import StatsCard from '../components/StatsCard';
import { SkeletonProductCard } from '../components/Skeleton';

const STOCK_FILTERS = [
  { id: 'all',  label: 'All' },
  { id: 'in',   label: 'In stock' },
  { id: 'low',  label: 'Low stock' },
  { id: 'out',  label: 'Out of stock' },
];

function productEmoji(name) {
  const n = (name || '').toLowerCase();
  if (n.includes('book')) return '📚';
  if (n.includes('phone') || n.includes('mobile')) return '📱';
  if (n.includes('laptop') || n.includes('computer')) return '💻';
  if (n.includes('headphone') || n.includes('audio')) return '🎧';
  if (n.includes('watch')) return '⌚';
  if (n.includes('camera')) return '📷';
  if (n.includes('shoe') || n.includes('sneaker')) return '👟';
  if (n.includes('shirt') || n.includes('cloth')) return '👕';
  if (n.includes('bag') || n.includes('backpack')) return '🎒';
  if (n.includes('coffee')) return '☕';
  return '📦';
}

function ProductsPage() {
  const { products, productsLoading, productsError, fetchProducts, addToCart, cartItems } = useStore();
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState('all');

  useEffect(() => { fetchProducts(); }, [fetchProducts]);

  const hasValidSession = Boolean(getAccessToken()) && !isTokenExpired();
  const tokenClaims = getTokenClaims();
  const realmRoles = tokenClaims?.realm_access?.roles || [];
  const isAdmin = realmRoles.includes('ADMIN') || realmRoles.includes('ROLE_ADMIN');

  const formatCurrency = (amount) => new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(Number(amount || 0));

  const stockTone = (stock) => {
    if (!stock || stock <= 0) return 'danger';
    if (stock < 10) return 'warning';
    return 'success';
  };

  const stockLabel = (stock) => {
    if (!stock || stock <= 0) return 'Out of stock';
    if (stock < 10) return `Low stock · ${stock}`;
    return `In stock · ${stock}`;
  };

  const filtered = useMemo(() => {
    return products.filter((p) => {
      const stock = Number(p.stock || 0);
      if (filter === 'in'  && stock < 10) return false;
      if (filter === 'low' && (stock === 0 || stock >= 10)) return false;
      if (filter === 'out' && stock > 0) return false;
      if (search.trim()) {
        const q = search.trim().toLowerCase();
        return (p.name || '').toLowerCase().includes(q) || (p.sku || '').toLowerCase().includes(q);
      }
      return true;
    });
  }, [products, search, filter]);

  const stats = useMemo(() => {
    const total = products.length;
    const inStock = products.filter((p) => Number(p.stock || 0) >= 10).length;
    const low = products.filter((p) => Number(p.stock || 0) > 0 && Number(p.stock) < 10).length;
    const out = products.filter((p) => Number(p.stock || 0) <= 0).length;
    return { total, inStock, low, out };
  }, [products]);

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

  const cartCount = cartItems.reduce((sum, i) => sum + i.quantity, 0);

  return (
    <div>
      {/* Header */}
      <div className="page-header">
        <div className="page-header__title">
          <span className="page-header__eyebrow">Catalogue</span>
          <h1 className="page-header__heading">Product Catalogue</h1>
          <p className="page-header__subtitle">
            Browse the live storefront powered by the product-service. Prices and stock update in real time via the API gateway.
          </p>
        </div>
        <div className="page-header__actions">
          <button className="btn btn-outline" onClick={fetchProducts} disabled={productsLoading}>
            <Icon name="refresh" size={14} />
            {productsLoading ? 'Refreshing…' : 'Refresh'}
          </button>
          {hasValidSession && isAdmin && (
            <button className="btn btn-primary" onClick={seedDemoProducts} disabled={productsLoading}>
              <Icon name="sparkles" size={14} />
              Load Demo Products
            </button>
          )}
        </div>
      </div>

      {/* Stats row */}
      <div className="grid grid--4 mb-6">
        <StatsCard icon="package"   tone="brand"   label="Total products" value={stats.total} />
        <StatsCard icon="check"     tone="success" label="In stock"       value={stats.inStock} />
        <StatsCard icon="warning"   tone="warning" label="Low stock"      value={stats.low} />
        <StatsCard icon="cart"      tone="accent"  label="In your cart"   value={cartCount} />
      </div>

      {/* Toolbar */}
      <div className="card card--padded mb-4" style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
        <div className="field__prefix-wrap" style={{ flex: 1, minWidth: 220 }}>
          <span className="field__prefix"><Icon name="search" size={16} /></span>
          <input
            className="input input--with-prefix"
            placeholder="Search by product name or SKU…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div style={{ display: 'inline-flex', gap: 6, padding: 4, background: 'var(--bg-app-soft)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border)' }}>
          {STOCK_FILTERS.map((f) => (
            <button
              key={f.id}
              type="button"
              className={`btn btn-sm ${filter === f.id ? 'btn-primary' : 'btn-ghost'}`}
              onClick={() => setFilter(f.id)}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* Loading skeletons */}
      {productsLoading && (
        <div className="grid">
          {Array.from({ length: 8 }).map((_, idx) => <SkeletonProductCard key={idx} />)}
        </div>
      )}

      {/* Error */}
      {productsError && !productsLoading && (
        <div className="alert alert--danger" role="alert">
          <Icon name="warning" size={18} className="alert__icon" />
          <div style={{ flex: 1 }}>
            <div className="alert__title">Could not load products</div>
            <div>{productsError}</div>
            <div style={{ marginTop: 10 }}>
              <button className="btn btn-danger btn-sm" onClick={fetchProducts}>
                <Icon name="refresh" size={14} /> Retry
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Empty */}
      {!productsLoading && !productsError && products.length === 0 && (
        <EmptyState
          icon="package"
          title="No products yet"
          description="The catalogue is currently empty. Sign in as an admin and use Load Demo Products to seed an example dataset."
        />
      )}

      {/* Filtered empty */}
      {!productsLoading && !productsError && products.length > 0 && filtered.length === 0 && (
        <EmptyState
          icon="search"
          title="No matches"
          description="Try clearing your search or switching the stock filter."
          action={
            <button className="btn btn-outline btn-sm" onClick={() => { setSearch(''); setFilter('all'); }}>
              Clear filters
            </button>
          }
        />
      )}

      {/* Grid */}
      {!productsLoading && filtered.length > 0 && (
        <div className="grid">
          {filtered.map((p) => {
            const outOfStock = !p.active || !p.stock;
            return (
              <article key={p.id} className="product-card">
                <div className="product-card__media">
                  <span className="product-card__media-emoji" aria-hidden="true">
                    {productEmoji(p.name)}
                  </span>
                  <div style={{ position: 'absolute', top: 10, left: 10 }}>
                    <Badge tone={stockTone(p.stock)}>{stockLabel(p.stock)}</Badge>
                  </div>
                  {!p.active && (
                    <div style={{ position: 'absolute', top: 10, right: 10 }}>
                      <Badge tone="neutral" plain>Inactive</Badge>
                    </div>
                  )}
                </div>
                <div className="product-card__body">
                  <span className="product-card__sku">SKU · {p.sku}</span>
                  <h3 className="product-card__title">{p.name}</h3>
                  <span className="product-card__price">
                    <span className="product-card__price-currency">USD</span>
                    {formatCurrency(p.price).replace('$', '')}
                  </span>
                </div>
                <div className="product-card__footer">
                  <button
                    className="btn btn-primary"
                    onClick={() => addToCart(p)}
                    disabled={outOfStock}
                  >
                    <Icon name="cart" size={14} />
                    {outOfStock ? 'Unavailable' : 'Add to Cart'}
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default ProductsPage;
