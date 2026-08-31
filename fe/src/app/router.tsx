import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from './providers/useAuth'
import { AuthPage } from '../features/auth/AuthPage'
import { PendingPage } from '../features/auth/PendingPage'
import { VerifyPage } from '../features/auth/VerifyPage'
import { ResetPage } from '../features/auth/ResetPage'
import { AppLayout } from './layout/AppLayout'
import { ChatPage } from '../features/chat/ChatPage'
import { ProfilePage } from '../features/profile/ProfilePage'

function ProtectedRoutes() {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />
}

function PlaceholderPage({ title, detail }: { title: string; detail: string }) {
  return <section className="workspace"><p className="eyebrow">CareerPilot workspace</p><h1>{title}</h1><p className="lede">{detail}</p></section>
}

export function AppRouter() {
  return <Routes>
    <Route path="/login" element={<AuthPage mode="login" />} />
    <Route path="/register" element={<AuthPage mode="register" />} />
    <Route path="/forgot-password" element={<AuthPage mode="forgot" />} />
    <Route path="/verify-registration" element={<VerifyPage />} />
    <Route path="/reset-password" element={<ResetPage />} />
    <Route path="/pending-registration" element={<PendingPage kind="registration" />} />
    <Route path="/pending-reset" element={<PendingPage kind="reset" />} />
    <Route element={<ProtectedRoutes />}>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/chat" replace />} />
        <Route path="chat/:conversationId?" element={<ChatPage />} />
        <Route path="cv" element={<PlaceholderPage title="Make your experience legible" detail="Upload and understand your CV here." />} />
        <Route path="interview" element={<PlaceholderPage title="Practice under pressure" detail="Your live interview room will open here." />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>
    </Route>
    <Route path="*" element={<Navigate to="/chat" replace />} />
  </Routes>
}
