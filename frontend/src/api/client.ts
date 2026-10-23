import { getToken } from '../auth/tokenStorage';

/** Dispatched on window when the API answers 401, so the auth layer can drop the token. */
export const UNAUTHORIZED_EVENT = 'pharma:unauthorized';

/** RFC 9457 problem details as produced by the backend's ApiExceptionHandler. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  /** Field -> messages, present on 400 validation errors. */
  errors?: Record<string, string[]>;
}

export class ApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetail;

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
  }
}

/** Mirrors com.pharma.erp.common.web.PageResponse. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type QueryValue = string | number | boolean | null | undefined;

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  query?: Record<string, QueryValue>;
}

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  if (!query) return path;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null) params.append(key, String(value));
  }
  const qs = params.toString();
  return qs ? `${path}${path.includes('?') ? '&' : '?'}${qs}` : path;
}

async function parseBody(response: Response): Promise<unknown> {
  if (response.status === 204) return undefined;
  const text = await response.text();
  if (!text) return undefined;
  const type = response.headers.get('content-type') ?? '';
  if (type.includes('json')) {
    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  }
  return text;
}

/** fetch wrapper: JSON in/out, bearer token from localStorage, non-2xx -> ApiError. */
export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, query, headers, ...init } = options;
  const finalHeaders = new Headers(headers);
  finalHeaders.set('Accept', 'application/json, application/problem+json');
  const token = getToken();
  if (token && !finalHeaders.has('Authorization')) {
    finalHeaders.set('Authorization', `Bearer ${token}`);
  }
  let payload: BodyInit | undefined;
  if (body !== undefined) {
    if (body instanceof FormData || body instanceof URLSearchParams || typeof body === 'string') {
      payload = body;
    } else {
      finalHeaders.set('Content-Type', 'application/json');
      payload = JSON.stringify(body);
    }
  }

  const response = await fetch(buildUrl(path, query), { ...init, headers: finalHeaders, body: payload });
  const data = await parseBody(response);
  if (!response.ok) {
    if (response.status === 401) window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    const problem: ProblemDetail =
      data && typeof data === 'object' ? (data as ProblemDetail) : { status: response.status, detail: typeof data === 'string' ? data : undefined };
    throw new ApiError(response.status, problem);
  }
  return data as T;
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) => apiFetch<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    apiFetch<T>(path, { ...options, method: 'POST', body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    apiFetch<T>(path, { ...options, method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    apiFetch<T>(path, { ...options, method: 'PATCH', body }),
  delete: <T = void>(path: string, options?: RequestOptions) => apiFetch<T>(path, { ...options, method: 'DELETE' }),
};
