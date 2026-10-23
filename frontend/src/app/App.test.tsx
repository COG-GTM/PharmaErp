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

  it('redirects to the sign-in screen for modules that require auth', () => {
    renderWithProviders(<App modules={[secured]} />, { route: '/secured' });

    expect(screen.getByRole('button', { name: 'Submit' })).toBeInTheDocument();
    expect(screen.getByLabelText('Email:')).toBeInTheDocument();
    expect(screen.queryByText('secret content')).not.toBeInTheDocument();
  });

  it('renders protected modules when a token is present', () => {
    setToken('t');

    renderWithProviders(<App modules={[secured]} />, { route: '/secured' });

    expect(screen.getByText('secret content')).toBeInTheDocument();
  });

  it('lands on the dashboard after sign-in by default', async () => {
    setToken('t');
    mockFetchJson({ totalElements: 0, content: [] });

    renderWithProviders(<App />, { route: '/' });

    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    expect(await screen.findByText(/Last updated/)).toBeInTheDocument();
  });

  it('shows a not-found page for unknown routes', () => {
    renderWithProviders(<App modules={[secured]} />, { route: '/nope' });

    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
  });
});
