import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { passwordError, usernameError, usersApi } from './api';
import { FormField } from './FormField';
import { errorMessage, fieldErrorsOf, type FieldErrors } from './forms';

/** Register a user (no :username param) or change an existing user's password. */
export function UserForm() {
  const { username: existing } = useParams();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const next: FieldErrors = {};
    const u = existing ? null : usernameError(username);
    if (u) next.username = [u];
    const p = passwordError(password);
    if (p) next.password = [p];
    if (password.trim() !== confirm.trim()) next.confirmPassword = ['Passwords do not match'];
    setErrors(next);
    setError(null);
    if (Object.keys(next).length) return;
    setSaving(true);
    try {
      if (existing) await usersApi.changePassword(existing, password.trim());
      else await usersApi.register({ username: username.trim(), password: password.trim() });
      navigate('..');
    } catch (err) {
      setErrors(fieldErrorsOf(err));
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const title = existing ? `Change password for ${existing}` : 'Register user';
  return (
    <section className="card">
      <h2>{title}</h2>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      <form onSubmit={onSubmit} noValidate aria-label={title}>
        <div className="form-grid">
          {!existing && (
            <FormField name="username" label="Username" errors={errors}>
              <input
                id="field-username"
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                aria-invalid={errors.username ? true : undefined}
              />
            </FormField>
          )}
          <FormField name="password" label="Password" errors={errors}>
            <input
              id="field-password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              aria-invalid={errors.password ? true : undefined}
            />
          </FormField>
          <FormField name="confirmPassword" label="Confirm password" errors={errors}>
            <input
              id="field-confirmPassword"
              type="password"
              autoComplete="new-password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              aria-invalid={errors.confirmPassword ? true : undefined}
            />
          </FormField>
        </div>
        <p className="hint">Usernames: 8-12 letters, digits or underscores (not only digits). Passwords: at least 8 characters, no spaces.</p>
        <div className="form-actions">
          <button type="submit" disabled={saving}>
            {saving ? 'Saving…' : existing ? 'Change password' : 'Register'}
          </button>
          <Link to=".." className="button-link button-secondary">
            Cancel
          </Link>
        </div>
      </form>
    </section>
  );
}
