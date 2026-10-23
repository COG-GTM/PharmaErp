import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { LoginPage, RequireAuth, SIGN_IN_PATH } from '../auth';
import { modules as registeredModules } from '../modules/registry';
import type { ModuleDefinition } from '../modules/types';
import { Layout } from './Layout';

function NotFound() {
  return (
    <section className="card empty-state">
      <h2>Page not found</h2>
      <p>
        The page you requested does not exist. <Link to="/">Go to the start page</Link>.
      </p>
    </section>
  );
}

export function App({ modules = registeredModules }: { modules?: ModuleDefinition[] }) {
  const home = modules.find((m) => !m.hideFromNav);
  return (
    <Routes>
      <Route path={SIGN_IN_PATH} element={<LoginPage />} />
      <Route element={<Layout modules={modules} />}>
        <Route
          index
          element={home ? <Navigate to={`/${home.path}`} replace /> : <p className="card">No modules installed.</p>}
        />
        {modules.map((m) => (
          <Route
            key={m.id}
            path={`${m.path}/*`}
            element={m.requiresAuth === false ? m.element : <RequireAuth>{m.element}</RequireAuth>}
          />
        ))}
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
