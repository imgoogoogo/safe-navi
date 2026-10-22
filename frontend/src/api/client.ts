import Constants from 'expo-constants';
import * as SecureStore from 'expo-secure-store';
import type { AuthTokens } from './types';

/** Set `expo.extra.apiBaseUrl` in app.json. Empty = use the in-app mock backend. */
export const API_BASE_URL: string =
  (Constants.expoConfig?.extra as { apiBaseUrl?: string } | undefined)?.apiBaseUrl ?? '';
export const USE_MOCK = !API_BASE_URL;

const ACCESS_KEY = 'safewalk.accessToken';
const REFRESH_KEY = 'safewalk.refreshToken';

let tokens: AuthTokens | null = null;
let onAuthLost: (() => void) | null = null;

export async function loadTokens(): Promise<AuthTokens | null> {
  const [accessToken, refreshToken] = await Promise.all([
    SecureStore.getItemAsync(ACCESS_KEY),
    SecureStore.getItemAsync(REFRESH_KEY),
  ]);
  tokens = accessToken && refreshToken ? { accessToken, refreshToken } : null;
  return tokens;
}

export async function saveTokens(next: AuthTokens | null) {
  tokens = next;
  if (next) {
    await SecureStore.setItemAsync(ACCESS_KEY, next.accessToken);
    await SecureStore.setItemAsync(REFRESH_KEY, next.refreshToken);
  } else {
    await SecureStore.deleteItemAsync(ACCESS_KEY);
    await SecureStore.deleteItemAsync(REFRESH_KEY);
  }
}

export function getTokens() {
  return tokens;
}

export function setOnAuthLost(cb: (() => void) | null) {
  onAuthLost = cb;
}

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE';
  query?: Record<string, string | number | undefined>;
  body?: unknown;
  auth?: boolean;
};

let refreshing: Promise<boolean> | null = null;

async function refreshTokens(): Promise<boolean> {
  if (!tokens) return false;
  refreshing ??= (async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: tokens!.refreshToken }),
      });
      if (!res.ok) return false;
      await saveTokens((await res.json()) as AuthTokens);
      return true;
    } catch {
      return false;
    } finally {
      refreshing = null;
    }
  })();
  return refreshing;
}

export async function request<T>(path: string, opts: RequestOptions = {}, retried = false): Promise<T> {
  const qs = opts.query
    ? '?' +
      Object.entries(opts.query)
        .filter(([, v]) => v !== undefined)
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
        .join('&')
    : '';
  const isForm = typeof FormData !== 'undefined' && opts.body instanceof FormData;
  const headers: Record<string, string> = {};
  if (opts.body !== undefined && !isForm) headers['Content-Type'] = 'application/json';
  if (tokens) headers.Authorization = `Bearer ${tokens.accessToken}`;

  const res = await fetch(`${API_BASE_URL}${path}${qs}`, {
    method: opts.method ?? 'GET',
    headers,
    body: opts.body === undefined ? undefined : isForm ? (opts.body as FormData) : JSON.stringify(opts.body),
  });

  if (res.status === 401 && tokens && !retried) {
    if (await refreshTokens()) return request<T>(path, opts, true);
    await saveTokens(null);
    onAuthLost?.();
  }
  if (!res.ok) {
    let message = `요청에 실패했어요 (${res.status})`;
    try {
      const data = await res.json();
      if (data?.message) message = data.message;
    } catch {}
    throw new ApiError(res.status, message);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}
