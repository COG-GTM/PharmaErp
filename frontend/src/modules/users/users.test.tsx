import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { setToken } from '../../auth/tokenStorage';
import { renderWithProviders } from '../../test/utils';
import users from './index';
import { passwordError, usernameError } from './api';
import { mockApi, page } from './testing';

function renderAt(route: string) {
  setToken('t');
  return renderWithProviders(
    <Routes>
      <Route path="/users/*" element={users.element} />
    </Routes>,
    { route },
  );
}

describe('legacy user validation rules', () => {
  it.each([
    ['', 'Username is required'],
    ['tarang parikh', 'Username must not contain spaces'],
    ['12345678', 'Username must not be only numbers'],
    ['tarang-pa', 'Username may only contain letters, digits and underscores'],
    ['short', 'Username must be 8-12 characters'],
    ['waytoolongusername', 'Username must be 8-12 characters'],
  ])('rejects username %j', (input, message) => {
    expect(usernameError(input)).toBe(message);
  });

  it('accepts trimmed 8-12 word-character usernames', () => {
    expect(usernameError('  tarangparikh ')).toBeNull();
    expect(usernameError(' pharma_admin ')).toBeNull();
  });

  it('applies the password rules', () => {
    expect(passwordError('short')).toBe('Password must be at least 8 characters');
    expect(passwordError('has space1')).toBe('Password must not contain spaces');
    expect(passwordError(' secret123 ')).toBeNull();
  });
});

describe('users module', () => {
  it('registers a module definition', () => {
    expect(users).toMatchObject({ id: 'users', path: 'users', navLabel: 'Users' });
  });

  it('lists users', async () => {
    mockApi(() => ({ body: page([{ username: 'pharma_admin' }, { username: 'storekeeper' }]) }));
    renderAt('/users');
    expect(await screen.findByText('pharma_admin')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Change password for storekeeper' })).toHaveAttribute('href', '/users/storekeeper/edit');
  });

  it('registers a user with trimmed credentials', async () => {
    const { calls } = mockApi(({ method }) =>
      method === 'POST' ? { status: 201, body: { username: 'pharma_admin' } } : { body: page([{ username: 'pharma_admin' }]) },
    );
    renderAt('/users/new');

    await userEvent.type(screen.getByLabelText('Username'), ' pharma_admin ');
    await userEvent.type(screen.getByLabelText('Password'), 'secret123');
    await userEvent.type(screen.getByLabelText('Confirm password'), 'secret123');
    await userEvent.click(screen.getByRole('button', { name: 'Register' }));

    expect(await screen.findByText('pharma_admin')).toBeInTheDocument();
    expect(calls.find((c) => c.method === 'POST')).toEqual({
      method: 'POST',
      url: '/api/users',
      body: { username: 'pharma_admin', password: 'secret123' },
    });
  });

  it('blocks invalid input client-side', async () => {
    const { calls } = mockApi(() => undefined);
    renderAt('/users/new');

    await userEvent.type(screen.getByLabelText('Username'), '12345678');
    await userEvent.type(screen.getByLabelText('Password'), 'secret123');
    await userEvent.type(screen.getByLabelText('Confirm password'), 'secret124');
    await userEvent.click(screen.getByRole('button', { name: 'Register' }));

    expect(screen.getByText('Username must not be only numbers')).toBeInTheDocument();
    expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
    expect(calls).toHaveLength(0);
  });

  it('shows a conflict when the username is taken', async () => {
    mockApi(() => ({ status: 409, body: { status: 409, title: 'Conflict', detail: 'Username already exists' } }));
    renderAt('/users/new');

    await userEvent.type(screen.getByLabelText('Username'), 'pharma_admin');
    await userEvent.type(screen.getByLabelText('Password'), 'secret123');
    await userEvent.type(screen.getByLabelText('Confirm password'), 'secret123');
    await userEvent.click(screen.getByRole('button', { name: 'Register' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Username already exists');
  });

  it('changes the password of an existing user', async () => {
    const { calls } = mockApi(({ method }) =>
      method === 'PUT' ? { status: 204 } : { body: page([{ username: 'store_keeper' }]) },
    );
    renderAt('/users/store_keeper/edit');

    expect(screen.getByRole('heading', { name: 'Change password for store_keeper' })).toBeInTheDocument();
    expect(screen.queryByLabelText('Username')).not.toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Password'), 'newsecret1');
    await userEvent.type(screen.getByLabelText('Confirm password'), 'newsecret1');
    await userEvent.click(screen.getByRole('button', { name: 'Change password' }));

    expect(await screen.findByRole('heading', { name: 'Users' })).toBeInTheDocument();
    expect(calls.find((c) => c.method === 'PUT')).toMatchObject({ url: '/api/users/store_keeper/password', body: { password: 'newsecret1' } });
  });
});
