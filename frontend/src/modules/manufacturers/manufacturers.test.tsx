import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { setToken } from '../../auth/tokenStorage';
import { renderWithProviders } from '../../test/utils';
import manufacturers from './index';
import type { Manufacturer } from './api';
import { mockApi, page } from './testing';

const acme: Manufacturer = {
  id: 7,
  name: 'Sun Labs',
  type: 'MANUFACTURER',
  licenceNumber: 'MH-DL-2024/001',
  contactPerson: 'R. Mehta',
  address: '1 Main St',
  phone: '9876543210',
  email: 'acme@example.com',
  city: 'Pune',
  district: 'Pune',
  state: 'Maharashtra',
  gstin: '27AAPFU0939F1ZV',
};

function renderAt(route: string) {
  setToken('test-token');
  return renderWithProviders(
    <Routes>
      <Route path="/manufacturers/*" element={manufacturers.element} />
    </Routes>,
    { route },
  );
}

describe('manufacturers module', () => {
  it('registers a module definition', () => {
    expect(manufacturers).toMatchObject({ id: 'manufacturers', path: 'manufacturers', navLabel: 'Manufacturers' });
  });

  it('lists manufacturers with the bearer token and searches by name', async () => {
    const { calls, fn } = mockApi(() => ({ body: page([acme]) }));
    renderAt('/manufacturers');

    expect(await screen.findByText('Sun Labs')).toBeInTheDocument();
    expect(screen.getByText('27AAPFU0939F1ZV')).toBeInTheDocument();
    const headers = fn.mock.calls[0]?.[1]?.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer test-token');

    await userEvent.type(screen.getByRole('textbox', { name: 'Search manufacturers' }), 'sun');
    await userEvent.click(screen.getByRole('button', { name: 'Search' }));
    await waitFor(() => expect(calls.at(-1)?.url).toContain('q=sun'));
  });

  it('shows the API error when the list fails', async () => {
    mockApi(() => ({ status: 500, body: { status: 500, detail: 'Database unavailable' } }));
    renderAt('/manufacturers');
    expect(await screen.findByRole('alert')).toHaveTextContent('Database unavailable');
  });

  it('creates a manufacturer and returns to the list', async () => {
    const { calls } = mockApi(({ method }) => (method === 'POST' ? { status: 201, body: acme } : { body: page([acme]) }));
    renderAt('/manufacturers/new');

    await userEvent.type(screen.getByLabelText('Name'), '  Sun Labs ');
    await userEvent.type(screen.getByLabelText('GSTIN'), '27aapfu0939f1zv');
    await userEvent.click(screen.getByRole('button', { name: 'Save manufacturer' }));

    expect(await screen.findByRole('heading', { name: 'Manufacturers' })).toBeInTheDocument();
    const post = calls.find((c) => c.method === 'POST');
    expect(post?.url).toBe('/api/manufacturers');
    expect(post?.body).toMatchObject({ name: 'Sun Labs', type: 'MANUFACTURER', gstin: '27AAPFU0939F1ZV', email: null });
  });

  it('validates required fields and formats before calling the API', async () => {
    const { calls } = mockApi(() => undefined);
    renderAt('/manufacturers/new');

    await userEvent.type(screen.getByLabelText('Email'), 'not-an-email');
    await userEvent.click(screen.getByRole('button', { name: 'Save manufacturer' }));

    expect(await screen.findByText('Name is required')).toBeInTheDocument();
    expect(screen.getByText('Email must be a valid email address')).toBeInTheDocument();
    expect(calls).toHaveLength(0);
  });

  it('loads an existing manufacturer for editing and shows server validation errors', async () => {
    const { calls } = mockApi(({ method }) =>
      method === 'PUT'
        ? { status: 400, body: { status: 400, title: 'Validation failed', errors: { email: ['already in use'] } } }
        : { body: acme },
    );
    renderAt('/manufacturers/7/edit');

    expect(await screen.findByDisplayValue('Sun Labs')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Save manufacturer' }));

    expect(await screen.findByText('already in use')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Validation failed');
    expect(calls.find((c) => c.method === 'PUT')?.url).toBe('/api/manufacturers/7');
  });
});
