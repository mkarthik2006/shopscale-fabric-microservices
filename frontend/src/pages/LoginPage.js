import React, { useEffect, useState } from 'react';
import api, { setAccessToken, getApiErrorMessage, beginPkceLogin, consumePkceState } from '../services/api';
import { showErrorToast, showSuccessToast } from '../utils/toast';
import { useLocation, useNavigate } from 'react-router-dom';
import Icon from '../components/Icon';
import Logo from '../components/Logo';

function FeatureRow({ icon, title, desc }) {
  return (
    <div className="auth-panel__feature">
      <span className="auth-panel__feature-icon">
        <Icon name={icon} size={16} />
      </span>
      <span>
        <span className="auth-panel__feature-title">{title}</span>
        <div className="auth-panel__feature-desc">{desc}</div>
      </span>
    </div>
  );
}

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
    <div className="auth-shell">
      {/* LEFT — brand panel */}
      <section className="auth-panel" aria-hidden="true">
        <div className="auth-panel__brand">
          <Logo size={36} withText />
        </div>

        <div className="auth-panel__pitch">
          <span
            style={{
              display: 'inline-block',
              fontSize: '0.7rem',
              fontWeight: 700,
              letterSpacing: '0.12em',
              textTransform: 'uppercase',
              padding: '4px 10px',
              borderRadius: 999,
              background: 'rgba(124, 58, 237, 0.18)',
              color: '#c4b5fd',
              border: '1px solid rgba(124, 58, 237, 0.3)',
              marginBottom: 16,
            }}
          >
            Enterprise Edition
          </span>
          <h1>The event-driven commerce platform.</h1>
          <p>
            Real-time orders. Distributed inventory. Saga-backed reliability.
            Sign in with Keycloak SSO to access the ShopScale Fabric console.
          </p>
        </div>

        <div className="auth-panel__features">
          <FeatureRow
            icon="shield"
            title="OAuth2 + PKCE"
            desc="Secure Authorization Code with PKCE flow via Keycloak"
          />
          <FeatureRow
            icon="zap"
            title="Kafka SAGA"
            desc="Order → Inventory → Notification choreography"
          />
          <FeatureRow
            icon="layers"
            title="Microservices"
            desc="9 Spring Boot services behind a Spring Cloud Gateway"
          />
        </div>

        <div className="auth-panel__footer">
          © {new Date().getFullYear()} ShopScale Fabric · Java 21 · Spring Boot 3.3
        </div>
      </section>

      {/* RIGHT — auth form */}
      <section className="auth-form-wrap">
        <div className="auth-card">
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 18 }}>
            <Logo size={32} />
            <span style={{ color: 'var(--text-muted)', fontSize: 12, fontWeight: 600, letterSpacing: '0.08em', textTransform: 'uppercase' }}>
              Sign in to your console
            </span>
          </div>

          {/* keep "Login to ShopScale" text exactly for tests */}
          <h2 className="auth-card__title">Login to ShopScale</h2>
          <p className="auth-card__subtitle">
            Continue securely with Authorization Code + PKCE flow via Keycloak.
          </p>

          {message.text && (
            <div className={`alert ${message.type === 'success' ? 'alert--success' : 'alert--danger'}`} role="status">
              <Icon
                name={message.type === 'success' ? 'check' : 'warning'}
                size={18}
                className="alert__icon"
              />
              <span>{message.text}</span>
            </div>
          )}

          <button
            type="button"
            className="btn btn-primary btn-lg btn-block"
            disabled={loading}
            onClick={handleLogin}
            style={{ marginTop: 18 }}
          >
            {loading ? (
              <>
                <span className="spinner" /> Redirecting...
              </>
            ) : (
              <>
                <Icon name="shield" size={16} />
                Continue with Keycloak
              </>
            )}
          </button>

          <div className="auth-card__divider">Demo credentials</div>

          <div className="auth-credentials" aria-label="Demo credentials">
            <div className="auth-credentials__title">
              <Icon name="sparkles" size={14} />
              Use these in the Keycloak login form
            </div>
            <div className="auth-credentials__row">
              <span className="auth-credentials__role">User</span>
              <code>testuser</code>
              <code>Test@1234</code>
            </div>
            <div className="auth-credentials__row">
              <span className="auth-credentials__role">Admin</span>
              <code>admin</code>
              <code>Admin@1234</code>
            </div>
          </div>

          <p className="muted text-xs" style={{ marginTop: 16, textAlign: 'center' }}>
            By continuing you agree to ShopScale Fabric’s acceptable-use policy.
          </p>
        </div>
      </section>
    </div>
  );
}

export default LoginPage;
