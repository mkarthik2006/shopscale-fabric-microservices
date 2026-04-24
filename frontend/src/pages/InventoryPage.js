import React, { useEffect, useState } from 'react';
import api, { getApiErrorMessage } from '../services/api';
import { showErrorToast } from '../utils/toast';

function InventoryPage() {
  const [items, setItems] = useState([]);
  const [productNameBySku, setProductNameBySku] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

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

  return (
    <div>
      <div className="page-header">
        <h2 style={{ margin: 0 }}>Inventory</h2>
        <button className="btn btn-primary" onClick={fetchInventory} disabled={loading}>
          {loading ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {loading && (
        <p className="muted">
          <span className="spinner" />
          Loading inventory...
        </p>
      )}

      {error && (
        <div className="status status-error">
          {error}
          <div style={{ marginTop: 8 }}>
            <button className="btn btn-primary" onClick={fetchInventory}>
              Retry
            </button>
          </div>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <div className="card muted">No inventory records available.</div>
      )}

      {!loading && !error && items.length > 0 && (
        <div className="table-wrap card" style={{ padding: 0 }}>
          <table>
            <thead>
              <tr>
                <th>SKU</th>
                <th>Name</th>
                <th>Available</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.sku}>
                  <td>{item.sku}</td>
                  <td>{item.name || productNameBySku[item.sku] || '-'}</td>
                  <td>{item.stock ?? '-'}</td>
                  <td>-</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default InventoryPage;
