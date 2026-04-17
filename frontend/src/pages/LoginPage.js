import React, { useState } from 'react';
import axios from 'axios';
import { showErrorToast, showSuccessToast } from '../utils/toast';
import { useNavigate } from 'react-router-dom';

function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('testuser');
  const [password, setPassword] = useState('password');
  const [message, setMessage] = useState({ type: '', text: '' });
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const keycloakRealm = process.env.REACT_APP_KEYCLOAK_REALM || 'shopscale';
      const keycloakClientId = process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'shopscale-gateway';
      // Enforce gateway-based auth flow only.
      const tokenUrl = `/auth/realms/${keycloakRealm}/protocol/openid-connect/token`;

      const res = await axios.post(
        tokenUrl,
        new URLSearchParams({
          grant_type: 'password',
          client_id: keycloakClientId,
          username,
          password,
        }),
        { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
      );
      localStorage.setItem('access_token', res.data.access_token);
      localStorage.setItem('userId', username);
      setMessage({ type: 'success', text: 'Login successful. You can now place orders.' });
      showSuccessToast('Login successful');
      navigate('/');
    } catch (err) {
      setMessage({ type: 'error', text: 'Login failed. Check credentials or Keycloak status.' });
      showErrorToast('Invalid credentials');
    } finally {
      setLoading(false);
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
      <form onSubmit={handleLogin}>
        <label>Username</label>
        <input className="input" value={username} onChange={(e) => setUsername(e.target.value)} />
        <label>Password</label>
        <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: 8 }} disabled={loading}>
          {loading ? (<><span className="spinner" /> Logging in...</>) : 'Login via Keycloak'}
        </button>
      </form>
      <p className="muted" style={{ marginTop: '16px', fontSize: '0.85rem', textAlign: 'center' }}>
        Default: testuser / password
      </p>
    </div>
  );
}

export default LoginPage;