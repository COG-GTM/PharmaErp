import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { setToken } from '../../auth/tokenStorage';
import { renderWithProviders } from '../../test/utils';
import stores from './index';
import type { ManagerOption, Store } from './api';
import { mockApi, page } from './testing';

const central: Store = { id: 3, code: 'CEN-01', name: 'Central Store', address: 'MG Road', gstin: '27AABCP1234F1Z5', managerStaffId: 11 };
const asha: ManagerOption = { id: 11, fullName: 'Asha Rao' };
const ravi: ManagerOption = { id: 12, fullName: 'Ravi' };

function renderAt(route: string) {
  setToken('t');
  return renderWithProviders(
    <Routes>
      <Route path="/stores/*" element={stores.element} />
    </Routes>,
    { route },
  );
}

describe('stores module', () => {
  it('registers a module definition', () => {
    expect(stores).toMatchObject({ id: 'stores', path: 'stores', navLabel: 'Stores' });
  });

  it('lists stores with their manager', async () => {
    mockApi(({ url }) =>
      url.startsWith('/api/staff')
        ? { body: page([asha]) }
        : { body: page([central, { ...central, id: 4, code: 'ANX', name: 'Annex', managerStaffId: undefined as unknown as null }, { ...central, id: 5, name: 'Depot', managerStaffId: 99 }]) },
    );
    renderAt('/stores');

    expect(await screen.findByText('Central Store')).toBeInTheDocument();
    expect(await screen.findByText('Asha Rao')).toBeInTheDocument();
    expect(screen.getByText('Annex')).toBeInTheDocument();
    expect(screen.getByText('Staff #99')).toBeInTheDocument();
    // The backend omits null fields (non_null inclusion): an absent manager renders as a dash.
    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.queryByText(/undefined/)).not.toBeInTheDocument();
  });

  it('creates a store with a manager picked from staff', async () => {
    const { calls } = mockApi(({ method, url }) => {
      if (url.startsWith('/api/staff')) return { body: page([asha, ravi]) };
      if (method === 'POST') return { status: 201, body: central };
      return { body: page([central]) };
    });
    renderAt('/stores/new');

    await userEvent.type(screen.getByLabelText('Store code'), 'cen-01');
    await userEvent.type(screen.getByLabelText('Store name'), 'Central Store');
    await userEvent.type(screen.getByLabelText('GSTIN'), '27aabcp1234f1z5');
    await userEvent.selectOptions(await screen.findByLabelText('Manager'), await screen.findByRole('option', { name: 'Ravi' }));
    await userEvent.click(screen.getByRole('button', { name: 'Save store' }));

    expect(await screen.findByRole('heading', { name: 'Stores' })).toBeInTheDocument();
    expect(calls.find((c) => c.method === 'POST')?.body).toEqual({
      code: 'CEN-01',
      name: 'Central Store',
      address: null,
      gstin: '27AABCP1234F1Z5',
      managerStaffId: 12,
    });
  });

  it('still allows editing when the staff endpoint fails and reports conflicts', async () => {
    const { calls } = mockApi(({ method, url }) => {
      if (url.startsWith('/api/staff')) return { status: 500, body: { status: 500 } };
      if (method === 'PUT') return { status: 409, body: { status: 409, title: 'Conflict', detail: 'Store name already exists' } };
      return { body: central };
    });
    renderAt('/stores/3/edit');

    expect(await screen.findByDisplayValue('Central Store')).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Staff #11' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Save store' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Store name already exists');
    expect(calls.find((c) => c.method === 'PUT')).toMatchObject({ url: '/api/stores/3', body: { managerStaffId: 11, code: 'CEN-01' } });
  });

  it('requires a store code and name and checks the GSTIN format', async () => {
    const { calls } = mockApi(({ url }) => (url.startsWith('/api/staff') ? { body: page([]) } : undefined));
    renderAt('/stores/new');
    await userEvent.type(screen.getByLabelText('GSTIN'), 'NOTAGSTIN');
    await userEvent.click(screen.getByRole('button', { name: 'Save store' }));
    expect(await screen.findByText('Store name is required')).toBeInTheDocument();
    expect(screen.getByText('Store code is required')).toBeInTheDocument();
    expect(screen.getByText('GSTIN must be 15 characters, e.g. 27AABCP1234F1Z5')).toBeInTheDocument();
    expect(calls.filter((c) => c.method !== 'GET')).toHaveLength(0);
  });
});
