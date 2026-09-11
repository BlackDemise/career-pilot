import { type ChangeEvent, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { FileText, Sparkles, UploadCloud } from 'lucide-react'
import { PageMessage } from '../../shared/components/PageMessage'
import { listAnalyses, matchJobDescription, reviewCv, uploadCv, type CvAnalysisRecord, type CvDocument, type CvJdMatchResult, type CvReviewResult } from './api'

const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024

function buildFileError(file: File): string | null {
  const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')
  if (!isPdf) return 'Only PDF CV files are supported.'
  if (file.size > MAX_FILE_SIZE_BYTES) return 'CV file must be 5 MB or smaller.'
  return null
}

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })
}

function ReviewCard({ analysis }: { analysis: CvAnalysisRecord }) {
  const result = analysis.result as CvReviewResult

  return <article className="analysis-card">
    <div className="analysis-card-header">
      <span className="pill pill--info">CV Review</span>
      <span className="analysis-date">{formatDate(analysis.createdAt)}</span>
    </div>
    <h3>Overall assessment</h3>
    <p className="analysis-summary">{result.overallAssessment}</p>
    <div className="analysis-grid">
      <div><h4>Strengths</h4><ul>{result.strengths.map((item) => <li key={item}>{item}</li>)}</ul></div>
      <div><h4>Weaknesses</h4><ul>{result.weaknesses.map((item) => <li key={item}>{item}</li>)}</ul></div>
      <div className="analysis-grid--wide"><h4>Recommendations</h4><ul>{result.recommendations.map((item) => <li key={item}>{item}</li>)}</ul></div>
    </div>
  </article>
}

function MatchCard({ analysis }: { analysis: CvAnalysisRecord }) {
  const result = analysis.result as CvJdMatchResult

  return <article className="analysis-card">
    <div className="analysis-card-header">
      <span className="pill pill--success">JD Match</span>
      <span className="analysis-date">{formatDate(analysis.createdAt)}</span>
    </div>
    <div className="match-score-row">
      <span className="match-score">{result.matchScore}</span>
      <div>
        <h3>Role fit</h3>
        <p className="analysis-summary">{analysis.jobDescription ?? 'Job description used for this analysis.'}</p>
      </div>
    </div>
    <div className="analysis-grid">
      <div><h4>Matched skills</h4><ul>{result.matchedSkills.length ? result.matchedSkills.map((item) => <li key={item}>{item}</li>) : <li>None captured</li>}</ul></div>
      <div><h4>Missing skills</h4><ul>{result.missingSkills.length ? result.missingSkills.map((item) => <li key={item}>{item}</li>) : <li>None flagged</li>}</ul></div>
      <div className="analysis-grid--wide"><h4>Experience gaps</h4><ul>{result.experienceGaps.length ? result.experienceGaps.map((item) => <li key={item}>{item}</li>) : <li>No notable gaps</li>}</ul></div>
      <div className="analysis-grid--wide"><h4>Recommendations</h4><ul>{result.recommendations.map((item) => <li key={item}>{item}</li>)}</ul></div>
    </div>
  </article>
}

