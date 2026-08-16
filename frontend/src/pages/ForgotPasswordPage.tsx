import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { api, ApiError } from '../lib/api'

const forgotSchema = z.object({ email: z.string().email() })
const resetSchema = z.object({
  token: z.string().min(10),
  newPassword: z.string().min(8),
})

export function ForgotPasswordPage() {
  const [searchParams] = useSearchParams()
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const forgotForm = useForm<z.infer<typeof forgotSchema>>({ resolver: zodResolver(forgotSchema) })
  const resetForm = useForm<z.infer<typeof resetSchema>>({ resolver: zodResolver(resetSchema) })

  useEffect(() => {
    const tokenFromLink = searchParams.get('token')
    if (tokenFromLink) {
      resetForm.setValue('token', tokenFromLink)
    }
  }, [searchParams, resetForm])

  return (
    <div className="auth-layout">
      <div className="auth-card stack">
        <div>
          <h1>Recuperar contraseña</h1>
          <p className="muted">Te enviaremos un correo con el enlace para restablecer tu acceso</p>
        </div>
        {message && <div className="alert success">{message}</div>}
        {error && <div className="alert error">{error}</div>}

        <form
          className="stack"
          onSubmit={forgotForm.handleSubmit(async (values) => {
            setError(null)
            try {
              const res = await api<{ message: string }>('/api/auth/forgot-password', {
                method: 'POST',
                body: JSON.stringify(values),
              })
              setMessage(res.message)
            } catch (e) {
              setError(e instanceof ApiError ? e.message : 'Error al solicitar recuperación')
            }
          })}
        >
          <label>
            Correo
            <input type="email" autoComplete="email" {...forgotForm.register('email')} />
          </label>
          <button className="btn">Enviar instrucciones</button>
        </form>

        <hr style={{ border: 'none', borderTop: '1px solid var(--border)' }} />

        <form
          className="stack"
          onSubmit={resetForm.handleSubmit(async (values) => {
            setError(null)
            try {
              const res = await api<{ message: string }>('/api/auth/reset-password', {
                method: 'POST',
                body: JSON.stringify(values),
              })
              setMessage(res.message)
              resetForm.reset()
            } catch (e) {
              setError(e instanceof ApiError ? e.message : 'Error al restablecer')
            }
          })}
        >
          <label>
            Token (si no usaste el enlace del correo)
            <input autoComplete="one-time-code" {...resetForm.register('token')} />
          </label>
          <label>
            Nueva contraseña
            <input type="password" autoComplete="new-password" {...resetForm.register('newPassword')} />
          </label>
          <button className="btn secondary">Restablecer contraseña</button>
        </form>
        <Link to="/login">Volver al inicio de sesión</Link>
      </div>
    </div>
  )
}
