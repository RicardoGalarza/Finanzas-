import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useAuth } from '../auth'
import { api, ApiError } from '../lib/api'

const schema = z.object({
  fullName: z.string().min(2),
  country: z.string().min(2),
  currencyCode: z.string().min(3),
  shared: z.boolean(),
  spaceName: z.string().min(2),
})

type FormData = z.infer<typeof schema>

export function OnboardingPage() {
  const { token, refreshProfile } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      country: 'Chile',
      currencyCode: 'CLP',
      shared: false,
      spaceName: 'Mi espacio',
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    setError(null)
    try {
      await api('/api/onboarding', {
        method: 'POST',
        body: JSON.stringify({
          fullName: values.fullName,
          country: values.country,
          currencyCode: values.currencyCode,
          initialBalance: 0,
          shared: values.shared,
          spaceName: values.spaceName,
          incomes: [],
          bills: [],
        }),
      }, token)
      await refreshProfile()
      navigate('/app')
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo completar la configuración')
    }
  })

  return (
    <div className="auth-layout">
      <form className="auth-card stack" onSubmit={onSubmit} style={{ width: 'min(560px, 100%)' }}>
        <div>
          <h1>Configuración inicial</h1>
          <p className="muted">
            Personaliza tu espacio. Después podrás agregar ingresos y gastos desde el menú.
          </p>
        </div>
        {error && <div className="alert error">{error}</div>}
        <div className="form-grid">
          <label>Nombre<input {...register('fullName')} /></label>
          <label>País<input {...register('country')} /></label>
          <label>Moneda
            <select {...register('currencyCode')}>
              <option value="CLP">CLP - Peso chileno</option>
              <option value="USD">USD</option>
              <option value="EUR">EUR</option>
              <option value="MXN">MXN</option>
            </select>
          </label>
          <label>Nombre del espacio<input {...register('spaceName')} /></label>
          <label style={{ flexDirection: 'row', alignItems: 'center', gap: '0.5rem' }}>
            <input type="checkbox" {...register('shared')} />
            Administraré con otras personas
          </label>
        </div>
        <button className="btn" disabled={isSubmitting}>
          {isSubmitting ? 'Guardando...' : 'Ir al dashboard'}
        </button>
      </form>
    </div>
  )
}
