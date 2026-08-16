import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useAuth } from '../auth'
import { api, apiBlob, ApiError, apiForm } from '../lib/api'
import { formatMoney, statusLabel } from '../lib/format'
import { EXPENSE_CATEGORIES, type Expense } from '../types'

const schema = z.object({
  name: z.string().min(2),
  amount: z.coerce.number().positive('El monto debe ser mayor a 0'),
  dueDate: z.string().min(1),
  category: z.string().min(1),
  responsiblePerson: z.string().min(1),
  expenseType: z.enum(['ONE_TIME', 'RECURRING']),
  frequency: z.enum(['WEEKLY', 'BIWEEKLY', 'MONTHLY']).optional().nullable(),
  paymentMethod: z.string().optional(),
  notes: z.string().optional(),
})

type FormData = z.infer<typeof schema>

export function ExpensesPage() {
  const { token, spaceId, spaces, user } = useAuth()
  const currency = spaces.find((s) => s.id === spaceId)?.currencyCode ?? 'CLP'
  const qc = useQueryClient()
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [status, setStatus] = useState('')
  const [editing, setEditing] = useState<Expense | null>(null)
  const [paying, setPaying] = useState<Expense | null>(null)
  const [paymentDate, setPaymentDate] = useState(new Date().toISOString().slice(0, 10))
  const [receipt, setReceipt] = useState<File | null>(null)
  const [preview, setPreview] = useState<{ url: string; type: string; name: string } | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!preview) return
    return () => URL.revokeObjectURL(preview.url)
  }, [preview])

  const query = useQuery({
    queryKey: ['expenses', spaceId, search, category, status],
    enabled: !!token && !!spaceId,
    queryFn: () => {
      const params = new URLSearchParams()
      if (search) params.set('search', search)
      if (category) params.set('category', category)
      if (status) params.set('status', status)
      const qs = params.toString()
      return api<Expense[]>(`/api/spaces/${spaceId}/expenses${qs ? `?${qs}` : ''}`, {}, token)
    },
  })

  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      dueDate: new Date().toISOString().slice(0, 10),
      category: 'Supermercado',
      responsiblePerson: user?.fullName ?? '',
      expenseType: 'ONE_TIME',
    },
  })

  const expenseType = form.watch('expenseType')

  const resetForm = () => {
    form.reset({
      name: '',
      amount: undefined as unknown as number,
      dueDate: new Date().toISOString().slice(0, 10),
      category: 'Supermercado',
      responsiblePerson: user?.fullName ?? '',
      expenseType: 'ONE_TIME',
      frequency: undefined,
      paymentMethod: '',
      notes: '',
    })
  }

  const startEditing = (item: Expense) => {
    setError(null)
    setEditing(item)
    form.reset({
      name: item.name,
      amount: Number(item.amount),
      dueDate: item.dueDate,
      category: item.category,
      responsiblePerson: item.responsiblePerson,
      expenseType: item.expenseType,
      frequency: item.frequency ?? undefined,
      paymentMethod: item.paymentMethod ?? '',
      notes: item.notes ?? '',
    })
  }

  const cancelEditing = () => {
    setEditing(null)
    resetForm()
  }

  const invalidate = async () => {
    await qc.invalidateQueries({ queryKey: ['expenses', spaceId] })
    await qc.invalidateQueries({ queryKey: ['dashboard', spaceId] })
    await qc.invalidateQueries({ queryKey: ['calendar', spaceId] })
  }

  const saveMutation = useMutation({
    mutationFn: async (values: FormData) => {
      const payload = {
        ...values,
        frequency: values.expenseType === 'RECURRING' ? values.frequency : null,
      }
      if (editing) {
        return api(`/api/spaces/${spaceId}/expenses/${editing.id}`, {
          method: 'PUT',
          body: JSON.stringify(payload),
        }, token)
      }
      return api(`/api/spaces/${spaceId}/expenses`, {
        method: 'POST',
        body: JSON.stringify(payload),
      }, token)
    },
    onSuccess: async () => {
      setMessage(editing ? 'Cuenta actualizada' : 'Cuenta creada')
      setError(null)
      setEditing(null)
      resetForm()
      await invalidate()
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : 'Error al guardar'),
  })

  const payMutation = useMutation({
    mutationFn: ({ id, paidAt, file }: { id: string; paidAt: string; file: File | null }) => {
      const formData = new FormData()
      formData.append('paidAt', paidAt)
      if (file) formData.append('receipt', file)
      return apiForm<Expense>(`/api/spaces/${spaceId}/expenses/${id}/pay`, formData, token)
    },
    onSuccess: async () => {
      setMessage(receipt ? 'Cuenta pagada y comprobante guardado' : 'Cuenta marcada como pagada')
      setError(null)
      setPaying(null)
      setReceipt(null)
      await invalidate()
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : 'No se pudo registrar el pago'),
  })

  const openReceipt = async (expense: Expense) => {
    try {
      setError(null)
      const blob = await apiBlob(
        `/api/spaces/${spaceId}/expenses/${expense.id}/receipt`,
        token,
      )
      const extension = blob.type === 'application/pdf' ? 'pdf' : blob.type.split('/')[1] ?? 'png'
      setPreview({
        url: URL.createObjectURL(blob),
        type: blob.type,
        name: `comprobante-${expense.name}.${extension}`,
      })
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo abrir el comprobante')
    }
  }

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api(`/api/spaces/${spaceId}/expenses/${id}`, { method: 'DELETE' }, token),
    onSuccess: async () => {
      setMessage('Cuenta eliminada')
      setError(null)
      await invalidate()
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : 'No se pudo eliminar la cuenta'),
  })

  const pendingTotal = useMemo(
    () => (query.data ?? [])
      .filter((e) => e.status !== 'PAID')
      .reduce((acc, item) => acc + Number(item.amount), 0),
    [query.data],
  )

  const expenseForm = (
    <form className="stack" onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))}>
      <div className="form-grid">
        <label>Nombre<input {...form.register('name')} /></label>
        <label>Monto<input type="number" step="1" {...form.register('amount')} /></label>
        <label>Vencimiento<input type="date" {...form.register('dueDate')} /></label>
        <label>Categoría
          <select {...form.register('category')}>
            {EXPENSE_CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </label>
        <label>Responsable<input {...form.register('responsiblePerson')} /></label>
        <label>Tipo
          <select {...form.register('expenseType')}>
            <option value="ONE_TIME">Único</option>
            <option value="RECURRING">Recurrente</option>
          </select>
        </label>
        {expenseType === 'RECURRING' && (
          <label>Frecuencia
            <select {...form.register('frequency')}>
              <option value="WEEKLY">Semanal</option>
              <option value="BIWEEKLY">Quincenal</option>
              <option value="MONTHLY">Mensual</option>
            </select>
          </label>
        )}
        <label>Medio de pago<input {...form.register('paymentMethod')} /></label>
        <label>Notas<textarea {...form.register('notes')} /></label>
      </div>
      <div className="row" style={{ justifyContent: editing ? 'flex-end' : 'flex-start' }}>
        {editing && (
          <button type="button" className="btn secondary" onClick={cancelEditing}>Cancelar</button>
        )}
        <button className="btn" disabled={saveMutation.isPending}>
          {saveMutation.isPending ? 'Guardando...' : editing ? 'Actualizar' : 'Guardar'}
        </button>
      </div>
    </form>
  )

  return (
    <div className="stack">
      <div className="topbar">
        <div>
          <h1 style={{ margin: 0 }}>Gastos y cuentas</h1>
          <p className="muted">Pendientes filtrados: {formatMoney(pendingTotal, currency)}</p>
        </div>
      </div>

      {message && <div className="alert success">{message}</div>}
      {error && <div className="alert error">{error}</div>}

      {!editing && (
        <article className="card stack">
          <h3 style={{ margin: 0, color: 'var(--text)' }}>Nueva cuenta / gasto</h3>
          {expenseForm}
        </article>
      )}

      <article className="card stack">
        <div className="row">
          <input placeholder="Buscar..." value={search} onChange={(e) => setSearch(e.target.value)} />
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="">Todas las categorías</option>
            {EXPENSE_CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Todos los estados</option>
            <option value="PENDING">Pendiente</option>
            <option value="PAID">Pagada</option>
            <option value="OVERDUE">Vencida</option>
          </select>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Cuenta</th>
                <th>Vence</th>
                <th>Categoría</th>
                <th>Estado</th>
                <th>Monto</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {(query.data ?? []).map((item) => (
                <tr key={item.id}>
                  <td>{item.name}</td>
                  <td>{item.dueDate}</td>
                  <td>{item.category}</td>
                  <td><span className={`badge ${item.status.toLowerCase()}`}>{statusLabel(item.status)}</span></td>
                  <td>{formatMoney(item.amount, currency)}</td>
                  <td className="actions-cell">
                    <div className="table-actions">
                      {item.status !== 'PAID' && (
                        <button
                          className="btn"
                          onClick={() => {
                            setPaying(item)
                            setPaymentDate(new Date().toISOString().slice(0, 10))
                            setReceipt(null)
                          }}
                        >
                          Pagar
                        </button>
                      )}
                      {item.receiptPath && (
                        <button
                          className="btn secondary"
                          title="Ver comprobante"
                          onClick={() => void openReceipt(item)}
                        >
                          Comprobante
                        </button>
                      )}
                      <button className="btn secondary" onClick={() => startEditing(item)}>
                        Editar
                      </button>
                      <button className="btn danger" onClick={() => deleteMutation.mutate(item.id)}>
                        Eliminar
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </article>

      {preview && (
        <div className="modal-backdrop" role="presentation" onMouseDown={() => setPreview(null)}>
          <section
            className="modal-card stack modal-wide"
            role="dialog"
            aria-modal="true"
            aria-labelledby="receipt-dialog-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <h2 id="receipt-dialog-title" style={{ margin: 0 }}>Comprobante</h2>
              <button className="btn secondary" onClick={() => setPreview(null)}>Cerrar</button>
            </div>
            {preview.type === 'application/pdf' ? (
              <iframe className="receipt-frame" src={preview.url} title="Comprobante en PDF" />
            ) : (
              <img className="receipt-image" src={preview.url} alt="Comprobante de pago" />
            )}
            <a className="btn" href={preview.url} download={preview.name}>
              Descargar
            </a>
          </section>
        </div>
      )}

      {editing && (
        <div className="modal-backdrop" role="presentation" onMouseDown={cancelEditing}>
          <section
            className="modal-card stack"
            role="dialog"
            aria-modal="true"
            aria-labelledby="edit-dialog-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div>
              <h2 id="edit-dialog-title" style={{ margin: 0 }}>Editar cuenta</h2>
              <p className="muted">{editing.name}</p>
            </div>
            {expenseForm}
          </section>
        </div>
      )}

      {paying && (
        <div className="modal-backdrop" role="presentation" onMouseDown={() => setPaying(null)}>
          <section
            className="modal-card stack"
            role="dialog"
            aria-modal="true"
            aria-labelledby="pay-dialog-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div>
              <h2 id="pay-dialog-title" style={{ margin: 0 }}>Registrar pago</h2>
              <p className="muted">
                {paying.name} · {formatMoney(paying.amount, currency)}
              </p>
            </div>
            <label>
              Fecha de pago
              <input
                type="date"
                value={paymentDate}
                onChange={(event) => setPaymentDate(event.target.value)}
              />
            </label>
            <label>
              Comprobante o screenshot (opcional)
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp,application/pdf"
                onChange={(event) => {
                  const file = event.target.files?.[0] ?? null
                  if (file && file.size > 10 * 1024 * 1024) {
                    setError('El comprobante no puede superar los 10 MB')
                    event.target.value = ''
                    setReceipt(null)
                    return
                  }
                  setError(null)
                  setReceipt(file)
                }}
              />
              <span className="muted">JPG, PNG, WEBP o PDF. Máximo 10 MB.</span>
            </label>
            {receipt && <div className="alert info">Archivo seleccionado: {receipt.name}</div>}
            <div className="row" style={{ justifyContent: 'flex-end' }}>
              <button className="btn secondary" onClick={() => setPaying(null)}>Cancelar</button>
              <button
                className="btn"
                disabled={payMutation.isPending || !paymentDate}
                onClick={() => payMutation.mutate({
                  id: paying.id,
                  paidAt: paymentDate,
                  file: receipt,
                })}
              >
                {payMutation.isPending ? 'Guardando...' : 'Confirmar pago'}
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}
