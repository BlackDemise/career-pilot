import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { resetPassword } from './api'
import { PageMessage } from '../../shared/components/PageMessage'

export function ResetPage() {
  const navigate = useNavigate(); const [params] = useSearchParams(); const token = params.get('token') ?? ''
  const [password, setPassword] = useState(''); const [confirmPassword, setConfirmPassword] = useState(''); const [error, setError] = useState<string | null>(null); const [busy, setBusy] = useState(false)
  async function submit(event: FormEvent) { event.preventDefault(); setError(null); if (!token) { setError('This reset link is missing its token.'); return } if (password !== confirmPassword) { setError('Passwords must match.'); return } setBusy(true); try { await resetPassword({ token, password, confirmPassword }); navigate('/login', { replace: true }) } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to reset password.') } finally { setBusy(false) } }
  return <main className="auth-page auth-page--single"><section className="auth-panel"><span className="brand-mini">CP</span><p className="eyebrow">Choose a new password</p><h1>Start fresh.</h1><form onSubmit={submit} className="auth-form"><div><label htmlFor="password">New password</label><input id="password" type="password" required minLength={8} value={password} onChange={(event) => setPassword(event.target.value)} /></div><div><label htmlFor="confirmPassword">Confirm password</label><input id="confirmPassword" type="password" required minLength={8} value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} /></div>{error && <PageMessage tone="error">{error}</PageMessage>}<button className="primary-button" disabled={busy}>{busy ? 'Saving...' : 'Reset password'}</button></form><Link to="/login" className="back-link">Back to sign in</Link></section></main>
}
