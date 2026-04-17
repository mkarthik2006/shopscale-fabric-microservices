import React, { useState } from 'react';
import useStore from '../store/useStore';
import api, { getApiErrorMessage } from '../services/api';
import { showErrorToast, showSuccessToast } from '../utils/toast';
import { useNavigate } from 'react-router-dom';

function CartPage() {
  const { cartItems, removeFromCart, placeOrder, orderLoading, clearCart } = useStore();
  const navigate = useNavigate();
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [fallbackNotice, setFallbackNotice] = useState('');

  const total = cartItems.reduce((sum, i) => sum + (i.price ?? 0) * i.quantity, 0);

  const handlePlaceOrder = async () => {
    setError('');
    setMessage('');
    try {
      const userId = localStorage.getItem('userId') || 'guest-user';
      await placeOrder(userId);
      setMessage('Order placed successfully. Check the Orders page.');
      showSuccessToast('Order placed successfully.');
      navigate('/orders');
    } catch (err) {
      const message = getApiErrorMessage(err);
      setError(message);
      showErrorToast(message);
    }
  };

  const checkFallbackMode = async () => {
    if (!cartItems.length) return;
    const userId = localStorage.getItem('userId') || 'guest-user';
    try {
      const firstSku = cartItems[0].sku;
      const res = await api.get(`/api/cart/${userId}/total?sku=${encodeURIComponent(firstSku)}`);
      const source = String(res.data?.priceSource || '').toUpperCase();
      if (source === 'FALLBACK') {
        setFallbackNotice('Price service unavailable, showing fallback');
      } else {
        setFallbackNotice('');
      }
    } catch {
      setFallbackNotice('');
    }
  };

  return (
    <div>
      <h2 style={{ marginBottom: '16px' }}>Shopping Cart</h2>
      {message && <div className="status status-success">{message}</div>}
      {error && <div className="status status-error">{error}</div>}
      {fallbackNotice && <div className="status" style={{ background: '#fffbeb', color: '#92400e' }}>{fallbackNotice}</div>}
      {cartItems.length === 0 ? (
        <div className="card muted">Your cart is empty.</div>
      ) : (
        <>
          <div className="table-wrap card" style={{ padding: 0 }}>
            <table>
              <thead>
                <tr>
                  <th>Product</th>
                  <th>SKU</th>
                  <th>Price</th>
                  <th>Qty</th>
                  <th>Subtotal</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {cartItems.map((item) => (
                  <tr key={item.sku}>
                    <td>{item.name}</td>
                    <td>{item.sku}</td>
                    <td>Rs {Number(item.price ?? 0).toLocaleString('en-IN')}</td>
                    <td>{item.quantity}</td>
                    <td>Rs {Number((item.price ?? 0) * item.quantity).toLocaleString('en-IN')}</td>
                    <td>
                      <button className="btn btn-danger" onClick={() => removeFromCart(item.sku)}>Remove</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{ textAlign: 'right', marginTop: '16px' }}>
            <p style={{ fontSize: '1.1rem', fontWeight: 'bold', margin: 0 }}>Total: Rs {total.toLocaleString('en-IN')}</p>
            <div style={{ marginTop: 10, display: 'flex', justifyContent: 'flex-end', gap: 8, flexWrap: 'wrap' }}>
              <button className="btn btn-primary" onClick={checkFallbackMode}>Check Live Price Mode</button>
              <button className="btn btn-danger" onClick={clearCart}>Clear Cart</button>
            </div>
            <button
              className="btn btn-success"
              style={{ marginTop: 10 }}
              onClick={handlePlaceOrder}
              disabled={orderLoading}
            >
              {orderLoading ? 'Placing Order...' : 'Place Order'}
            </button>
          </div>
        </>
      )}
    </div>
  );
}

export default CartPage;