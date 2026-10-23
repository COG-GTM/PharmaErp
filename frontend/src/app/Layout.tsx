import { NavLink, Outlet } from 'react-router-dom';
import type { ModuleDefinition } from '../modules/types';

export function Layout({ modules }: { modules: ModuleDefinition[] }) {
  return (
    <div className="shell">
      <header className="shell-header">
        <h1>Pharma ERP</h1>
      </header>
      <nav className="shell-nav" aria-label="Modules">
        <ul>
          {modules
            .filter((m) => !m.hideFromNav)
            .map((m) => (
              <li key={m.id}>
                <NavLink to={`/${m.path}`}>{m.navLabel}</NavLink>
              </li>
            ))}
        </ul>
      </nav>
      <main className="shell-main">
        <Outlet />
      </main>
    </div>
  );
}
