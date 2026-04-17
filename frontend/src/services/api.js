import axios from 'axios';

const api = axios.create({
  // CLEAN CODE: Empty baseURL = relative paths → routed through nginx proxy
  // Override via REACT_APP_GATEWAY_URL for local dev outside Docker
  baseURL: process.env.REACT_APP_GATEWAY_URL || '',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

export function getApiErrorMessage(error) {
  if (!error.response) {
    return 'Network error. Check gateway availability and try again.';
  }

  const { status, data } = error.response;
  const serverMessage = data?.message || data?.error || data?.details;
  if (typeof serverMessage === 'string' && serverMessage.trim()) {
    return serverMessage;
  }

  if (status === 401) return 'Session expired. Please login again.';
  if (status === 403) return 'You do not have permission for this action.';
  if (status === 404) return 'Requested resource was not found.';
  if (status === 429) return 'Rate limit exceeded. Please wait and retry.';
  if (status >= 500) return 'Server error. Please try again shortly.';
  return 'Request failed. Please retry.';
}

// Attach JWT token to every request if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Global error handler
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token');
      localStorage.removeItem('userId');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;