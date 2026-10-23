import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { UNAUTHORIZED_EVENT } from '../api/client';
import { clearToken, getToken, setToken } from './tokenStorage';

export interface AuthState {
  token: string | null;
  isAuthenticated: boolean;
  /** Stores a token obtained by the auth module's sign-in flow. */
  signIn: (token: string) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

/** Stub provider: holds the bearer token. The auth module (T01) supplies the sign-in UI. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string | null>(() => getToken());

  const signIn = useCallback((value: string) => {
    setToken(value);
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

  const value = useMemo<AuthState>(
    () => ({ token, isAuthenticated: token !== null, signIn, signOut }),
    [token, signIn, signOut],
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
