import { api, ApiError, UNAUTHORIZED_EVENT } from './client';
import { setToken } from '../auth/tokenStorage';
import { mockFetchJson } from '../test/utils';

describe('api client', () => {
  it('sends the bearer token from localStorage and parses JSON', async () => {
    setToken('abc.def.ghi');
    const fetchMock = mockFetchJson({ status: 'UP' });

    const result = await api.get<{ status: string }>('/actuator/health', { query: { page: 0, q: undefined } });

    expect(result.status).toBe('UP');
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/actuator/health?page=0');
    expect(new Headers(init.headers).get('Authorization')).toBe('Bearer abc.def.ghi');
  });

  it('serialises JSON bodies', async () => {
    const fetchMock = mockFetchJson({ id: 1 }, 201);

    await api.post('/api/things', { name: 'x' });

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(init.method).toBe('POST');
    expect(init.body).toBe('{"name":"x"}');
    expect(new Headers(init.headers).get('Content-Type')).toBe('application/json');
  });

  it('throws ApiError carrying problem+json details', async () => {
    mockFetchJson(
      { status: 400, detail: 'Validation failed', errors: { name: ['must not be blank'] } },
      400,
      'application/problem+json',
    );

    const error = await api.post('/api/things', {}).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(400);
    expect((error as ApiError).message).toBe('Validation failed');
    expect((error as ApiError).problem.errors?.name).toEqual(['must not be blank']);
  });

  it('broadcasts 401 responses', async () => {
    mockFetchJson({ status: 401, detail: 'Authentication required' }, 401, 'application/problem+json');
    const listener = vi.fn();
    window.addEventListener(UNAUTHORIZED_EVENT, listener);

    await expect(api.get('/api/things')).rejects.toBeInstanceOf(ApiError);

    expect(listener).toHaveBeenCalledTimes(1);
    window.removeEventListener(UNAUTHORIZED_EVENT, listener);
  });
});
