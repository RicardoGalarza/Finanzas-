export type AuthResponse = {
  accessToken: string
  userId: string
  email: string
  fullName: string
  onboardingCompleted: boolean
  defaultSpaceId: string | null
}

export type UserProfile = {
  id: string
  email: string
  fullName: string
  country: string
  currencyCode: string
  onboardingCompleted: boolean
  hasAvatar: boolean
  reminderDays: number
}

export type Space = {
  id: string
  name: string
  type: string
  currencyCode: string
  role: string
  initialBalance: number
}

export type Income = {
  id: string
  spaceId: string
  description: string
  amount: number
  incomeDate: string
  category: string
  receivedBy: string
  incomeType: 'ONE_TIME' | 'RECURRING'
  frequency?: 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | null
  paymentMethod?: string
  notes?: string
}

export type Expense = {
  id: string
  spaceId: string
  name: string
  amount: number
  dueDate: string
  category: string
  responsiblePerson: string
  status: 'PENDING' | 'PAID' | 'OVERDUE'
  expenseType: 'ONE_TIME' | 'RECURRING'
  frequency?: 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | null
  recurrenceEndDate?: string | null
  paymentMethod?: string
  notes?: string
  paidAt?: string | null
  receiptPath?: string | null
}

export type DashboardSummary = {
  currentBalance: number
  monthlyIncomes: number
  monthlyPaidExpenses: number
  pendingObligations: number
  availableMoney: number
  incomeUsagePercentage: number
  upcoming: Array<{
    id: string
    name: string
    amount: number
    dueDate: string
    status: string
    category: string
  }>
  expensesByCategory: Array<{ category: string; amount: number }>
  monthlyComparison: Array<{ month: string; incomes: number; expenses: number }>
}

export type CalendarEvent = {
  id: string
  type: 'INCOME' | 'EXPENSE'
  title: string
  amount: number
  date: string
  status: string
  category: string
  recurring: boolean
}

export const INCOME_CATEGORIES = [
  'Sueldo',
  'Trabajo independiente',
  'Ventas',
  'Bonos',
  'Arriendos',
  'Otros ingresos',
]

export const EXPENSE_CATEGORIES = [
  'Arriendo o dividendo',
  'Agua',
  'Electricidad',
  'Gas',
  'Internet',
  'Teléfono',
  'Supermercado',
  'Transporte',
  'Combustible',
  'Auto / cuota',
  'Permiso de circulación',
  'Seguro auto',
  'Mantención auto',
  'TAG / peajes',
  'Estacionamiento',
  'Créditos',
  'Crédito hipotecario',
  'Crédito de consumo',
  'CAE / estudios',
  'Tarjetas',
  'Seguros',
  'Salud',
  'Isapre / Fonasa',
  'Medicamentos',
  'Educación',
  'Universidad / colegio',
  'Suscripciones',
  'Streaming',
  'Entretenimiento',
  'Restaurantes / delivery',
  'Ropa',
  'Hogar / mantención',
  'Mascotas',
  'Viajes',
  'Impuestos',
  'Gastos legales',
  'Ahorro / inversión',
  'Otros',
]

/** Medios de pago y bancos de Chile para ingresos/gastos */
export const PAYMENT_METHODS = [
  'Efectivo',
  'Transferencia bancaria',
  'Tarjeta de débito',
  'Tarjeta de crédito',
  'Cheque',
  'Webpay / Transbank',
  'Khipu',
  'Flow',
  'Banco de Chile',
  'Banco Estado',
  'Banco Santander',
  'Banco BCI',
  'Scotiabank',
  'Banco Itaú',
  'Banco Security',
  'Banco Falabella',
  'Banco Ripley',
  'Banco BICE',
  'Banco Consorcio',
  'Banco Internacional',
  'Banco Edwards',
  'Coopeuch',
  'Tenpo',
  'MACH',
  'Mercado Pago',
  'Prepago Los Héroes',
  'Caja Los Andes',
  'Caja 18',
  'Otro',
] as const
