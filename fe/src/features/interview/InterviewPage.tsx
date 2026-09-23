import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Clock3, PlayCircle, SendHorizontal, TimerReset } from 'lucide-react'
import { getAccessToken } from '../../shared/api/authSession'
import { PageMessage } from '../../shared/components/PageMessage'
import { getInterview, getInterviewCatalog, createInterview, getInterviewReport, type InterviewCatalog, type InterviewQuestion, type InterviewSession } from './api'

const DEFAULT_DURATION = 900
const DEFAULT_MIN_QUESTIONS = 3
const DEFAULT_MAX_QUESTIONS = 8

function getPhaseTitle(phase: string): string {
  return phase.replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
}

function InterviewSetupForm({ catalog, onCreate }: { catalog: InterviewCatalog; onCreate: (payload: { roleId: string; levelId: string; durationSeconds: number; minimumPrimaryQuestions: number; maximumPrimaryQuestions: number; randomPlan: boolean; topicIds?: string[] }) => Promise<void> }) {
  const [roleId, setRoleId] = useState(catalog.roles[0]?.id ?? '')
  const [levelId, setLevelId] = useState(catalog.levels[0]?.id ?? '')
  const [selectedTopicIds, setSelectedTopicIds] = useState<string[]>([])
  const [useRandomPlan, setUseRandomPlan] = useState(true)
  const [durationSeconds, setDurationSeconds] = useState(DEFAULT_DURATION)
  const [minimumPrimaryQuestions, setMinimumPrimaryQuestions] = useState(DEFAULT_MIN_QUESTIONS)
  const [maximumPrimaryQuestions, setMaximumPrimaryQuestions] = useState(DEFAULT_MAX_QUESTIONS)

  const availableTopics = useMemo(() => {
    if (!roleId || !levelId) return []
    return catalog.topics.filter((topic) => !topic.required || topic.selectionWeight > 0)
  }, [catalog.topics, levelId, roleId])

  function toggleTopic(topicId: string) {
    setSelectedTopicIds((current) => current.includes(topicId) ? current.filter((item) => item !== topicId) : [...current, topicId])
  }

  async function handleSubmit() {
    if (!roleId || !levelId) return
    await onCreate({
      roleId,
      levelId,
      topicIds: useRandomPlan ? undefined : selectedTopicIds,
      durationSeconds,
      minimumPrimaryQuestions,
      maximumPrimaryQuestions,
      randomPlan: useRandomPlan,
    })
  }

  return <div className="workspace-grid">
    <aside className="chat-sidebar">
      <div className="chat-sidebar-header">
        <h2>Interview setup</h2>
      </div>

      <div className="field-group">
        <label htmlFor="interview-role">Role</label>
        <select id="interview-role" value={roleId} onChange={(event) => setRoleId(event.target.value)}>
          {catalog.roles.map((role) => <option key={role.id} value={role.id}>{role.label}</option>)}
        </select>
      </div>

      <div className="field-group" style={{ marginTop: '1rem' }}>
        <label htmlFor="interview-level">Level</label>
        <select id="interview-level" value={levelId} onChange={(event) => setLevelId(event.target.value)}>
          {catalog.levels.map((level) => <option key={level.id} value={level.id}>{level.label}</option>)}
        </select>
      </div>

      <div className="field-group" style={{ marginTop: '1rem' }}>
        <label htmlFor="interview-duration">Duration (seconds)</label>
        <input id="interview-duration" type="number" min={60} max={3600} value={durationSeconds} onChange={(event) => setDurationSeconds(Number(event.target.value) || DEFAULT_DURATION)} />
      </div>

      <div className="field-group" style={{ marginTop: '1rem' }}>
        <label htmlFor="interview-min">Min questions</label>
        <input id="interview-min" type="number" min={1} max={30} value={minimumPrimaryQuestions} onChange={(event) => setMinimumPrimaryQuestions(Number(event.target.value) || DEFAULT_MIN_QUESTIONS)} />
      </div>

      <div className="field-group" style={{ marginTop: '1rem' }}>
        <label htmlFor="interview-max">Max questions</label>
        <input id="interview-max" type="number" min={1} max={30} value={maximumPrimaryQuestions} onChange={(event) => setMaximumPrimaryQuestions(Number(event.target.value) || DEFAULT_MAX_QUESTIONS)} />
      </div>

      <div style={{ marginTop: '1rem' }}>
        <label><input type="checkbox" checked={useRandomPlan} onChange={(event) => setUseRandomPlan(event.target.checked)} /> Random plan</label>
      </div>
    </aside>

    <div className="chat-panel">
      <header className="chat-panel-header">
        <h2>Available topics</h2>
      </header>

      <div className="chat-thread">
        {availableTopics.length === 0 ? <p className="empty-state">No topics are available for the selected role and level yet.</p> : availableTopics.map((topic) => (
          <button
            key={topic.id}
            type="button"
            className={selectedTopicIds.includes(topic.id) ? 'conversation-button conversation-button--active' : 'conversation-button'}
            onClick={() => toggleTopic(topic.id)}
            disabled={useRandomPlan}
            style={{ opacity: useRandomPlan ? 0.7 : 1 }}
          >
            <strong>{topic.label}</strong>
            <small>{topic.phase} • {topic.required ? 'Required' : 'Optional'} • {topic.selectionWeight} weight</small>
          </button>
        ))}
      </div>

      <div className="chat-composer" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div className="meta-row"><Clock3 size={16} /> <span>{durationSeconds / 60} min</span></div>
        <button type="button" className="primary-button" onClick={() => void handleSubmit()}>
          <PlayCircle size={16} /> Start interview
        </button>
      </div>
    </div>
  </div>
}

