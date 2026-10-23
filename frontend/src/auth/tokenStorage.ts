const TOKEN_KEY = 'pharma.erp.token';

/**
 * "Remember me" tokens live in localStorage (survive browser restarts); otherwise the token is kept in
 * sessionStorage and disappears with the tab, mirroring the legacy login page's checkbox.
 */
export function getToken(): string | null {
  return window.localStorage.getItem(TOKEN_KEY) ?? window.sessionStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string, persistent = true): void {
  clearToken();
  (persistent ? window.localStorage : window.sessionStorage).setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  window.localStorage.removeItem(TOKEN_KEY);
  window.sessionStorage.removeItem(TOKEN_KEY);
}
