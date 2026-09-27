import { FormEvent, useEffect, useMemo, useState } from 'react'
import AdminStudio from './AdminStudio'
import LessonQuizPanel from './LessonQuizPanel'
import PricingSection, { type PlanCode } from './PricingSection'
import {
  ArrowLeft,
  BookOpen,
  BrainCircuit,
  CheckCircle2,
  ChevronRight,
  Cloud,
  Code2,
  Database,
  Download,
  FileArchive,
  FileText,
  Image as ImageIcon,
  LockKeyhole,
  LogOut,
  PlayCircle,
  ShieldCheck,
  Sparkles,
  Trophy,
  UserPlus
} from 'lucide-react'

type Course = {
  id: number
  slug: string
  title: string
  description: string
  category: string
  technology: string
  level: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  totalLessons: number
}

type Progress = {
  completedLessons: number
  totalLessons: number
  percentage: number
  xp: number
  level: string
}

type User = {
  id: number
  name: string
  email: string
  role: string
}

type AuthResponse = {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

type Enrollment = {
  id: number
  courseId: number
  courseSlug: string
  courseTitle: string
  completedLessons: number
  totalLessons: number
  xp: number
  percentage: number
}

type LessonResource = {
  id: number
  type: 'VIDEO' | 'PROJECT_ZIP' | 'IMAGE' | 'EBOOK'
  title: string
  description: string | null
  fileName: string
  contentType: string
  sizeBytes: number
  position: number
}

type Lesson = {
  id: number
  slug: string
  title: string
  summary: string
  position: number
  xpReward: number
  resources: LessonResource[]
}

type LearningModule = {
  id: number
  title: string
  description: string
  position: number
  lessons: Lesson[]
}

type CourseCurriculum = {
  courseId: number
  courseSlug: string
  courseTitle: string
  plannedLessons: number
  availableLessons: number
  modules: LearningModule[]
}

type LearningProgress = {
  courseId: number
  completedLessonIds: number[]
  completedLessons: number
  plannedLessons: number
  xp: number
  percentage: number
}

type CompleteLessonResponse = {
  lessonId: number
  newlyCompleted: boolean
  xpAwarded: number
  courseCompletedLessons: number
  courseXp: number
  percentage: number
}

type PublicPlatformSettings = {
  academyName: string
  tagline: string
  supportEmail: string | null
  registrationEnabled: boolean
}

const icons: Record<string, typeof Code2> = {
  'java-backend': Code2,
  'spring-boot': BrainCircuit,
  'aws-cloud': Cloud,
  'data-ai': Database
}

const TOKEN_KEY = 'techmind.accessToken'
const PLAN_KEY = 'techmind.selectedPlan'

const DEFAULT_PLATFORM_SETTINGS: PublicPlatformSettings = {
  academyName: 'TechMind AI Academy',
  tagline: 'Do conteúdo ao projeto real.',
  supportEmail: null,
  registrationEnabled: true
}

async function api<T>(path: string, options: RequestInit = {}, token?: string | null): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(path, { ...options, headers })

  if (!response.ok) {
    const raw = await response.text()
    let payload: { detail?: string; message?: string } | null = null

    try {
      payload = raw ? JSON.parse(raw) : null
    } catch {
      payload = null
    }

    const reason =
      payload?.detail ||
      payload?.message ||
      raw ||
      response.statusText ||
      'Erro sem detalhes retornado pela API.'

    throw new Error(`HTTP ${response.status} — ${reason}`)
  }

  return response.json()
}

