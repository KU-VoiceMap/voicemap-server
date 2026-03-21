import { useState, useCallback, useEffect } from 'react';
import { fetchJson } from '../api/client';
import {
  getAccessToken,
  getRefreshToken,
  saveTokens,
  clearTokens,
  getPendingGoogleIdToken,
  clearPendingGoogleIdToken,
  decodeJwtPayload,
  extractMemberNumber,
} from './tokenStorage';

export interface AuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  statusMessage: string;
  memberNumber: string;
}

export function useAuth() {
  const [state, setState] = useState<AuthState>({
    isAuthenticated: false,
    isLoading: true,
    statusMessage: '',
    memberNumber: '',
  });

  const loginWithGoogleToken = useCallback(async (googleIdToken: string) => {
    const loginResponse = await fetchJson<{ accessToken: string; refreshToken: string }>('/auth/login', {
      method: 'POST',
      auth: false,
      body: { provider: 'GOOGLE', providerToken: googleIdToken },
    });

    if (loginResponse.status === 200 && loginResponse.json) {
      saveTokens(loginResponse.json.accessToken, loginResponse.json.refreshToken);
      return;
    }

    if (loginResponse.status !== 404) {
      throw new Error(`/auth/login 실패 (${loginResponse.status})`);
    }

    let email = '';
    try {
      const payload = decodeJwtPayload(googleIdToken);
      email = (payload.email as string) ?? '';
    } catch {
      throw new Error('Google ID 토큰에서 이메일을 추출할 수 없습니다.');
    }

    const registerResponse = await fetchJson<{ accessToken: string; refreshToken: string }>('/v1/register', {
      method: 'POST',
      auth: false,
      body: { provider: 'GOOGLE', providerToken: googleIdToken, email },
    });

    if (registerResponse.status !== 200 || !registerResponse.json) {
      throw new Error(`/v1/register 실패 (${registerResponse.status})`);
    }

    saveTokens(registerResponse.json.accessToken, registerResponse.json.refreshToken);
  }, []);

  const handleCallbackLogin = useCallback(async () => {
    const pendingToken = getPendingGoogleIdToken();
    if (!pendingToken) {
      setState({
        isAuthenticated: false,
        isLoading: false,
        statusMessage: '콜백 토큰이 존재하지 않습니다. 다시 로그인해주세요.',
        memberNumber: '',
      });
      window.history.replaceState({}, '', '/');
      return;
    }

    try {
      await loginWithGoogleToken(pendingToken);
      clearPendingGoogleIdToken();
      window.history.replaceState({}, '', '/');
      const accessToken = getAccessToken();
      setState({
        isAuthenticated: true,
        isLoading: false,
        statusMessage: '',
        memberNumber: extractMemberNumber(accessToken),
      });
    } catch (error) {
      console.error('[auth] callback login failed', error);
      setState({
        isAuthenticated: false,
        isLoading: false,
        statusMessage: `로그인 실패: ${(error as Error).message}`,
        memberNumber: '',
      });
    }
  }, [loginWithGoogleToken]);

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();
    try {
      if (refreshToken) {
        await fetchJson('/auth/logout', {
          method: 'POST',
          auth: false,
          body: { refreshToken },
        });
      }
    } catch (error) {
      console.error('[auth] logout request failed', error);
    } finally {
      clearTokens();
      setState({
        isAuthenticated: false,
        isLoading: false,
        statusMessage: 'Google 로그인을 다시 진행해주세요.',
        memberNumber: '',
      });
    }
  }, []);

  useEffect(() => {
    const isCallback =
      window.location.pathname === '/oauth/callback' ||
      window.location.pathname === '/oauth/callback/';

    if (isCallback) {
      handleCallbackLogin();
      return;
    }

    const accessToken = getAccessToken();
    const refreshToken = getRefreshToken();

    if (accessToken && refreshToken) {
      setState({
        isAuthenticated: true,
        isLoading: false,
        statusMessage: '',
        memberNumber: extractMemberNumber(accessToken),
      });
      return;
    }

    setState({
      isAuthenticated: false,
      isLoading: false,
      statusMessage: 'Google 로그인을 진행해주세요.',
      memberNumber: '',
    });
  }, [handleCallbackLogin]);

  return { ...state, logout };
}
