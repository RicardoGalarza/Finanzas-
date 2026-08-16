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
  initialBalance: z.coerce.number().min(0),
  shared: z.boolean(),
  spaceName: z.string().min(2),
  incomeDescription: z.string().optional(),
  incomeAmount: z.coerce.number().min(0).optional(),
  billName: z.string().optional(),
  billAmount: z.coerce.number().min(0).optional(),
  billDueDay: z.coerce.number().min(1).max(28).optional(),
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
      initialBalance: 0,
      shared: false,
      spaceName: 'Mi espacio',
      billDueDay: 5,
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    setError(null)
    try {
      const incomes = values.incomeAmount && values.incomeAmount > 0
        ? [{
            description: values.incomeDescription || 'Ingreso habitual',
            amount: values.incomeAmount,
            category: 'Sueldo',
            frequency: 'MONTHLY',
          }]
        : []
      const bills = values.billAmount && values.billAmount > 0
        ? [{
            name: values.billName || 'Cuenta mensual',
            amount: values.billAmount,
            category: 'Otros',
            dueDay: values.billDueDay || 5,
          }]
        : []

      await api('/api/onboarding', {
        method: 'POST',
        body: JSON.stringify({
          fullName: values.fullName,
          country: values.country,
          currencyCode: values.currencyCode,
          initialBalance: values.initialBalance,
          shared: values.shared,
          spaceName: values.spaceName,
          incomes,
          bills,
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
      <form className="auth-card stack" onSubmit={onSubmit} style={{ width: 'min(720px, 100%)' }}>
        <div>
          <h1>Configuración inicial</h1>
          <p className="muted">Personaliza tu espacio financiero en unos minutos</p>
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
          <label>Saldo inicial<input type="number" step="1" {...register('initialBalance')} /></label>
          <label>Nombre del espacio<input {...register('spaceName')} /></label>
          <label style={{ flexDirection: 'row', alignItems: 'center', gap: '0.5rem' }}>
            <input type="checkbox" {...register('shared')} />
            Administraré con otras personas
          </label>
        </div>
        <h3>Ingreso habitual (opcional)</h3>
        <div className="form-grid">
          <label>Descripción<input {...register('incomeDescription')} placeholder="Sueldo" /></label>
          <label>Monto<input type="number" {...register('incomeAmount')} /></label>
        </div>
        <h3>Cuenta mensual principal (opcional)</h3>
        <div className="form-grid">
          <label>Nombre<input {...register('billName')} placeholder="Arriendo" /></label>
          <label>Monto<input type="number" {...register('billAmount')} /></label>
          <label>Día de vencimiento<input type="number" {...register('billDueDay')} /></label>
        </div>
        <button className="btn" disabled={isSubmitting}>
          {isSubmitting ? 'Guardando...' : 'Ir al dashboard'}
        </button>
      </form>
    </div>
  )
}
