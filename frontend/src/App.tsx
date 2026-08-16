import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth'
import { AppLayout } from './components/AppLayout'
import { LandingPage } from './pages/LandingPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { OnboardingPage } from './pages/OnboardingPage'
import { DashboardPage } from './pages/DashboardPage'
import { IncomesPage } from './pages/IncomesPage'
import { ExpensesPage } from './pages/ExpensesPage'
import { CalendarPage } from './pages/CalendarPage'
import { ProfilePage } from './pages/ProfilePage'

function useSessionStatus() {
  const { token, user, loading } = useAuth()
  return { token, user, resolving: loading || (!!token && !user) }
}

function PublicOnly({ children }: { children: React.ReactNode }) {
  const { token, user, resolving } = useSessionStatus()
  if (resolving) return <div className="auth-layout">Cargando...</div>
  if (token && user) {
    return <Navigate to={user.onboardingCompleted ? '/app' : '/onboarding'} replace />
  }
  return children
}

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { token, user, resolving } = useSessionStatus()
  if (resolving) return <div className="auth-layout">Cargando...</div>
  if (!token || !user) return <Navigate to="/login" replace />
  return children
}

function RequireOnboarded() {
  const { user } = useAuth()
  if (!user?.onboardingCompleted) return <Navigate to="/onboarding" replace />
  return <Outlet />
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<PublicOnly><LoginPage /></PublicOnly>} />
      <Route path="/registro" element={<PublicOnly><RegisterPage /></PublicOnly>} />
      <Route path="/recuperar" element={<PublicOnly><ForgotPasswordPage /></PublicOnly>} />
      <Route
        path="/onboarding"
        element={(
          <RequireAuth>
            <OnboardingPage />
          </RequireAuth>
        )}
      />
      <Route
        path="/app"
        element={(
          <RequireAuth>
            <RequireOnboarded />
          </RequireAuth>
        )}
      >
        <Route element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="ingresos" element={<IncomesPage />} />
          <Route path="gastos" element={<ExpensesPage />} />
          <Route path="calendario" element={<CalendarPage />} />
          <Route path="perfil" element={<ProfilePage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
