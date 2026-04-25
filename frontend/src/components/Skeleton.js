import React from 'react';

export function SkeletonProductCard() {
  return (
    <div className="card">
      <div className="skeleton skeleton-card-media" />
      <div className="skeleton-card-body">
        <div className="skeleton skeleton-title" />
        <div className="skeleton skeleton-line" style={{ width: '40%' }} />
        <div className="skeleton skeleton-line" style={{ width: '30%' }} />
        <div className="skeleton skeleton-line" style={{ height: 36, marginTop: 12 }} />
      </div>
    </div>
  );
}

export function SkeletonRow({ columns = 4 }) {
  return (
    <tr>
      {Array.from({ length: columns }).map((_, idx) => (
        <td key={idx}>
          <div className="skeleton skeleton-line" style={{ width: `${50 + Math.random() * 40}%` }} />
        </td>
      ))}
    </tr>
  );
}

export function SkeletonOrderCard() {
  return (
    <div className="order-card">
      <div className="skeleton" style={{ width: 44, height: 44, borderRadius: 10 }} />
      <div style={{ minWidth: 0, flex: 1 }}>
        <div className="skeleton skeleton-line" style={{ width: '40%' }} />
        <div className="skeleton skeleton-line" style={{ width: '25%' }} />
      </div>
      <div className="skeleton" style={{ width: 80, height: 24, borderRadius: 999 }} />
    </div>
  );
}
