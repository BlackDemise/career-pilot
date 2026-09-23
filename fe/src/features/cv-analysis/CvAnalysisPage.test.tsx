import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as cvApi from './api'
import { CvAnalysisPage } from './CvAnalysisPage'

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <CvAnalysisPage />
    </QueryClientProvider>,
  )
}

describe('CvAnalysisPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('uploads a PDF and shows analyses after review/match', async () => {
    const uploadCvData = {
      id: 'cv-1',
      fileName: 'jane-doe.pdf',
      extractedText: 'Senior engineer with Node and Java experience',
      createdAt: '2024-01-01T00:00:00Z',
    }
    vi.spyOn(cvApi, 'uploadCv').mockResolvedValue(uploadCvData)

    const reviewData = {
      id: 'a-1',
      cvId: 'cv-1',
      type: 'REVIEW' as const,
      jobDescription: null,
      result: {
        overallAssessment: 'Strong profile with clear backend ownership.',
        strengths: ['Strong leadership'],
        weaknesses: ['Needs growth'],
        recommendations: ['Add more detail'],
      },
      createdAt: '2024-01-01T00:00:00Z',
    }
    const reviewSpy = vi.spyOn(cvApi, 'reviewCv').mockResolvedValue(reviewData)

    const matchJob = { jobId: 'job-1', cvId: 'cv-1', type: 'JD_MATCH' as const, status: 'COMPLETED' as const, stage: 'COMPLETED', analysisId: 'a-2', errorMessage: null, createdAt: '2024-01-01T00:01:00Z', updatedAt: '2024-01-01T00:01:00Z' }
    const matchData: cvApi.CvAnalysisRecord = {
      id: 'a-2', cvId: 'cv-1', type: 'JD_MATCH', jobDescription: 'Senior backend engineer',
      result: { matchScore: 88, requirements: [], requirementMatches: [], sectionScores: [], matchedSkills: ['Java'], missingSkills: ['Kubernetes'], experienceGaps: ['Infra'], recommendations: ['Highlight it'] },
      createdAt: '2024-01-01T00:01:00Z',
    }
    const matchSpy = vi.spyOn(cvApi, 'matchJobDescription').mockResolvedValue(matchJob)
    vi.spyOn(cvApi, 'getAnalysisJob').mockResolvedValue(matchJob)

    // Mock listAnalyses to return empty initially, then with analyses after mutations
    const analysesState: cvApi.CvAnalysisRecord[] = []
    vi.spyOn(cvApi, 'listAnalyses').mockImplementation(() => {
      return Promise.resolve(analysesState)
    })

    const user = userEvent.setup()
    renderPage()

    // Upload CV
    const input = screen.getByLabelText(/upload cv pdf, docx, or txt/i)
    const file = new File(['pdf text'], 'jane-doe.pdf', { type: 'application/pdf' })
    fireEvent.change(input, { target: { files: [file] } })

    await waitFor(() => expect(screen.getByRole('heading', { name: 'jane-doe.pdf' })).toBeInTheDocument())

    // Review CV — mutation updates local state
    await user.click(screen.getByRole('button', { name: /review cv/i }))
    await waitFor(() => expect(reviewSpy).toHaveBeenCalled())
    // Simulate what the mutation's onSuccess does: update analysesState
    analysesState.push(reviewData)
    await waitFor(() => expect(screen.getByText('CV Review')).toBeInTheDocument())

    // Match JD
    const jdInput = screen.getByLabelText(/job description/i)
    await user.clear(jdInput)
    await user.type(jdInput, 'Senior backend engineer')
    await user.click(screen.getByRole('button', { name: /analyze jd match/i }))

    await waitFor(() => expect(matchSpy).toHaveBeenCalledWith('cv-1', 'Senior backend engineer'))
    // Simulate what the mutation's onSuccess does
    analysesState.push(matchData)
    await waitFor(() => expect(screen.getByText('JD Match')).toBeInTheDocument())
    expect(screen.getByText('88')).toBeInTheDocument()
  })

  it('blocks invalid uploads and renders an empty analysis state gracefully', async () => {
    vi.spyOn(cvApi, 'listAnalyses').mockResolvedValue([])
    const uploadSpy = vi.spyOn(cvApi, 'uploadCv')
    renderPage()

    const input = screen.getByLabelText(/upload cv pdf, docx, or txt/i)
    const invalidFile = new File(['nope'], 'notes.txt', { type: 'text/plain' })

    fireEvent.change(input, { target: { files: [invalidFile] } })

    await waitFor(() => expect(screen.getByText(/only pdf, docx, and txt cv files are supported/i)).toBeInTheDocument())
    expect(uploadSpy).not.toHaveBeenCalled()
  })

  it('accepts TXT uploads', async () => {
    const uploadCvData = {
      id: 'cv-1',
      fileName: 'jane-doe.txt',
      extractedText: 'Senior engineer with Java experience',
      createdAt: '2024-01-01T00:00:00Z',
    }
    const uploadSpy = vi.spyOn(cvApi, 'uploadCv').mockResolvedValue(uploadCvData)
    vi.spyOn(cvApi, 'listAnalyses').mockResolvedValue([])
    renderPage()

    const input = screen.getByLabelText(/upload cv pdf, docx, or txt/i)
    const file = new File(['Senior engineer with Java experience'], 'jane-doe.txt', { type: 'text/plain' })
    fireEvent.change(input, { target: { files: [file] } })

    await waitFor(() => expect(uploadSpy).toHaveBeenCalledWith(file))
    expect(screen.getByRole('heading', { name: 'jane-doe.txt' })).toBeInTheDocument()
  })
})
