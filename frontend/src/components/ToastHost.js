import React, { useEffect, useState } from 'react';
import { subscribeToToasts } from '../utils/toast';

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
        >
          {toast.message}
        </div>
      ))}
    </div>
  );
}

export default ToastHost;
