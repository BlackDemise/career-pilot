import { useCallback, useRef, useState } from 'react'
import { getAccessToken } from '../../shared/api/authSession'
import type { ChatMessage } from './api'

type StreamStatus = 'idle' | 'streaming' | 'error'

type ParsedEvent = { name: string; data: string }

function parseSseBlock(rawEvent: string): ParsedEvent {
  let name = 'message'
  const dataLines: string[] = []
  for (const line of rawEvent.split('\n')) {
    if (line.startsWith('event:')) {
      name = line.slice('event:'.length).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).replace(/^ /, ''))
    }
  }
  return { name, data: dataLines.join('\n') }
}

// Consumes a backend SSE stream (chunk/done/error events) and exposes the growing assistant
// text plus the final persisted message, without pulling in an SSE/WebSocket library.
export function useMessageStream() {
  const [streamingText, setStreamingText] = useState('')
  const [status, setStatus] = useState<StreamStatus>('idle')
  const [error, setError] = useState<string | null>(null)
  const finalMessageRef = useRef<ChatMessage | null>(null)

  const start = useCallback((url: string, body?: unknown): Promise<ChatMessage> => {
    setStreamingText('')
    setError(null)
    setStatus('streaming')
    finalMessageRef.current = null

    return new Promise((resolve, reject) => {
      const token = getAccessToken()
      fetch(url, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: body === undefined ? undefined : JSON.stringify(body),
      })
        .then(async (response) => {
          if (!response.ok || !response.body) {
            throw new Error(`Request failed with status ${response.status}`)
          }

          const reader = response.body.getReader()
          const decoder = new TextDecoder()
          let buffer = ''
          let accumulated = ''

          while (true) {
            const { value, done } = await reader.read()
            if (done) break
            buffer += decoder.decode(value, { stream: true })

            let boundary = buffer.indexOf('\n\n')
            while (boundary !== -1) {
              const event = parseSseBlock(buffer.slice(0, boundary))
              buffer = buffer.slice(boundary + 2)

              if (event.name === 'chunk') {
                accumulated += event.data
                setStreamingText(accumulated)
              } else if (event.name === 'done') {
                const message = JSON.parse(event.data) as ChatMessage
                finalMessageRef.current = message
                setStatus('idle')
                resolve(message)
              } else if (event.name === 'error') {
                throw new Error(event.data || 'The AI response failed.')
              }

              boundary = buffer.indexOf('\n\n')
            }
          }

          if (!finalMessageRef.current) {
            throw new Error('The stream ended before a response was received.')
          }
        })
        .catch((reason: unknown) => {
          const message = reason instanceof Error ? reason.message : 'Could not stream the response.'
          setStatus('error')
          setError(message)
          reject(reason instanceof Error ? reason : new Error(message))
        })
    })
  }, [])

  const reset = useCallback(() => {
    setStreamingText('')
    setStatus('idle')
    setError(null)
    finalMessageRef.current = null
  }, [])

  return { streamingText, status, error, start, reset }
}
