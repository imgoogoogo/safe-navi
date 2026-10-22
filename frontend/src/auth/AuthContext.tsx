import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { router } from 'expo-router';
import { api, type User } from '../api';
import { getTokens, loadTokens, saveTokens, setOnAuthLost, USE_MOCK } from '../api/client';
import { mock } from '../api/mock';
import { invalidate } from '../api/useQuery';

type AuthState = {
  user: User | null;
  ready: boolean;
  login: (email: string, password: string) => Promise<void>;
  signup: (name: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  /** Returns true if logged in; otherwise opens the login screen. */
  requireAuth: () => boolean;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    setOnAuthLost(() => setUser(null));
    (async () => {
      try {
        if (await loadTokens()) setUser(USE_MOCK ? await mock.restore() : await api.me());
      } catch {
        await saveTokens(null);
      } finally {
        setReady(true);
      }
    })();
    return () => setOnAuthLost(null);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await api.login(email, password);
    await saveTokens({ accessToken: res.accessToken, refreshToken: res.refreshToken });
    setUser(res.user);
    invalidate('');
  }, []);

  const signup = useCallback(async (name: string, email: string, password: string) => {
    const res = await api.signup(name, email, password);
    await saveTokens({ accessToken: res.accessToken, refreshToken: res.refreshToken });
    setUser(res.user);
    invalidate('');
  }, []);

  const logout = useCallback(async () => {
    const t = getTokens();
    try {
      if (t) await api.logout(t.refreshToken);
    } catch {}
    await saveTokens(null);
    setUser(null);
    invalidate('');
  }, []);

  const requireAuth = useCallback(() => {
    if (user) return true;
    router.push('/login');
    return false;
  }, [user]);

  const value = useMemo(() => ({ user, ready, login, signup, logout, requireAuth }), [user, ready, login, signup, logout, requireAuth]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