function InterviewLiveSession({ session, currentQuestion, answer, onAnswerChange, onSubmit, onTimeout, submitting }: {
  session: InterviewSession
  currentQuestion: InterviewQuestion | null
  answer: string
  onAnswerChange: (value: string) => void
  onSubmit: () => void
  onTimeout: () => void
  submitting: boolean
}) {
  return <section className="workspace">
    <div className="cv-page">
      <header className="cv-page-header">
        <div>
          <p className="eyebrow">Practice under pressure</p>
          <h1>{session.role} • {session.level}</h1>
        </div>
        <span className="pill pill--info">{session.status}</span>
      </header>

      <div className="analysis-card">
        <div className="summary-header">
          <h2>{getPhaseTitle(session.currentPhase)}</h2>
          <span className="analysis-date">{session.primaryQuestionsAsked}/{session.maximumPrimaryQuestions} answered</span>
        </div>
        <p className="analysis-summary">Question budget: {session.minimumPrimaryQuestions} min / {session.maximumPrimaryQuestions} max</p>
        {currentQuestion ? (
          <>
            <div className="extracted-text-box">
              <p>{currentQuestion.content}</p>
            </div>
            <div style={{ display: 'grid', gap: '0.75rem', marginTop: '1rem' }}>
              <label htmlFor="interview-answer">Your answer</label>
              <textarea id="interview-answer" value={answer} onChange={(event) => onAnswerChange(event.target.value)} placeholder="Type your answer here..." rows={7} disabled={submitting || session.status === 'COMPLETED'} />
              <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                <button type="button" className="secondary-button" onClick={onTimeout} disabled={submitting || session.status === 'COMPLETED'}>
                  <TimerReset size={16} /> Timeout
                </button>
                <button type="button" className="primary-button" onClick={onSubmit} disabled={submitting || !answer.trim() || session.status === 'COMPLETED'}>
                  <SendHorizontal size={16} /> Submit answer
                </button>
              </div>
            </div>
          </>
        ) : <p className="empty-state">The live interviewer is preparing the next question.</p>}
      </div>
    </div>
  </section>
}

