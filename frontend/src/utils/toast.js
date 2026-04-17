const TOAST_EVENT = 'shopscale:toast';

export function showToast(message, type = 'success') {
  window.dispatchEvent(
    new CustomEvent(TOAST_EVENT, {
      detail: { id: Date.now() + Math.random(), message, type },
    })
  );
}

export function showSuccessToast(message) {
  showToast(message, 'success');
}

export function showErrorToast(message) {
  showToast(message, 'error');
}

export function subscribeToToasts(listener) {
  const handler = (event) => listener(event.detail);
  window.addEventListener(TOAST_EVENT, handler);
  return () => window.removeEventListener(TOAST_EVENT, handler);
}
