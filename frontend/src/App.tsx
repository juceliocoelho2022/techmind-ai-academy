import { FormEvent, useEffect, useMemo, useState } from 'react'
import { BrainCircuit, Cloud, Code2, Database, LogOut, ShieldCheck, Sparkles, Trophy, UserPlus } from 'lucide-react'

type Course = { id: number; slug: string; title: string; description: string; totalLessons: number }
type Progress = { completedLessons: number; totalLessons: number; percentage: number; xp: number; level: string }
type User = { id: number; name: string; email: string; role: string }
type AuthResponse = { accessToken: string; tokenType: string; expiresIn: number; user: User }
type Enrollment = { id: number; courseId: number; courseSlug: string; courseTitle: string; completedLessons: number; totalLessons: number; xp: number; percentage: number }

const icons: Record<string, typeof Code2> = {
  'java-backend': Code2,
  'spring-boot': BrainCircuit,
  'aws-cloud': Cloud,
  'data-ai': Database
}

const TOKEN_KEY = 'techmind.accessToken'

async function api<T>(path: string, options: RequestInit = {}, token?: string | null): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(path, { ...options, headers })
  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.detail || payload?.message || 'Não foi possível concluir a operação.')
  }
  return response.json()
}

export default function App() {
  const [courses, setCourses] = useState<Course[]>([])
  const [progress, setProgress] = useState<Progress | null>(null)
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY))
  const [authMode, setAuthMode] = useState<'login' | 'register'>('register')
  const [authBusy, setAuthBusy] = useState(false)
  const [message, setMessage] = useState('')

  const enrolledIds = useMemo(() => new Set(enrollments.map(item => item.courseId)), [enrollments])

  useEffect(() => {
    api<Course[]>('/api/v1/courses').then(setCourses).catch(() => setCourses([]))
  }, [])

  useEffect(() => {
    if (!token) {
      setUser(null)
      setProgress(null)
      setEnrollments([])
      return
    }

    Promise.all([
      api<User>('/api/v1/users/me', {}, token),
      api<Progress>('/api/v1/progress/me', {}, token),
      api<Enrollment[]>('/api/v1/enrollments/me', {}, token)
    ]).then(([me, myProgress, myEnrollments]) => {
      setUser(me)
      setProgress(myProgress)
      setEnrollments(myEnrollments)
    }).catch(() => logout())
  }, [token])

  function saveSession(response: AuthResponse) {
    localStorage.setItem(TOKEN_KEY, response.accessToken)
    setToken(response.accessToken)
    setUser(response.user)
    setMessage(`Bem-vindo à TechMind, ${response.user.name.split(' ')[0]}!`)
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
      setMessage(
          error instanceof Error
              ? error.message
              : 'Falha na autenticação.'
      )
    } finally {
      setAuthBusy(false)
    }
  }

  async function enroll(courseId: number) {
    if (!token) {
      setMessage('Crie sua conta ou entre para começar uma trilha.')
      document.getElementById('auth')?.scrollIntoView({ behavior: 'smooth' })
      return
    }
    try {
      const enrollment = await api<Enrollment>(`/api/v1/enrollments/courses/${courseId}`, { method: 'POST' }, token)
      setEnrollments(current => current.some(item => item.courseId === courseId) ? current : [enrollment, ...current])
      const updatedProgress = await api<Progress>('/api/v1/progress/me', {}, token)
      setProgress(updatedProgress)
      setMessage('Trilha adicionada à sua jornada.')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Não foi possível realizar a matrícula.')
    }
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY)
    setToken(null)
    setUser(null)
    setProgress(null)
    setEnrollments([])
    setMessage('Sessão encerrada.')
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <img src="/techmind-logo.png" alt="TechMind AI Academy" />
          <div>
            <span className="eyebrow">TECHMIND</span>
            <h1>AI Academy</h1>
          </div>
        </div>
        <div className="top-actions">
          {user && <span className="user-chip">{user.name}</span>}
          <div className="xp"><Trophy size={18}/> {progress?.xp ?? 0} XP</div>
          {user && <button className="icon-button" onClick={logout} title="Sair"><LogOut size={18}/></button>}
        </div>
      </header>

      <main>
        <section className="hero">
          <div>
            <span className="badge"><Sparkles size={15}/> Aprenda construindo</span>
            <h2>Do conteúdo ao projeto real.</h2>
            <p>Trilhas práticas de Java, Spring Boot, AWS, Dados e IA com desafios, feedback, entrevistas simuladas e evolução mensurável.</p>
            <div className="hero-points">
              <span><ShieldCheck size={17}/> Jornada autenticada</span>
              <span><BrainCircuit size={17}/> Mentor IA</span>
              <span><Code2 size={17}/> Projetos práticos</span>
            </div>
          </div>
          <div className="progress-card">
            <span>Seu progresso</span>
            <strong>{progress ? Math.round(progress.percentage) : 0}%</strong>
            <div className="bar"><div style={{width: `${progress?.percentage ?? 0}%`}} /></div>
            <small>{progress?.level ?? (user ? 'Escolha uma trilha para começar' : 'Entre para acompanhar sua evolução')}</small>
          </div>
        </section>

        {!user && (
          <section className="auth-panel" id="auth">
            <div className="auth-copy">
              <span className="eyebrow">SUA CONTA TECHMIND</span>
              <h3>{authMode === 'register' ? 'Comece sua jornada.' : 'Continue de onde parou.'}</h3>
              <p>Seu progresso, XP e trilhas ficam vinculados ao seu perfil.</p>
              <div className="auth-switch">
                <button className={authMode === 'register' ? 'active' : ''} onClick={() => setAuthMode('register')}>Criar conta</button>
                <button className={authMode === 'login' ? 'active' : ''} onClick={() => setAuthMode('login')}>Entrar</button>
              </div>
            </div>
            <form className="auth-form" onSubmit={handleAuth}>
              {authMode === 'register' && <input name="name" minLength={2} maxLength={160} placeholder="Seu nome" required />}
              <input name="email" type="email" placeholder="seu@email.com" required />
              <input name="password" type="password" minLength={8} placeholder="Senha com no mínimo 8 caracteres" required />
              <button className="primary" disabled={authBusy}>
                <UserPlus size={17}/>{authBusy ? 'Processando...' : authMode === 'register' ? 'Criar minha conta' : 'Entrar na plataforma'}
              </button>
            </form>
          </section>
        )}

        {message && <div className="notice">{message}</div>}

        <section>
          <div className="section-head">
            <div>
              <span className="eyebrow">TRILHAS</span>
              <h3>Escolha sua próxima evolução</h3>
            </div>
            {user && <span className="muted">{enrollments.length} trilha(s) na sua jornada</span>}
          </div>
          <div className="grid">
            {courses.map(course => {
              const Icon = icons[course.slug] ?? Code2
              const enrolled = enrolledIds.has(course.id)
              const enrollment = enrollments.find(item => item.courseId === course.id)
              return <article className={`course-card ${enrolled ? 'enrolled' : ''}`} key={course.id}>
                <div className="icon"><Icon size={24}/></div>
                <h4>{course.title}</h4>
                <p>{course.description}</p>
                {enrollment && <div className="mini-progress"><div style={{width: `${enrollment.percentage}%`}} /></div>}
                <div className="course-footer">
                  <span>{enrollment ? `${enrollment.completedLessons}/${course.totalLessons} aulas` : `${course.totalLessons} aulas`}</span>
                  <button onClick={() => enroll(course.id)} disabled={enrolled}>{enrolled ? 'Matriculado' : 'Começar'}</button>
                </div>
              </article>
            })}
          </div>
        </section>

        <section className="ai-panel">
          <div>
            <span className="eyebrow">MENTOR IA</span>
            <h3>Treine como se estivesse em uma entrevista técnica.</h3>
            <p>Receba perguntas, feedback estruturado e pontos concretos para melhorar suas respostas. A próxima fase conectará esse módulo a um LLM real.</p>
          </div>
          <button className="primary" disabled={!user}>Iniciar simulação</button>
        </section>
      </main>
    </div>
  )
}
