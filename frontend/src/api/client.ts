import { getAccessToken, getRefreshToken, saveTokens, clearTokens } from '../auth/tokenStorage';
import { API_BASE_URL } from './config';

export interface ApiResponse<T = unknown> {
  status: number;
  json: T | null;
}

async function rotateAccessToken(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;

  try {
    const response = await fetch(`${API_BASE_URL}/auth/access`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });

    if (response.status !== 200) return false;

    const body = await response.json();
    if (!body.accessToken) return false;

    saveTokens(body.accessToken, body.refreshToken || refreshToken);
    return true;
  } catch (error) {
    console.error('[api] token refresh failed', error);
    return false;
  }
}

async function request(
  path: string,
  options: {
    method?: string;
    body?: unknown;
    auth?: boolean;
    retryOnUnauthorized?: boolean;
  } = {},
): Promise<Response> {
  const {
    method = 'GET',
    body = null,
    auth = true,
    retryOnUnauthorized = true,
  } = options;

  const headers: Record<string, string> = {};
  if (body !== null) {
    headers['Content-Type'] = 'application/json';
  }
  if (auth) {
    headers['Authorization'] = `Bearer ${getAccessToken()}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body !== null ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401 && auth && retryOnUnauthorized) {
    const refreshed = await rotateAccessToken();
    if (refreshed) {
      return request(path, { ...options, retryOnUnauthorized: false });
    }
    clearTokens();
  }

  return response;
}

export async function fetchJson<T = unknown>(
  path: string,
  options: {
    method?: string;
    body?: unknown;
    auth?: boolean;
  } = {},
): Promise<ApiResponse<T>> {
  const response = await request(path, options);
  let json: T | null = null;
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    json = await response.json();
  }
  return { status: response.status, json };
}
