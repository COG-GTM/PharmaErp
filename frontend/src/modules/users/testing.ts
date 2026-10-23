import { vi } from 'vitest';

export interface RecordedCall {
  method: string;
  url: string;
  body: unknown;
}

type Handler = (call: RecordedCall) => { status?: number; body?: unknown } | undefined;

/** Stubs fetch with a router: the first handler returning a value answers; records every call. */
export function mockApi(handler: Handler) {
  const calls: RecordedCall[] = [];
  const fn = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
    const method = (init?.method ?? 'GET').toUpperCase();
    const body = typeof init?.body === 'string' ? (JSON.parse(init.body) as unknown) : undefined;
    const call = { method, url, body };
    calls.push(call);
    const res = handler(call) ?? { status: 404, body: { status: 404, title: 'Not Found' } };
    const status = res.status ?? 200;
    if (status === 204) return new Response(null, { status });
    const type = status >= 400 ? 'application/problem+json' : 'application/json';
    return new Response(JSON.stringify(res.body ?? null), { status, headers: { 'Content-Type': type } });
  });
  vi.stubGlobal('fetch', fn);
  return { fn, calls };
}

export function page<T>(content: T[], page = 0, size = 20) {
  return { content, page, size, totalElements: content.length, totalPages: Math.max(1, Math.ceil(content.length / size)) };
}
