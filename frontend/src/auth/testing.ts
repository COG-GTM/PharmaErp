import type { JwtClaims } from './jwt';

function encode(value: unknown): string {
  return btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/** Builds an unsigned JWT-shaped string for tests. */
export function makeJwt(claims: JwtClaims): string {
  return `${encode({ alg: 'RS256', typ: 'JWT' })}.${encode(claims)}.signature`;
}

export const inOneHour = () => Math.floor(Date.now() / 1000) + 3600;
