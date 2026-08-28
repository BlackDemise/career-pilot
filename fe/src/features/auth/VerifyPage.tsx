import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { verifyRegistration } from './api'

export function VerifyPage() {
  const navigate = useNavigate(); const [params] = useSearchParams(); const token = params.get('token'); const [message, setMessage] = useState(token ? 'Verifying your email...' : 'This verification link is missing its token.')
  useEffect(() => { if (!token) return; let active = true; void verifyRegistration(token).then(() => { if (!active) return; setMessage('Your email is verified. Redirecting to sign in...'); window.setTimeout(() => navigate('/login', { replace: true }), 1200) }).catch((error: unknown) => { if (active) setMessage(error instanceof Error ? error.message : 'This verification link is no longer valid.') }); return () => { active = false } }, [navigate, token])
  return <main className="auth-page auth-page--single"><section className="auth-panel pending-panel"><span className="brand-mini">CP</span><p className="eyebrow">Email verification</p><h1>{message}</h1></section></main>
}
