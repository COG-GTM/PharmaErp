import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

export const SIGN_IN_PATH = '/signin';

/** Location state handed to the sign-in page so it can return the user where they were going. */
export interface SignInLocationState {
  from?: string;
}

export function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) {
    const state: SignInLocationState = { from: `${location.pathname}${location.search}${location.hash}` };
    return <Navigate to={SIGN_IN_PATH} replace state={state} />;
  }
  return <>{children}</>;
}
