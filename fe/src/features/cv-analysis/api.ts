import apiClient, { unwrap } from '../../shared/api/axiosClient'
import type { ApiResponse } from '../../shared/api/types'

export type CvDocument = {
  id: string
  fileName: string
  extractedText: string
  createdAt: string
}

export type CvAnalysisType = 'REVIEW' | 'JD_MATCH'

export type CvReviewResult = {
  overallAssessment: string
  strengths: string[]
  weaknesses: string[]
  recommendations: string[]
}

export type CvJdMatchResult = {
  matchScore: number
  matchedSkills: string[]
  missingSkills: string[]
  experienceGaps: string[]
  recommendations: string[]
}

export type CvAnalysisRecord = {
  id: string
  cvId: string
  type: CvAnalysisType
  result: CvReviewResult | CvJdMatchResult
  jobDescription: string | null
  createdAt: string
}

export async function uploadCv(file: File): Promise<CvDocument> {
  const formData = new FormData()
  formData.append('file', file)

  return unwrap<CvDocument>(apiClient.post<ApiResponse<CvDocument>>('/cvs', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }))
}

export async function reviewCv(cvId: string): Promise<CvAnalysisRecord> {
  return unwrap<CvAnalysisRecord>(apiClient.post<ApiResponse<CvAnalysisRecord>>(`/cvs/${cvId}/analyses/review`))
}

export async function matchJobDescription(cvId: string, jobDescription: string): Promise<CvAnalysisRecord> {
  return unwrap<CvAnalysisRecord>(apiClient.post<ApiResponse<CvAnalysisRecord>>(`/cvs/${cvId}/analyses/jd-match`, { jobDescription }))
}

export async function listAnalyses(cvId: string): Promise<CvAnalysisRecord[]> {
  return unwrap<CvAnalysisRecord[]>(apiClient.get<ApiResponse<CvAnalysisRecord[]>>(`/cvs/${cvId}/analyses`))
}
