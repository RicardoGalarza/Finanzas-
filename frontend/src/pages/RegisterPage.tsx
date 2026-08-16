import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useAuth } from '../auth'
import { ApiError } from '../lib/api'

const schema = z.object({
  fullName: z.string().min(2, 'Nombre requerido'),
  email: z.string().email('Correo inválido'),
  password: z.string().min(8, 'Mínimo 8 caracteres'),
})

type FormData = z.infer<typeof schema>

export function RegisterPage() {
  const { register: registerUser } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const onSubmit = handleSubmit(async (values) => {
    setError(null)
    try {
      await registerUser(values.fullName, values.email, values.password)
      navigate('/onboarding')
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo registrar')
    }
  })

  return (
    <div className="auth-layout">
      <form className="auth-card stack" onSubmit={onSubmit}>
        <div>
          <h1>Crear cuenta</h1>
          <p className="muted">Empieza a ordenar tus finanzas hoy</p>
        </div>
        {error && <div className="alert error">{error}</div>}
        <label>
          Nombre
          <input {...register('fullName')} />
          {errors.fullName && <span className="muted">{errors.fullName.message}</span>}
        </label>
        <label>
          Correo
          <input type="email" {...register('email')} />
          {errors.email && <span className="muted">{errors.email.message}</span>}
        </label>
        <label>
          Contraseña
          <input type="password" {...register('password')} />
          {errors.password && <span className="muted">{errors.password.message}</span>}
        </label>
        <button className="btn" disabled={isSubmitting}>
          {isSubmitting ? 'Creando...' : 'Registrarme'}
        </button>
        <Link to="/login">Ya tengo cuenta</Link>
      </form>
    </div>
  )
}