export function CvAnalysisPage() {
  const queryClient = useQueryClient()
  const [selectedCv, setSelectedCv] = useState<CvDocument | null>(null)
  const [jdInput, setJdInput] = useState('')
  const [fileError, setFileError] = useState<string | null>(null)
  const [uploadNotice, setUploadNotice] = useState<string | null>(null)

  const { data: analyses = [], isLoading: isAnalysesLoading, isError: isAnalysesError, error: analysesError } = useQuery({
    queryKey: ['cv-analyses', selectedCv?.id],
    enabled: Boolean(selectedCv?.id),
    queryFn: () => listAnalyses(selectedCv!.id),
  })

  const uploadMutation = useMutation({
    mutationFn: uploadCv,
    onSuccess: (cv) => {
      setSelectedCv(cv)
      setUploadNotice('CV uploaded successfully.')
      setFileError(null)
      queryClient.setQueryData<CvAnalysisRecord[]>(['cv-analyses', cv.id], [])
      void queryClient.invalidateQueries({ queryKey: ['cv-analyses', cv.id] })
    },
    onError: (reason) => {
      setFileError(reason instanceof Error ? reason.message : 'Could not upload the CV.')
      setUploadNotice(null)
    },
  })

  const reviewMutation = useMutation({
    mutationFn: () => reviewCv(selectedCv!.id),
    onSuccess: async (analysis) => {
      queryClient.setQueryData<CvAnalysisRecord[]>(['cv-analyses', selectedCv?.id], (current = []) => {
        const next = current.filter((item) => item.type !== 'REVIEW')
        return [...next, analysis]
      })
      await queryClient.invalidateQueries({ queryKey: ['cv-analyses', selectedCv?.id] })
      setUploadNotice('CV review completed.')
    },
    onError: (reason) => {
      setFileError(reason instanceof Error ? reason.message : 'Could not review the CV.')
      setUploadNotice(null)
    },
  })

  const jdMatchMutation = useMutation({
    mutationFn: () => matchJobDescription(selectedCv!.id, jdInput.trim()),
    onSuccess: async (analysis) => {
      queryClient.setQueryData<CvAnalysisRecord[]>(['cv-analyses', selectedCv?.id], (current = []) => {
        const next = current.filter((item) => item.type !== 'JD_MATCH')
        return [...next, analysis]
      })
      await queryClient.invalidateQueries({ queryKey: ['cv-analyses', selectedCv?.id] })
      setUploadNotice('JD match analysis completed.')
    },
    onError: (reason) => {
      setFileError(reason instanceof Error ? reason.message : 'Could not analyze the job description.')
      setUploadNotice(null)
    },
  })

  const latestReview = useMemo(() => analyses.find((analysis) => analysis.type === 'REVIEW') ?? null, [analyses])
  const latestMatch = useMemo(() => analyses.find((analysis) => analysis.type === 'JD_MATCH') ?? null, [analyses])

  async function handleUpload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return

    const validationError = buildFileError(file)
    if (validationError) {
      setFileError(validationError)
      event.target.value = ''
      return
    }

    await uploadMutation.mutateAsync(file)
    event.target.value = ''
  }

  return <section className="workspace">
    <div className="cv-page">
      <header className="cv-page-header">
        <div>
          <p className="eyebrow">CV studio</p>
          <h1>CV Analysis</h1>
        </div>
      </header>

      <div className="cv-layout">
        <aside className="cv-upload-card">
          <div className="upload-box">
            <UploadCloud size={24} />
            <label htmlFor="cv-upload" className="upload-label">Upload CV PDF</label>
            <input id="cv-upload" type="file" accept=".pdf,application/pdf" onChange={handleUpload} aria-label="Upload CV PDF" />
          </div>

          {fileError && <PageMessage tone="error">{fileError}</PageMessage>}
          {uploadNotice && <PageMessage tone="success">{uploadNotice}</PageMessage>}

          {selectedCv ? <div className="cv-meta">
            <div className="meta-row"><FileText size={16} /><strong>{selectedCv.fileName}</strong></div>
            <p>Uploaded {formatDate(selectedCv.createdAt)}</p>
            <button type="button" className="primary-button" onClick={() => void reviewMutation.mutateAsync()} disabled={reviewMutation.isPending || !selectedCv.id}>
              Review CV
            </button>
          </div> : <p className="empty-state">Upload a PDF to start your analysis.</p>}

          {selectedCv && <div className="jd-form">
            <label htmlFor="job-description">Job description</label>
            <textarea id="job-description" value={jdInput} onChange={(event) => setJdInput(event.target.value)} placeholder="Paste the target job description..." />
            <button type="button" className="secondary-button" onClick={() => void jdMatchMutation.mutateAsync()} disabled={jdMatchMutation.isPending || !jdInput.trim()}>
              Analyze JD match
            </button>
          </div>}
        </aside>

        <div className="cv-main">
          {selectedCv ? <>
            <div className="cv-summary-card">
              <div className="summary-header">
                <span className="pill pill--info">Selected CV</span>
                <span className="analysis-date">{selectedCv.fileName}</span>
              </div>
              <h2>{selectedCv.fileName}</h2>
              <div className="extracted-text-box">
                <h3>Extracted content preview</h3>
                <p>{selectedCv.extractedText || 'No extractable text was returned for this document.'}</p>
              </div>
            </div>

            {isAnalysesLoading ? <p className="empty-state">Loading analyses...</p> : isAnalysesError ? <PageMessage tone="error">{analysesError instanceof Error ? analysesError.message : 'Could not load previous analyses.'}</PageMessage> : (
              <div className="cv-analysis-list">
                {analyses.length === 0 ? <p className="empty-state">No analyses yet. Review the CV or compare it with a job description.</p> : (
                  <>
                    {latestReview && <ReviewCard analysis={latestReview} />}
                    {latestMatch && <MatchCard analysis={latestMatch} />}
                  </>
                )}
              </div>
            )}
          </> : <div className="empty-state large-empty"><Sparkles size={24} /> <span>Upload a CV and review your strengths, gaps, and role-fit summary.</span></div>}
        </div>
      </div>
    </div>
  </section>
}
