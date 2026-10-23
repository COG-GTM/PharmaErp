import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from './AuthContext';

export const SIGN_IN_PATH = '/signin';

export function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    return (
      <section className="card" role="alert">
        <h2>Sign in required</h2>
        <p>
          <Link to={SIGN_IN_PATH}>Sign in</Link> to continue.
        </p>
      </section>
    );
  }
  return <>{children}</>;
}
