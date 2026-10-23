import { Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth } from '../auth';
import { modules as registeredModules } from '../modules/registry';
import type { ModuleDefinition } from '../modules/types';
import { Layout } from './Layout';

export function App({ modules = registeredModules }: { modules?: ModuleDefinition[] }) {
  const home = modules.find((m) => !m.hideFromNav);
  return (
    <Routes>
      <Route element={<Layout modules={modules} />}>
        <Route index element={home ? <Navigate to={`/${home.path}`} replace /> : <p>No modules installed.</p>} />
        {modules.map((m) => (
          <Route
            key={m.id}
            path={`${m.path}/*`}
            element={m.requiresAuth === false ? m.element : <RequireAuth>{m.element}</RequireAuth>}
          />
        ))}
        <Route path="*" element={<p>Page not found.</p>} />
      </Route>
    </Routes>
  );
}
