import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useAuth } from '../auth'
import { api, apiBlob, ApiError, apiForm } from '../lib/api'
import { formatMoney } from '../lib/format'

export function ProfilePage() {
  const { user, spaces, spaceId, token, logout, refreshProfile } = useAuth()
  const space = spaces.find((s) => s.id === spaceId)
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [avatarVersion, setAvatarVersion] = useState(0)
  const [fullName, setFullName] = useState('')
  const [country, setCountry] = useState('')
  const [currencyCode, setCurrencyCode] = useState('CLP')
  const [reminderDays, setReminderDays] = useState(3)
  const [savingProfile, setSavingProfile] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [savingPassword, setSavingPassword] = useState(false)
  const [theme, setTheme] = useState(() => localStorage.getItem('flujoclaro_theme') ?? 'light')
  const messageTimer = useRef<number | null>(null)

  useEffect(() => {
    return () => {
      if (messageTimer.current) window.clearTimeout(messageTimer.current)
    }
  }, [])

  useEffect(() => {
    if (!user) return
    setFullName(user.fullName)
    setCountry(user.country)
    setCurrencyCode(user.currencyCode)
    setReminderDays(user.reminderDays ?? 3)
  }, [user])

  const showTemporaryMessage = (text: string) => {
    if (messageTimer.current) window.clearTimeout(messageTimer.current)
    setMessage(text)
    messageTimer.current = window.setTimeout(() => setMessage(null), 5000)
  }

  useEffect(() => {
    if (!user?.hasAvatar || !token) {
      setAvatarUrl(null)
      return
    }
    let objectUrl: string | null = null
    void apiBlob('/api/auth/me/avatar', token)
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob)
        setAvatarUrl(objectUrl)
      })
      .catch(() => setAvatarUrl(null))
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [token, user?.hasAvatar, avatarVersion])

  const uploadAvatar = async (file: File | null) => {
    if (!file) return
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      setError('La foto debe ser JPG, PNG o WEBP')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      setError('La foto no puede superar los 5 MB')
      return
    }
    setUploading(true)
    setError(null)
    setMessage(null)
    try {
      const formData = new FormData()
      formData.append('avatar', file)
      await apiForm('/api/auth/me/avatar', formData, token)
      await refreshProfile()
      setAvatarVersion((value) => value + 1)
      showTemporaryMessage('Foto de perfil actualizada')
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo subir la foto')
    } finally {
      setUploading(false)
    }
  }

  const saveProfile = async (event: FormEvent) => {
    event.preventDefault()
    setSavingProfile(true)
    setError(null)
    try {
      await api('/api/auth/me/profile', {
        method: 'PUT',
        body: JSON.stringify({ fullName, country, currencyCode, reminderDays, spaceId }),
      }, token)
      await refreshProfile()
      showTemporaryMessage('Configuración actualizada')
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo actualizar el perfil')
    } finally {
      setSavingProfile(false)
    }
  }

  const changePassword = async (event: FormEvent) => {
    event.preventDefault()
    if (newPassword !== confirmPassword) {
      setError('La confirmación no coincide con la nueva contraseña')
      return
    }
    setSavingPassword(true)
    setError(null)
    try {
      await api('/api/auth/me/change-password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword, newPassword }),
      }, token)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      showTemporaryMessage('Contraseña actualizada correctamente')
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo cambiar la contraseña')
    } finally {
      setSavingPassword(false)
    }
  }

  const changeTheme = (nextTheme: string) => {
    setTheme(nextTheme)
    localStorage.setItem('flujoclaro_theme', nextTheme)
    document.documentElement.setAttribute('data-theme', nextTheme)
  }

  const initials = user?.fullName
    .split(' ')
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase() ?? 'FC'

  return (
    <div className="stack">
      <h1 style={{ margin: 0 }}>Perfil y configuración</h1>
      <article className="card stack">
        {message && <div className="alert success">{message}</div>}
        {error && <div className="alert error">{error}</div>}
        <div className="profile-header">
          <div className="profile-avatar" aria-label="Foto de perfil">
            {avatarUrl ? <img src={avatarUrl} alt={`Foto de ${user?.fullName}`} /> : <span>{initials}</span>}
          </div>
          <div className="profile-identity">
            <h3 style={{ margin: 0, color: 'var(--text)' }}>{user?.fullName}</h3>
            <p className="muted">{user?.email}</p>
            <label className={`btn secondary avatar-upload${uploading ? ' disabled' : ''}`}>
              {uploading ? 'Subiendo...' : avatarUrl ? 'Cambiar foto' : 'Agregar foto'}
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                disabled={uploading}
                onChange={(event) => {
                  void uploadAvatar(event.target.files?.[0] ?? null)
                  event.target.value = ''
                }}
              />
            </label>
          </div>
        </div>
        <div className="form-grid">
          <div><strong>Espacio activo</strong><div className="muted">{space?.name}</div></div>
          <div><strong>Saldo inicial</strong><div className="muted">{formatMoney(space?.initialBalance ?? 0, space?.currencyCode ?? 'CLP')}</div></div>
          <div><strong>Rol</strong><div className="muted">{space?.role}</div></div>
        </div>
      </article>

      <article className="card stack">
        <div>
          <h2 style={{ margin: 0, fontSize: '1.15rem' }}>Datos y preferencias</h2>
          <p className="muted">Configura cómo se muestra y organiza tu información.</p>
        </div>
        <form className="stack" onSubmit={saveProfile}>
          <div className="form-grid">
            <label>
              Nombre completo
              <input value={fullName} onChange={(event) => setFullName(event.target.value)} required />
            </label>
            <label>
              País
              <input value={country} onChange={(event) => setCountry(event.target.value)} required />
            </label>
            <label>
              Moneda
              <select value={currencyCode} onChange={(event) => setCurrencyCode(event.target.value)}>
                <option value="CLP">CLP — Peso chileno</option>
                <option value="USD">USD — Dólar</option>
                <option value="EUR">EUR — Euro</option>
                <option value="ARS">ARS — Peso argentino</option>
                <option value="MXN">MXN — Peso mexicano</option>
              </select>
            </label>
            <label>
              Avisar antes de un vencimiento
              <select
                value={reminderDays}
                onChange={(event) => setReminderDays(Number(event.target.value))}
              >
                <option value={0}>El mismo día</option>
                <option value={1}>1 día antes</option>
                <option value={3}>3 días antes</option>
                <option value={5}>5 días antes</option>
                <option value={7}>7 días antes</option>
                <option value={15}>15 días antes</option>
                <option value={30}>30 días antes</option>
              </select>
            </label>
            <label>
              Apariencia
              <select value={theme} onChange={(event) => changeTheme(event.target.value)}>
                <option value="light">Modo claro</option>
                <option value="dark">Modo oscuro</option>
              </select>
            </label>
          </div>
          <button className="btn profile-save-button" disabled={savingProfile}>
            {savingProfile ? 'Guardando...' : 'Guardar configuración'}
          </button>
        </form>
      </article>

      <article className="card stack">
        <div>
          <h2 style={{ margin: 0, fontSize: '1.15rem' }}>Seguridad</h2>
          <p className="muted">Cambia tu contraseña de acceso.</p>
        </div>
        <form className="stack" onSubmit={changePassword}>
          <div className="form-grid">
            <label>
              Contraseña actual
              <input
                type="password"
                value={currentPassword}
                onChange={(event) => setCurrentPassword(event.target.value)}
                required
              />
            </label>
            <label>
              Nueva contraseña
              <input
                type="password"
                minLength={8}
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                required
              />
            </label>
            <label>
              Confirmar nueva contraseña
              <input
                type="password"
                minLength={8}
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                required
              />
            </label>
          </div>
          <button className="btn profile-save-button" disabled={savingPassword}>
            {savingPassword ? 'Actualizando...' : 'Cambiar contraseña'}
          </button>
        </form>
      </article>

      <button className="btn secondary" onClick={logout}>Cerrar sesión</button>
    </div>
  )
}
