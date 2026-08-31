import apiClient, { unwrap } from '../../shared/api/axiosClient'
import type { ApiResponse } from '../../shared/api/types'

export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM'

export type ChatMessage = {
  id: string
  role: MessageRole
  content: string
  createdAt: string
}

export type Conversation = {
  id: string
  title: string
  createdAt: string
  updatedAt: string
  messages: ChatMessage[]
}

export async function listConversations(): Promise<Conversation[]> {
  return unwrap<Conversation[]>(apiClient.get<ApiResponse<Conversation[]>>('/conversations'))
}

export async function createConversation(title?: string): Promise<Conversation> {
  return unwrap<Conversation>(apiClient.post<ApiResponse<Conversation>>('/conversations', { title }))
}

export async function sendMessage(conversationId: string, content: string): Promise<Conversation> {
  return unwrap<Conversation>(apiClient.post<ApiResponse<Conversation>>(`/conversations/${conversationId}/messages`, { content }))
}

export async function deleteConversation(conversationId: string): Promise<void> {
  await unwrap<null>(apiClient.delete<ApiResponse<null>>(`/conversations/${conversationId}`))
}
