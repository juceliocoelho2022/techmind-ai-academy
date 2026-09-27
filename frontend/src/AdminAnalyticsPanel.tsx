import { useEffect, useMemo, useState } from 'react'
import {
  BarChart3,
  BookOpenCheck,
  CheckCircle2,
  ClipboardCheck,
  GraduationCap,
  LoaderCircle,
  RefreshCw,
  Sparkles,
  Target,
  Users
} from 'lucide-react'
import './admin-analytics-panel.css'

type PlatformSummary = {
  totalUsers: number
  students: number
  admins: number
  courses: number
  enrollments: number
  completedLessons: number
  totalXp: number
  quizzes: number
  quizAttempts: number
  averageQuizScore: number
  quizApprovalRate: number
}

type CoursePerformance = {
  courseId: number
  title: string
  category: string
  technology: string
  level: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  plannedLessons: number
  enrollments: number
  completedLessons: number
  completionRate: number
  totalXp: number
  quizzes: number
  quizAttempts: number
  averageQuizScore: number
  quizApprovalRate: number
}

type AnalyticsResponse = {
  generatedAt: string
  summary: PlatformSummary
  courses: CoursePerformance[]
}

type Props = {
  token: string
  refreshKey: number
  onMessage: (message: string) => void
}

const levelLabel: Record<CoursePerformance['level'], string> = {
  BEGINNER: 'Iniciante',
  INTERMEDIATE: 'Intermediário',
  ADVANCED: 'Avançado'
}

function formatNumber(value: number) {
  return new Intl.NumberFormat('pt-BR').format(value)
}

function formatPercent(value: number) {
  return `${Number(value).toFixed(1)}%`
}

