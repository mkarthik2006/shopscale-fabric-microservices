import {
  clearAccessToken,
  consumePkceState,
  getApiErrorMessage,
  getAuthenticatedUserId,
  hasValidSession,
  isTokenExpired,
  setAccessToken,
} from './api';

function buildJwt(payload) {
  const encoded = btoa(JSON.stringify(payload))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `header.${encoded}.signature`;
}

describe('api auth and error helpers', () => {
  beforeEach(() => {
    clearAccessToken();
    sessionStorage.clear();
  });

  test('extracts authenticated user from token claims', () => {
    setAccessToken(buildJwt({ preferred_username: 'alice', exp: Math.floor(Date.now() / 1000) + 3600 }));
    expect(getAuthenticatedUserId()).toBe('alice');
    expect(hasValidSession()).toBe(true);
  });

  test('marks token as expired when exp is in past', () => {
    setAccessToken(buildJwt({ sub: 'user-1', exp: Math.floor(Date.now() / 1000) - 10 }));
    expect(isTokenExpired()).toBe(true);
    expect(hasValidSession()).toBe(false);
  });

  test('parses and removes PKCE state from session storage', () => {
    sessionStorage.setItem('pkce_state', JSON.stringify({ verifier: 'v', state: 's' }));
    expect(consumePkceState()).toEqual({ verifier: 'v', state: 's' });
    expect(sessionStorage.getItem('pkce_state')).toBeNull();
  });

  test('returns explicit backend message when present', () => {
    const error = { response: { status: 400, data: { message: 'invalid payload' } } };
    expect(getApiErrorMessage(error)).toBe('invalid payload');
  });

  test('returns rate-limit message for 429 responses', () => {
    const error = { response: { status: 429, data: {} } };
    expect(getApiErrorMessage(error)).toBe('Rate limit exceeded. Please wait and retry.');
  });
});
