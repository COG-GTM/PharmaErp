import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { UNAUTHORIZED_EVENT } from '../api/client';
import { decodeJwt, isExpired, msUntilExpiry } from './jwt';
import { clearToken, getToken, setToken } from './tokenStorage';

export interface AuthUser {
  username: string | null;
  scopes: string[];
}

export interface SignInOptions {
  /** Keep the token across browser restarts (localStorage). Defaults to true. */
  remember?: boolean;
}

export interface AuthState {
  token: string | null;
  isAuthenticated: boolean;
  /** Claims read from the token (username, scopes); null when signed out. */
  user: AuthUser | null;
  signIn: (token: string, options?: SignInOptions) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

/** Max delay accepted by setTimeout (~24.8 days). */
const MAX_TIMEOUT = 2 ** 31 - 1;

function initialToken(): string | null {
  const stored = getToken();
  if (stored && isExpired(decodeJwt(stored))) {
    clearToken();
    return null;
  }
  return stored;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string | null>(initialToken);

  const signIn = useCallback((value: string, options: SignInOptions = {}) => {
    setToken(value, options.remember ?? true);
    setTokenState(value);
  }, []);

  const signOut = useCallback(() => {
    clearToken();
    setTokenState(null);
  }, []);

  useEffect(() => {
    window.addEventListener(UNAUTHORIZED_EVENT, signOut);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, signOut);
  }, [signOut]);

  const claims = useMemo(() => (token ? decodeJwt(token) : null), [token]);

  useEffect(() => {
    const remaining = msUntilExpiry(claims);
    if (remaining === null) return undefined;
    if (remaining <= 0) {
      signOut();
      return undefined;
    }
    const timer = window.setTimeout(signOut, Math.min(remaining, MAX_TIMEOUT));
    return () => window.clearTimeout(timer);
  }, [claims, signOut]);

  const value = useMemo<AuthState>(
    () => ({
      token,
      isAuthenticated: token !== null,
      user: token
        ? { username: claims?.username ?? null, scopes: claims?.scope?.split(' ').filter(Boolean) ?? [] }
        : null,
      signIn,
      signOut,
    }),
    [token, claims, signIn, signOut],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return ctx;
}
