import { useState, type FormEvent } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorBanner } from '../components/Ui';

export function LoginPage() {
  const { login } = useAuth(); const location = useLocation(); const message = (location.state as { message?: string } | null)?.message;
  const [email, setEmail] = useState(''); const [password, setPassword] = useState(''); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) { event.preventDefault(); setError(''); setBusy(true); try { await login(email.trim(), password); } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); } }
  return <main className="auth-layout"><section className="auth-card"><span className="eyebrow">WELCOME BACK</span><h1>Sign in to your account</h1><p className="muted">Follow every parcel, from pickup to delivery.</p>{message && <ErrorBanner>{message}</ErrorBanner>}{error && <ErrorBanner>{error}</ErrorBanner>}<form onSubmit={submit} className="form-stack"><label>Email address<input type="email" autoComplete="email" maxLength={254} required value={email} onChange={(e) => setEmail(e.target.value)} /></label><label>Password<input type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} /></label><button className="button primary full" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button></form><p className="auth-footer">New to CourierTrack? <Link to="/register">Create an account</Link></p></section></main>;
}

export function RegisterPage() {
  const { register } = useAuth(); const [form, setForm] = useState({ name: '', email: '', password: '', phone: '' }); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  const set = (key: keyof typeof form, value: string) => setForm((old) => ({ ...old, [key]: value }));
  async function submit(event: FormEvent) { event.preventDefault(); setError(''); if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return setError('Enter a valid email address.'); if (form.password.length < 8 || form.password.length > 72) return setError('Password must be between 8 and 72 characters.'); setBusy(true); try { await register(form); } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); } }
  return <main className="auth-layout"><section className="auth-card"><span className="eyebrow">JOIN COURIERTRACK</span><h1>Create your customer account</h1><p className="muted">Register as a customer to send and track shipments.</p>{error && <ErrorBanner>{error}</ErrorBanner>}<form onSubmit={submit} className="form-stack"><label>Full name<input autoComplete="name" maxLength={120} required value={form.name} onChange={(e) => set('name', e.target.value)} /></label><label>Email address<input type="email" autoComplete="email" maxLength={254} required value={form.email} onChange={(e) => set('email', e.target.value)} /></label><label>Password<input type="password" autoComplete="new-password" minLength={8} maxLength={72} required value={form.password} onChange={(e) => set('password', e.target.value)} /><small>Use 8 to 72 characters.</small></label><label>Phone <span className="muted">(optional)</span><input type="tel" maxLength={30} value={form.phone} onChange={(e) => set('phone', e.target.value)} /></label><button className="button primary full" disabled={busy}>{busy ? 'Creating account…' : 'Create account'}</button></form><p className="auth-footer">Already registered? <Link to="/login">Sign in</Link></p></section></main>;
}
