import { screen } from '@testing-library/react';
import { Route, Routes, useLocation } from 'react-router-dom';
import { RequireAuth, SIGN_IN_PATH, type SignInLocationState } from './RequireAuth';
import { inOneHour, makeJwt } from './testing';
import { getToken, setToken } from './tokenStorage';
import { renderWithProviders } from '../test/utils';

function SignInProbe() {
  const location = useLocation();
  return <p data-testid="signin">from={(location.state as SignInLocationState | null)?.from}</p>;
}

function renderGuarded(route: string) {
  return renderWithProviders(
    <Routes>
      <Route path={SIGN_IN_PATH} element={<SignInProbe />} />
      <Route
        path="/secured/*"
        element={
          <RequireAuth>
            <p>secret content</p>
          </RequireAuth>
        }
      />
    </Routes>,
    { route },
  );
}

describe('RequireAuth', () => {
  beforeEach(() => window.sessionStorage.clear());

  it('redirects anonymous users to the sign-in page, remembering where they were going', () => {
    renderGuarded('/secured/reports?year=2024');

    expect(screen.getByTestId('signin')).toHaveTextContent('from=/secured/reports?year=2024');
    expect(screen.queryByText('secret content')).not.toBeInTheDocument();
  });

  it('renders protected content with a valid token', () => {
    setToken(makeJwt({ username: 'tarang', scope: 'admin', exp: inOneHour() }));

    renderGuarded('/secured');

    expect(screen.getByText('secret content')).toBeInTheDocument();
  });

  it('accepts a session-only token', () => {
    setToken(makeJwt({ username: 'tarang', exp: inOneHour() }), false);

    renderGuarded('/secured');

    expect(screen.getByText('secret content')).toBeInTheDocument();
  });

  it('treats an expired token as signed out and discards it', () => {
    setToken(makeJwt({ username: 'tarang', exp: Math.floor(Date.now() / 1000) - 60 }));

    renderGuarded('/secured');

    expect(screen.getByTestId('signin')).toBeInTheDocument();
    expect(getToken()).toBeNull();
  });
});
