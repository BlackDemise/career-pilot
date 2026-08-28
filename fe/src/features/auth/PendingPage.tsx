import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { PageMessage } from '../../shared/components/PageMessage'
import { resendPasswordReset, resendRegistration } from './api'

type Kind = 'registration' | 'reset'

export function PendingPage({ kind }: { kind: Kind }) {
  const [params] = useSearchParams(); const email = params.get('email') ?? ''
  const [seconds, setSeconds] = useState(0); const [message, setMessage] = useState<string | null>(null)
  const resend = kind === 'registration' ? resendRegistration : resendPasswordReset
  const title = kind === 'registration' ? 'Check your inbox.' : 'Recovery link on its way.'
  const description = kind === 'registration' ? 'Your account will be created after you verify your email.' : 'Keep this page open while the recovery email arrives.'
  const cooldown = useMemo(() => seconds > 0 ? `Resend available in ${seconds}s` : 'Resend email', [seconds])
  async function handleResend() { if (!email || seconds > 0) return; setMessage(null); await resend(email); setMessage('A new link has been requested.'); setSeconds(60); const timer = window.setInterval(() => setSeconds((value) => { if (value <= 1) { window.clearInterval(timer); return 0 } return value - 1 }), 1000) }
  return <main className="auth-page auth-page--single"><section className="auth-panel pending-panel"><span className="brand-mini">CP</span><p className="eyebrow">{kind === 'registration' ? 'Verify your email' : 'Password recovery'}</p><h1>{title}</h1><p className="lede">{description}</p>{email && <p className="email-highlight">{email}</p>}<button className="primary-button" disabled={!email || seconds > 0} onClick={() => void handleResend()}>{cooldown}</button>{message && <PageMessage tone="success">{message}</PageMessage>}<Link to="/login" className="back-link">Back to sign in</Link></section></main>
}
