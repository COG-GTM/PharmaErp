import { extractToken } from './authApi';

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