export default function App() {
  const [courses, setCourses] = useState<Course[]>([])
  const [coursesError, setCoursesError] = useState('')
  const [progress, setProgress] = useState<Progress | null>(null)
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY))
  const [authMode, setAuthMode] = useState<'login' | 'register'>('register')
  const [authBusy, setAuthBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [adminMode, setAdminMode] = useState(false)
  const [platformSettings, setPlatformSettings] = useState<PublicPlatformSettings>(
    DEFAULT_PLATFORM_SETTINGS
  )

  const [selectedCourseId, setSelectedCourseId] = useState<number | null>(null)
  const [curriculum, setCurriculum] = useState<CourseCurriculum | null>(null)
  const [curriculumBusy, setCurriculumBusy] = useState(false)
  const [learningProgress, setLearningProgress] = useState<LearningProgress | null>(null)
  const [completingLessonId, setCompletingLessonId] = useState<number | null>(null)

  const enrolledIds = useMemo(
    () => new Set(enrollments.map(item => item.courseId)),
    [enrollments]
  )

  const completedLessonIds = useMemo(
    () => new Set(learningProgress?.completedLessonIds ?? []),
    [learningProgress]
  )

  useEffect(() => {
    api<PublicPlatformSettings>('/api/v1/settings/public')
      .then(settings => {
        setPlatformSettings(settings)
        if (!settings.registrationEnabled) {
          setAuthMode('login')
        }
      })
      .catch(() => {
        // Mantém os padrões locais se as configurações públicas estiverem indisponíveis.
      })
  }, [])

  const selectedCourse = useMemo(
    () => courses.find(course => course.id === selectedCourseId) ?? null,
    [courses, selectedCourseId]
  )

  const selectedEnrollment = useMemo(
    () => enrollments.find(item => item.courseId === selectedCourseId) ?? null,
    [enrollments, selectedCourseId]
  )

  useEffect(() => {
    api<Course[]>('/api/v1/courses')
      .then(data => {
        setCourses(data)
        setCoursesError(
          data.length === 0
            ? 'O catálogo está vazio. Reinicie o backend para aplicar a migration de recuperação.'
            : ''
        )
      })
      .catch(error => {
        setCourses([])
        setCoursesError(
          error instanceof Error
            ? `Falha ao carregar trilhas: ${error.message}`
            : 'Falha ao carregar trilhas.'
        )
      })
  }, [])

  useEffect(() => {
    if (!token) {
      setUser(null)
      setProgress(null)
      setEnrollments([])
      setLearningProgress(null)
      return
    }

    Promise.all([
      api<User>('/api/v1/users/me', {}, token),
      api<Progress>('/api/v1/progress/me', {}, token),
      api<Enrollment[]>('/api/v1/enrollments/me', {}, token)
    ])
      .then(([me, myProgress, myEnrollments]) => {
        setUser(me)
        setProgress(myProgress)
        setEnrollments(myEnrollments)
      })
      .catch(() => logout())
  }, [token])

  useEffect(() => {
    if (!selectedCourseId || !token || !enrolledIds.has(selectedCourseId)) {
      setLearningProgress(null)
      return
    }

    api<LearningProgress>(
      `/api/v1/learning/courses/${selectedCourseId}/progress`,
      {},
      token
    )
      .then(setLearningProgress)
      .catch(() => setLearningProgress(null))
  }, [selectedCourseId, token, enrolledIds])

  function saveSession(response: AuthResponse) {
    localStorage.setItem(TOKEN_KEY, response.accessToken)
    setToken(response.accessToken)
    setUser(response.user)
    setMessage(
      `Bem-vindo à ${platformSettings.academyName}, ${response.user.name.split(' ')[0]}!`
    )
  }

  async function handleAuth(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const form = event.currentTarget

    setAuthBusy(true)
    setMessage('')

    const data = new FormData(form)

    const body =
      authMode === 'register'
        ? {
            name: data.get('name'),
            email: data.get('email'),
            password: data.get('password')
          }
        : {
            email: data.get('email'),
            password: data.get('password')
          }

    try {
      const response = await api<AuthResponse>(
        `/api/v1/auth/${authMode}`,
        {
          method: 'POST',
          body: JSON.stringify(body)
        }
      )

      saveSession(response)
      form.reset()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Falha na autenticação.')
    } finally {
      setAuthBusy(false)
    }
  }

  async function refreshStudentData(activeToken: string) {
    const [updatedProgress, updatedEnrollments] = await Promise.all([
      api<Progress>('/api/v1/progress/me', {}, activeToken),
      api<Enrollment[]>('/api/v1/enrollments/me', {}, activeToken)
    ])

    setProgress(updatedProgress)
    setEnrollments(updatedEnrollments)
  }

  async function enroll(courseId: number) {
    if (!token) {
      setMessage('Crie sua conta ou entre para começar uma trilha.')
      document.getElementById('auth')?.scrollIntoView({ behavior: 'smooth' })
      return
    }

    try {
      const enrollment = await api<Enrollment>(
        `/api/v1/enrollments/courses/${courseId}`,
        { method: 'POST' },
        token
      )

      setEnrollments(current =>
        current.some(item => item.courseId === courseId)
          ? current
          : [enrollment, ...current]
      )

      await refreshStudentData(token)

      const detailedProgress = await api<LearningProgress>(
        `/api/v1/learning/courses/${courseId}/progress`,
        {},
        token
      )
      setLearningProgress(detailedProgress)
      setMessage('Trilha adicionada à sua jornada. Bora para a primeira aula!')
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível realizar a matrícula.'
      )
    }
  }

  async function openCourse(course: Course) {
    setSelectedCourseId(course.id)
    setCurriculumBusy(true)
    setMessage('')

    try {
      const data = await api<CourseCurriculum>(
        `/api/v1/courses/${course.id}/curriculum`
      )
      setCurriculum(data)

      requestAnimationFrame(() => {
        document.getElementById('learning-path')?.scrollIntoView({
          behavior: 'smooth',
          block: 'start'
        })
      })
    } catch (error) {
      setCurriculum(null)
      setMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível carregar o conteúdo da trilha.'
      )
    } finally {
      setCurriculumBusy(false)
    }
  }

  async function downloadResource(resource: LessonResource) {
    if (!token) {
      setMessage('Entre na sua conta para baixar os materiais da aula.')
      return
    }

    if (!selectedEnrollment) {
      setMessage('Matricule-se nesta trilha para baixar os materiais.')
      return
    }

    try {
      const response = await fetch(
        `/api/v1/learning/resources/${resource.id}/download`,
        {
          headers: {
            Authorization: `Bearer ${token}`
          }
        }
      )

      if (!response.ok) {
        const raw = await response.text()
        throw new Error(`HTTP ${response.status} — ${raw || response.statusText}`)
      }

      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = resource.fileName
      document.body.appendChild(anchor)
      anchor.click()
      anchor.remove()
      URL.revokeObjectURL(url)

      setMessage(`Download iniciado: ${resource.fileName}`)
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível baixar o material.'
      )
    }
  }

  async function uploadResource(
    event: FormEvent<HTMLFormElement>,
    lessonId: number
  ) {
    event.preventDefault()

    if (!token || user?.role !== 'ADMIN') {
      setMessage('Somente administradores podem anexar materiais.')
      return
    }

    const form = event.currentTarget
    const data = new FormData(form)

    try {
      const response = await fetch(
        `/api/v1/admin/lessons/${lessonId}/resources`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`
          },
          body: data
        }
      )

      if (!response.ok) {
        const raw = await response.text()
        throw new Error(`HTTP ${response.status} — ${raw || response.statusText}`)
      }

      form.reset()

      if (selectedCourseId) {
        const updated = await api<CourseCurriculum>(
          `/api/v1/courses/${selectedCourseId}/curriculum`
        )
        setCurriculum(updated)
      }

      setMessage('Material anexado à aula com sucesso.')
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível anexar o material.'
      )
    }
  }

  function resourceLabel(type: LessonResource['type']) {
    if (type === 'VIDEO') return 'Vídeo'
    if (type === 'PROJECT_ZIP') return 'Projeto ZIP'
    if (type === 'IMAGE') return 'Imagem'
    return 'E-book'
  }

  function formatBytes(bytes: number) {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }

  async function completeLesson(lessonId: number) {
    if (!token) {
      setMessage('Entre na sua conta para concluir aulas.')
      return
    }

    if (!selectedCourseId || !enrolledIds.has(selectedCourseId)) {
      setMessage('Matricule-se nesta trilha antes de concluir uma aula.')
      return
    }

    setCompletingLessonId(lessonId)

    try {
      const result = await api<CompleteLessonResponse>(
        `/api/v1/learning/lessons/${lessonId}/complete`,
        { method: 'POST' },
        token
      )

      const [detailedProgress] = await Promise.all([
        api<LearningProgress>(
          `/api/v1/learning/courses/${selectedCourseId}/progress`,
          {},
          token
        ),
        refreshStudentData(token)
      ])

      setLearningProgress(detailedProgress)

      setMessage(
        result.newlyCompleted
          ? `Aula concluída! +${result.xpAwarded} XP conquistados.`
          : 'Esta aula já estava concluída. Seu XP foi preservado.'
      )
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível concluir a aula.'
      )
    } finally {
      setCompletingLessonId(null)
    }
  }

  function closeCourse() {
    setSelectedCourseId(null)
    setCurriculum(null)
    setLearningProgress(null)
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY)
    setToken(null)
    setUser(null)
    setProgress(null)
    setEnrollments([])
    setLearningProgress(null)
    setMessage('Sessão encerrada.')
  }

  function handlePlanSelect(plan: PlanCode) {
    localStorage.setItem(PLAN_KEY, plan)

    if (plan === 'EDUCATION') {
      if (platformSettings.supportEmail) {
        const subject = encodeURIComponent('Interesse no TechMind Education / Business')
        const body = encodeURIComponent(
          'Olá! Tenho interesse em conhecer os planos TechMind Education / Business.'
        )
        window.location.href =
          `mailto:${platformSettings.supportEmail}?subject=${subject}&body=${body}`
        return
      }

      setMessage(
        'Interesse registrado. Configure o e-mail de suporte no Admin Studio para ativar o contato comercial direto.'
      )
      return
    }

    if (user) {
      if (plan === 'FREE') {
        setMessage('Sua conta já possui acesso ao plano Free.')
      } else {
        setMessage(
          `Plano ${plan === 'PRO' ? 'Pro' : 'Career'} selecionado. A preferência foi salva para a próxima etapa de assinatura.`
        )
      }
      return
    }

    if (!platformSettings.registrationEnabled) {
      setAuthMode('login')
      setMessage(
        'Os novos cadastros estão fechados no momento. Entre com sua conta para continuar.'
      )
    } else {
      setAuthMode('register')
      setMessage(
        plan === 'FREE'
          ? 'Plano Free selecionado. Crie sua conta para começar.'
          : `Plano ${plan === 'PRO' ? 'Pro' : 'Career'} selecionado. Crie sua conta para continuar.`
      )
    }

    window.requestAnimationFrame(() => {
      document.getElementById('auth')?.scrollIntoView({
        behavior: 'smooth',
        block: 'center'
      })
    })
  }


  if (adminMode && user?.role === 'ADMIN' && token) {
    return (
      <AdminStudio
        token={token}
        userName={user.name}
        courses={courses}
        onCoursesChanged={setCourses}
        onExit={() => setAdminMode(false)}
      />
    )
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <img src="/techmind-logo.png" alt={platformSettings.academyName} />
          <div>
            <span className="eyebrow">ACADEMY</span>
            <h1>{platformSettings.academyName}</h1>
          </div>
        </div>

        <div className="top-actions">
          {user?.role === 'ADMIN' && (
            <button className="admin-toggle" onClick={() => setAdminMode(true)}>
              <ShieldCheck size={17} /> Admin Studio
            </button>
          )}
          {user && <span className="user-chip">{user.name}</span>}
          <div className="xp">
            <Trophy size={18} /> {progress?.xp ?? 0} XP
          </div>
          {user && (
            <button className="icon-button" onClick={logout} title="Sair">
              <LogOut size={18} />
            </button>
          )}
        </div>
      </header>

      <main>
        <section className="hero">
          <div>
            <span className="badge">
              <Sparkles size={15} /> Aprenda construindo
            </span>
            <h2>{platformSettings.tagline}</h2>
            <p>
              Trilhas práticas de Java, Spring Boot, AWS, Dados e IA com desafios,
              feedback, entrevistas simuladas e evolução mensurável.
            </p>
            <div className="hero-points">
              <span>
                <ShieldCheck size={17} /> Jornada autenticada
              </span>
              <span>
                <BrainCircuit size={17} /> Mentor IA
              </span>
              <span>
                <Code2 size={17} /> Projetos práticos
              </span>
            </div>

            <div className="hero-actions">
              <button
                type="button"
                className="secondary-cta"
                onClick={() =>
                  document.getElementById('plans')?.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                  })
                }
              >
                Ver planos
              </button>
            </div>
          </div>

          <div className="progress-card">
            <span>Seu progresso</span>
            <strong>{progress ? Math.round(progress.percentage) : 0}%</strong>
            <div className="bar">
              <div style={{ width: `${progress?.percentage ?? 0}%` }} />
            </div>
            <small>
              {progress?.level ??
                (user
                  ? 'Escolha uma trilha para começar'
                  : 'Entre para acompanhar sua evolução')}
            </small>
          </div>
        </section>

        {!user && (
          <section className="auth-panel" id="auth">
            <div className="auth-copy">
              <span className="eyebrow">SUA CONTA · {platformSettings.academyName.toUpperCase()}</span>
              <h3>
                {authMode === 'register'
                  ? 'Comece sua jornada.'
                  : 'Continue de onde parou.'}
              </h3>
              <p>Seu progresso, XP e trilhas ficam vinculados ao seu perfil.</p>

              <div className="auth-switch">
                {platformSettings.registrationEnabled && (
                  <button
                    className={authMode === 'register' ? 'active' : ''}
                    onClick={() => setAuthMode('register')}
                  >
                    Criar conta
                  </button>
                )}
                <button
                  className={authMode === 'login' ? 'active' : ''}
                  onClick={() => setAuthMode('login')}
                >
                  Entrar
                </button>
              </div>

              {!platformSettings.registrationEnabled && (
                <p className="muted">
                  Novos cadastros estão temporariamente fechados.
                  {platformSettings.supportEmail
                    ? ` Contato: ${platformSettings.supportEmail}`
                    : ''}
                </p>
              )}
            </div>

            <form className="auth-form" onSubmit={handleAuth}>
              {authMode === 'register' && (
                <input
                  name="name"
                  minLength={2}
                  maxLength={160}
                  placeholder="Seu nome"
                  required
                />
              )}
              <input
                name="email"
                type="email"
                placeholder="seu@email.com"
                required
              />
              <input
                name="password"
                type="password"
                minLength={8}
                placeholder="Senha com no mínimo 8 caracteres"
                required
              />
              <button className="primary" disabled={authBusy}>
                <UserPlus size={17} />
                {authBusy
                  ? 'Processando...'
                  : authMode === 'register'
                    ? 'Criar minha conta'
                    : 'Entrar na plataforma'}
              </button>
            </form>
          </section>
        )}

        {message && <div className="notice">{message}</div>}

        <PricingSection
          supportEmail={platformSettings.supportEmail}
          onSelectPlan={handlePlanSelect}
        />

        <section>
          <div className="section-head">
            <div>
              <span className="eyebrow">TRILHAS</span>
              <h3>Escolha sua próxima evolução</h3>
            </div>
            {user && (
              <span className="muted">
                {enrollments.length} trilha(s) na sua jornada
              </span>
            )}
          </div>

          {coursesError && (
            <div className="catalog-state">
              <strong>Catálogo indisponível</strong>
              <span>{coursesError}</span>
            </div>
          )}

          <div className="grid">
            {courses.map(course => {
              const Icon = icons[course.slug] ?? Code2
              const enrolled = enrolledIds.has(course.id)
              const enrollment = enrollments.find(
                item => item.courseId === course.id
              )

              return (
                <article
                  className={`course-card ${enrolled ? 'enrolled' : ''}`}
                  key={course.id}
                  onClick={() => openCourse(course)}
                >
                  <div className="course-card-top">
                    <div className="icon">
                      <Icon size={24} />
                    </div>
                    {enrolled && (
                      <span className="enrolled-badge">
                        <CheckCircle2 size={14} /> Em andamento
                      </span>
                    )}
                  </div>

                  <h4>{course.title}</h4>
                  <p>{course.description}</p>

                  {enrollment && (
                    <div className="mini-progress">
                      <div style={{ width: `${enrollment.percentage}%` }} />
                    </div>
                  )}

                  <div className="course-footer">
                    <span>
                      {enrollment
                        ? `${enrollment.completedLessons}/${course.totalLessons} aulas`
                        : `${course.totalLessons} aulas`}
                    </span>
                    <button
                      onClick={event => {
                        event.stopPropagation()
                        openCourse(course)
                      }}
                    >
                      {enrolled ? 'Continuar' : 'Explorar'} <ChevronRight size={15} />
                    </button>
                  </div>
                </article>
              )
            })}
          </div>
        </section>

        {(curriculumBusy || curriculum) && (
          <section className="learning-path" id="learning-path">
            {curriculumBusy && (
              <div className="curriculum-loading">
                <BrainCircuit size={28} />
                <div>
                  <strong>Montando sua trilha...</strong>
                  <span>Carregando módulos e aulas.</span>
                </div>
              </div>
            )}

            {!curriculumBusy && curriculum && selectedCourse && (
              <>
                <div className="learning-header">
                  <div>
                    <button className="back-button" onClick={closeCourse}>
                      <ArrowLeft size={16} /> Voltar às trilhas
                    </button>
                    <span className="eyebrow">LEARNING ENGINE v0.3</span>
                    <h3>{curriculum.courseTitle}</h3>
                    <p>{selectedCourse.description}</p>
                  </div>

                  <div className="learning-summary">
                    <div>
                      <span>Aulas disponíveis</span>
                      <strong>{curriculum.availableLessons}</strong>
                    </div>
                    <div>
                      <span>Plano completo</span>
                      <strong>{curriculum.plannedLessons}</strong>
                    </div>
                    <div>
                      <span>XP na trilha</span>
                      <strong>{selectedEnrollment?.xp ?? 0}</strong>
                    </div>
                  </div>
                </div>

                {selectedEnrollment && learningProgress && (
                  <div className="track-progress">
                    <div className="track-progress-copy">
                      <div>
                        <span className="eyebrow">SEU PROGRESSO</span>
                        <strong>
                          {learningProgress.completedLessons}/
                          {learningProgress.plannedLessons} aulas
                        </strong>
                      </div>
                      <strong>{Math.round(learningProgress.percentage)}%</strong>
                    </div>
                    <div className="bar">
                      <div
                        style={{
                          width: `${learningProgress.percentage}%`
                        }}
                      />
                    </div>
                  </div>
                )}

                {!selectedEnrollment && (
                  <div className="start-track">
                    <div>
                      <BookOpen size={24} />
                      <div>
                        <strong>Pronto para começar?</strong>
                        <span>
                          Matricule-se para registrar aulas concluídas e ganhar XP.
                        </span>
                      </div>
                    </div>

                    <button
                      className="primary"
                      onClick={() => enroll(curriculum.courseId)}
                    >
                      {user ? (
                        <>
                          <PlayCircle size={18} /> Começar trilha
                        </>
                      ) : (
                        <>
                          <LockKeyhole size={18} /> Entrar para começar
                        </>
                      )}
                    </button>
                  </div>
                )}

                <div className="modules-list">
                  {curriculum.modules.map(module => (
                    <article className="module-card" key={module.id}>
                      <div className="module-heading">
                        <div className="module-number">
                          {String(module.position).padStart(2, '0')}
                        </div>
                        <div>
                          <span>MÓDULO {module.position}</span>
                          <h4>{module.title}</h4>
                          <p>{module.description}</p>
                        </div>
                      </div>

                      <div className="lessons-list">
                        {module.lessons.map(lesson => {
                          const completed = completedLessonIds.has(lesson.id)
                          const completing = completingLessonId === lesson.id

                          return (
                            <div
                              className={`lesson-row ${completed ? 'completed' : ''}`}
                              key={lesson.id}
                            >
                              <div className="lesson-status">
                                {completed ? (
                                  <CheckCircle2 size={21} />
                                ) : (
                                  <BookOpen size={21} />
                                )}
                              </div>

                              <div className="lesson-copy">
                                <div className="lesson-title-line">
                                  <strong>{lesson.title}</strong>
                                  <span>+{lesson.xpReward} XP</span>
                                </div>
                                <p>{lesson.summary}</p>

                                {lesson.resources.length > 0 && (
                                  <div className="lesson-resources">
                                    {lesson.resources.map(resource => {
                                      const ResourceIcon =
                                        resource.type === 'VIDEO'
                                          ? PlayCircle
                                          : resource.type === 'PROJECT_ZIP'
                                            ? FileArchive
                                            : resource.type === 'IMAGE'
                                              ? ImageIcon
                                              : FileText

                                      return (
                                        <button
                                          className="resource-chip"
                                          key={resource.id}
                                          onClick={() => downloadResource(resource)}
                                          disabled={!user || !selectedEnrollment}
                                          title={
                                            selectedEnrollment
                                              ? `Baixar ${resource.fileName}`
                                              : 'Matricule-se para baixar'
                                          }
                                        >
                                          <ResourceIcon size={15} />
                                          <span>
                                            <strong>{resource.title}</strong>
                                            <small>
                                              {resourceLabel(resource.type)} · {formatBytes(resource.sizeBytes)}
                                            </small>
                                          </span>
                                          <Download size={15} />
                                        </button>
                                      )
                                    })}
                                  </div>
                                )}

                                {user && token && (
                                  <LessonQuizPanel
                                    token={token}
                                    lessonId={lesson.id}
                                    enrolled={Boolean(selectedEnrollment)}
                                    onXpChanged={() => refreshStudentData(token)}
                                  />
                                )}

                                {user?.role === 'ADMIN' && (
                                  <details className="resource-admin">
                                    <summary>Adicionar material à aula</summary>
                                    <form onSubmit={event => uploadResource(event, lesson.id)}>
                                      <select name="type" defaultValue="PROJECT_ZIP" required>
                                        <option value="VIDEO">Vídeo</option>
                                        <option value="PROJECT_ZIP">Projeto .zip</option>
                                        <option value="IMAGE">Imagem</option>
                                        <option value="EBOOK">E-book PDF/EPUB</option>
                                      </select>
                                      <input
                                        name="title"
                                        placeholder="Título do material"
                                        maxLength={200}
                                        required
                                      />
                                      <input
                                        name="description"
                                        placeholder="Descrição opcional"
                                        maxLength={500}
                                      />
                                      <input
                                        name="file"
                                        type="file"
                                        accept=".mp4,.mov,.avi,.webm,.zip,.png,.jpg,.jpeg,.webp,.pdf,.epub"
                                        required
                                      />
                                      <button type="submit">Anexar material</button>
                                    </form>
                                  </details>
                                )}
                              </div>

                              <button
                                className={completed ? 'lesson-done' : 'lesson-action'}
                                disabled={
                                  completed ||
                                  completing ||
                                  !user ||
                                  !selectedEnrollment
                                }
                                onClick={() => completeLesson(lesson.id)}
                              >
                                {completed
                                  ? 'Concluída'
                                  : completing
                                    ? 'Salvando...'
                                    : 'Concluir aula'}
                              </button>
                            </div>
                          )
                        })}
                      </div>
                    </article>
                  ))}
                </div>
              </>
            )}
          </section>
        )}

        <section className="ai-panel">
          <div>
            <span className="eyebrow">MENTOR IA</span>
            <h3>Treine como se estivesse em uma entrevista técnica.</h3>
            <p>
              Receba perguntas, feedback estruturado e pontos concretos para
              melhorar suas respostas. A próxima fase conectará esse módulo a um
              LLM real.
            </p>
          </div>
          <button className="primary" disabled={!user}>
            Iniciar simulação
          </button>
        </section>
      </main>
    </div>
  )
}
