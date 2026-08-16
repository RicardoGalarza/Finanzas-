import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useAuth } from '../auth'
import { api, apiBlob, ApiError, apiForm } from '../lib/api'
import { formatMoney, statusLabel } from '../lib/format'
import { useTemporaryMessage } from '../lib/useTemporaryMessage'
import { EXPENSE_CATEGORIES, PAYMENT_METHODS, type Expense } from '../types'

const schema = z.object({
  name: z.string().min(2),
  amount: z.coerce.number().positive('El monto debe ser mayor a 0'),
  dueDate: z.string().min(1),
  category: z.string().min(1),
  responsiblePerson: z.string().min(1),
  expenseType: z.enum(['ONE_TIME', 'RECURRING']),
  frequency: z.enum(['WEEKLY', 'BIWEEKLY', 'MONTHLY']).optional().nullable(),
  recurrenceEndDate: z.string().optional().nullable(),
  paymentMethod: z.string().optional(),
  notes: z.string().optional(),
}).superRefine((values, context) => {
  if (values.expenseType !== 'RECURRING') return
  if (!values.frequency) {
    context.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['frequency'],
      message: 'Selecciona la frecuencia',
    })
  }
  if (!values.recurrenceEndDate) {
    context.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['recurrenceEndDate'],
      message: 'Ingresa la fecha de la última cuota',
    })
  } else if (values.recurrenceEndDate < values.dueDate) {
    context.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['recurrenceEndDate'],
      message: 'La última cuota no puede ser anterior al primer vencimiento',
    })
  }
})

type FormData = z.infer<typeof schema>

