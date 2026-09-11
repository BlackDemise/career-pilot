import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import rehypeSanitize from 'rehype-sanitize'
import { MessageSquarePlus, Trash2, SendHorizontal, RotateCcw, Pencil, X, Check } from 'lucide-react'
import { PageMessage } from '../../shared/components/PageMessage'
import {
  createConversation,
  deleteConversation,
  editMessageStreamUrl,
  listConversations,
  regenerateMessageStreamUrl,
  sendMessageStreamUrl,
  type ChatMessage,
  type Conversation,
} from './api'
import { useMessageStream } from './useMessageStream'

const emptyConversation: Conversation = {
  id: '',
  title: 'New conversation',
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  messages: [],
}

type ActiveStream =
  | { type: 'send' }
  | { type: 'regenerate'; messageId: string }
  | { type: 'edit'; messageId: string; content: string }

function formatTimestamp(value: string): string {
  return new Date(value).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })
}


export function ChatPage() {
  const navigate = useNavigate()
  const { conversationId } = useParams()
  const queryClient = useQueryClient()

  const { data: conversations = [], isLoading, isError, error } = useQuery({
    queryKey: ['conversations'],
    queryFn: listConversations,
  })

  const [draft, setDraft] = useState('')
  const [lastSentContent, setLastSentContent] = useState('')
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null)
  const [editDraft, setEditDraft] = useState('')
  const [activeStream, setActiveStream] = useState<ActiveStream | null>(null)
  const [localError, setLocalError] = useState<string | null>(null)

  const { streamingText, status: streamStatus, start, reset } = useMessageStream()

  const selectedConversation = useMemo(() => {
    return conversations.find((conversation) => conversation.id === conversationId) ?? conversations[0] ?? emptyConversation
  }, [conversationId, conversations])

  const visibleMessages = useMemo(() => {
    const messages = selectedConversation.messages
    if (!activeStream) return messages
    if (activeStream.type === 'regenerate') {
      return messages.filter((message) => message.id !== activeStream.messageId)
    }
    if (activeStream.type === 'edit') {
      const index = messages.findIndex((message) => message.id === activeStream.messageId)
      if (index === -1) return messages
      return messages.slice(0, index + 1).map((message, i) => (i === index ? { ...message, content: activeStream.content } : message))
    }
    return messages
  }, [selectedConversation.messages, activeStream])

  useEffect(() => {
    if (!conversationId && conversations[0]) {
      navigate(`/chat/${conversations[0].id}`, { replace: true })
    }
  }, [conversationId, conversations, navigate])

  const createMutation = useMutation({
    mutationFn: () => createConversation('New conversation'),
    onSuccess: (conversation) => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] })
      navigate(`/chat/${conversation.id}`)
    },
    onError: (reason) => {
      setLocalError(reason instanceof Error ? reason.message : 'Could not create a conversation.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteConversation(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['conversations'] })
      const next = queryClient.getQueryData<Conversation[]>(['conversations']) ?? []
      if (next[0]) {
        navigate(`/chat/${next[0].id}`, { replace: true })
      } else {
        navigate('/chat', { replace: true })
      }
    },
    onError: (reason) => {
      setLocalError(reason instanceof Error ? reason.message : 'Could not delete the conversation.')
    },
  })

  async function runStream(stream: ActiveStream, url: string, body?: unknown) {
    setLocalError(null)
    setActiveStream(stream)
    try {
      await start(url, body)
      await queryClient.invalidateQueries({ queryKey: ['conversations'] })
    } catch (reason) {
      setLocalError(reason instanceof Error ? reason.message : 'Could not get a response from the AI.')
    } finally {
      setActiveStream(null)
      reset()
    }
  }

  async function handleSend() {
    const message = draft.trim()
    if (!message || !selectedConversation.id || activeStream) return
    setDraft('')
    setLastSentContent(message)
    await runStream({ type: 'send' }, sendMessageStreamUrl(selectedConversation.id), { content: message })
  }

  async function handleRegenerate(messageId: string) {
    if (!selectedConversation.id || activeStream) return
    await runStream({ type: 'regenerate', messageId }, regenerateMessageStreamUrl(selectedConversation.id, messageId))
  }

  function startEdit(message: ChatMessage) {
    if (activeStream) return
    setEditingMessageId(message.id)
    setEditDraft(message.content)
  }

  function cancelEdit() {
    setEditingMessageId(null)
    setEditDraft('')
  }

  async function submitEdit(messageId: string) {
    const content = editDraft.trim()
    if (!content || !selectedConversation.id) return
    setEditingMessageId(null)
    setEditDraft('')
    await runStream({ type: 'edit', messageId, content }, editMessageStreamUrl(selectedConversation.id, messageId), { content })
  }

  async function handleCreate() {
    await createMutation.mutateAsync()
  }

  async function handleDelete() {
    if (!selectedConversation.id) return
    await deleteMutation.mutateAsync(selectedConversation.id)
  }

  return <section className="workspace"><div className="workspace-grid">
    <aside className="chat-sidebar">
      <div className="chat-sidebar-header">
        <h2>Conversations</h2>
        <button className="primary-button" type="button" onClick={() => void handleCreate()} disabled={createMutation.isPending}> <MessageSquarePlus size={16} /> New</button>
      </div>

      {isLoading ? <p className="empty-state">Loading conversations...</p> : isError ? <PageMessage tone="error">{error instanceof Error ? error.message : 'Could not load conversations.'}</PageMessage> : (
        <div className="sidebar-list">
          {conversations.length === 0 ? <p className="empty-state">No conversations yet. Start one with a clear goal.</p> : conversations.map((conversation) => (
            <button key={conversation.id} type="button" className={conversation.id === selectedConversation.id ? 'conversation-button conversation-button--active' : 'conversation-button'} onClick={() => navigate(`/chat/${conversation.id}`)}>
              <strong>{conversation.title}</strong>
              <small>{conversation.messages.at(-1)?.content ?? 'No messages yet'}</small>
            </button>
          ))}
        </div>
      )}
    </aside>

    <div className="chat-panel">
      <header className="chat-panel-header">
        <h2>{selectedConversation.title}</h2>
        <div className="chat-panel-header-actions">
          <button type="button" className="icon-button icon-button--ghost" aria-label="Delete conversation" onClick={() => void handleDelete()} disabled={!selectedConversation.id || deleteMutation.isPending}><Trash2 size={16} /></button>
        </div>
      </header>

      <div className="chat-thread">
        {visibleMessages.length === 0 && !activeStream ? <div className="empty-state">Ask about your goals, CV, or interview prep to begin.</div> : visibleMessages.map((message, index) => {
          const isEditing = editingMessageId === message.id
          const isLastAssistant = !activeStream && message.role === 'ASSISTANT' && index === visibleMessages.length - 1
          return (
            <div key={message.id} className={message.role === 'USER' ? 'message-row message-row--user' : 'message-row message-row--assistant'}>
              <div className={message.role === 'USER' ? 'message-card message-card--user' : 'message-card'}>
                <div className="message-meta"><span>{message.role === 'USER' ? 'You' : 'CareerPilot'}</span><span>{formatTimestamp(message.createdAt)}</span></div>
                {isEditing ? (
                  <div className="message-edit">
                    <textarea className="form-textarea" value={editDraft} onChange={(event) => setEditDraft(event.target.value)} aria-label="Edit message" />
                    <div className="message-actions">
                      <button type="button" className="icon-button icon-button--ghost" aria-label="Save edit" onClick={() => void submitEdit(message.id)}><Check size={14} /></button>
                      <button type="button" className="icon-button icon-button--ghost" aria-label="Cancel edit" onClick={cancelEdit}><X size={14} /></button>
                    </div>
                  </div>
                ) : message.role === 'ASSISTANT' ? (
                  <div className="message-markdown"><ReactMarkdown rehypePlugins={[rehypeSanitize]}>{message.content}</ReactMarkdown></div>
                ) : message.content}

                {!isEditing && !activeStream && (message.role === 'USER' || isLastAssistant) && (
                  <div className="message-actions">
                    {message.role === 'USER' && <button type="button" className="icon-button icon-button--ghost" aria-label="Edit message" onClick={() => startEdit(message)}><Pencil size={14} /></button>}
                    {isLastAssistant && <button type="button" className="icon-button icon-button--ghost" aria-label="Regenerate response" onClick={() => void handleRegenerate(message.id)}><RotateCcw size={14} /></button>}
                  </div>
                )}
              </div>
            </div>
          )
        })}

        {activeStream?.type === 'send' && (
          <div className="message-row message-row--user">
            <div className="message-card message-card--user">
              <div className="message-meta"><span>You</span><span>Sending...</span></div>
              {lastSentContent}
            </div>
          </div>
        )}

        {activeStream && (
          <div className="message-row message-row--assistant">
            <div className="message-card">
              <div className="message-meta"><span>CareerPilot</span><span>{streamStatus === 'streaming' ? 'Typing...' : 'Finishing...'}</span></div>
              <div className="message-markdown"><ReactMarkdown rehypePlugins={[rehypeSanitize]}>{streamingText || '...'}</ReactMarkdown></div>
            </div>
          </div>
        )}
      </div>

      {localError && <div style={{ padding: '0 1.25rem 0.5rem' }}><PageMessage tone="error">{localError}</PageMessage></div>}

      <div className="chat-composer">
        <label className="sr-only" htmlFor="message-box">Message to {selectedConversation.title}</label>
        <textarea id="message-box" value={draft} onChange={(event) => setDraft(event.target.value)} placeholder="Share your next career question..." aria-label={`Message to ${selectedConversation.title}`} disabled={!!activeStream} />
        <button type="button" className="primary-button" onClick={() => void handleSend()} disabled={!!activeStream || !draft.trim() || !selectedConversation.id}><SendHorizontal size={16} /> Send</button>
      </div>
    </div>
  </div></section>
}

