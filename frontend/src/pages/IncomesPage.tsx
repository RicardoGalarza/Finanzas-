import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useAuth } from '../auth'
import { api, ApiError } from '../lib/api'
import { formatMoney } from '../lib/format'
import { useTemporaryMessage } from '../lib/useTemporaryMessage'
import { INCOME_CATEGORIES, PAYMENT_METHODS, type Income } from '../types'

const schema = z.object({
  description: z.string().min(2),
  amount: z.coerce.number().positive('El monto debe ser mayor a 0'),
  incomeDate: z.string().min(1),
  category: z.string().min(1),
  receivedBy: z.string().min(1),
  incomeType: z.enum(['ONE_TIME', 'RECURRING']),
  frequency: z.enum(['WEEKLY', 'BIWEEKLY', 'MONTHLY']).optional().nullable(),
  paymentMethod: z.string().optional(),
  notes: z.string().optional(),
})

type FormData = z.infer<typeof schema>

export function IncomesPage() {
  const { token, spaceId, spaces, user } = useAuth()
  const currency = spaces.find((s) => s.id === spaceId)?.currencyCode ?? 'CLP'
  const qc = useQueryClient()
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [editing, setEditing] = useState<Income | null>(null)
  const { message, showMessage } = useTemporaryMessage()
  const [error, setError] = useState<string | null>(null)

  const query = useQuery({
    queryKey: ['incomes', spaceId, search, category],
    enabled: !!token && !!spaceId,
    queryFn: () => {
      const params = new URLSearchParams()
      if (search) params.set('search', search)
      if (category) params.set('category', category)
      const qs = params.toString()
      return api<Income[]>(`/api/spaces/${spaceId}/incomes${qs ? `?${qs}` : ''}`, {}, token)
    },
  })

  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      incomeDate: new Date().toISOString().slice(0, 10),
      category: 'Sueldo',
      receivedBy: user?.fullName ?? '',
      incomeType: 'ONE_TIME',
      paymentMethod: 'Transferencia',
    },
  })

  const incomeType = form.watch('incomeType')

  const saveMutation = useMutation({
    mutationFn: async (values: FormData) => {
      const payload = {
        ...values,
        frequency: values.incomeType === 'RECURRING' ? values.frequency : null,
      }
      if (editing) {
        return api(`/api/spaces/${spaceId}/incomes/${editing.id}`, {
          method: 'PUT',
          body: JSON.stringify(payload),
        }, token)
      }
      return api(`/api/spaces/${spaceId}/incomes`, {
        method: 'POST',
        body: JSON.stringify(payload),
      }, token)
    },
    onSuccess: async () => {
      showMessage(editing ? 'Ingreso actualizado' : 'Ingreso creado')
      setError(null)
      setEditing(null)
      form.reset({
        description: '',
        amount: undefined as unknown as number,
        incomeDate: new Date().toISOString().slice(0, 10),
        category: 'Sueldo',
        receivedBy: user?.fullName ?? '',
        incomeType: 'ONE_TIME',
        paymentMethod: 'Transferencia',
        notes: '',
      })
      await qc.invalidateQueries({ queryKey: ['incomes', spaceId] })
      await qc.invalidateQueries({ queryKey: ['dashboard', spaceId] })
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : 'Error al guardar'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api(`/api/spaces/${spaceId}/incomes/${id}`, { method: 'DELETE' }, token),
    onSuccess: async () => {
      showMessage('Ingreso eliminado')
      await qc.invalidateQueries({ queryKey: ['incomes', spaceId] })
      await qc.invalidateQueries({ queryKey: ['dashboard', spaceId] })
    },
  })

  const total = useMemo(
    () => (query.data ?? []).reduce((acc, item) => acc + Number(item.amount), 0),
    [query.data],
  )

  return (
    <div className="stack">
      <div className="topbar">
        <div>
          <h1 style={{ margin: 0 }}>Ingresos</h1>
          <p className="muted">Total filtrado: {formatMoney(total, currency)}</p>
        </div>
      </div>

      {message && <div className="alert success">{message}</div>}
      {error && <div className="alert error">{error}</div>}

      <article className="card stack">
        <h3 style={{ margin: 0, color: 'var(--text)' }}>{editing ? 'Editar ingreso' : 'Nuevo ingreso'}</h3>
        <form
          className="stack"
          onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))}
        >
          <div className="form-grid">
            <label>Descripción<input {...form.register('description')} /></label>
            <label>Monto<input type="number" step="1" {...form.register('amount')} /></label>
            <label>Fecha<input type="date" {...form.register('incomeDate')} /></label>
            <label>Categoría
              <select {...form.register('category')}>
                {INCOME_CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
              </select>
            </label>
            <label>Persona que recibió<input {...form.register('receivedBy')} /></label>
            <label>Tipo
              <select {...form.register('incomeType')}>
                <option value="ONE_TIME">Único</option>
                <option value="RECURRING">Recurrente</option>
              </select>
            </label>
            {incomeType === 'RECURRING' && (
              <label>Frecuencia
                <select {...form.register('frequency')}>
                  <option value="WEEKLY">Semanal</option>
                  <option value="BIWEEKLY">Quincenal</option>
                  <option value="MONTHLY">Mensual</option>
                </select>
              </label>
            )}
            <label>Medio / banco
              <select {...form.register('paymentMethod')}>
                <option value="">Seleccionar...</option>
                {PAYMENT_METHODS.map((method) => (
                  <option key={method} value={method}>{method}</option>
                ))}
              </select>
            </label>
            <label>Notas<textarea {...form.register('notes')} /></label>
          </div>
          <div className="row">
            <button className="btn" disabled={saveMutation.isPending}>{editing ? 'Actualizar' : 'Guardar'}</button>
            {editing && (
              <button type="button" className="btn secondary" onClick={() => setEditing(null)}>Cancelar</button>
            )}
          </div>
        </form>
      </article>

      <article className="card stack">
        <div className="row">
          <input placeholder="Buscar..." value={search} onChange={(e) => setSearch(e.target.value)} />
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="">Todas las categorías</option>
            {INCOME_CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Descripción</th>
                <th>Fecha</th>
                <th>Categoría</th>
                <th>Persona</th>
                <th>Monto</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {(query.data ?? []).map((item) => (
                <tr key={item.id}>
                  <td>{item.description}</td>
                  <td>{item.incomeDate}</td>
                  <td>{item.category}</td>
                  <td>{item.receivedBy}</td>
                  <td>{formatMoney(item.amount, currency)}</td>
                  <td className="row">
                    <button
                      className="btn secondary"
                      onClick={() => {
                        setEditing(item)
                        form.reset({
                          description: item.description,
                          amount: Number(item.amount),
                          incomeDate: item.incomeDate,
                          category: item.category,
                          receivedBy: item.receivedBy,
                          incomeType: item.incomeType,
                          frequency: item.frequency ?? undefined,
                          paymentMethod: item.paymentMethod ?? '',
                          notes: item.notes ?? '',
                        })
                      }}
                    >
                      Editar
                    </button>
                    <button className="btn danger" onClick={() => deleteMutation.mutate(item.id)}>Eliminar</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </article>
    </div>
  )
}
