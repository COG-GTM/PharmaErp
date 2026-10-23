export interface LoginForm {
  username: string;
  password: string;
}

export type LoginErrors = Partial<Record<keyof LoginForm, string>>;

/** Validation hints carried over from the legacy main.jsp popovers. */
export const LEGACY_MESSAGES = {
  unknownUser: "The email address or username that you've entered doesn't match any account.",
  wrongPassword: "The password that you've entered is incorrect.",
} as const;

/**
 * Client-side checks only block what the legacy servlet could never accept: blank fields and a username
 * containing inner whitespace (the servlet trims the username, so padding is fine).
 */
export function validateLoginForm({ username, password }: LoginForm): LoginErrors {
  const errors: LoginErrors = {};
  const trimmed = username.trim();
  if (!trimmed) errors.username = 'Enter your email or username.';
  else if (/\s/.test(trimmed)) errors.username = 'Username must not contain spaces.';
  if (!password) errors.password = 'Enter your password.';
  return errors;
}
