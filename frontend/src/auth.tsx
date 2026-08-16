import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from './lib/api'
import type { AuthResponse, Space, UserProfile } from './types'

type AuthState = {
  token: string | null
  user: UserProfile | null
  spaces: Space[]
  spaceId: string | null
  loading: boolean
  login: (email: string, password: string) => Promise<AuthResponse>
  register: (fullName: string, email: string, password: string) => Promise<AuthResponse>
  logout: () => void
  refreshProfile: () => Promise<void>
  setSpaceId: (id: string) => void
}

const AuthContext = createContext<AuthState | null>(null)

const TOKEN_KEY = 'flujoclaro_token'
const SPACE_KEY = 'flujoclaro_space'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState<UserProfile | null>(null)
  const [spaces, setSpaces] = useState<Space[]>([])
  const [spaceId, setSpaceIdState] = useState<string | null>(() => localStorage.getItem(SPACE_KEY))
  const [loading, setLoading] = useState(true)

  const persistSession = (auth: AuthResponse) => {
    localStorage.setItem(TOKEN_KEY, auth.accessToken)
    // El perfil se resuelve en el efecto de [token]; sin esto los guards
    // ven token sin usuario y devuelven a /login.
    setLoading(true)
    setToken(auth.accessToken)
    if (auth.defaultSpaceId) {
      localStorage.setItem(SPACE_KEY, auth.defaultSpaceId)
      setSpaceIdState(auth.defaultSpaceId)
    }
  }

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(SPACE_KEY)
    setToken(null)
    setUser(null)
    setSpaces([])
    setSpaceIdState(null)
    setLoading(false)
  }

  const refreshProfile = async () => {
    if (!token) return
    const me = await api<UserProfile>('/api/auth/me', {}, token)
    const spaceList = await api<Space[]>('/api/spaces', {}, token)
    setUser(me)
    setSpaces(spaceList)
    if (!spaceId && spaceList[0]) {
      localStorage.setItem(SPACE_KEY, spaceList[0].id)
      setSpaceIdState(spaceList[0].id)
    }
  }

  useEffect(() => {
    const boot = async () => {
      if (!token) {
        setLoading(false)
        return
      }
      try {
        await refreshProfile()
      } catch {
        logout()
      } finally {
        setLoading(false)
      }
    }
    void boot()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  const value = useMemo<AuthState>(
    () => ({
      token,
      user,
      spaces,
      spaceId,
      loading,
      login: async (email, password) => {
        const auth = await api<AuthResponse>('/api/auth/login', {
          method: 'POST',
          body: JSON.stringify({ email, password }),
        })
        persistSession(auth)
        return auth
      },
      register: async (fullName, email, password) => {
        const auth = await api<AuthResponse>('/api/auth/register', {
          method: 'POST',
          body: JSON.stringify({ fullName, email, password }),
        })
        persistSession(auth)
        return auth
      },
      logout,
      refreshProfile,
      setSpaceId: (id: string) => {
        localStorage.setItem(SPACE_KEY, id)
        setSpaceIdState(id)
      },
    }),
    [token, user, spaces, spaceId, loading],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
