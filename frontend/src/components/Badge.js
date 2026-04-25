import React from 'react';

function Badge({ tone = 'neutral', children, plain = false, className = '' }) {
  const cls = ['badge', `badge--${tone}`, plain ? 'badge--plain' : '', className]
    .filter(Boolean)
    .join(' ');
  return <span className={cls}>{children}</span>;
}

export default Badge;