export default function AdminAnalyticsPanel({
  token,
  refreshKey,
  onMessage
}: Props) {
  const [analytics, setAnalytics] = useState<AnalyticsResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [courseQuery, setCourseQuery] = useState('')

  async function loadAnalytics() {
    setLoading(true)

    try {
      const response = await fetch('/api/v1/admin/analytics', {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })

      if (!response.ok) {
        const raw = await response.text()
        let detail = raw

        try {
          const payload = JSON.parse(raw)
          detail = payload.detail || payload.message || payload.title || raw
        } catch {
          // mantém resposta textual
        }

        throw new Error(detail || `HTTP ${response.status}`)
      }

      setAnalytics(await response.json())
    } catch (error) {
      onMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível carregar os indicadores.'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadAnalytics()
  }, [token, refreshKey])

  const filteredCourses = useMemo(() => {
    if (!analytics) return []

    const normalized = courseQuery.trim().toLowerCase()

    if (!normalized) return analytics.courses

    return analytics.courses.filter(course =>
      course.title.toLowerCase().includes(normalized) ||
      course.category.toLowerCase().includes(normalized) ||
      course.technology.toLowerCase().includes(normalized)
    )
  }, [analytics, courseQuery])

  const summary = analytics?.summary

  return (
    <section
      className="admin-card admin-analytics-panel admin-scroll-target"
      id="admin-analytics"
    >
      <div className="analytics-header">
        <div>
          <span className="admin-kicker">ANALYTICS</span>
          <h2>Indicadores da TechMind AI Academy</h2>
          <p>Dados calculados a partir de usuários, matrículas, progresso e quizzes reais.</p>
        </div>

        <button
          className="analytics-refresh"
          type="button"
          disabled={loading}
          onClick={loadAnalytics}
        >
          {loading ? (
            <LoaderCircle className="spin" size={16} />
          ) : (
            <RefreshCw size={16} />
          )}
          Atualizar
        </button>
      </div>

      {loading && !analytics ? (
        <div className="analytics-loading">
          <LoaderCircle className="spin" size={20} />
          Calculando indicadores...
        </div>
      ) : summary ? (
        <>
          <div className="analytics-summary-grid">
            <article>
              <Users size={21} />
              <div>
                <span>Usuários</span>
                <strong>{formatNumber(summary.totalUsers)}</strong>
                <small>{summary.students} alunos · {summary.admins} admins</small>
              </div>
            </article>

            <article>
              <GraduationCap size={21} />
              <div>
                <span>Matrículas</span>
                <strong>{formatNumber(summary.enrollments)}</strong>
                <small>{summary.courses} trilhas disponíveis</small>
              </div>
            </article>

            <article>
              <BookOpenCheck size={21} />
              <div>
                <span>Aulas concluídas</span>
                <strong>{formatNumber(summary.completedLessons)}</strong>
                <small>registros de progresso</small>
              </div>
            </article>

            <article>
              <Sparkles size={21} />
              <div>
                <span>XP acumulado</span>
                <strong>{formatNumber(summary.totalXp)}</strong>
                <small>somado nas matrículas</small>
              </div>
            </article>

            <article>
              <ClipboardCheck size={21} />
              <div>
                <span>Quizzes</span>
                <strong>{formatNumber(summary.quizzes)}</strong>
                <small>{summary.quizAttempts} tentativas</small>
              </div>
            </article>

            <article>
              <BarChart3 size={21} />
              <div>
                <span>Média dos quizzes</span>
                <strong>{formatPercent(summary.averageQuizScore)}</strong>
                <small>todas as tentativas</small>
              </div>
            </article>

            <article>
              <Target size={21} />
              <div>
                <span>Taxa de aprovação</span>
                <strong>{formatPercent(summary.quizApprovalRate)}</strong>
                <small>tentativas aprovadas</small>
              </div>
            </article>

            <article>
              <CheckCircle2 size={21} />
              <div>
                <span>Tentativas</span>
                <strong>{formatNumber(summary.quizAttempts)}</strong>
                <small>avaliações realizadas</small>
              </div>
            </article>
          </div>

          <div className="analytics-course-section">
            <div className="analytics-course-title">
              <div>
                <span className="admin-kicker">DESEMPENHO POR TRILHA</span>
                <h3>Engajamento e aprendizagem</h3>
              </div>

              <input
                value={courseQuery}
                onChange={event => setCourseQuery(event.target.value)}
                placeholder="Buscar trilha ou tecnologia"
              />
            </div>

            <div className="analytics-table-wrap">
              <table className="analytics-table">
                <thead>
                  <tr>
                    <th>Trilha</th>
                    <th>Matrículas</th>
                    <th>Conclusões</th>
                    <th>Progresso</th>
                    <th>XP</th>
                    <th>Quizzes</th>
                    <th>Tentativas</th>
                    <th>Média</th>
                    <th>Aprovação</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredCourses.map(course => (
                    <tr key={course.courseId}>
                      <td>
                        <div className="analytics-course-name">
                          <strong>{course.title}</strong>
                          <span>
                            {course.category} · {course.technology} · {levelLabel[course.level]}
                          </span>
                        </div>
                      </td>
                      <td>{formatNumber(course.enrollments)}</td>
                      <td>
                        {formatNumber(course.completedLessons)}
                        <small>
                          {course.enrollments > 0
                            ? ` de ${formatNumber(course.enrollments * course.plannedLessons)}`
                            : ''}
                        </small>
                      </td>
                      <td>
                        <div className="analytics-progress-cell">
                          <div>
                            <span
                              style={{
                                width: `${Math.min(Math.max(course.completionRate, 0), 100)}%`
                              }}
                            />
                          </div>
                          <strong>{formatPercent(course.completionRate)}</strong>
                        </div>
                      </td>
                      <td>{formatNumber(course.totalXp)}</td>
                      <td>{formatNumber(course.quizzes)}</td>
                      <td>{formatNumber(course.quizAttempts)}</td>
                      <td>{formatPercent(course.averageQuizScore)}</td>
                      <td>
                        <div className="analytics-approval-cell">
                          <span
                            className={
                              course.quizAttempts === 0
                                ? 'neutral'
                                : course.quizApprovalRate >= 70
                                  ? 'good'
                                  : 'attention'
                            }
                          >
                            {course.quizAttempts === 0
                              ? 'Sem tentativas'
                              : formatPercent(course.quizApprovalRate)}
                          </span>
                        </div>
                      </td>
                    </tr>
                  ))}

                  {filteredCourses.length === 0 && (
                    <tr>
                      <td colSpan={9}>
                        <div className="analytics-empty">
                          Nenhuma trilha encontrada.
                        </div>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            <div className="analytics-footnote">
              <span>
                Progresso = aulas concluídas ÷ (matrículas × aulas planejadas).
              </span>
              <span>
                Atualizado em{' '}
                {new Date(analytics.generatedAt).toLocaleString('pt-BR')}.
              </span>
            </div>
          </div>
        </>
      ) : (
        <div className="analytics-empty">
          Não há dados de Analytics disponíveis.
        </div>
      )}
    </section>
  )
}
