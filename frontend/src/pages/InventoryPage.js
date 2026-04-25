import React, { useEffect, useMemo, useState } from 'react';
import api, { getApiErrorMessage } from '../services/api';
import { showErrorToast } from '../utils/toast';
import Icon from '../components/Icon';
import Badge from '../components/Badge';
import EmptyState from '../components/EmptyState';
import StatsCard from '../components/StatsCard';
import { SkeletonRow } from '../components/Skeleton';

function stockTone(stock) {
  if (!stock || stock <= 0) return 'danger';
  if (stock < 10) return 'warning';
  return 'success';
}

function stockLabel(stock) {
  if (!stock || stock <= 0) return 'Out of stock';
  if (stock < 10) return `Low · ${stock}`;
  return `In stock · ${stock}`;
}

function InventoryPage() {
  const [items, setItems] = useState([]);
  const [productNameBySku, setProductNameBySku] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');

  const fetchInventory = async () => {
    setLoading(true);
    setError('');
    try {
      const [inventoryRes, productsRes] = await Promise.all([
        api.get('/api/inventory'),
        api.get('/api/products'),
      ]);
      const inventory = Array.isArray(inventoryRes.data) ? inventoryRes.data : [];
      const products = Array.isArray(productsRes.data) ? productsRes.data : [];
      const names = {};
      products.forEach((p) => {
        if (p?.sku && p?.name) names[p.sku] = p.name;
      });
      setItems(inventory);
      setProductNameBySku(names);
    } catch (err) {
      const message = getApiErrorMessage(err);
      setError(message);
      showErrorToast(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInventory();
  }, []);

  const filtered = useMemo(() => {
    if (!search.trim()) return items;
    const q = search.trim().toLowerCase();
    return items.filter((it) => {
      const name = it.name || productNameBySku[it.sku] || '';
      return (it.sku || '').toLowerCase().includes(q) || name.toLowerCase().includes(q);
    });
  }, [items, productNameBySku, search]);

  const stats = useMemo(() => {
    const total = items.length;
    const inStock = items.filter((i) => Number(i.stock || 0) >= 10).length;
    const low = items.filter((i) => Number(i.stock || 0) > 0 && Number(i.stock) < 10).length;
    const out = items.filter((i) => Number(i.stock || 0) <= 0).length;
    return { total, inStock, low, out };
  }, [items]);

  return (
    <div>
      <div className="page-header">
        <div className="page-header__title">
          <span className="page-header__eyebrow">Operations</span>
          <h1 className="page-header__heading">Inventory</h1>
          <p className="page-header__subtitle">
            Real-time stock levels reconciled from the inventory-service event store. Updated whenever{' '}
            <code className="mono">order.placed</code> events are consumed.
          </p>
        </div>
        <div className="page-header__actions">
          <button className="btn btn-outline" onClick={fetchInventory} disabled={loading}>
            <Icon name="refresh" size={14} />
            {loading ? 'Refreshing…' : 'Refresh'}
          </button>
        </div>
      </div>

      <div className="grid grid--4 mb-6">
        <StatsCard icon="layers"  tone="brand"   label="Tracked SKUs" value={stats.total} />
        <StatsCard icon="check"   tone="success" label="In stock"     value={stats.inStock} />
        <StatsCard icon="warning" tone="warning" label="Low stock"    value={stats.low} />
        <StatsCard icon="package" tone="danger"  label="Out of stock" value={stats.out} />
      </div>

      {error && !loading && (
        <div className="alert alert--danger" role="alert">
          <Icon name="warning" size={18} className="alert__icon" />
          <div style={{ flex: 1 }}>
            <div className="alert__title">Could not load inventory</div>
            <div>{error}</div>
            <div style={{ marginTop: 10 }}>
              <button className="btn btn-danger btn-sm" onClick={fetchInventory}>
                <Icon name="refresh" size={14} /> Retry
              </button>
            </div>
          </div>
        </div>
      )}

      {!error && (
        <>
          <div className="table-toolbar">
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <Icon name="layers" size={16} />
              <strong style={{ fontSize: '0.95rem' }}>Inventory ledger</strong>
              <Badge tone="brand">{filtered.length}</Badge>
            </div>
            <div className="field__prefix-wrap" style={{ width: 280, maxWidth: '100%' }}>
              <span className="field__prefix"><Icon name="search" size={16} /></span>
              <input
                className="input input--with-prefix"
                placeholder="Filter by SKU or name…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>

          <div className="table-wrap table-wrap--with-toolbar">
            <table>
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Product</th>
                  <th>Available</th>
                  <th>Status</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                {loading && Array.from({ length: 6 }).map((_, i) => <SkeletonRow key={i} columns={5} />)}

                {!loading && filtered.length === 0 && (
                  <tr>
                    <td colSpan={5} style={{ padding: 0 }}>
                      <EmptyState
                        icon="inventory"
                        title={items.length === 0 ? 'Inventory is empty' : 'No matches'}
                        description={
                          items.length === 0
                            ? 'No inventory records have been published yet by the inventory-service.'
                            : 'Try clearing your search.'
                        }
                      />
                    </td>
                  </tr>
                )}

                {!loading && filtered.map((item) => (
                  <tr key={item.sku}>
                    <td className="cell-mono">{item.sku}</td>
                    <td className="cell-strong">{item.name || productNameBySku[item.sku] || '—'}</td>
                    <td>{item.stock ?? '—'}</td>
                    <td>
                      <Badge tone={stockTone(item.stock)}>{stockLabel(item.stock)}</Badge>
                    </td>
                    <td className="muted">—</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}

export default InventoryPage;
