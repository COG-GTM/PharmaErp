import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { requestLogout, SIGN_IN_PATH, useAuth } from '../auth';
import type { ModuleDefinition } from '../modules/types';
import { Logo } from './Logo';

export function Layout({ modules }: { modules: ModuleDefinition[] }) {
  const { isAuthenticated, user, signOut } = useAuth();
  const [navOpen, setNavOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    setNavOpen(false);
  }, [location.pathname]);

  const visible = modules.filter((m) => !m.hideFromNav && (isAuthenticated || m.requiresAuth === false));

  const handleSignOut = () => {
    void requestLogout();
    signOut();
    navigate(SIGN_IN_PATH, { replace: true });
  };

  return (
    <div className={`shell${navOpen ? ' shell--nav-open' : ''}`}>
      <header className="shell-header">
        <button
          type="button"
          className="shell-header__toggle"
          aria-label="Toggle navigation"
          aria-controls="shell-nav"
          aria-expanded={navOpen}
          onClick={() => setNavOpen((open) => !open)}
        >
          <span aria-hidden="true">☰</span>
        </button>
        <Link to="/" className="shell-header__brand">
          <Logo size={30} />
          <span>Pharma ERP</span>
        </Link>
        <div className="shell-header__user">
          {isAuthenticated ? (
            <>
              <span className="user-chip" title={user?.scopes.length ? `Scopes: ${user.scopes.join(', ')}` : undefined}>
                <span className="user-chip__avatar" aria-hidden="true">
                  {(user?.username ?? '?').charAt(0).toUpperCase()}
                </span>
                <span className="user-chip__name">{user?.username ?? 'Signed in'}</span>
              </span>
              <button type="button" className="btn btn--ghost" onClick={handleSignOut}>
                Sign out
              </button>
            </>
          ) : (
            <Link to={SIGN_IN_PATH} className="btn btn--ghost">
              Sign in
            </Link>
          )}
        </div>
      </header>
      <nav id="shell-nav" className="shell-nav" aria-label="Modules">
        <ul>
          {visible.map((m) => (
            <li key={m.id}>
              <NavLink to={`/${m.path}`}>{m.navLabel}</NavLink>
            </li>
          ))}
        </ul>
      </nav>
      {navOpen && <div className="shell-backdrop" aria-hidden="true" onClick={() => setNavOpen(false)} />}
      <main className="shell-main">
        <Outlet />
      </main>
    </div>
  );
}
