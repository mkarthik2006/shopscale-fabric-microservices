import React from 'react';
import Logo from './Logo';

function Footer() {
  const year = new Date().getFullYear();
  return (
    <footer className="app-footer">
      <span className="app-footer__brand">
        <Logo size={20} />
        <span>
          ShopScale Fabric
          <span className="muted" style={{ marginLeft: 6 }}>
            · Event-driven commerce
          </span>
        </span>
      </span>
      <span className="app-footer__links">
        <span className="muted">© {year} ShopScale Fabric</span>
        <span className="muted">·</span>
        <span className="muted">Java 21 · Spring Boot · Kafka · Keycloak</span>
      </span>
    </footer>
  );
}

export default Footer;
