import { NavLink, Outlet } from 'react-router-dom'
import {
  CalendarDays,
  CreditCard,
  Home,
  LogOut,
  TrendingUp,
  Wallet,
} from 'lucide-react'
import { useAuth } from '../auth'

export function AppLayout() {
  const { user, spaces, spaceId, setSpaceId, logout } = useAuth()

  const links = [
    { to: '/app', label: 'Inicio', icon: Home, end: true },
    { to: '/app/ingresos', label: 'Ingresos', icon: TrendingUp },
    { to: '/app/gastos', label: 'Gastos', icon: CreditCard },
    { to: '/app/calendario', label: 'Calendario', icon: CalendarDays },
    { to: '/app/perfil', label: 'Perfil', icon: Wallet },
  ]

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div>
          <h1>FlujoClaro</h1>
          <p className="muted" style={{ color: 'rgba(255,255,255,0.7)' }}>
            {user?.fullName}
          </p>
        </div>
        {spaces.length > 0 && (
          <select
            value={spaceId ?? ''}
            onChange={(e) => setSpaceId(e.target.value)}
            style={{ background: 'rgba(255,255,255,0.12)', color: 'white', borderColor: 'transparent' }}
          >
            {spaces.map((s) => (
              <option key={s.id} value={s.id} style={{ color: '#0f172a' }}>
                {s.name}
              </option>
            ))}
          </select>
        )}
        <nav>
          {links.map((link) => (
            <NavLink key={link.to} to={link.to} end={link.end} className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
              <span style={{ display: 'inline-flex', gap: '0.55rem', alignItems: 'center' }}>
                <link.icon size={18} />
                {link.label}
              </span>
            </NavLink>
          ))}
        </nav>
        <div style={{ marginTop: 'auto' }} className="stack">
          <button className="btn secondary" style={{ color: 'white', borderColor: 'rgba(255,255,255,0.25)' }} onClick={logout}>
            <LogOut size={16} /> Cerrar sesión
          </button>
        </div>
      </aside>

      <main className="content">
        <Outlet />
      </main>

      <nav className="bottom-nav">
        {links.map((link) => (
          <NavLink key={link.to} to={link.to} end={link.end} className={({ isActive }) => (isActive ? 'active' : '')}>
            <link.icon size={18} />
            {link.label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
