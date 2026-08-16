# FlujoClaro backend - Quarkus

## Cómo ejecutar

Requiere PostgreSQL (ver docker-compose en la raíz del monorepo).

```bash
mvn quarkus:dev
```

## Estructura hexagonal

```
cl.flujoclaro
├── domain          # reglas y puertos
├── application     # casos de uso
├── adapters.in     # REST
└── adapters.out    # JPA, seguridad, correo
```

## Endpoints principales

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `GET  /api/auth/me`
- `POST /api/onboarding`
- `GET  /api/spaces`
- `GET/POST /api/spaces/{id}/incomes`
- `GET/POST /api/spaces/{id}/expenses`
- `POST /api/spaces/{id}/expenses/{expenseId}/pay`
- `GET  /api/spaces/{id}/dashboard`
- `GET  /api/spaces/{id}/calendar?year=&month=`
