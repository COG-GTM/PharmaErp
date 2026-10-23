import { useId, useRef, useState, type FormEvent } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { ApiError } from '../api/client';
import { Logo } from '../app/Logo';
import { useAuth } from './AuthContext';
import { requestLogin } from './authApi';
import { SIGN_IN_PATH, type SignInLocationState } from './RequireAuth';
import { LEGACY_MESSAGES, validateLoginForm, type LoginErrors } from './validation';

function redirectTarget(state: unknown): string {
  const from = (state as SignInLocationState | null)?.from;
  return from && from.startsWith('/') && !from.startsWith(SIGN_IN_PATH) ? from : '/';
}

function firstMessage(errors: Record<string, string[]> | undefined, field: string): string | undefined {
  return errors?.[field]?.[0];
}

/** Replaces the legacy JSP login page (PharmaWebClinetIntellij/.../jsp/main.jsp). */
export function LoginPage() {
  const { isAuthenticated, signIn } = useAuth();
  const location = useLocation();
  const ids = useId();
  const usernameRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [remember, setRemember] = useState(false);
  const [errors, setErrors] = useState<LoginErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) {
    return <Navigate to={redirectTarget(location.state)} replace />;
  }

  const fail = (next: LoginErrors, message: string | null) => {
    setErrors(next);
    setFormError(message);
    (next.username ? usernameRef : passwordRef).current?.focus();
  };

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submitting) return;
    const invalid = validateLoginForm({ username, password });
    if (Object.keys(invalid).length > 0) {
      fail(invalid, null);
      return;
    }
    setErrors({});
    setFormError(null);
    setSubmitting(true);
    try {
      const token = await requestLogin(username, password);
      signIn(token, { remember });
    } catch (err) {
      setSubmitting(false);
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        fail(
          { username: LEGACY_MESSAGES.unknownUser, password: LEGACY_MESSAGES.wrongPassword },
          'Sign-in failed. Check your username and password.',
        );
      } else if (err instanceof ApiError && err.status === 400) {
        const fieldErrors: LoginErrors = {
          username: firstMessage(err.problem.errors, 'username') ?? firstMessage(err.problem.errors, 'email'),
          password: firstMessage(err.problem.errors, 'password'),
        };
        fail(fieldErrors, err.message);
      } else if (err instanceof ApiError) {
        fail({}, `The server could not sign you in (HTTP ${err.status}). Try again shortly.`);
      } else {
        const offline = !(err instanceof Error) || err instanceof TypeError;
        fail({}, offline ? 'Unable to reach the server. Check your connection and try again.' : err.message);
      }
    }
  };

  const usernameId = `${ids}-email`;
  const passwordId = `${ids}-pwd`;

  return (
    <div className="login-screen">
      <main className="login-card" aria-labelledby={`${ids}-title`}>
        <header className="login-card__header">
          <Logo size={44} />
          <div>
            <h1 id={`${ids}-title`}>Pharma ERP</h1>
            <p>Sign in to continue</p>
          </div>
        </header>
        <form className="login-form" onSubmit={onSubmit} noValidate aria-busy={submitting}>
          {formError && (
            <div className="alert alert--error" role="alert">
              {formError}
            </div>
          )}
          <div className="field">
            <label htmlFor={usernameId}>Email:</label>
            <div className={`input-group${errors.username ? ' input-group--invalid' : ''}`}>
              <span className="input-group__addon" aria-hidden="true">
                @
              </span>
              <input
                ref={usernameRef}
                id={usernameId}
                name="email"
                type="text"
                placeholder="Username"
                autoComplete="username"
                autoFocus
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                aria-invalid={errors.username ? true : undefined}
                aria-describedby={errors.username ? `${usernameId}-error` : undefined}
              />
            </div>
            {errors.username && (
              <p className="field__error" id={`${usernameId}-error`}>
                {errors.username}
              </p>
            )}
          </div>
          <div className="field">
            <label htmlFor={passwordId}>Password:</label>
            <div className={`input-group${errors.password ? ' input-group--invalid' : ''}`}>
              <span className="input-group__addon" aria-hidden="true">
                &#128274;
              </span>
              <input
                ref={passwordRef}
                id={passwordId}
                name="password"
                type="password"
                placeholder="Password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                aria-invalid={errors.password ? true : undefined}
                aria-describedby={errors.password ? `${passwordId}-error` : undefined}
              />
            </div>
            {errors.password && (
              <p className="field__error" id={`${passwordId}-error`}>
                {errors.password}
              </p>
            )}
          </div>
          <label className="checkbox">
            <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} /> Remember me
          </label>
          <button type="submit" className="btn btn--primary btn--block" disabled={submitting}>
            {submitting ? 'Signing in…' : 'Submit'}
          </button>
        </form>
      </main>
    </div>
  );
}
