import { useQuery } from '@tanstack/react-query'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { useAuth } from '../auth'
import { api } from '../lib/api'
import { formatMoney, statusLabel } from '../lib/format'
import type { DashboardSummary } from '../types'

const COLORS = ['#1d6a9f', '#2f9e6a', '#d4a017', '#d64545', '#7c3aed', '#0ea5e9']

export function DashboardPage() {
  const { token, spaceId, spaces } = useAuth()
  const currency = spaces.find((s) => s.id === spaceId)?.currencyCode ?? 'CLP'

  const { data, isLoading, error } = useQuery({
    queryKey: ['dashboard', spaceId],
    enabled: !!token && !!spaceId,
    queryFn: () => api<DashboardSummary>(`/api/spaces/${spaceId}/dashboard`, {}, token),
  })

  if (!spaceId) return <div className="alert info">Selecciona un espacio financiero</div>
  if (isLoading) return <div className="card">Cargando resumen...</div>
  if (error || !data) return <div className="alert error">No se pudo cargar el dashboard</div>

  return (
    <div className="stack">
      <div className="topbar">
        <div>
          <h1 style={{ margin: 0 }}>Resumen financiero</h1>
          <p className="muted">Dinero disponible = saldo actual − cuentas pendientes</p>
        </div>
      </div>

      <div className="card-grid">
        <article className="card blue"><h3>Saldo actual</h3><div className="value">{formatMoney(data.currentBalance, currency)}</div></article>
        <article className="card green"><h3>Ingresos del mes</h3><div className="value">{formatMoney(data.monthlyIncomes, currency)}</div></article>
        <article className="card green"><h3>Gastos pagados</h3><div className="value">{formatMoney(data.monthlyPaidExpenses, currency)}</div></article>
        <article className="card red"><h3>Cuentas pendientes</h3><div className="value">{formatMoney(data.pendingObligations, currency)}</div></article>
        <article className="card yellow"><h3>Dinero disponible</h3><div className="value">{formatMoney(data.availableMoney, currency)}</div></article>
        <article className="card blue"><h3>% ingresos usado</h3><div className="value">{data.incomeUsagePercentage}%</div></article>
      </div>

      <div className="card-grid" style={{ gridTemplateColumns: '2fr 1fr' }}>
        <article className="card">
          <h3 style={{ color: 'var(--text)', fontSize: '1.05rem' }}>Ingresos vs gastos (6 meses)</h3>
          <div style={{ width: '100%', height: 280 }}>
            <ResponsiveContainer>
              <BarChart data={data.monthlyComparison}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" />
                <YAxis />
                <Tooltip formatter={(v) => formatMoney(Number(v), currency)} />
                <Bar dataKey="incomes" fill="#2f9e6a" name="Ingresos" radius={6} />
                <Bar dataKey="expenses" fill="#d64545" name="Gastos" radius={6} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </article>
        <article className="card">
          <h3 style={{ color: 'var(--text)', fontSize: '1.05rem' }}>Gastos por categoría</h3>
          <div style={{ width: '100%', height: 280 }}>
            <ResponsiveContainer>
              <PieChart>
                <Pie data={data.expensesByCategory} dataKey="amount" nameKey="category" outerRadius={90}>
                  {data.expensesByCategory.map((_, index) => (
                    <Cell key={index} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(v) => formatMoney(Number(v), currency)} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </article>
      </div>

      <article className="card">
        <h3 style={{ color: 'var(--text)', fontSize: '1.05rem' }}>Próximos vencimientos</h3>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Cuenta</th>
                <th>Categoría</th>
                <th>Vence</th>
                <th>Estado</th>
                <th>Monto</th>
              </tr>
            </thead>
            <tbody>
              {data.upcoming.map((item) => (
                <tr key={item.id}>
                  <td>{item.name}</td>
                  <td>{item.category}</td>
                  <td>{item.dueDate}</td>
                  <td><span className={`badge ${item.status.toLowerCase()}`}>{statusLabel(item.status)}</span></td>
                  <td>{formatMoney(item.amount, currency)}</td>
                </tr>
              ))}
              {data.upcoming.length === 0 && (
                <tr><td colSpan={5} className="muted">No hay vencimientos próximos</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </article>
    </div>
  )
}
