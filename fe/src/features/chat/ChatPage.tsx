import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { MessageSquarePlus, Trash2, SendHorizontal } from 'lucide-react'
import { PageMessage } from '../../shared/components/PageMessage'
import { createConversation, deleteConversation, listConversations, sendMessage, type Conversation } from './api'

const emptyConversation: Conversation = {
  id: '',
  title: 'New conversation',
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  messages: [],
}

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
  const [localError, setLocalError] = useState<string | null>(null)

  const selectedConversation = useMemo(() => {
    return conversations.find((conversation) => conversation.id === conversationId) ?? conversations[0] ?? emptyConversation
  }, [conversationId, conversations])

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

  const sendMutation = useMutation({
    mutationFn: (content: string) => sendMessage(selectedConversation.id, content),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] })
      setDraft('')
      setLocalError(null)
    },
    onError: (reason) => {
      setLocalError(reason instanceof Error ? reason.message : 'Could not send the message.')
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

  async function handleSend() {
    const message = draft.trim()
    if (!message || !selectedConversation.id) return
    await sendMutation.mutateAsync(message)
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
        {selectedConversation.messages.length === 0 ? <div className="empty-state">Ask about your goals, CV, or interview prep to begin.</div> : selectedConversation.messages.map((message) => (
          <div key={message.id} className={message.role === 'USER' ? 'message-row message-row--user' : 'message-row message-row--assistant'}>
            <div className={message.role === 'USER' ? 'message-card message-card--user' : 'message-card'}>
              <div className="message-meta"><span>{message.role === 'USER' ? 'You' : 'CareerPilot'}</span><span>{formatTimestamp(message.createdAt)}</span></div>
              {message.content}
            </div>
          </div>
        ))}
      </div>

      {localError && <div style={{ padding: '0 1.25rem 0.5rem' }}><PageMessage tone="error">{localError}</PageMessage></div>}

      <div className="chat-composer">
        <label className="sr-only" htmlFor="message-box">Message to {selectedConversation.title}</label>
        <textarea id="message-box" value={draft} onChange={(event) => setDraft(event.target.value)} placeholder="Share your next career question..." aria-label={`Message to ${selectedConversation.title}`} />
        <button type="button" className="primary-button" onClick={() => void handleSend()} disabled={sendMutation.isPending || !draft.trim() || !selectedConversation.id}><SendHorizontal size={16} /> Send</button>
      </div>
    </div>
  </div></section>
}
