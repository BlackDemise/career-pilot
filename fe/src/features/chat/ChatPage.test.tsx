import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as chatApi from './api'
import { ChatPage } from './ChatPage'

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/chat/c-1']}>
        <Routes>
          <Route path="/chat/:conversationId?" element={<ChatPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ChatPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads conversations and allows sending a message', async () => {
    const listSpy = vi.spyOn(chatApi, 'listConversations').mockResolvedValue([
      {
        id: 'c-1',
        title: 'Career prep',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        messages: [
          { id: 'm-1', role: 'USER', content: 'Hi there', createdAt: '2024-01-01T00:00:00Z' },
          { id: 'm-2', role: 'ASSISTANT', content: 'Hello! How can I help?', createdAt: '2024-01-01T00:01:00Z' },
        ],
      },
    ])
    const sendSpy = vi.spyOn(chatApi, 'sendMessage').mockResolvedValue({
      id: 'c-1',
      title: 'Career prep',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      messages: [
        { id: 'm-1', role: 'USER', content: 'Hi there', createdAt: '2024-01-01T00:00:00Z' },
        { id: 'm-2', role: 'ASSISTANT', content: 'Hello! How can I help?', createdAt: '2024-01-01T00:01:00Z' },
        { id: 'm-3', role: 'USER', content: 'Can you help with interviews?', createdAt: '2024-01-01T00:02:00Z' },
      ],
    })

    const user = userEvent.setup()
    renderPage()

    await waitFor(() => expect(screen.getByText('Career prep')).toBeInTheDocument())
    expect(listSpy).toHaveBeenCalled()

    const composer = screen.getByLabelText(/message to career prep/i)
    await user.clear(composer)
    await user.type(composer, 'Can you help with interviews?')
    await user.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() => expect(sendSpy).toHaveBeenCalledWith('c-1', 'Can you help with interviews?'))
  })
})
