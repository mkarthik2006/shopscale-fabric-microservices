import React, { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopBar from './TopBar';
import Footer from './Footer';

function Layout({ children, isAuthenticated, isAdmin, user, isAuthRoute }) {
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    setSidebarOpen(false);
  }, [location.pathname]);

  if (isAuthRoute) {
    return <div className="app-shell app-shell--auth">{children}</div>;
  }

  return (
    <div className="app-shell">
      <Sidebar
        isAdmin={isAdmin}
        isAuthenticated={isAuthenticated}
        user={user}
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
      />
      <TopBar
        isAuthenticated={isAuthenticated}
        isAdmin={isAdmin}
        user={user}
        onMenuClick={() => setSidebarOpen((s) => !s)}
      />
      <main className="main" id="main-content">
        <div className="main__inner fade-in">{children}</div>
        <Footer />
      </main>
    </div>
  );
}

export default Layout;
