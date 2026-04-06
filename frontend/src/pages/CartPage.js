import React, { useState } from 'react';
import useStore from '../store/useStore';

const btn = {
  padding: '8px 16px', border: 'none', borderRadius: '4px',
  cursor: 'pointer', fontWeight: 'bold',
};

function CartPage() {
  const { cartItems, removeFromCart, placeOrder, orderLoading } = useStore();
  const [message, setMessage] = useState('');

  const total = cartItems.reduce((sum, i) => sum + (i.price ?? 0) * i.quantity, 0);

  const handlePlaceOrder = async () => {
    try {
      const userId = localStorage.getItem('userId') || 'guest-user';
      await placeOrder(userId);
      setMessage('✅ Order placed successfully! Check Orders page.');
    } catch {
      setMessage('❌ Failed to place order. Please try again.');
    }
  };

  return (
    <div>
      <h2 style={{ marginBottom: '16px' }}>Shopping Cart</h2>
      {message && <p style={{ padding: '12px', background: message.includes('✅') ? '#d4edda' : '#f8d7da', borderRadius: '4px', marginBottom: '12px' }}>{message}</p>}
      {cartItems.length === 0 ? (
        <p>Your cart is empty.</p>
      ) : (
        <>
          <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff' }}>
            <thead>
              <tr style={{ background: '#1a1a2e', color: '#fff' }}>
                <th style={{ padding: '12px', textAlign: 'left' }}>Product</th>
                <th style={{ padding: '12px' }}>SKU</th>
                <th style={{ padding: '12px' }}>Price</th>
                <th style={{ padding: '12px' }}>Qty</th>
                <th style={{ padding: '12px' }}>Subtotal</th>
                <th style={{ padding: '12px' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {cartItems.map((item) => (
                <tr key={item.sku} style={{ borderBottom: '1px solid #ddd' }}>
                  <td style={{ padding: '12px' }}>{item.name}</td>
                  <td style={{ padding: '12px', textAlign: 'center' }}>{item.sku}</td>
                  <td style={{ padding: '12px', textAlign: 'center' }}>${item.price?.toFixed?.(2) ?? item.price}</td>
                  <td style={{ padding: '12px', textAlign: 'center' }}>{item.quantity}</td>
                  <td style={{ padding: '12px', textAlign: 'center' }}>${((item.price ?? 0) * item.quantity).toFixed(2)}</td>
                  <td style={{ padding: '12px', textAlign: 'center' }}>
                    <button style={{ ...btn, background: '#c0392b', color: '#fff' }} onClick={() => removeFromCart(item.sku)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ textAlign: 'right', marginTop: '16px' }}>
            <p style={{ fontSize: '1.3rem', fontWeight: 'bold' }}>Total: ${total.toFixed(2)}</p>
            <button
              style={{ ...btn, background: '#27ae60', color: '#fff', marginTop: '8px', fontSize: '1.1rem', padding: '12px 24px' }}
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