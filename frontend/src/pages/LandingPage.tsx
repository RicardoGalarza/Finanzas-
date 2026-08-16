import { Link } from 'react-router-dom'

export function LandingPage() {
  return (
    <div className="landing">
      <div className="landing-hero">
        <section className="hero-panel">
          <p style={{ opacity: 0.85, marginTop: 0 }}>Finanzas personales y familiares</p>
          <h1 style={{ fontSize: 'clamp(2rem, 4vw, 3.2rem)', margin: '0 0 1rem' }}>
            FlujoClaro: sabe cuánto puedes gastar realmente
          </h1>
          <p style={{ maxWidth: 640, lineHeight: 1.6, opacity: 0.95 }}>
            Controla ingresos, cuentas por pagar, vencimientos y el dinero disponible después de tus obligaciones.
            Pensado para personas, parejas y familias.
          </p>
          <div className="row" style={{ marginTop: '1.5rem' }}>
            <Link className="btn" to="/registro" style={{ background: 'white', color: '#0b3a5b' }}>
              Crear cuenta gratis
            </Link>
            <Link className="btn secondary" to="/login" style={{ color: 'white', borderColor: 'rgba(255,255,255,0.4)' }}>
              Iniciar sesión
            </Link>
          </div>
        </section>

        <section className="features">
          {[
            ['Dashboard claro', 'Saldo, pendientes y dinero disponible en un vistazo.'],
            ['Cuentas al día', 'Nunca olvides un vencimiento importante.'],
            ['Espacios compartidos', 'Administra finanzas en pareja o familia.'],
            ['Listo para crecer', 'Arquitectura preparada para planes Premium y Familiar.'],
          ].map(([title, text]) => (
            <article key={title} className="card">
              <h3 style={{ color: 'var(--text)', fontSize: '1.05rem' }}>{title}</h3>
              <p className="muted">{text}</p>
            </article>
          ))}
        </section>

        <section className="card">
          <h2>Planes</h2>
          <div className="card-grid">
            <div>
              <h3>Gratis</h3>
              <p className="muted">Dashboard básico e ingresos/gastos esenciales.</p>
            </div>
            <div>
              <h3>Premium</h3>
              <p className="muted">Presupuestos, recordatorios, reportes y exportación.</p>
            </div>
            <div>
              <h3>Familiar</h3>
              <p className="muted">Roles, metas compartidas e historial familiar.</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
