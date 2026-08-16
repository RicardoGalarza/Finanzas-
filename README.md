# FlujoClaro

Aplicación SaaS de finanzas personales y familiares.

## Arquitectura

- **Backend:** Quarkus 3 + Java 21, arquitectura hexagonal
- **Frontend:** React + TypeScript + Vite (PWA)
- **Base de datos:** PostgreSQL 16 (Docker Compose)
- **Autenticación:** JWT (SmallRye) + BCrypt

```
frontend/   → React SPA
backend/    → API hexagonal (domain / application / adapters)
docker-compose.yml → PostgreSQL
```

### Capas del backend

| Capa | Responsabilidad |
|------|-----------------|
| `domain` | Entidades, reglas y puertos |
| `application` | Casos de uso |
| `adapters.in.rest` | Controladores JAX-RS |
| `adapters.out.*` | JPA, JWT, correo, etc. |

## Requisitos

- Java 21+ (si usas Java 25, el proyecto incluye `.mvn/jvm.config` con `net.bytebuddy.experimental=true`)
- Maven 3.9+
- Node.js 20+
- Docker Desktop (para PostgreSQL)

## Puesta en marcha

### 1. Base de datos

```bash
docker compose up -d
```

Credenciales por defecto:

- DB: `flujoclaro`
- User: `flujoclaro`
- Password: `flujoclaro`
- Puerto: `5432`

### 2. Backend

```bash
cd backend
mvn quarkus:dev
```

API en `http://localhost:8080`

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

App en `http://localhost:5173`

## Correo electrónico (recuperación de contraseña)

Por defecto el modo es `log` (el token aparece en la consola del backend).
Para **enviar correos reales por SMTP** (Gmail u otro):

1. Copia `backend/mail.env.example` a `backend/mail.env` y completa tus datos.
2. En Gmail crea una [contraseña de aplicaciones](https://myaccount.google.com/apppasswords).
3. Carga las variables y reinicia el backend:

```powershell
cd backend
Get-Content .\mail.env | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
  $k,$v = $_.Split('=',2); Set-Item -Path "Env:$k" -Value $v
}
mvn quarkus:dev
```

Variables clave: `MAIL_MODE=smtp`, `MAIL_MOCK=false`, `MAIL_USERNAME`, `MAIL_PASSWORD`.
El correo incluye un enlace a `/recuperar?token=...` y el token como respaldo.

## Cuenta demo

Solo se crea automáticamente al arrancar el backend (no en registros normales):

- Correo: `demo@flujoclaro.cl`
- Contraseña: `Demo1234!`

## MVP incluido

- Registro / login / recuperación de contraseña
- Onboarding guiado
- Dashboard con dinero disponible (`saldo - pendientes`)
- CRUD de ingresos
- CRUD de gastos/cuentas + marcar como pagada
- Estado vencido automático
- Calendario mensual de movimientos
- Aislamiento por espacio financiero y JWT
- Diseño responsive (sidebar + navegación inferior)
- Modo claro/oscuro
- Base PWA instalable

## Scripts útiles

```bash
# Tests de reglas de dominio
cd backend && mvn test

# Build frontend
cd frontend && npm run build
```

## Variables de entorno (backend)

| Variable | Default |
|----------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/flujoclaro` |
| `DB_USER` | `flujoclaro` |
| `DB_PASSWORD` | `flujoclaro` |
| `HTTP_PORT` | `8080` |
| `CORS_ORIGINS` | `http://localhost:5173` |
| `MAIL_MODE` | `log` (`smtp` para envío real) |
| `MAIL_MOCK` | `true` (`false` para SMTP real) |
| `MAIL_HOST` | `smtp.gmail.com` |
| `MAIL_PORT` | `587` |
| `MAIL_USERNAME` | _(vacío)_ |
| `MAIL_PASSWORD` | _(vacío)_ |
| `MAIL_FROM` | `FlujoClaro <noreply@flujoclaro.cl>` |
| `FRONTEND_URL` | `http://localhost:5173` |

## Próximos pasos (fuera del MVP)

- Presupuestos, alertas push y reportes PDF/Excel
- Invitaciones a integrantes y roles avanzados
- Integración de pagos de suscripción
- Almacenamiento cloud de comprobantes
