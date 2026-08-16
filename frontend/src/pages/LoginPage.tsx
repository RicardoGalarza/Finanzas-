import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useAuth } from '../auth'
import { ApiError } from '../lib/api'

const schema = z.object({
  email: z.string().email('Correo inválido'),
  password: z.string().min(8, 'Mínimo 8 caracteres'),
})

type FormData = z.infer<typeof schema>

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const onSubmit = handleSubmit(async (values) => {
    setError(null)
    try {
      const auth = await login(values.email, values.password)
      navigate(auth.onboardingCompleted ? '/app' : '/onboarding')
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo iniciar sesión')
    }
  })

  return (
    <div className="auth-layout">
      <form className="auth-card stack" onSubmit={onSubmit}>
        <div>
          <h1>Iniciar sesión</h1>
          <p className="muted">Accede a tu espacio FlujoClaro</p>
        </div>
        {error && <div className="alert error">{error}</div>}
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
          {isSubmitting ? 'Entrando...' : 'Entrar'}
        </button>
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <Link to="/recuperar">Olvidé mi contraseña</Link>
          <Link to="/registro">Crear cuenta</Link>
        </div>
        <p className="muted">Demo: demo@flujoclaro.cl / Demo1234!</p>
      </form>
    </div>
  )
}
