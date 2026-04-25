import React, { useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import useStore from '../store/useStore';
import { getAuthenticatedUserId } from '../services/api';
import Icon from '../components/Icon';
import Badge from '../components/Badge';
import EmptyState from '../components/EmptyState';
import StatsCard from '../components/StatsCard';
import { SkeletonOrderCard } from '../components/Skeleton';

function statusMeta(status) {
  switch (status) {
    case 'PLACED':
      return { tone: 'brand',   icon: 'orders', iconClass: 'order-card__icon--placed' };
    case 'CANCELLED':
      return { tone: 'danger',  icon: 'close',  iconClass: 'order-card__icon--cancelled' };
    case 'COMPLETED':
    case 'FULFILLED':
      return { tone: 'success', icon: 'check',  iconClass: 'order-card__icon--default' };
    default:
      return { tone: 'neutral', icon: 'package', iconClass: 'order-card__icon--default' };
  }
}

function OrdersPage() {
  const navigate = useNavigate();
  const { orders, ordersLoading, ordersError, fetchOrders } = useStore();
  const userId = getAuthenticatedUserId();

  useEffect(() => {
    if (!userId) {
      navigate('/login');
      return;
    }
    fetchOrders();
  }, [fetchOrders, navigate, userId]);

  const stats = useMemo(() => {
    const total = orders.length;
    const placed = orders.filter((o) => o.status === 'PLACED').length;
    const cancelled = orders.filter((o) => o.status === 'CANCELLED').length;
    const completed = orders.filter((o) => o.status === 'COMPLETED' || o.status === 'FULFILLED').length;
    return { total, placed, cancelled, completed };
  }, [orders]);

  return (
    <div>
      <div className="page-header">
        <div className="page-header__title">
          <span className="page-header__eyebrow">Orders</span>
          <h1 className="page-header__heading">My Orders</h1>
          <p className="page-header__subtitle">
            Track every order placed through the gateway. Status flows through Kafka — <code className="mono">order.placed</code> → <code className="mono">inventory.failure</code> / <code className="mono">order.cancelled</code>.
          </p>
        </div>
        <div className="page-header__actions">
          <button className="btn btn-outline" onClick={() => fetchOrders()} disabled={ordersLoading}>
            <Icon name="refresh" size={14} />
            {ordersLoading ? 'Refreshing…' : 'Refresh'}
          </button>
        </div>
      </div>

      <div className="grid grid--4 mb-6">
        <StatsCard icon="orders"  tone="brand"   label="Total orders" value={stats.total} />
        <StatsCard icon="zap"     tone="accent"  label="Placed"       value={stats.placed} />
        <StatsCard icon="check"   tone="success" label="Completed"    value={stats.completed} />
        <StatsCard icon="warning" tone="danger"  label="Cancelled"    value={stats.cancelled} />
      </div>

      {ordersLoading && (
        <div>
          {Array.from({ length: 4 }).map((_, idx) => (
            <div key={idx} style={{ marginTop: idx === 0 ? 0 : 12 }}>
              <SkeletonOrderCard />
            </div>
          ))}
        </div>
      )}

      {ordersError && !ordersLoading && (
        <div className="alert alert--danger" role="alert">
          <Icon name="warning" size={18} className="alert__icon" />
          <div style={{ flex: 1 }}>
            <div className="alert__title">Could not load orders</div>
            <div>{ordersError}</div>
            <div style={{ marginTop: 10 }}>
              <button className="btn btn-danger btn-sm" onClick={() => fetchOrders()}>
                <Icon name="refresh" size={14} /> Retry
              </button>
            </div>
          </div>
        </div>
      )}

      {!ordersLoading && !ordersError && orders.length === 0 && (
        <EmptyState
          icon="orders"
          title="No orders yet"
          description="Your placed orders will show up here in real time. Add items to your cart and place an order to trigger the SAGA flow."
          action={
            <button className="btn btn-primary" onClick={() => navigate('/')}>
              <Icon name="arrowRight" size={14} /> Browse products
            </button>
          }
        />
      )}

      {!ordersLoading && !ordersError && orders.length > 0 && (
        <div>
          {orders.map((o) => {
            const meta = statusMeta(o.status);
            return (
              <div key={o.id} className="order-card">
                <span className={`order-card__icon ${meta.iconClass}`}>
                  <Icon name={meta.icon} size={20} />
                </span>
                <div style={{ minWidth: 0 }}>
                  <div className="order-card__id">{o.id}</div>
                  <div className="order-card__meta">
                    User · <span className="mono">{o.userId}</span>
                  </div>
                </div>
                <Badge tone={meta.tone}>{o.status}</Badge>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default OrdersPage;
