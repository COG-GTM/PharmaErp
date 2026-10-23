import { api } from '../api/client';

export const LOGIN_ENDPOINT = '/api/auth/login';
export const LOGOUT_ENDPOINT = '/api/auth/logout';

const JWT_PATTERN = /[\w-]+\.[\w-]+\.[\w-]+/;
const TOKEN_FIELDS = ['token', 'accessToken', 'access_token', 'jwt'] as const;

/** Accepts `{ token }` (or a common alias) or a bare/JSON-quoted JWT string, as the legacy endpoint returned. */
export function extractToken(data: unknown): string | null {
  if (typeof data === 'string') return JWT_PATTERN.exec(data)?.[0] ?? null;
  if (data && typeof data === 'object') {
    for (const field of TOKEN_FIELDS) {
      const value = (data as Record<string, unknown>)[field];
      if (typeof value === 'string' && value) return value;
    }
  }
  return null;
}

export async function requestLogin(username: string, password: string): Promise<string> {
  const data = await api.post<unknown>(LOGIN_ENDPOINT, { username: username.trim(), password });
  const token = extractToken(data);
  if (!token) throw new Error('The server did not return an access token.');
  return token;
}

/** Best effort: lets the backend clear its `token` cookie. Failures (e.g. 404) are ignored. */
export async function requestLogout(): Promise<void> {
  await api.post(LOGOUT_ENDPOINT).catch(() => undefined);
}
