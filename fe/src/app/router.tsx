import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from './providers/useAuth'
import { AuthPage } from '../features/auth/AuthPage'
import { PendingPage } from '../features/auth/PendingPage'
import { VerifyPage } from '../features/auth/VerifyPage'
import { ResetPage } from '../features/auth/ResetPage'
import { AppLayout } from './layout/AppLayout'
import { ChatPage } from '../features/chat/ChatPage'
import { CvAnalysisPage } from '../features/cv-analysis/CvAnalysisPage'
import { ProfilePage } from '../features/profile/ProfilePage'
import { InterviewPage } from '../features/interview/InterviewPage'

function ProtectedRoutes() {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />
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
        <Route path="cv" element={<CvAnalysisPage />} />
        <Route path="interview" element={<InterviewPage />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>
    </Route>
    <Route path="*" element={<Navigate to="/chat" replace />} />
  </Routes>
}
