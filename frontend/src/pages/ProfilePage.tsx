import { useEffect, useState, type FormEvent } from 'react'
import { useAuth } from '../auth'
import { api, apiBlob, ApiError, apiForm } from '../lib/api'
import { useTemporaryMessage } from '../lib/useTemporaryMessage'

export function ProfilePage() {
  const { user, spaces, spaceId, token, logout, refreshProfile } = useAuth()
  const space = spaces.find((s) => s.id === spaceId)
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)
  const { message, showMessage, clearMessage } = useTemporaryMessage()
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

  useEffect(() => {
    if (!user) return
    setFullName(user.fullName)
    setCountry(user.country)
    setCurrencyCode(user.currencyCode)
    setReminderDays(user.reminderDays ?? 3)
  }, [user])

  const showTemporaryMessage = showMessage

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
    clearMessage()
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
        body: JSON.stringify({
          fullName,
          country,
          currencyCode,
          reminderDays,
          spaceId,
        }),
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

  const initials = user?.fullName
    .split(' ')
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase() ?? 'FC'

  return (
    <div className="profile-page">
      <div className="profile-page__header">
        <h1 style={{ margin: 0 }}>Perfil y configuración</h1>
        <p className="muted">Administra tu cuenta y preferencias</p>
      </div>

      {message && <div className="alert success">{message}</div>}
      {error && <div className="alert error">{error}</div>}

      <article className="card profile-card profile-card--hero">
        <div className="profile-hero">
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
          <div className="profile-meta">
            <div className="profile-meta__item">
              <span className="profile-meta__label">Espacio</span>
              <strong>{space?.name}</strong>
            </div>
            <div className="profile-meta__item">
              <span className="profile-meta__label">Rol</span>
              <strong>{space?.role}</strong>
            </div>
          </div>
          <button type="button" className="btn secondary profile-logout" onClick={logout}>
            Cerrar sesión
          </button>
        </div>
      </article>

      <article className="card profile-card stack">
        <div className="profile-section-title">
          <h2>Datos y preferencias</h2>
          <p className="muted">Configura cómo se muestra y organiza tu información.</p>
        </div>
        <form className="stack" onSubmit={saveProfile}>
          <div className="profile-form-grid">
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
          </div>
          <div className="profile-actions">
            <button className="btn" disabled={savingProfile}>
              {savingProfile ? 'Guardando...' : 'Guardar configuración'}
            </button>
          </div>
        </form>
      </article>

      <article className="card profile-card stack">
        <div className="profile-section-title">
          <h2>Seguridad</h2>
          <p className="muted">Cambia tu contraseña de acceso.</p>
        </div>
        <form className="stack" onSubmit={changePassword}>
          <div className="profile-form-grid">
            <label>
              Contraseña actual
              <input
                type="password"
                autoComplete="current-password"
                value={currentPassword}
                onChange={(event) => setCurrentPassword(event.target.value)}
                required
              />
            </label>
            <label>
              Nueva contraseña
              <input
                type="password"
                autoComplete="new-password"
                minLength={8}
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                required
              />
            </label>
            <label className="profile-form-grid__full">
              Confirmar nueva contraseña
              <input
                type="password"
                autoComplete="new-password"
                minLength={8}
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                required
              />
            </label>
          </div>
          <div className="profile-actions">
            <button className="btn" disabled={savingPassword}>
              {savingPassword ? 'Actualizando...' : 'Cambiar contraseña'}
            </button>
          </div>
        </form>
      </article>
    </div>
  )
}
