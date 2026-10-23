import { createElement } from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from './App';
import type { ModuleDefinition } from '../modules/types';
import { inOneHour, makeJwt } from '../auth/testing';
import { getToken, setToken } from '../auth/tokenStorage';
import { mockFetchJson, renderWithProviders } from '../test/utils';

const mod = (id: string, overrides: Partial<ModuleDefinition> = {}): ModuleDefinition => ({
  id,
  path: id,
  navLabel: id.charAt(0).toUpperCase() + id.slice(1),
  element: createElement('p', null, `${id} page`),
  ...overrides,
});

const modules = [mod('inventory', { order: 1 }), mod('clients', { order: 2 }), mod('hidden', { hideFromNav: true }), mod('status', { requiresAuth: false, order: 99 })];

describe('Layout', () => {
  it('builds the sidebar from registered modules and shows the signed-in user', () => {
    setToken(makeJwt({ username: 'tarang', scope: 'admin', exp: inOneHour() }));

    renderWithProviders(<App modules={modules} />, { route: '/clients' });

    const nav = screen.getByRole('navigation', { name: 'Modules' });
    const labels = Array.from(nav.querySelectorAll('a')).map((a) => a.textContent);
    expect(labels).toEqual(['Inventory', 'Clients', 'Status']);
    expect(screen.getByRole('link', { name: 'Clients' })).toHaveClass('active');
    expect(screen.getByText('tarang')).toBeInTheDocument();
  });

  it('only lists public modules to anonymous visitors', () => {
    renderWithProviders(<App modules={modules} />, { route: '/status' });

    const nav = screen.getByRole('navigation', { name: 'Modules' });
    expect(Array.from(nav.querySelectorAll('a')).map((a) => a.textContent)).toEqual(['Status']);
    expect(screen.getByRole('link', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('toggles the mobile navigation', async () => {
    setToken('t');
    renderWithProviders(<App modules={modules} />, { route: '/inventory' });

    const toggle = screen.getByRole('button', { name: 'Toggle navigation' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await userEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    await userEvent.click(screen.getByRole('link', { name: 'Clients' }));
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
  });

  it('signs out, clears the token, notifies the backend and returns to the sign-in screen', async () => {
    setToken(makeJwt({ username: 'tarang', exp: inOneHour() }));
    const fetchMock = mockFetchJson({});
    renderWithProviders(<App modules={modules} />, { route: '/inventory' });

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }));

    expect(getToken()).toBeNull();
    expect(screen.getByLabelText('Email:')).toBeInTheDocument();
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/auth/logout');
    expect(init.method).toBe('POST');
  });
});
