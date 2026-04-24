import axios from 'axios';

const gatewayUrl = process.env.REACT_APP_GATEWAY_URL || '';
const isProdBuild = process.env.NODE_ENV === 'production';
if (isProdBuild && gatewayUrl) {
  throw new Error('REACT_APP_GATEWAY_URL must not be set in production. Use same-origin gateway proxy.');
}

const api = axios.create({
  // Empty baseURL = relative paths routed through nginx proxy.
  // REACT_APP_GATEWAY_URL is allowed only for non-production local dev.
  baseURL: gatewayUrl,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

let inMemoryToken = null;
const PKCE_STORAGE_KEY = 'pkce_state';

export function setAccessToken(token) {
  inMemoryToken = token || null;
}

export function clearAccessToken() {
  inMemoryToken = null;
}

export function getAccessToken() {
  return inMemoryToken;
}

export function getTokenClaims() {
  const token = getAccessToken();
  if (!token) return null;
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);
    return JSON.parse(atob(padded));
  } catch {
    return null;
  }
}

export function getAuthenticatedUserId() {
  const claims = getTokenClaims();
  return claims?.preferred_username || claims?.sub || null;
}

export function isTokenExpired() {
  const claims = getTokenClaims();
  if (!claims?.exp) return true;
  return Date.now() >= claims.exp * 1000;
}

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

function base64UrlEncode(uint8) {
  const raw = String.fromCharCode(...uint8);
  return btoa(raw).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function randomString(bytes = 32) {
  const data = new Uint8Array(bytes);
  window.crypto.getRandomValues(data);
  return base64UrlEncode(data);
}

async function sha256Base64Url(input) {
  const data = new TextEncoder().encode(input);
  const digest = await window.crypto.subtle.digest('SHA-256', data);
  return base64UrlEncode(new Uint8Array(digest));
}

export function consumePkceState() {
  const raw = sessionStorage.getItem(PKCE_STORAGE_KEY);
  sessionStorage.removeItem(PKCE_STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function hasValidSession() {
  const token = getAccessToken();
  return Boolean(token) && !isTokenExpired();
}

export async function beginPkceLogin() {
  const keycloakRealm = process.env.REACT_APP_KEYCLOAK_REALM || 'shopscale';
  const keycloakClientId = process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'shopscale-gateway';
  const verifier = randomString(64);
  const state = randomString(24);
  const challenge = await sha256Base64Url(verifier);
  const redirectUri = `${window.location.origin}/login`;

  sessionStorage.setItem(PKCE_STORAGE_KEY, JSON.stringify({ verifier, state }));
  const authUrl = `/auth/realms/${keycloakRealm}/protocol/openid-connect/auth?` +
    new URLSearchParams({
      response_type: 'code',
      client_id: keycloakClientId,
      redirect_uri: redirectUri,
      scope: 'openid profile email',
      state,
      code_challenge: challenge,
      code_challenge_method: 'S256',
    }).toString();
  window.location.assign(authUrl);
}

// Attach JWT token to every request if available
api.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Global error handler
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error.config?.url || '';
    const isAuthEndpoint = requestUrl.includes('/auth/');
    const isLoginRoute = window.location.pathname === '/login';
    if (error.response?.status === 401 && !isAuthEndpoint && !isLoginRoute) {
      clearAccessToken();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;