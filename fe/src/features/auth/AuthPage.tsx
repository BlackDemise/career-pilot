import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { PageMessage } from '../../shared/components/PageMessage'
import { useAuth } from '../../app/providers/useAuth'
import { register, requestPasswordReset } from './api'

type AuthMode = 'login' | 'register' | 'forgot'

const copy = {
  login: { eyebrow: 'Welcome back', title: 'Return to your next move.', submit: 'Sign in' },
  register: { eyebrow: 'Start with context', title: 'Build a better career practice.', submit: 'Create account' },
  forgot: { eyebrow: 'Account recovery', title: 'Get back to your workspace.', submit: 'Send recovery link' },
}

export function AuthPage({ mode }: { mode: AuthMode }) {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '' })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const content = copy[mode]

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(null); setBusy(true)
    try {
      if (mode === 'login') { await login(form.email, form.password); navigate('/chat', { replace: true }) }
      if (mode === 'register') { await register(form); navigate(`/pending-registration?email=${encodeURIComponent(form.email)}`) }
      if (mode === 'forgot') { await requestPasswordReset(form.email); navigate(`/pending-reset?email=${encodeURIComponent(form.email)}`) }
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Something went wrong.') } finally { setBusy(false) }
  }

  return <main className="auth-page"><section className="auth-intro"><p className="eyebrow">{content.eyebrow}</p><h1>{content.title}</h1><p className="lede">A private workspace for sharper applications, stronger answers, and deliberate progress.</p><p className="auth-note">Keep the signal. Lose the noise.</p></section><section className="auth-panel"><div className="panel-heading"><span className="brand-mini">CP</span><span>CareerPilot</span></div><form onSubmit={submit} className="auth-form"><div><label htmlFor="email">Email</label><input id="email" type="email" required value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></div>{mode === 'register' && <div className="form-row"><div><label htmlFor="firstName">First name</label><input id="firstName" required value={form.firstName} onChange={(event) => setForm({ ...form, firstName: event.target.value })} /></div><div><label htmlFor="lastName">Last name</label><input id="lastName" required value={form.lastName} onChange={(event) => setForm({ ...form, lastName: event.target.value })} /></div></div>}{mode !== 'forgot' && <div><label htmlFor="password">Password</label><input id="password" type="password" required minLength={8} value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /></div>}{error && <PageMessage tone="error">{error}</PageMessage>}<button className="primary-button" disabled={busy}>{busy ? 'Working...' : content.submit}</button></form><div className="auth-links">{mode === 'login' && <><Link to="/forgot-password">Forgot password?</Link><span>New here? <Link to="/register">Create an account</Link></span></>}{mode === 'register' && <span>Already have an account? <Link to="/login">Sign in</Link></span>}{mode === 'forgot' && <span>Remembered it? <Link to="/login">Return to sign in</Link></span>}</div></section></main>
}
