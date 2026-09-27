import { FormEvent, useState } from 'react'
import {
  CheckCircle2,
  ChevronDown,
  CircleX,
  ClipboardCheck,
  History,
  LoaderCircle,
  Trophy
} from 'lucide-react'
import './lesson-quiz.css'

type QuizOption = {
  id: number
  text: string
  position: number
}

type QuizQuestion = {
  id: number
  prompt: string
  position: number
  options: QuizOption[]
}

type StudentQuiz = {
  id: number
  lessonId: number
  title: string
  description: string | null
  passingScore: number
  xpReward: number
  questions: QuizQuestion[]
}

type QuestionResult = {
  questionId: number
  selectedOptionId: number
  correctOptionId: number
  correct: boolean
}

type AttemptResult = {
  attemptId: number
  score: number
  correctAnswers: number
  totalQuestions: number
  passed: boolean
  xpAwarded: number
  submittedAt: string
  results: QuestionResult[]
}

type AttemptSummary = {
  attemptId: number
  score: number
  passed: boolean
  xpAwarded: number
  submittedAt: string
}

type Props = {
  token: string
  lessonId: number
  enrolled: boolean
  onXpChanged: () => Promise<void>
}

async function authorized<T>(
  path: string,
  token: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Authorization', `Bearer ${token}`)
  if (options.body) headers.set('Content-Type', 'application/json')

  const response = await fetch(path, { ...options, headers })

  if (!response.ok) {
    const raw = await response.text()
    let detail = raw
    try {
      const payload = JSON.parse(raw)
      detail = payload.detail || payload.message || payload.title || raw
    } catch {
      // resposta textual
    }
    const error = new Error(detail || `HTTP ${response.status}`)
    ;(error as Error & { status?: number }).status = response.status
    throw error
  }

  return response.json()
}

