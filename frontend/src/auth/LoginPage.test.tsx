import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { routerFuture } from '../app/routerFuture';
import { AuthProvider } from './AuthContext';
import { LoginPage } from './LoginPage';
import { SIGN_IN_PATH } from './RequireAuth';
import { inOneHour, makeJwt } from './testing';
import { getToken, setToken } from './tokenStorage';
import { LEGACY_MESSAGES } from './validation';
import { mockFetchJson } from '../test/utils';

function Landing() {
  const location = useLocation();
  return <p data-testid="landed">{`${location.pathname}${location.search}`}</p>;
}

function renderLogin(state?: { from: string }) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[{ pathname: SIGN_IN_PATH, state }]} future={routerFuture}>
        <Routes>
          <Route path={SIGN_IN_PATH} element={<LoginPage />} />
          <Route path="*" element={<Landing />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  );
}

const fields = () => ({
  username: screen.getByLabelText('Email:'),
  password: screen.getByLabelText('Password:'),
  remember: screen.getByRole('checkbox', { name: 'Remember me' }),
  submit: screen.getByRole('button', { name: 'Submit' }),
});

describe('LoginPage', () => {
  beforeEach(() => window.sessionStorage.clear());

  it('renders the legacy main.jsp fields', () => {
    renderLogin();
    const { username, password, remember, submit } = fields();

    expect(username).toHaveAttribute('name', 'email');
    expect(username).toHaveAttribute('placeholder', 'Username');
    expect(password).toHaveAttribute('type', 'password');
    expect(password).toHaveAttribute('placeholder', 'Password');
    expect(remember).not.toBeChecked();
    expect(submit).toBeEnabled();
  });

  it('shows validation hints and does not call the API when fields are blank', async () => {
    const fetchMock = mockFetchJson({});
    renderLogin();

    await userEvent.click(fields().submit);

    expect(screen.getByText('Enter your email or username.')).toBeInTheDocument();
    expect(screen.getByText('Enter your password.')).toBeInTheDocument();
    expect(fields().username).toHaveAttribute('aria-invalid', 'true');
    expect(fields().username).toHaveFocus();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('rejects a username with inner spaces', async () => {
    const fetchMock = mockFetchJson({});
    renderLogin();

    await userEvent.type(fields().username, 'tar ang');
    await userEvent.type(fields().password, 'parisdkh');
    await userEvent.click(fields().submit);

    expect(screen.getByText('Username must not contain spaces.')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('posts trimmed credentials to /api/auth/login, stores the token for the session and redirects', async () => {
    const token = makeJwt({ sub: 'user/login', username: 'tarang', scope: 'admin', exp: inOneHour() });
    const fetchMock = mockFetchJson({ token });
    renderLogin();

    await userEvent.type(fields().username, '  tarang  ');
    await userEvent.type(fields().password, 'parisdkh');
    await userEvent.click(fields().submit);

    expect(await screen.findByTestId('landed')).toHaveTextContent(/^\/$/);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/auth/login');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body as string)).toEqual({ username: 'tarang', password: 'parisdkh' });
    expect(window.sessionStorage.getItem('pharma.erp.token')).toBe(token);
    expect(window.localStorage.getItem('pharma.erp.token')).toBeNull();
  });

  it('persists the token in localStorage when "Remember me" is ticked', async () => {
    const token = makeJwt({ username: 'tarang', exp: inOneHour() });
    mockFetchJson({ token });
    renderLogin();

    await userEvent.type(fields().username, 'tarang');
    await userEvent.type(fields().password, 'parisdkh');
    await userEvent.click(fields().remember);
    await userEvent.click(fields().submit);

    await screen.findByTestId('landed');
    expect(window.localStorage.getItem('pharma.erp.token')).toBe(token);
  });

  it('returns to the page the guard redirected from', async () => {
    mockFetchJson({ token: makeJwt({ username: 'tarang', exp: inOneHour() }) });
    renderLogin({ from: '/inventory?page=2' });

    await userEvent.type(fields().username, 'tarang');
    await userEvent.type(fields().password, 'parisdkh');
    await userEvent.click(fields().submit);

    expect(await screen.findByTestId('landed')).toHaveTextContent('/inventory?page=2');
  });

  it('shows the legacy error hints on 401 and keeps the user signed out', async () => {
    mockFetchJson({ status: 401, detail: 'Bad credentials' }, 401, 'application/problem+json');
    renderLogin();

    await userEvent.type(fields().username, 'tarang');
    await userEvent.type(fields().password, 'wrongpass');
    await userEvent.click(fields().submit);

    expect(await screen.findByRole('alert')).toHaveTextContent('Sign-in failed');
    expect(screen.getByText(LEGACY_MESSAGES.unknownUser)).toBeInTheDocument();
    expect(screen.getByText(LEGACY_MESSAGES.wrongPassword)).toBeInTheDocument();
    expect(fields().submit).toBeEnabled();
    expect(getToken()).toBeNull();
    expect(screen.queryByTestId('landed')).not.toBeInTheDocument();
  });

  it('maps 400 problem+json field errors onto the form', async () => {
    mockFetchJson(
      { status: 400, detail: 'Validation failed', errors: { password: ['size must be between 8 and 64'] } },
      400,
      'application/problem+json',
    );
    renderLogin();

    await userEvent.type(fields().username, 'tarang');
    await userEvent.type(fields().password, 'x');
    await userEvent.click(fields().submit);

    expect(await screen.findByRole('alert')).toHaveTextContent('Validation failed');
    expect(screen.getByText('size must be between 8 and 64')).toBeInTheDocument();
  });

  it('reports an unreachable backend', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    renderLogin();

    await userEvent.type(fields().username, 'tarang');
    await userEvent.type(fields().password, 'parisdkh');
    await userEvent.click(fields().submit);

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the server');
  });

  it('reports a response without a token', async () => {
    mockFetchJson({ ok: true });
    renderLogin();

    await userEvent.type(fields().username, 'tarang');
    await userEvent.type(fields().password, 'parisdkh');
    await userEvent.click(fields().submit);

    expect(await screen.findByRole('alert')).toHaveTextContent('did not return an access token');
    await waitFor(() => expect(getToken()).toBeNull());
  });

  it('skips the form when already signed in', () => {
    setToken(makeJwt({ username: 'tarang', exp: inOneHour() }));
    renderLogin();

    expect(screen.getByTestId('landed')).toHaveTextContent(/^\/$/);
  });
});
