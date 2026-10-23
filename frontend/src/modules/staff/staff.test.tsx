import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { setToken } from '../../auth/tokenStorage';
import { renderWithProviders } from '../../test/utils';
import staff from './index';
import type { Staff } from './api';
import { mockApi, page } from './testing';

const asha: Staff = {
  id: 11,
  fullName: 'Asha Rao',
  loginType: { id: 3, name: 'Manager' },
  username: 'asha_rao01',
  storeId: 2,
  staffType: 'INTERNAL',
  firstName: 'Asha',
  middleName: null,
  lastName: 'Rao',
  dateOfBirth: '1990-04-01',
  joinDate: '2020-01-15',
  experience: '5 years',
  salary: 55000,
  homeTown: 'Pune',
  homeTownAddress: null,
  jobAddress: null,
  leavingAddress: null,
  mobileNumber: '9876543210',
  email: 'asha@example.com',
  lastCompany: null,
  resumePath: null,
  feedback: 'Great team lead',
  suggestion: null,
  active: true,
};

const lookups = ({ url }: { url: string }) => {
  if (url.startsWith('/api/login-types'))
    return { body: page([{ id: 1, name: 'jr.Employee' }, { id: 2, name: 'sr.Employee' }, { id: 3, name: 'Manager' }]) };
  if (url.startsWith('/api/stores')) return { body: page([{ id: 2, code: 'CEN', name: 'Central Store' }]) };
  return undefined;
};

function renderAt(route: string) {
  setToken('t');
  return renderWithProviders(
    <Routes>
      <Route path="/staff/*" element={staff.element} />
    </Routes>,
    { route },
  );
}

describe('staff module', () => {
  it('registers a module definition', () => {
    expect(staff).toMatchObject({ id: 'staff', path: 'staff', navLabel: 'Staff' });
  });

  it('lists staff with type, login type, store and status', async () => {
    mockApi(
      (call) =>
        lookups(call) ?? {
          body: page([asha, { ...asha, id: 12, fullName: 'Ravi', firstName: 'Ravi', lastName: null, loginType: null, storeId: 7, active: false, staffType: 'SUPPLIER' }, { ...asha, id: 13, fullName: 'Meera', firstName: 'Meera', loginType: null, storeId: undefined as unknown as null }]),
        },
    );
    renderAt('/staff');

    expect(await screen.findByText('Asha Rao')).toBeInTheDocument();
    expect(screen.getByText('SUPPLIER')).toBeInTheDocument();
    expect(screen.getByText('Inactive')).toBeInTheDocument();
    expect(screen.getByText('Manager')).toBeInTheDocument();
    expect(await screen.findByText('Central Store')).toBeInTheDocument();
    expect(screen.getByText('Store #7')).toBeInTheDocument();
    // The backend omits null fields (non_null inclusion): an absent store must not render as "Store #undefined".
    expect(await screen.findByText('Meera')).toBeInTheDocument();
    expect(screen.queryByText(/undefined/)).not.toBeInTheDocument();
  });

  it('creates a staff member with lookups and typed values', async () => {
    const { calls } = mockApi((call) => lookups(call) ?? (call.method === 'POST' ? { status: 201, body: asha } : { body: page([asha]) }));
    renderAt('/staff/new');

    await userEvent.selectOptions(screen.getByLabelText('Staff type'), 'DISTRIBUTOR');
    await userEvent.selectOptions(screen.getByLabelText('Login type'), await screen.findByRole('option', { name: 'sr.Employee' }));
    await userEvent.selectOptions(screen.getByLabelText('Store'), await screen.findByRole('option', { name: 'Central Store' }));
    await userEvent.type(screen.getByLabelText('First name'), 'Ravi');
    await userEvent.type(screen.getByLabelText('Salary (₹)'), '42000.50');
    await userEvent.type(screen.getByLabelText('Join date'), '2024-02-01');
    await userEvent.click(screen.getByRole('button', { name: 'Save staff member' }));

    expect(await screen.findByRole('heading', { name: 'Staff' })).toBeInTheDocument();
    expect(calls.find((c) => c.method === 'POST')?.body).toMatchObject({
      firstName: 'Ravi',
      staffType: 'DISTRIBUTOR',
      loginTypeId: 2,
      storeId: 2,
      salary: 42000.5,
      joinDate: '2024-02-01',
      email: null,
      active: true,
    });
  });

  it('edits an existing staff member and can deactivate them', async () => {
    const { calls } = mockApi((call) => lookups(call) ?? (call.url.startsWith('/api/staff?') ? { body: page([asha]) } : { body: asha }));
    renderAt('/staff/11/edit');

    expect(await screen.findByDisplayValue('Asha')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('Active'));
    await userEvent.click(screen.getByRole('button', { name: 'Save staff member' }));

    await screen.findByRole('heading', { name: 'Staff' });
    expect(calls.find((c) => c.method === 'PUT')).toMatchObject({
      url: '/api/staff/11',
      body: { firstName: 'Asha', loginTypeId: 3, storeId: 2, salary: 55000, feedback: 'Great team lead', active: false },
    });
  });

  it('validates required name, email, mobile, username and salary', async () => {
    const { calls } = mockApi((call) => lookups(call));
    renderAt('/staff/new');

    await userEvent.type(screen.getByLabelText('Email'), 'nope');
    await userEvent.type(screen.getByLabelText('Salary (₹)'), '-5');
    await userEvent.type(screen.getByLabelText('Mobile number'), '12345');
    await userEvent.type(screen.getByLabelText('System username'), '123456789');
    await userEvent.click(screen.getByRole('button', { name: 'Save staff member' }));

    expect(screen.getByText('First name is required')).toBeInTheDocument();
    expect(screen.getByText('Email must be a valid email address')).toBeInTheDocument();
    expect(screen.getByText('Salary must be a positive amount with up to 2 decimals')).toBeInTheDocument();
    expect(screen.getByText('Mobile number must be 10-15 digits, optionally prefixed with +')).toBeInTheDocument();
    expect(screen.getByText('Username must be 8-12 letters, digits or underscores and not only digits')).toBeInTheDocument();
    expect(calls.filter((c) => c.method !== 'GET')).toHaveLength(0);
  });
});
