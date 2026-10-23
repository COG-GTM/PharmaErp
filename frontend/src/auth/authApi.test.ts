import { extractToken, LOGIN_ENDPOINT, LOGOUT_ENDPOINT, requestLogin, requestLogout } from './authApi';

describe('extractToken', () => {
  it.each([
    [{ token: 'a.b.c' }, 'a.b.c'],
    [{ accessToken: 'a.b.c' }, 'a.b.c'],
    [{ access_token: 'a.b.c' }, 'a.b.c'],
    ['"aaa.bbb.ccc"', 'aaa.bbb.ccc'],
    ['{"token":"aa-1.bb_2.cc"}', 'aa-1.bb_2.cc'],
  ])('reads %j', (input, expected) => {
    expect(extractToken(input)).toBe(expected);
  });

  it.each([[null], [{}], ['not a token'], [{ token: 42 }]])('returns null for %j', (input) => {
    expect(extractToken(input)).toBeNull();
  });
});

describe('requestLogin', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('waits for a pending logout so its response cannot clear the new session', async () => {
    const calls: string[] = [];
    let finishLogout: () => void = () => undefined;
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        calls.push(`start ${url}`);
        if (url === LOGOUT_ENDPOINT) {
          return new Promise<Response>((resolve) => {
            finishLogout = () => {
              calls.push(`end ${url}`);
              resolve(new Response(null, { status: 204 }));
            };
          });
        }
        return Promise.resolve(
          new Response(JSON.stringify({ token: 'a.b.c' }), { status: 200, headers: { 'Content-Type': 'application/json' } }),
        );
      }),
    );

    void requestLogout();
    const login = requestLogin('tarang', 'pw');
    await Promise.resolve();
    expect(calls).toEqual([`start ${LOGOUT_ENDPOINT}`]);

    finishLogout();
    await expect(login).resolves.toBe('a.b.c');
    expect(calls).toEqual([`start ${LOGOUT_ENDPOINT}`, `end ${LOGOUT_ENDPOINT}`, `start ${LOGIN_ENDPOINT}`]);
  });
});
