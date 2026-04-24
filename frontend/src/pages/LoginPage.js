import React, { useEffect, useState } from 'react';
import api, { setAccessToken, getApiErrorMessage, beginPkceLogin, consumePkceState } from '../services/api';
import { showErrorToast, showSuccessToast } from '../utils/toast';
import { useLocation, useNavigate } from 'react-router-dom';

function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [message, setMessage] = useState({ type: '', text: '' });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const exchangeCode = async () => {
      const params = new URLSearchParams(location.search);
      const authCode = params.get('code');
      const authState = params.get('state');
      if (!authCode) return;

      setLoading(true);
      try {
        const keycloakRealm = process.env.REACT_APP_KEYCLOAK_REALM || 'shopscale';
        const keycloakClientId = process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'shopscale-gateway';
        const redirectUri = `${window.location.origin}/login`;
        const pkceState = consumePkceState();
        if (!pkceState || pkceState.state !== authState) {
          throw new Error('Invalid login state. Please retry login.');
        }

        const tokenUrl = `/auth/realms/${keycloakRealm}/protocol/openid-connect/token`;
        const res = await api.post(
          tokenUrl,
          new URLSearchParams({
            grant_type: 'authorization_code',
            client_id: keycloakClientId,
            code: authCode,
            redirect_uri: redirectUri,
            code_verifier: pkceState.verifier,
          }),
          { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
        );
        setAccessToken(res.data.access_token);
        setMessage({ type: 'success', text: 'Login successful. You can now place orders.' });
        showSuccessToast('Login successful');
        navigate('/', { replace: true });
      } catch (err) {
        const errorMessage = err?.message || getApiErrorMessage(err);
        setMessage({ type: 'error', text: errorMessage });
        showErrorToast(errorMessage);
      } finally {
        setLoading(false);
      }
    };

    exchangeCode();
  }, [location.search, navigate]);

  const handleLogin = async () => {
    setLoading(true);
    try {
      await beginPkceLogin();
    } catch (err) {
      setLoading(false);
      const errorMessage = err?.message || 'Unable to start login flow.';
      setMessage({ type: 'error', text: errorMessage });
      showErrorToast(errorMessage);
    }
  };

  return (
    <div className="card" style={{ maxWidth: '460px', margin: '32px auto' }}>
      <h2 style={{ marginBottom: '24px', textAlign: 'center' }}>Login to ShopScale</h2>
      {message.text && (
        <div className={`status ${message.type === 'success' ? 'status-success' : 'status-error'}`}>
          {message.text}
        </div>
      )}
      <p style={{ marginBottom: 16 }}>
        Use enterprise login with Authorization Code + PKCE.
      </p>
      <button type="button" className="btn btn-primary" style={{ width: '100%', marginTop: 8 }} disabled={loading} onClick={handleLogin}>
        {loading ? (<><span className="spinner" /> Redirecting...</>) : 'Continue with Keycloak'}
      </button>
    </div>
  );
}

export default LoginPage;