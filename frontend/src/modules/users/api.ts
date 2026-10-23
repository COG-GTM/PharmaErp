import { api, type PageResponse } from '../../api/client';

/** REST contract of the users backend (T02). Adjust here if the backend differs. */
export const USERS_PATH = '/api/users';

export interface User {
  username: string;
}

export interface RegisterUserRequest {
  username: string;
  password: string;
}

export const usersApi = {
  list: ({ page = 0, size = 20 }: { page?: number; size?: number } = {}) =>
    api.get<PageResponse<User>>(USERS_PATH, { query: { page, size, sort: 'username' } }),
  register: (body: RegisterUserRequest) => api.post<User>(USERS_PATH, body),
  changePassword: (username: string, password: string) =>
    api.put<void>(`${USERS_PATH}/${encodeURIComponent(username)}/password`, { password }),
};

/** Legacy UserValidation.usernameValidation (trimmed): 8-12 word characters, no spaces, not only digits. */
export function usernameError(raw: string): string | null {
  const username = raw.trim();
  if (username === '') return 'Username is required';
  if (username.includes(' ')) return 'Username must not contain spaces';
  if (/^\d+$/.test(username)) return 'Username must not be only numbers';
  if (!/^\w+$/.test(username)) return 'Username may only contain letters, digits and underscores';
  if (!/^\w{8,12}$/.test(username)) return 'Username must be 8-12 characters';
  return null;
}

/** Legacy UserValidation.passwordValidation (trimmed): at least 8 characters, no spaces. */
export function passwordError(raw: string): string | null {
  const password = raw.trim();
  if (password === '') return 'Password is required';
  if (password.includes(' ')) return 'Password must not contain spaces';
  if (password.length < 8) return 'Password must be at least 8 characters';
  return null;
}
