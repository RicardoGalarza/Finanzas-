export function formatMoney(value: number | string | null | undefined, currency = 'CLP') {
  const amount = Number(value ?? 0)
  return new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency,
    maximumFractionDigits: currency === 'CLP' ? 0 : 2,
  }).format(amount)
}

export function statusLabel(status: string) {
  switch (status) {
    case 'PAID':
      return 'Pagada'
    case 'OVERDUE':
      return 'Vencida'
    case 'PENDING':
      return 'Pendiente'
    case 'RECEIVED':
      return 'Recibido'
    default:
      return status
  }
}
