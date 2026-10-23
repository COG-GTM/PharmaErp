import { createElement } from 'react';
import { screen } from '@testing-library/react';
import { App } from './App';
import type { ModuleDefinition } from '../modules/types';
import { setToken } from '../auth/tokenStorage';
import { mockFetchJson, renderWithProviders } from '../test/utils';

const secured: ModuleDefinition = {
  id: 'secured',
  path: 'secured',
  navLabel: 'Secured',
  element: createElement('p', null, 'secret content'),
  order: 1,
};

describe('App shell', () => {
  it('renders navigation for registered modules and the health page', async () => {
    mockFetchJson({ status: 'UP' });

    renderWithProviders(<App />, { route: '/health' });

    expect(screen.getByRole('link', { name: 'System health' })).toBeInTheDocument();
    expect(await screen.findByTestId('health-status')).toHaveTextContent('UP');
  });

  it('guards modules that require auth', () => {
    renderWithProviders(<App modules={[secured]} />, { route: '/secured' });

    expect(screen.getByRole('alert')).toHaveTextContent('Sign in required');
    expect(screen.queryByText('secret content')).not.toBeInTheDocument();
  });

  it('renders protected modules when a token is present', () => {
    setToken('t');

    renderWithProviders(<App modules={[secured]} />, { route: '/secured' });

    expect(screen.getByText('secret content')).toBeInTheDocument();
  });
});
