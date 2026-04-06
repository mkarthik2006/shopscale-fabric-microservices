import React, { useEffect } from 'react';
import useStore from '../store/useStore';

function OrdersPage() {
  const { orders, fetchOrders } = useStore();
  const userId = localStorage.getItem('userId') || 'guest-user';

  useEffect(() => { fetchOrders(userId); }, [fetchOrders, userId]);

  const statusColor = (s) => {
    if (s === 'PLACED') return '#2980b9';
    if (s === 'CANCELLED') return '#c0392b';
    return '#27ae60';
  };

  return (
    <div>
      <h2 style={{ marginBottom: '16px' }}>My Orders</h2>
      {orders.length === 0 ? (
        <p>No orders yet.</p>
      ) : (
        orders.map((o) => (
          <div key={o.id} style={{
            border: '1px solid #ddd', borderRadius: '8px', padding: '16px',
            marginBottom: '12px', background: '#fff',
          }}>
            <p><strong>Order ID:</strong> {o.id}</p>
            <p><strong>Status:</strong> <span style={{ color: statusColor(o.status), fontWeight: 'bold' }}>{o.status}</span></p>
            <p><strong>Total:</strong> ${o.totalAmount} {o.currency}</p>
            <p><strong>Items:</strong> {o.items?.length ?? 0} item(s)</p>
            <p style={{ color: '#888', fontSize: '0.85rem' }}>Created: {o.createdAt}</p>
          </div>
        ))
      )}
    </div>
  );
}

export default OrdersPage;