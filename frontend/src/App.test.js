import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { render, screen } from '@testing-library/react';
import App from './App';
import { clearAccessToken, setAccessToken } from './services/api';

describe('App route protection', () => {
  beforeEach(() => {
    clearAccessToken();
  });

  test('redirects unauthenticated user to login on protected route', () => {
    render(
      <MemoryRouter initialEntries={['/orders']}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByText(/login to shopscale/i)).toBeInTheDocument();
  });

  test('redirects user with expired token to login on protected route', () => {
    const expiredPayload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) - 60 }));
    setAccessToken(`header.${expiredPayload}.signature`);
    render(
      <MemoryRouter initialEntries={['/orders']}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByText(/login to shopscale/i)).toBeInTheDocument();
  });
});
