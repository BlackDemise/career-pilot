import apiClient, { unwrap } from '../../shared/api/axiosClient'
import type { ApiResponse } from '../../shared/api/types'

export type CvDocument = {
  id: string
  fileName: string
  extractedText: string
  createdAt: string
}

export type CvAnalysisType = 'REVIEW' | 'JD_MATCH'
export type CvAnalysisJobStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED'
export type RequirementCategory = 'REQUIRED' | 'PREFERRED' | 'OPTIONAL'
export type CvMatchStatus = 'SUPPORTED' | 'PARTIALLY_SUPPORTED' | 'UNCLEAR' | 'NOT_SUPPORTED'
export type EvidenceVerificationStatus = 'VERIFIED_EXACT' | 'VERIFIED_NORMALIZED' | 'UNVERIFIED' | 'INVALID_REFERENCE'

export type CvRequirement = {
  id: string
  requirement: string
  category: RequirementCategory
  categoryConfidence: number
  categoryRationale: string
  sourceText: string
  section: string
}

export type CvRequirementMatch = {
  requirementId: string
  status: CvMatchStatus
  section: string
  evidenceQuote: string | null
  sourceBlockIds: string[]
  confidence: number
  verification: EvidenceVerificationStatus | null
}

export type CvSectionScore = {
  section: string
  score: number
  matchedRequirementIds: string[]
  missingRequirementIds: string[]
  gaps: string[]
}

export type CvReviewResult = {
  overallAssessment: string
  strengths: string[]
  weaknesses: string[]
  recommendations: string[]
}

export type CvJdMatchResult = {
  matchScore: number
  requirements: CvRequirement[]
  requirementMatches: CvRequirementMatch[]
  sectionScores: CvSectionScore[]
  matchedSkills: string[]
  missingSkills: string[]
  experienceGaps: string[]
  recommendations: string[]
}

export type CvAnalysisJob = {
  jobId: string
  cvId: string
  type: CvAnalysisType
  status: CvAnalysisJobStatus
  stage: string
  analysisId: string | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
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

export async function matchJobDescription(cvId: string, jobDescription: string): Promise<CvAnalysisJob> {
  return unwrap<CvAnalysisJob>(apiClient.post<ApiResponse<CvAnalysisJob>>(`/cvs/${cvId}/analyses/jd-match`, { jobDescription }))
}

export async function getAnalysisJob(jobId: string): Promise<CvAnalysisJob> {
  return unwrap<CvAnalysisJob>(apiClient.get<ApiResponse<CvAnalysisJob>>(`/cvs/analysis-jobs/${jobId}`))
}

export async function listAnalyses(cvId: string): Promise<CvAnalysisRecord[]> {
  return unwrap<CvAnalysisRecord[]>(apiClient.get<ApiResponse<CvAnalysisRecord[]>>(`/cvs/${cvId}/analyses`))
}