export default function LessonQuizPanel({
  token,
  lessonId,
  enrolled,
  onXpChanged
}: Props) {
  const [open, setOpen] = useState(false)
  const [quiz, setQuiz] = useState<StudentQuiz | null>(null)
  const [answers, setAnswers] = useState<Record<number, number>>({})
  const [result, setResult] = useState<AttemptResult | null>(null)
  const [history, setHistory] = useState<AttemptSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState('')

  async function toggle() {
    if (!enrolled) {
      setMessage('Matricule-se na trilha para acessar o quiz.')
      return
    }

    if (open) {
      setOpen(false)
      return
    }

    setOpen(true)

    if (quiz) return

    setLoading(true)
    setMessage('')

    try {
      const [quizData, attemptHistory] = await Promise.all([
        authorized<StudentQuiz>(
          `/api/v1/learning/lessons/${lessonId}/quiz`,
          token
        ),
        authorized<AttemptSummary[]>(
          `/api/v1/learning/lessons/${lessonId}/quiz/attempts`,
          token
        )
      ])
      setQuiz(quizData)
      setHistory(attemptHistory)
    } catch (error) {
      const status = (error as Error & { status?: number }).status
      setMessage(
        status === 404
          ? 'Quiz ainda não publicado para esta aula.'
          : error instanceof Error
            ? error.message
            : 'Não foi possível carregar o quiz.'
      )
    } finally {
      setLoading(false)
    }
  }

  function choose(questionId: number, optionId: number) {
    if (result) return
    setAnswers(current => ({ ...current, [questionId]: optionId }))
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!quiz) return

    if (Object.keys(answers).length !== quiz.questions.length) {
      setMessage('Responda todas as questões antes de finalizar.')
      return
    }

    setSubmitting(true)
    setMessage('')

    try {
      const attempt = await authorized<AttemptResult>(
        `/api/v1/learning/lessons/${lessonId}/quiz/attempts`,
        token,
        {
          method: 'POST',
          body: JSON.stringify({
            answers: quiz.questions.map(question => ({
              questionId: question.id,
              optionId: answers[question.id]
            }))
          })
        }
      )

      setResult(attempt)
      setHistory(current => [
        {
          attemptId: attempt.attemptId,
          score: attempt.score,
          passed: attempt.passed,
          xpAwarded: attempt.xpAwarded,
          submittedAt: attempt.submittedAt
        },
        ...current
      ])

      if (attempt.xpAwarded > 0) {
        await onXpChanged()
      }
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : 'Não foi possível corrigir o quiz.'
      )
    } finally {
      setSubmitting(false)
    }
  }

  function retry() {
    setAnswers({})
    setResult(null)
    setMessage('')
  }

  const resultByQuestion = new Map(
    result?.results.map(item => [item.questionId, item]) ?? []
  )

  return (
    <div className="lesson-quiz">
      <button className="lesson-quiz-trigger" type="button" onClick={toggle}>
        <ClipboardCheck size={16} />
        <span>Quiz da aula</span>
        {history[0] && (
          <small className={history[0].passed ? 'passed' : ''}>
            Última nota: {Number(history[0].score).toFixed(0)}%
          </small>
        )}
        <ChevronDown className={open ? 'rotated' : ''} size={16} />
      </button>

      {message && <div className="lesson-quiz-message">{message}</div>}

      {open && (
        <div className="lesson-quiz-body">
          {loading && (
            <div className="lesson-quiz-loading">
              <LoaderCircle className="spin" size={18} /> Carregando avaliação...
            </div>
          )}

          {!loading && quiz && (
            <>
              <div className="lesson-quiz-heading">
                <div>
                  <strong>{quiz.title}</strong>
                  {quiz.description && <p>{quiz.description}</p>}
                </div>
                <div className="lesson-quiz-rules">
                  <span>Aprovação: {quiz.passingScore}%</span>
                  <span>+{quiz.xpReward} XP</span>
                </div>
              </div>

              {result && (
                <div className={`quiz-result-card ${result.passed ? 'passed' : 'failed'}`}>
                  {result.passed ? <Trophy size={25} /> : <CircleX size={25} />}
                  <div>
                    <strong>
                      {result.passed ? 'Aprovado!' : 'Ainda não foi desta vez'}
                    </strong>
                    <span>
                      Nota {Number(result.score).toFixed(0)}% · {result.correctAnswers}/
                      {result.totalQuestions} corretas
                      {result.xpAwarded > 0 ? ` · +${result.xpAwarded} XP` : ''}
                    </span>
                  </div>
                </div>
              )}

              <form onSubmit={submit}>
                <div className="student-question-list">
                  {quiz.questions.map((question, questionIndex) => {
                    const correction = resultByQuestion.get(question.id)

                    return (
                      <article
                        className={`student-question ${
                          correction
                            ? correction.correct
                              ? 'correct'
                              : 'incorrect'
                            : ''
                        }`}
                        key={question.id}
                      >
                        <div className="student-question-title">
                          <span>{questionIndex + 1}</span>
                          <strong>{question.prompt}</strong>
                          {correction &&
                            (correction.correct ? (
                              <CheckCircle2 size={18} />
                            ) : (
                              <CircleX size={18} />
                            ))}
                        </div>

                        <div className="student-options">
                          {question.options.map(option => {
                            const selected = answers[question.id] === option.id
                            const isCorrectAnswer =
                              correction?.correctOptionId === option.id

                            return (
                              <label
                                className={`${selected ? 'selected' : ''} ${
                                  correction && isCorrectAnswer ? 'correct-answer' : ''
                                }`}
                                key={option.id}
                              >
                                <input
                                  type="radio"
                                  name={`question-${question.id}`}
                                  checked={selected}
                                  disabled={Boolean(result)}
                                  onChange={() => choose(question.id, option.id)}
                                />
                                <span>{option.text}</span>
                              </label>
                            )
                          })}
                        </div>
                      </article>
                    )
                  })}
                </div>

                <div className="student-quiz-actions">
                  {result ? (
                    <button type="button" onClick={retry}>
                      Tentar novamente
                    </button>
                  ) : (
                    <button className="primary" disabled={submitting}>
                      {submitting ? 'Corrigindo...' : 'Finalizar quiz'}
                    </button>
                  )}
                </div>
              </form>

              {history.length > 0 && (
                <details className="quiz-history">
                  <summary>
                    <History size={15} /> Histórico de tentativas ({history.length})
                  </summary>
                  <div>
                    {history.map(item => (
                      <span key={item.attemptId}>
                        {new Date(item.submittedAt).toLocaleString('pt-BR')} ·{' '}
                        {Number(item.score).toFixed(0)}% ·{' '}
                        {item.passed ? 'Aprovado' : 'Não aprovado'}
                        {item.xpAwarded > 0 ? ` · +${item.xpAwarded} XP` : ''}
                      </span>
                    ))}
                  </div>
                </details>
              )}
            </>
          )}
        </div>
      )}
    </div>
  )
}
