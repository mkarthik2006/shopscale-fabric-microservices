import React, { useState } from 'react';
import useStore from '../store/useStore';
import api, { getApiErrorMessage, getAuthenticatedUserId } from '../services/api';
import { showErrorToast, showSuccessToast } from '../utils/toast';
import { useNavigate } from 'react-router-dom';
import Icon from '../components/Icon';
import Badge from '../components/Badge';
import EmptyState from '../components/EmptyState';

function CartPage() {
  const { cartItems, removeFromCart, placeOrder, orderLoading, clearCart } = useStore();
  const navigate = useNavigate();
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [fallbackNotice, setFallbackNotice] = useState('');

  const itemCount = cartItems.reduce((sum, i) => sum + i.quantity, 0);
  const subtotal = cartItems.reduce((sum, i) => sum + (i.price ?? 0) * i.quantity, 0);
  const formatCurrency = (amount) => new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(Number(amount || 0));

  const handlePlaceOrder = async () => {
    setError('');
    setMessage('');
    try {
      const userId = getAuthenticatedUserId();
      if (!userId) {
        setError('Please login to place an order.');
        navigate('/login');
        return;
      }
      await placeOrder();
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
    const userId = getAuthenticatedUserId();
    if (!userId) {
      setError('Please login to check cart pricing.');
      navigate('/login');
      return;
    }
    try {
      const firstSku = cartItems[0].sku;
      const res = await api.get(`/api/cart/${userId}/total?sku=${encodeURIComponent(firstSku)}`);
      const source = String(res.data?.data?.priceResponse?.priceSource || '').toUpperCase();
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
      <div className="page-header">
        <div className="page-header__title">
          <span className="page-header__eyebrow">Checkout</span>
          <h1 className="page-header__heading">Shopping Cart</h1>
          <p className="page-header__subtitle">
            {itemCount > 0
              ? `${itemCount} item${itemCount === 1 ? '' : 's'} ready for checkout. Place an order to trigger the Kafka SAGA workflow.`
              : 'Your cart is empty — pick a few items from the catalogue to get started.'}
          </p>
        </div>
        {cartItems.length > 0 && (
          <div className="page-header__actions">
            <Badge tone="brand">{itemCount} items</Badge>
          </div>
        )}
      </div>

      {message && (
        <div className="alert alert--success" role="status">
          <Icon name="check" size={18} className="alert__icon" />
          <span>{message}</span>
        </div>
      )}
      {error && (
        <div className="alert alert--danger" role="alert">
          <Icon name="warning" size={18} className="alert__icon" />
          <span>{error}</span>
        </div>
      )}
      {fallbackNotice && (
        <div className="alert alert--warning" role="status">
          <Icon name="info" size={18} className="alert__icon" />
          <span>
            <span className="alert__title">Resilience fallback active</span>
            <div>{fallbackNotice}</div>
          </span>
        </div>
      )}

      {cartItems.length === 0 ? (
        <EmptyState
          icon="cart"
          title="Your cart is empty"
          description="Add a few products to your cart to place an order. Orders trigger the Kafka SAGA flow across inventory and notification services."
          action={
            <button className="btn btn-primary" onClick={() => navigate('/')}>
              <Icon name="arrowRight" size={14} />
              Browse products
            </button>
          }
        />
      ) : (
        <div className="cart-layout">
          {/* Items table */}
          <div>
            <div className="card" style={{ overflow: 'hidden' }}>
              <div className="card__header">
                <div>
                  <div className="card__title">Items</div>
                  <div className="card__subtitle">Review your selection before placing the order.</div>
                </div>
                <button className="btn btn-ghost btn-sm" onClick={clearCart}>
                  <Icon name="trash" size={14} /> Clear cart
                </button>
              </div>
              <div className="table-wrap" style={{ borderRadius: 0, border: 'none', boxShadow: 'none' }}>
                <table>
                  <thead>
                    <tr>
                      <th>Product</th>
                      <th>SKU</th>
                      <th>Price</th>
                      <th>Qty</th>
                      <th>Subtotal</th>
                      <th aria-label="Actions" />
                    </tr>
                  </thead>
                  <tbody>
                    {cartItems.map((item) => (
                      <tr key={item.sku}>
                        <td className="cell-strong">{item.name}</td>
                        <td className="cell-mono">{item.sku}</td>
                        <td>{formatCurrency(item.price)}</td>
                        <td>
                          <Badge tone="brand">{item.quantity}</Badge>
                        </td>
                        <td className="cell-strong">
                          {formatCurrency((item.price ?? 0) * item.quantity)}
                        </td>
                        <td className="cell-actions">
                          <button
                            className="btn btn-ghost btn-sm"
                            onClick={() => removeFromCart(item.sku)}
                            aria-label={`Remove ${item.name}`}
                          >
                            <Icon name="trash" size={14} /> Remove
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Order summary panel */}
          <aside className="summary" aria-label="Order summary">
            <div className="summary__header">
              <div className="summary__title">Order summary</div>
              <div className="card__subtitle">Live pricing via Spring Cloud Gateway</div>
            </div>
            <div className="summary__rows">
              <div className="summary__row">
                <span className="muted">Items</span>
                <span>{itemCount}</span>
              </div>
              <div className="summary__row">
                <span className="muted">Subtotal</span>
                <span>{formatCurrency(subtotal)}</span>
              </div>
              <div className="summary__row">
                <span className="muted">Shipping</span>
                <span><Badge tone="success" plain>Free</Badge></span>
              </div>
              <div className="summary__row">
                <span className="muted">Tax</span>
                <span className="muted">Calculated at fulfilment</span>
              </div>
              <div className="summary__row summary__row--total">
                <span>Total</span>
                <span>{formatCurrency(subtotal)}</span>
              </div>
            </div>
            <div className="summary__actions">
              <button
                className="btn btn-success btn-lg btn-block"
                onClick={handlePlaceOrder}
                disabled={orderLoading}
              >
                {orderLoading ? (
                  <>
                    <span className="spinner" /> Placing order…
                  </>
                ) : (
                  <>
                    <Icon name="zap" size={16} /> Place order
                  </>
                )}
              </button>
              <button className="btn btn-outline btn-block" onClick={checkFallbackMode}>
                <Icon name="activity" size={14} />
                Check price-service mode
              </button>
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}

export default CartPage;
