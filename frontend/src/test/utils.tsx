import { render, type RenderOptions } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { routerFuture } from '../app/routerFuture';
import { AuthProvider } from '../auth';

/** Renders inside AuthProvider + MemoryRouter at `route`. */
export function renderWithProviders(ui: ReactElement, { route = '/', ...options }: RenderOptions & { route?: string } = {}) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[route]} future={routerFuture}>{ui}</MemoryRouter>
    </AuthProvider>,
    options,
  );
}

/** Stubs global fetch with a single JSON response. */
export function mockFetchJson(body: unknown, status = 200, contentType = 'application/json') {
  const fn = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': contentType } }),
  );
  vi.stubGlobal('fetch', fn);
  return fn;
}
