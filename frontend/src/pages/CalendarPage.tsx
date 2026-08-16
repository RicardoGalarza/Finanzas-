import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  addMonths,
  eachDayOfInterval,
  endOfMonth,
  endOfWeek,
  format,
  isSameMonth,
  startOfMonth,
  startOfWeek,
} from 'date-fns'
import { es } from 'date-fns/locale'
import { useAuth } from '../auth'
import { api } from '../lib/api'
import { formatMoney, statusLabel } from '../lib/format'
import type { CalendarEvent } from '../types'

export function CalendarPage() {
  const { token, spaceId, spaces } = useAuth()
  const currency = spaces.find((s) => s.id === spaceId)?.currencyCode ?? 'CLP'
  const [cursor, setCursor] = useState(new Date())
  const [selected, setSelected] = useState(format(new Date(), 'yyyy-MM-dd'))

  const year = cursor.getFullYear()
  const month = cursor.getMonth() + 1

  const { data = [] } = useQuery({
    queryKey: ['calendar', spaceId, year, month],
    enabled: !!token && !!spaceId,
    queryFn: () => api<CalendarEvent[]>(`/api/spaces/${spaceId}/calendar?year=${year}&month=${month}`, {}, token),
  })

  const byDate = useMemo(() => {
    const map = new Map<string, CalendarEvent[]>()
    for (const event of data) {
      const list = map.get(event.date) ?? []
      list.push(event)
      map.set(event.date, list)
    }
    return map
  }, [data])

  const days = eachDayOfInterval({
    start: startOfWeek(startOfMonth(cursor), { weekStartsOn: 1 }),
    end: endOfWeek(endOfMonth(cursor), { weekStartsOn: 1 }),
  })

  const selectedEvents = byDate.get(selected) ?? []

  return (
    <div className="stack">
      <div className="topbar">
        <div>
          <h1 style={{ margin: 0 }}>Calendario financiero</h1>
          <p className="muted">{format(cursor, 'MMMM yyyy', { locale: es })}</p>
        </div>
        <div className="row">
          <button className="btn secondary" onClick={() => setCursor(addMonths(cursor, -1))}>Anterior</button>
          <button className="btn secondary" onClick={() => setCursor(new Date())}>Hoy</button>
          <button className="btn secondary" onClick={() => setCursor(addMonths(cursor, 1))}>Siguiente</button>
        </div>
      </div>

      <div className="calendar-grid" style={{ marginBottom: '0.5rem' }}>
        {['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'].map((d) => (
          <div key={d} className="muted" style={{ textAlign: 'center', fontWeight: 700 }}>{d}</div>
        ))}
      </div>

      <div className="calendar-grid">
        {days.map((day) => {
          const key = format(day, 'yyyy-MM-dd')
          const events = byDate.get(key) ?? []
          return (
            <button
              key={key}
              type="button"
              className={`calendar-cell${selected === key ? ' selected' : ''}`}
              style={{ opacity: isSameMonth(day, cursor) ? 1 : 0.45, textAlign: 'left' }}
              onClick={() => setSelected(key)}
            >
              <strong>{format(day, 'd')}</strong>
              <div style={{ marginTop: '0.35rem' }}>
                {events.slice(0, 3).map((event) => (
                  <span
                    key={event.id}
                    className={`dot ${event.type === 'INCOME' ? 'income' : event.status.toLowerCase()}`}
                    title={event.title}
                  />
                ))}
              </div>
            </button>
          )
        })}
      </div>

      <article className="card">
        <h3 style={{ color: 'var(--text)' }}>Movimientos del {selected}</h3>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Tipo</th>
                <th>Título</th>
                <th>Categoría</th>
                <th>Estado</th>
                <th>Monto</th>
              </tr>
            </thead>
            <tbody>
              {selectedEvents.map((event) => (
                <tr key={`${event.type}-${event.id}`}>
                  <td>{event.type === 'INCOME' ? 'Ingreso' : 'Gasto'}</td>
                  <td>{event.title}</td>
                  <td>{event.category}</td>
                  <td><span className={`badge ${event.status.toLowerCase()}`}>{statusLabel(event.status)}</span></td>
                  <td>{formatMoney(event.amount, currency)}</td>
                </tr>
              ))}
              {selectedEvents.length === 0 && (
                <tr><td colSpan={5} className="muted">Sin movimientos este día</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </article>
    </div>
  )
}