export function ExpensesPage() {
  const { token, spaceId, spaces, user } = useAuth()
  const currency = spaces.find((s) => s.id === spaceId)?.currencyCode ?? 'CLP'
  const qc = useQueryClient()
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [status, setStatus] = useState('')
  const [showFuture, setShowFuture] = useState(false)
  const [editing, setEditing] = useState<Expense | null>(null)
  const [paying, setPaying] = useState<Expense | null>(null)
  const [paymentDate, setPaymentDate] = useState(new Date().toISOString().slice(0, 10))
  const [payMethod, setPayMethod] = useState('')
  const [receipt, setReceipt] = useState<File | null>(null)
  const [replacementReceipt, setReplacementReceipt] = useState<File | null>(null)
  const [preview, setPreview] = useState<{
    expenseId: string
    url: string
    type: string
    name: string
  } | null>(null)
  const { message, showMessage } = useTemporaryMessage()
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
      recurrenceEndDate: null,
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
      recurrenceEndDate: null,
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
      recurrenceEndDate: item.recurrenceEndDate ?? null,
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
        recurrenceEndDate: values.expenseType === 'RECURRING' ? values.recurrenceEndDate : null,
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
      showMessage(editing ? 'Cuenta actualizada' : 'Cuenta creada')
      setError(null)
      setEditing(null)
      resetForm()
      await invalidate()
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : 'Error al guardar'),
  })

  const payMutation = useMutation({
    mutationFn: ({ id, paidAt, paymentMethod, file }: {
      id: string
      paidAt: string
      paymentMethod: string
      file: File | null
    }) => {
      const formData = new FormData()
      formData.append('paidAt', paidAt)
      if (paymentMethod) formData.append('paymentMethod', paymentMethod)
      if (file) formData.append('receipt', file)
      return apiForm<Expense>(`/api/spaces/${spaceId}/expenses/${id}/pay`, formData, token)
    },
    onSuccess: async () => {
      const recurringMessage = paying?.expenseType === 'RECURRING'
        ? ' La próxima cuota se generará solo si aún está dentro de la fecha final.'
        : ''
      showMessage(
        (receipt ? 'Cuenta pagada y comprobante guardado.' : 'Cuenta marcada como pagada.')
        + recurringMessage,
      )
      setError(null)
      setPaying(null)
      setPayMethod('')
      setReceipt(null)
      await invalidate()
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : 'No se pudo registrar el pago'),
  })

  const openReceipt = async (expense: Expense) => {
    try {
      setError(null)
      setReplacementReceipt(null)
      const blob = await apiBlob(
        `/api/spaces/${spaceId}/expenses/${expense.id}/receipt`,
        token,
      )
      const extension = blob.type === 'application/pdf' ? 'pdf' : blob.type.split('/')[1] ?? 'png'
      setPreview({
        expenseId: expense.id,
        url: URL.createObjectURL(blob),
        type: blob.type,
        name: `comprobante-${expense.name}.${extension}`,
      })
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo abrir el comprobante')
    }
  }

  const replaceReceiptMutation = useMutation({
    mutationFn: ({ id, file }: { id: string; file: File }) => {
      const formData = new FormData()
      formData.append('receipt', file)
      return apiForm<Expense>(`/api/spaces/${spaceId}/expenses/${id}/receipt`, formData, token)
    },
    onSuccess: async (updated) => {
      showMessage('Comprobante actualizado correctamente')
      setError(null)
      setReplacementReceipt(null)
      await invalidate()
      if (preview && updated.receiptPath) {
        await openReceipt(updated)
      }
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : 'No se pudo actualizar el comprobante'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api(`/api/spaces/${spaceId}/expenses/${id}`, { method: 'DELETE' }, token),
    onSuccess: async () => {
      showMessage('Cuenta eliminada')
      setError(null)
      await invalidate()
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : 'No se pudo eliminar la cuenta'),
  })

  const today = new Date().toISOString().slice(0, 10)
  const currentMonth = today.slice(0, 7)

  const allExpenses = query.data ?? []
  const futurePending = useMemo(
    () => allExpenses.filter(
      (e) => e.dueDate.slice(0, 7) > currentMonth && e.status !== 'PAID',
    ),
    [allExpenses, currentMonth],
  )
  const futureCount = futurePending.length
  const visibleExpenses = useMemo(
    () => (showFuture
      ? futurePending
      : allExpenses.filter((e) => e.dueDate.slice(0, 7) <= currentMonth)),
    [allExpenses, showFuture, currentMonth, futurePending],
  )

  const pendingTotal = useMemo(
    () => visibleExpenses
      .filter((e) => e.status !== 'PAID')
      .reduce((acc, item) => acc + Number(item.amount), 0),
    [visibleExpenses],
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
          <>
            <label>Frecuencia
              <select {...form.register('frequency')}>
                <option value="">Seleccionar...</option>
                <option value="WEEKLY">Semanal</option>
                <option value="BIWEEKLY">Quincenal</option>
                <option value="MONTHLY">Mensual</option>
              </select>
              {form.formState.errors.frequency && (
                <small className="error-text">{form.formState.errors.frequency.message}</small>
              )}
            </label>
            <label>Fecha de la última cuota
              <input
                type="date"
                min={form.watch('dueDate')}
                {...form.register('recurrenceEndDate')}
              />
              {form.formState.errors.recurrenceEndDate && (
                <small className="error-text">{form.formState.errors.recurrenceEndDate.message}</small>
              )}
            </label>
          </>
        )}
        <label>Medio de pago
          <select {...form.register('paymentMethod')}>
            <option value="">Seleccionar...</option>
            {PAYMENT_METHODS.map((method) => (
              <option key={method} value={method}>{method}</option>
            ))}
          </select>
        </label>
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
        <div className="filter-bar">
          <input
            className="filter-search"
            placeholder="Buscar..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
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
          <label className={`filter-toggle${showFuture ? ' is-active' : ''}`}>
            <input
              type="checkbox"
              checked={showFuture}
              onChange={(event) => setShowFuture(event.target.checked)}
            />
            <span className="filter-toggle__switch" aria-hidden="true" />
            <span>Ver meses siguientes</span>
            {futureCount > 0 && <span className="filter-toggle__count">{futureCount}</span>}
          </label>
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
              {visibleExpenses.map((item) => (
                <tr key={item.id}>
                  <td>
                    <div className="cell-title">
                      <span>{item.name}</span>
                      {item.expenseType === 'RECURRING' && item.recurrenceEndDate && (
                        <small className="muted">Última cuota: {item.recurrenceEndDate}</small>
                      )}
                    </div>
                  </td>
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
                            setPayMethod(item.paymentMethod ?? '')
                            setPaymentDate(new Date().toISOString().slice(0, 10))
                            setReceipt(null)
                          }}
                        >
                          Pagar
                        </button>
                      )}
                      {item.receiptPath ? (
                        <button
                          className="btn secondary"
                          title="Ver comprobante"
                          onClick={() => void openReceipt(item)}
                        >
                          Comprobante
                        </button>
                      ) : item.status === 'PAID' ? (
                        <button
                          className="btn secondary"
                          title="Subir comprobante"
                          onClick={() => {
                            setPreview({
                              expenseId: item.id,
                              url: '',
                              type: '',
                              name: item.name,
                            })
                            setReplacementReceipt(null)
                          }}
                        >
                          Subir comprobante
                        </button>
                      ) : null}
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
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={() => {
            setPreview(null)
            setReplacementReceipt(null)
          }}
        >
          <section
            className="modal-card stack modal-wide"
            role="dialog"
            aria-modal="true"
            aria-labelledby="receipt-dialog-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <h2 id="receipt-dialog-title" style={{ margin: 0 }}>Comprobante</h2>
              <button
                className="btn secondary"
                onClick={() => {
                  setPreview(null)
                  setReplacementReceipt(null)
                }}
              >
                Cerrar
              </button>
            </div>
            {preview.url ? (
              preview.type === 'application/pdf' ? (
                <iframe className="receipt-frame" src={preview.url} title="Comprobante en PDF" />
              ) : (
                <img className="receipt-image" src={preview.url} alt="Comprobante de pago" />
              )
            ) : (
              <div className="alert info">Esta cuenta aún no tiene comprobante. Sube uno abajo.</div>
            )}
            <div className="file-picker-block">
              <strong style={{ fontSize: '0.9rem', color: 'var(--text)' }}>
                {preview.url ? 'Cambiar comprobante' : 'Subir comprobante'}
              </strong>
              <div className="file-picker">
                <label className="btn secondary file-picker__trigger">
                  Seleccionar archivo
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp,application/pdf"
                    onChange={(event) => {
                      const file = event.target.files?.[0] ?? null
                      if (file && file.size > 10 * 1024 * 1024) {
                        setError('El comprobante no puede superar los 10 MB')
                        event.target.value = ''
                        setReplacementReceipt(null)
                        return
                      }
                      setError(null)
                      setReplacementReceipt(file)
                    }}
                  />
                </label>
                <span className="file-picker__name">
                  {replacementReceipt?.name ?? 'Ningún archivo seleccionado'}
                </span>
              </div>
            </div>
            <div className="row" style={{ justifyContent: 'flex-end', flexWrap: 'wrap', gap: '0.5rem' }}>
              {preview.url && (
                <a className="btn secondary" href={preview.url} download={preview.name}>
                  Descargar
                </a>
              )}
              <button
                className="btn"
                disabled={replaceReceiptMutation.isPending || !replacementReceipt}
                onClick={() => {
                  if (replacementReceipt) {
                    replaceReceiptMutation.mutate({
                      id: preview.expenseId,
                      file: replacementReceipt,
                    })
                  }
                }}
              >
                {replaceReceiptMutation.isPending ? 'Guardando...' : 'Guardar comprobante'}
              </button>
            </div>
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
              {paying.dueDate > today && (
                <p className="alert error">
                  Esta cuota vence el {paying.dueDate} y todavía no corresponde pagarla.
                  Si la pagas ahora se generará la cuota del período siguiente.
                </p>
              )}
              {paying.expenseType === 'RECURRING' && (
                <p className="alert info">
                  Al confirmar, se creará automáticamente la próxima cuenta según su frecuencia.
                </p>
              )}
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
              Medio de pago / banco
              <select
                value={payMethod}
                onChange={(event) => setPayMethod(event.target.value)}
              >
                <option value="">Seleccionar...</option>
                {PAYMENT_METHODS.map((method) => (
                  <option key={method} value={method}>{method}</option>
                ))}
              </select>
            </label>
            <div className="file-picker-block">
              <strong style={{ fontSize: '0.9rem', color: 'var(--text)' }}>
                Comprobante o screenshot (opcional)
              </strong>
              <div className="file-picker">
                <label className="btn secondary file-picker__trigger">
                  Seleccionar archivo
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
                </label>
                <span className="file-picker__name">
                  {receipt?.name ?? 'Ningún archivo seleccionado'}
                </span>
              </div>
            </div>
            <div className="row" style={{ justifyContent: 'flex-end' }}>
              <button className="btn secondary" onClick={() => {
                setPaying(null)
                setPayMethod('')
              }}>Cancelar</button>
              <button
                className="btn"
                disabled={payMutation.isPending || !paymentDate}
                onClick={() => payMutation.mutate({
                  id: paying.id,
                  paidAt: paymentDate,
                  paymentMethod: payMethod || paying.paymentMethod || '',
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
