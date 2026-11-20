import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, SESSION_KEY, setAuthFailureHandlers } from '../api/client';
import type { AuthResponse, Role, User } from '../api/types';

interface Session { token: string; expiresAt: number; role: Role; userId: number; email: string; name?: string }
interface AuthValue { session: Session | null; user: User | null; role: Role | null; login: (email: string, password: string) => Promise<void>; register: (body: { name: string; email: string; password: string; phone: string }) => Promise<void>; logout: (message?: string) => void; refreshUser: () => Promise<void>; notice: string; clearNotice: () => void }
const AuthContext = createContext<AuthValue | null>(null);
const USER_KEY = 'courier.user';
function decodeToken(token: string): Partial<Session> {
  try { return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))) as Partial<Session>; } catch { return {}; }
}
function readSession(): Session | null {
  const token = localStorage.getItem(SESSION_KEY);
  if (!token) return null;
  const claims = decodeToken(token);
  const expiresAt = Number(claims.expiresAt || (claims as { exp?: number }).exp && (claims as { exp: number }).exp * 1000);
  const role = claims.role as Role | undefined;
  const userId = Number(claims.userId);
  if (!expiresAt || expiresAt <= Date.now() || !role || !userId) { localStorage.removeItem(SESSION_KEY); return null; }
  return { token, expiresAt, role, userId, email: String(claims.email || '') };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const [session, setSession] = useState<Session | null>(readSession);
  const [user, setUser] = useState<User | null>(() => { try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null') as User | null; } catch { return null; } });
  const [notice, setNotice] = useState('');
  const logout = useCallback((message = '') => { localStorage.removeItem(SESSION_KEY); localStorage.removeItem(USER_KEY); setSession(null); setUser(null); setNotice(message); navigate('/login', { replace: true, state: message ? { message } : undefined }); }, [navigate]);
  const refreshUser = useCallback(async () => { const response = await api.get<User>('/api/users/me'); setUser(response.data); localStorage.setItem(USER_KEY, JSON.stringify(response.data)); }, []);
  const login = useCallback(async (email: string, password: string) => {
    const { data } = await api.post<AuthResponse>('/api/auth/login', { email, password });
    const claims = decodeToken(data.token);
    const expiresAt = Date.parse(data.expiresAt) || Number((claims as { exp?: number }).exp || 0) * 1000;
    const next: Session = { token: data.token, expiresAt, role: data.role, userId: data.userId, email: data.email };
    localStorage.setItem(SESSION_KEY, data.token); setSession(next);
    try { const response = await api.get<User>('/api/users/me'); setUser(response.data); localStorage.setItem(USER_KEY, JSON.stringify(response.data)); } catch { setUser(null); }
    navigate(data.role === 'ADMIN' ? '/admin' : data.role === 'COURIER' ? '/courier' : '/customer', { replace: true });
  }, [navigate]);
  const register = useCallback(async (body: { name: string; email: string; password: string; phone: string }) => { await api.post('/api/auth/register', body); await login(body.email, body.password); }, [login]);
  useEffect(() => { setAuthFailureHandlers(() => logout('Your session expired. Please sign in again.'), () => setNotice('You are not allowed to perform that action.')); }, [logout]);
  useEffect(() => { if (!session) return; const timer = window.setTimeout(() => logout('Your session expired. Please sign in again.'), Math.max(0, session.expiresAt - Date.now())); return () => window.clearTimeout(timer); }, [session, logout]);
  const value = useMemo(() => ({ session, user, role: session?.role || null, login, register, logout, refreshUser, notice, clearNotice: () => setNotice('') }), [session, user, login, register, logout, refreshUser, notice]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export function useAuth() { const value = useContext(AuthContext); if (!value) throw new Error('useAuth must be inside AuthProvider'); return value; }
