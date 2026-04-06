import React, { useState } from 'react';
import api from '../services/api';

const inputStyle = {
  padding: '10px', width: '100%', border: '1px solid #ddd',
  borderRadius: '4px', marginBottom: '12px', fontSize: '1rem',
};
const btnStyle = {
  padding: '12px 24px', background: '#e94560', color: '#fff',
  border: 'none', borderRadius: '4px', cursor: 'pointer',
  fontWeight: 'bold', fontSize: '1rem', width: '100%',
};

function LoginPage() {
  const [username, setUsername] = useState('testuser');
  const [password, setPassword] = useState('password');
  const [message, setMessage] = useState('');

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const keycloakUrl = process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8180';
      const res = await api.post(
        `${keycloakUrl}/realms/shopscale/protocol/openid-connect/token`,
        new URLSearchParams({
          grant_type: 'password',
          client_id: 'shopscale-gateway',
          username,
          password,
        }),
        { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
      );
      localStorage.setItem('access_token', res.data.access_token);
      localStorage.setItem('userId', username);
      setMessage('✅ Login successful! You can now place orders.');
    } catch (err) {
      setMessage('❌ Login failed. Check credentials or Keycloak status.');
    }
  };

  return (
    <div style={{ maxWidth: '400px', margin: '60px auto', background: '#fff', padding: '32px', borderRadius: '8px', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}>
      <h2 style={{ marginBottom: '24px', textAlign: 'center' }}>Login to ShopScale</h2>
      {message && <p style={{ padding: '12px', background: message.includes('✅') ? '#d4edda' : '#f8d7da', borderRadius: '4px', marginBottom: '12px' }}>{message}</p>}
      <form onSubmit={handleLogin}>
        <label>Username</label>
        <input style={inputStyle} value={username} onChange={(e) => setUsername(e.target.value)} />
        <label>Password</label>
        <input style={inputStyle} type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        <button type="submit" style={btnStyle}>Login via Keycloak</button>
      </form>
      <p style={{ marginTop: '16px', color: '#888', fontSize: '0.85rem', textAlign: 'center' }}>
        Default: testuser / password
      </p>
    </div>
  );
}

export default LoginPage;