export function InterviewPage() {
  const { data: catalog, isLoading, isError, error } = useQuery({
    queryKey: ['interview-catalog'],
    queryFn: () => getInterviewCatalog(),
  })

  const [session, setSession] = useState<InterviewSession | null>(null)
  const [currentQuestion, setCurrentQuestion] = useState<InterviewQuestion | null>(null)
  const [answer, setAnswer] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState<string | null>(null)
  const socketRef = useRef<WebSocket | null>(null)

  const createMutation = useMutation({
    mutationFn: createInterview,
    onSuccess: (created) => {
      setSession(created)
      setCurrentQuestion(null)
      setAnswer('')
      setLocalError(null)
    },
    onError: (reason) => {
      setLocalError(reason instanceof Error ? reason.message : 'Could not start the interview.')
    },
  })

  const reportQuery = useQuery({
    queryKey: ['interview-report', session?.id],
    enabled: Boolean(session?.id) && session?.status === 'COMPLETED',
    queryFn: () => getInterviewReport(session!.id),
  })

  useEffect(() => {
    if (!session?.id) return

    const token = getAccessToken()
    if (!token) return

    const socket = new WebSocket(`${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/api/v1/interviews/${session.id}/stream?accessToken=${encodeURIComponent(token)}`)
    socketRef.current = socket

    socket.onmessage = async (event) => {
      try {
        const message = JSON.parse(event.data) as { type: string; payload?: unknown }
        const payload = message.payload as Record<string, unknown> | undefined

        if (payload && typeof payload === 'object' && 'status' in payload && 'currentPhase' in payload) {
          setSession(payload as InterviewSession)
          const nextSession = payload as InterviewSession
          setCurrentQuestion(nextSession.questions.at(-1) ?? null)
          setSubmitting(false)
          return
        }

        if (payload && typeof payload === 'object' && 'id' in payload && 'content' in payload) {
          setCurrentQuestion(payload as InterviewQuestion)
          setSubmitting(false)
          try {
            const refreshed = await getInterview(session.id)
            setSession(refreshed)
            setCurrentQuestion(refreshed.questions.at(-1) ?? null)
          } catch (refreshError) {
            setLocalError(refreshError instanceof Error ? refreshError.message : 'Could not refresh the interview session.')
          }
          return
        }

        if (message.type === 'INTERVIEW_COMPLETED') {
          setSubmitting(false)
          const refreshed = await getInterview(session.id)
          setSession(refreshed)
          setCurrentQuestion(null)
        }
      } catch (parseError) {
        setLocalError(parseError instanceof Error ? parseError.message : 'The interview stream returned invalid data.')
      }
    }

    socket.onerror = () => {
      setLocalError('The interview connection was interrupted.')
    }

    socket.onclose = () => {
      socketRef.current = null
    }

    return () => {
      socket.close()
      socketRef.current = null
    }
  }, [session?.id])

  async function handleCreate(payload: Parameters<typeof createInterview>[0]) {
    if (!getAccessToken()) {
      setLocalError('Your session has expired. Please sign in again.')
      return
    }
    await createMutation.mutateAsync(payload)
  }

  function handleSubmitAnswer() {
    if (!socketRef.current || !currentQuestion || !answer.trim()) return
    socketRef.current.send(JSON.stringify({
      type: 'ANSWER_SUBMITTED',
      questionId: currentQuestion.id,
      answer: { content: answer.trim() },
    }))
    setAnswer('')
    setSubmitting(true)
  }

  function handleTimeout() {
    if (!socketRef.current || !currentQuestion) return
    socketRef.current.send(JSON.stringify({
      type: 'ANSWER_TIMEOUT',
      questionId: currentQuestion.id,
    }))
    setAnswer('')
    setSubmitting(true)
  }

  if (isLoading) return <section className="workspace"><p className="empty-state">Loading interview catalog...</p></section>
  if (isError) return <section className="workspace"><PageMessage tone="error">{error instanceof Error ? error.message : 'Could not load the interview catalog.'}</PageMessage></section>
  if (!catalog) return <section className="workspace"><p className="empty-state">No interview catalog is available.</p></section>

  if (session) {
    return <>
      <InterviewLiveSession
        session={session}
        currentQuestion={currentQuestion}
        answer={answer}
        onAnswerChange={setAnswer}
        onSubmit={handleSubmitAnswer}
        onTimeout={handleTimeout}
        submitting={submitting}
      />
      {session.status === 'COMPLETED' && reportQuery.data && <section className="workspace" style={{ marginTop: '1.25rem' }}>
        <div className="analysis-card">
          <div className="summary-header">
            <h2>Interview report</h2>
            <span className="pill pill--success">Score {reportQuery.data.overallScore}/10</span>
          </div>
          <div className="analysis-grid">
            <div><h4>Strengths</h4><ul>{reportQuery.data.strengths.map((item) => <li key={item}>{item}</li>)}</ul></div>
            <div><h4>Weaknesses</h4><ul>{reportQuery.data.weaknesses.map((item) => <li key={item}>{item}</li>)}</ul></div>
            <div className="analysis-grid--wide"><h4>Recommendations</h4><ul>{reportQuery.data.recommendations.map((item) => <li key={item}>{item}</li>)}</ul></div>
          </div>
        </div>
      </section>}
      {localError && <section className="workspace"><PageMessage tone="error">{localError}</PageMessage></section>}
    </>
  }

  return <>
    <section className="workspace">
      <p className="eyebrow">CareerPilot workspace</p>
      <h1>Mock Interview</h1>
      <p className="lede">Choose a role, level, and interview rhythm, then let the AI interviewer guide the live session.</p>
    </section>
    {localError && <section className="workspace"><PageMessage tone="error">{localError}</PageMessage></section>}
    <InterviewSetupForm catalog={catalog} onCreate={handleCreate} />
  </>
}
