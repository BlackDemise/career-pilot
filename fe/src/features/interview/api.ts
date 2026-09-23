import apiClient, { unwrap } from '../../shared/api/axiosClient'
import type { ApiResponse } from '../../shared/api/types'

export type InterviewCatalogItem = {
  id: string
  code: string
  label: string
}

export type InterviewTopicCatalogItem = {
  id: string
  code: string
  label: string
  phase: string
  required: boolean
  allowRepeat: boolean
  selectionWeight: number
}

export type InterviewCatalog = {
  roles: InterviewCatalogItem[]
  levels: InterviewCatalogItem[]
  topics: InterviewTopicCatalogItem[]
}

export type InterviewSetupRequest = {
  roleId: string
  levelId: string
  topicIds?: string[]
  durationSeconds: number
  minimumPrimaryQuestions: number
  maximumPrimaryQuestions: number
  randomPlan: boolean
}

export type InterviewQuestion = {
  id: string
  orderIndex: number
  content: string
  topic: string
  difficulty: string | null
  answer: string | null
}

export type InterviewSession = {
  id: string
  role: string
  level: string
  topic: string
  difficulty: string | null
  numQuestions: number | null
  durationSeconds: number
  minimumPrimaryQuestions: number
  maximumPrimaryQuestions: number
  primaryQuestionsAsked: number
  totalTurns: number
  endsAt: string | null
  currentPhase: string
  status: string
  createdAt: string
  updatedAt: string
  questions: InterviewQuestion[]
}

export type InterviewReport = {
  overallScore: number
  strengths: string[]
  weaknesses: string[]
  recommendations: string[]
}

export async function getInterviewCatalog(roleId?: string, levelId?: string): Promise<InterviewCatalog> {
  const params = new URLSearchParams()
  if (roleId) params.set('roleId', roleId)
  if (levelId) params.set('levelId', levelId)
  const query = params.size ? `?${params.toString()}` : ''
  return unwrap<InterviewCatalog>(apiClient.get<ApiResponse<InterviewCatalog>>(`/interviews/catalog${query}`))
}

export async function createInterview(request: InterviewSetupRequest): Promise<InterviewSession> {
  return unwrap<InterviewSession>(apiClient.post<ApiResponse<InterviewSession>>('/interviews', request))
}

export async function getInterview(sessionId: string): Promise<InterviewSession> {
  return unwrap<InterviewSession>(apiClient.get<ApiResponse<InterviewSession>>(`/interviews/${sessionId}`))
}

export async function getInterviewReport(sessionId: string): Promise<InterviewReport> {
  return unwrap<InterviewReport>(apiClient.get<ApiResponse<InterviewReport>>(`/interviews/${sessionId}/report`))
}
