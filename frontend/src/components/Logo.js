import React from 'react';

function Logo({ size = 32, withText = false, textColor = '#fff' }) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 10 }}>
      <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
        <defs>
          <linearGradient id="ssf-logo-grad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#3b6bf6" />
            <stop offset="100%" stopColor="#7c3aed" />
          </linearGradient>
        </defs>
        <rect width="32" height="32" rx="8" fill="url(#ssf-logo-grad)" />
        <path
          d="M9 20.5C9 22 10.4 23 12.5 23h7c2.1 0 3.5-1 3.5-2.5 0-1.6-1.3-2.4-3.7-2.9l-3-.6c-1.2-.2-1.8-.6-1.8-1.2 0-.7.8-1.1 2-1.1 1.4 0 2.3.5 2.5 1.4h3.7c-.2-2-2-3.4-5.9-3.4-3.6 0-5.7 1.3-5.7 3.5 0 1.6 1.3 2.5 3.7 3l3 .6c1.2.2 1.8.6 1.8 1.2 0 .7-.8 1.1-2 1.1-1.5 0-2.4-.5-2.6-1.5H9z"
          fill="#fff"
        />
      </svg>
      {withText && (
        <span style={{ display: 'inline-flex', flexDirection: 'column', lineHeight: 1.05 }}>
          <span style={{ color: textColor, fontWeight: 700, fontSize: '0.95rem', letterSpacing: '-0.01em' }}>
            ShopScale
          </span>
          <span
            style={{
              color: textColor === '#fff' ? '#94a3b8' : '#6b7591',
              fontWeight: 500,
              fontSize: '0.65rem',
              letterSpacing: '0.06em',
              textTransform: 'uppercase',
            }}
          >
            Fabric
          </span>
        </span>
      )}
    </span>
  );
}

export default Logo;
