import React, { useEffect } from 'react';
import useStore from '../store/useStore';

function OrdersPage() {
  const { orders, ordersLoading, ordersError, fetchOrders } = useStore();
  const userId = localStorage.getItem('userId') || 'guest-user';

  useEffect(() => { fetchOrders(userId); }, [fetchOrders, userId]);

  const statusColor = (s) => {
    if (s === 'PLACED') return '#2980b9';
    if (s === 'CANCELLED') return '#c0392b';
    return '#27ae60';
  };

  return (
    <div>
      <div className="page-header">
        <h2 style={{ margin: 0 }}>My Orders</h2>
        <button className="btn btn-primary" onClick={() => fetchOrders(userId)} disabled={ordersLoading}>
          {ordersLoading ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {ordersLoading && (
        <p className="muted">
          <span className="spinner" />
          Loading orders...
        </p>
      )}
      {ordersError && (
        <div className="status status-error">
          {ordersError}
          <div style={{ marginTop: 8 }}>
            <button className="btn btn-primary" onClick={() => fetchOrders(userId)}>Retry</button>
          </div>
        </div>
      )}

      {!ordersLoading && !ordersError && orders.length === 0 ? (
        <div className="card muted">No orders yet.</div>
      ) : (
        orders.map((o) => (
          <div key={o.id} className="card" style={{ marginBottom: '12px' }}>
            <p><strong>Order ID:</strong> {o.id}</p>
            <p><strong>Status:</strong> <span style={{ color: statusColor(o.status), fontWeight: 'bold' }}>{o.status}</span></p>
            <p><strong>Total:</strong> ${o.totalAmount} {o.currency}</p>
            <p><strong>Items:</strong> {o.items?.length ?? 0} item(s)</p>
            <p className="muted" style={{ fontSize: '0.85rem' }}>Created: {o.createdAt}</p>
          </div>
        ))
      )}
    </div>
  );
}

export default OrdersPage;