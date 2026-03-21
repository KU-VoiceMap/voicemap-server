const STORAGE_KEYS = {
  accessToken: 'voicemap.accessToken',
  refreshToken: 'voicemap.refreshToken',
  pendingGoogleIdToken: 'voicemap.pendingGoogleIdToken',
} as const;

export function getAccessToken(): string {
  return localStorage.getItem(STORAGE_KEYS.accessToken) ?? '';
}

export function getRefreshToken(): string {
  return localStorage.getItem(STORAGE_KEYS.refreshToken) ?? '';
}

export function saveTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(STORAGE_KEYS.accessToken, accessToken);
  localStorage.setItem(STORAGE_KEYS.refreshToken, refreshToken);
}

export function clearTokens(): void {
  localStorage.removeItem(STORAGE_KEYS.accessToken);
  localStorage.removeItem(STORAGE_KEYS.refreshToken);
}

export function getPendingGoogleIdToken(): string {
  return sessionStorage.getItem(STORAGE_KEYS.pendingGoogleIdToken) ?? '';
}

export function setPendingGoogleIdToken(token: string): void {
  sessionStorage.setItem(STORAGE_KEYS.pendingGoogleIdToken, token);
}

export function clearPendingGoogleIdToken(): void {
  sessionStorage.removeItem(STORAGE_KEYS.pendingGoogleIdToken);
}

export function decodeJwtPayload(token: string): Record<string, unknown> {
  const parts = token.split('.');
  if (parts.length < 2) throw new Error('Invalid JWT token');
  const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
  return JSON.parse(atob(padded));
}

export function extractMemberNumber(accessToken: string): string {
  try {
    const payload = decodeJwtPayload(accessToken);
    return (payload.memberNumber as string) ?? '';
  } catch {
    return '';
  }
}
