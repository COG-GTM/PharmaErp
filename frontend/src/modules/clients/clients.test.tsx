import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { setToken } from '../../auth/tokenStorage';
import { renderWithProviders } from '../../test/utils';
import clients from './index';
import type { Client } from './api';
import { mockApi, page } from './testing';

const acme: Client = {
  id: 7,
  name: 'Acme Pharmacy',
  address: '1 Main St',
  phone: '9876543210',
  email: 'acme@example.com',
  city: 'Pune',
  district: 'Pune',
  state: 'Maharashtra',
  gstin: '27AAPFU0939F1ZV',
  feedbackEmail: null,
};

function renderAt(route: string) {
  setToken('test-token');
  return renderWithProviders(
    <Routes>
      <Route path="/clients/*" element={clients.element} />
    </Routes>,
    { route },
  );
}

describe('clients module', () => {
  it('registers a module definition', () => {
    expect(clients).toMatchObject({ id: 'clients', path: 'clients', navLabel: 'Clients' });
  });

  it('lists clients with the bearer token and searches by name', async () => {
    const { calls, fn } = mockApi(() => ({ body: page([acme]) }));
    renderAt('/clients');

    expect(await screen.findByText('Acme Pharmacy')).toBeInTheDocument();
    expect(screen.getByText('27AAPFU0939F1ZV')).toBeInTheDocument();
    const headers = fn.mock.calls[0]?.[1]?.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer test-token');

    await userEvent.type(screen.getByRole('textbox', { name: 'Search clients' }), 'acme');
    await userEvent.click(screen.getByRole('button', { name: 'Search' }));
    await waitFor(() => expect(calls.at(-1)?.url).toContain('q=acme'));
  });

  it('shows the API error when the list fails', async () => {
    mockApi(() => ({ status: 500, body: { status: 500, detail: 'Database unavailable' } }));
    renderAt('/clients');
    expect(await screen.findByRole('alert')).toHaveTextContent('Database unavailable');
  });

  it('drops stale rows when a later search fails', async () => {
    mockApi(({ url }) =>
      url.includes('q=beta') ? { status: 500, body: { status: 500, detail: 'Search failed' } } : { body: page([acme]) },
    );
    renderAt('/clients');
    expect(await screen.findByText('Acme Pharmacy')).toBeInTheDocument();
    await userEvent.type(screen.getByRole('textbox', { name: 'Search clients' }), 'beta');
    await userEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Search failed');
    expect(screen.queryByText('Acme Pharmacy')).not.toBeInTheDocument();
  });

  it('creates a client and returns to the list', async () => {
    const { calls } = mockApi(({ method }) => (method === 'POST' ? { status: 201, body: acme } : { body: page([acme]) }));
    renderAt('/clients/new');

    await userEvent.type(screen.getByLabelText('Name'), '  Acme Pharmacy ');
    await userEvent.type(screen.getByLabelText('GSTIN'), '27aapfu0939f1zv');
    await userEvent.click(screen.getByRole('button', { name: 'Save client' }));

    expect(await screen.findByRole('heading', { name: 'Clients' })).toBeInTheDocument();
    const post = calls.find((c) => c.method === 'POST');
    expect(post?.url).toBe('/api/clients');
    expect(post?.body).toMatchObject({ name: 'Acme Pharmacy', gstin: '27AAPFU0939F1ZV', email: null });
  });

  it('validates required fields and formats before calling the API', async () => {
    const { calls } = mockApi(() => undefined);
    renderAt('/clients/new');

    await userEvent.type(screen.getByLabelText('Email'), 'not-an-email');
    await userEvent.click(screen.getByRole('button', { name: 'Save client' }));

    expect(await screen.findByText('Name is required')).toBeInTheDocument();
    expect(screen.getByText('Email must be a valid email address')).toBeInTheDocument();
    expect(calls).toHaveLength(0);
  });

  it('loads an existing client for editing and shows server validation errors', async () => {
    const { calls } = mockApi(({ method }) =>
      method === 'PUT'
        ? { status: 400, body: { status: 400, title: 'Validation failed', errors: { email: ['already in use'] } } }
        : { body: acme },
    );
    renderAt('/clients/7/edit');

    expect(await screen.findByDisplayValue('Acme Pharmacy')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Save client' }));

    expect(await screen.findByText('already in use')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Validation failed');
    expect(calls.find((c) => c.method === 'PUT')?.url).toBe('/api/clients/7');
  });
});
