import React, { useEffect, useState } from 'react';
import { subscribeToToasts } from '../utils/toast';
import Icon from './Icon';

function ToastHost() {
  const [toasts, setToasts] = useState([]);

  useEffect(() => {
    const unsubscribe = subscribeToToasts((toast) => {
      setToasts((prev) => [...prev, toast].slice(-3));
      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== toast.id));
      }, 3200);
    });
    return unsubscribe;
  }, []);

  return (
    <div className="toast-host" aria-live="polite" aria-atomic="true">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`toast-item ${toast.type === 'error' ? 'toast-error' : 'toast-success'}`}
          role={toast.type === 'error' ? 'alert' : 'status'}
        >
          <span className="toast-item__icon">
            <Icon name={toast.type === 'error' ? 'warning' : 'check'} size={16} />
          </span>
          <span>{toast.message}</span>
        </div>
      ))}
    </div>
  );
}

export default ToastHost;
