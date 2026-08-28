import { NavLink, Outlet } from 'react-router-dom'
import { MessageCircle, FileText, Mic2, UserRound, LogOut } from 'lucide-react'
import { useAuth } from '../providers/useAuth'

const navigation = [
  { to: '/chat', label: 'Chat', icon: MessageCircle },
  { to: '/cv', label: 'CV studio', icon: FileText },
  { to: '/interview', label: 'Interview', icon: Mic2 },
  { to: '/profile', label: 'Profile', icon: UserRound },
]

export function AppLayout() {
  const { displayName, logout } = useAuth()
  return <div className="app-shell">
    <aside className="side-rail">
      <div className="brand-mark"><span>CP</span><strong>CareerPilot</strong></div>
      <nav aria-label="Main navigation">{navigation.map(({ to, label, icon: Icon }) => <NavLink key={to} to={to} className={({ isActive }) => isActive ? 'nav-link nav-link--active' : 'nav-link'}><Icon size={18} strokeWidth={1.8} /><span>{label}</span></NavLink>)}</nav>
      <div className="rail-footer"><span className="avatar">{displayName?.slice(0, 1).toUpperCase() ?? '?'}</span><span className="rail-name">{displayName ?? 'Signed in'}</span><button className="icon-button" title="Sign out" aria-label="Sign out" onClick={() => void logout()}><LogOut size={17} /></button></div>
    </aside>
    <main className="main-content"><Outlet /></main>
  </div>
}
