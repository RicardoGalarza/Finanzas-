import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Send, User, X } from 'lucide-react'
import { useAuth } from '../auth'
import { api, ApiError } from '../lib/api'

const ROBOT_AVATAR = '/asistente-robot.png'

type ChatResponse = {
  reply: string
  intent: string
  suggestions: string[]
}

type ChatMessage = {
  id: number
  role: 'assistant' | 'user'
  text: string
}

const QUICK_QUESTIONS = [
  'Revisa mis finanzas',
  '¿Cuánto tengo disponible?',
  '¿Qué cuentas tengo pendientes?',
]

const WELCOME_MESSAGE =
  '¡Hola! Soy tu asistente de FlujoClaro. Pídeme algo como "revisa mis finanzas" y te cuento cómo vas.'

export function FinanceChatWidget() {
  const { token, spaceId } = useAuth()
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [suggestions, setSuggestions] = useState(QUICK_QUESTIONS)
  const [messages, setMessages] = useState<ChatMessage[]>([
    { id: 1, role: 'assistant', text: WELCOME_MESSAGE },
  ])
  const nextId = useRef(2)
  const messagesEnd = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    messagesEnd.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, sending])

  useEffect(() => {
    setMessages([{ id: 1, role: 'assistant', text: WELCOME_MESSAGE }])
    setSuggestions(QUICK_QUESTIONS)
    nextId.current = 2
  }, [spaceId])

  useEffect(() => {
    if (!open) return
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [open])

  const ask = async (question: string) => {
    const cleanQuestion = question.trim()
    if (!cleanQuestion || sending || !spaceId) return

    setMessages((current) => [
      ...current,
      { id: nextId.current++, role: 'user', text: cleanQuestion },
    ])
    setInput('')
    setSending(true)

    try {
      const response = await api<ChatResponse>(
        `/api/spaces/${spaceId}/assistant`,
        {
          method: 'POST',
          body: JSON.stringify({ message: cleanQuestion }),
        },
        token,
      )
      setMessages((current) => [
        ...current,
        { id: nextId.current++, role: 'assistant', text: response.reply },
      ])
      setSuggestions(response.suggestions ?? QUICK_QUESTIONS)
    } catch (error) {
      const text = error instanceof ApiError
        ? error.message
        : 'No pude consultar tus datos en este momento.'
      setMessages((current) => [
        ...current,
        { id: nextId.current++, role: 'assistant', text },
      ])
    } finally {
      setSending(false)
    }
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    void ask(input)
  }

  return (
    <div className={`finance-chat${open ? ' is-open' : ''}`}>
      {open && (
        <section
          className="finance-chat__panel"
          role="dialog"
          aria-modal="false"
          aria-label="Asistente financiero"
        >
          <header className="finance-chat__header">
            <div className="finance-chat__identity">
              <span className="finance-chat__avatar">
                <img src={ROBOT_AVATAR} alt="Asistente robot de FlujoClaro" />
              </span>
              <div>
                <strong>Asistente financiero</strong>
                <small>Datos de tu espacio actual</small>
              </div>
            </div>
            <button
              type="button"
              className="finance-chat__icon-button"
              onClick={() => setOpen(false)}
              aria-label="Cerrar asistente"
            >
              <X size={19} />
            </button>
          </header>

          <div className="finance-chat__messages" aria-live="polite">
            {messages.map((message) => (
              <div key={message.id} className={`finance-chat__message is-${message.role}`}>
                <span className="finance-chat__message-icon">
                  {message.role === 'assistant'
                    ? <img src={ROBOT_AVATAR} alt="" />
                    : <User size={15} />}
                </span>
                <p>{message.text}</p>
              </div>
            ))}
            {sending && (
              <div className="finance-chat__message is-assistant">
                <span className="finance-chat__message-icon">
                  <img src={ROBOT_AVATAR} alt="" />
                </span>
                <div className="finance-chat__typing" aria-label="Escribiendo">
                  <span /><span /><span />
                </div>
              </div>
            )}
            <div ref={messagesEnd} />
          </div>

          <div className="finance-chat__suggestions">
            {suggestions.map((suggestion) => (
              <button
                type="button"
                key={suggestion}
                disabled={sending}
                onClick={() => void ask(suggestion)}
              >
                {suggestion}
              </button>
            ))}
          </div>

          <form className="finance-chat__form" onSubmit={submit}>
            <input
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="Pregunta por tus finanzas..."
              maxLength={500}
              aria-label="Mensaje para el asistente"
            />
            <button
              type="submit"
              disabled={sending || !input.trim()}
              aria-label="Enviar pregunta"
            >
              <Send size={18} />
            </button>
          </form>
          <small className="finance-chat__notice">
            Las respuestas se calculan con tus datos registrados.
          </small>
        </section>
      )}

      <button
        type="button"
        className="finance-chat__launcher"
        onClick={() => setOpen((current) => !current)}
        aria-expanded={open}
        aria-label={open ? 'Cerrar asistente financiero' : 'Abrir asistente financiero'}
      >
        {open
          ? <X size={23} />
          : <img className="finance-chat__launcher-avatar" src={ROBOT_AVATAR} alt="" />}
        {!open && <span>Asistente</span>}
      </button>
    </div>
  )
}
