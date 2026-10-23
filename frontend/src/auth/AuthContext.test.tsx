import { act, render, screen } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';
import { UNAUTHORIZED_EVENT } from '../api/client';
import { makeJwt } from './testing';
import { getToken, setToken } from './tokenStorage';

function Probe() {
  const { isAuthenticated, user } = useAuth();
  return (
    <p data-testid="probe">
      {isAuthenticated ? `in:${user?.username ?? '-'}:${user?.scopes.join(',')}` : 'out'}
    </p>
  );
}

const renderProbe = () =>
  render(
    <AuthProvider>
      <Probe />
    </AuthProvider>,
  );

describe('AuthProvider', () => {
  beforeEach(() => window.sessionStorage.clear());
  afterEach(() => vi.useRealTimers());

  it('exposes the username and scopes from the JWT', () => {
    setToken(makeJwt({ username: 'tarang', scope: 'admin audit', exp: Math.floor(Date.now() / 1000) + 60 }));

    renderProbe();

    expect(screen.getByTestId('probe')).toHaveTextContent('in:tarang:admin,audit');
  });

  it('signs out automatically when the token expires', () => {
    vi.useFakeTimers();
    setToken(makeJwt({ username: 'tarang', exp: Math.floor(Date.now() / 1000) + 5 }));
    renderProbe();
    expect(screen.getByTestId('probe')).toHaveTextContent('in:tarang');

    act(() => {
      vi.advanceTimersByTime(6000);
    });

    expect(screen.getByTestId('probe')).toHaveTextContent('out');
    expect(getToken()).toBeNull();
  });

  it('keeps long-lived sessions until the real expiry beyond the setTimeout cap', () => {
    vi.useFakeTimers();
    const day = 24 * 60 * 60 * 1000;
    setToken(makeJwt({ username: 'tarang', exp: Math.floor((Date.now() + 30 * day) / 1000) }));
    renderProbe();

    act(() => {
      vi.advanceTimersByTime(26 * day);
    });
    expect(screen.getByTestId('probe')).toHaveTextContent('in:tarang');

    act(() => {
      vi.advanceTimersByTime(5 * day);
    });
    expect(screen.getByTestId('probe')).toHaveTextContent('out');
  });

  it('signs out when the API reports 401', () => {
    setToken('opaque-token');
    renderProbe();
    expect(screen.getByTestId('probe')).toHaveTextContent('in:-');

    act(() => {
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    });

    expect(screen.getByTestId('probe')).toHaveTextContent('out');
  });
});
