import { useCallback, useEffect, useRef, useState } from 'react'

const DEFAULT_MS = 4000

/** Mensaje temporal de éxito (por defecto 4 segundos). */
export function useTemporaryMessage(durationMs = DEFAULT_MS) {
  const [message, setMessage] = useState<string | null>(null)
  const timer = useRef<number | null>(null)

  useEffect(() => {
    return () => {
      if (timer.current) window.clearTimeout(timer.current)
    }
  }, [])

  const showMessage = useCallback((text: string) => {
    if (timer.current) window.clearTimeout(timer.current)
    setMessage(text)
    timer.current = window.setTimeout(() => setMessage(null), durationMs)
  }, [durationMs])

  const clearMessage = useCallback(() => {
    if (timer.current) window.clearTimeout(timer.current)
    setMessage(null)
  }, [])

  return { message, showMessage, clearMessage }
}
