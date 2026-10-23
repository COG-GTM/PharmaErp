export interface JwtClaims {
  sub?: string;
  username?: string;
  scope?: string;
  /** Expiry, seconds since epoch. */
  exp?: number;
}

function decodeSegment(segment: string): string {
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
  const bytes = Uint8Array.from(atob(padded), (c) => c.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

/** Reads the (unverified) payload of a JWT; returns null when the value is not a decodable JWT. */
export function decodeJwt(token: string): JwtClaims | null {
  const payload = token.split('.')[1];
  if (!payload) return null;
  try {
    const claims: unknown = JSON.parse(decodeSegment(payload));
    return claims && typeof claims === 'object' ? (claims as JwtClaims) : null;
  } catch {
    return null;
  }
}

/** Milliseconds until the token expires, or null when it carries no `exp` claim. */
export function msUntilExpiry(claims: JwtClaims | null, now = Date.now()): number | null {
  return typeof claims?.exp === 'number' ? claims.exp * 1000 - now : null;
}

export function isExpired(claims: JwtClaims | null, now = Date.now()): boolean {
  const remaining = msUntilExpiry(claims, now);
  return remaining !== null && remaining <= 0;
}
