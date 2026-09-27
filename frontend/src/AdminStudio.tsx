import { FormEvent, useEffect, useMemo, useState } from 'react'
import {
  BarChart3,
  BookOpen,
  ChevronRight,
  Download,
  Eye,
  FileArchive,
  FileText,
  Image as ImageIcon,
  LayoutDashboard,
  ListVideo,
  LoaderCircle,
  LogOut,
  Play,
  Plus,
  Save,
  Settings,
  Sparkles,
  Trash2,
  UploadCloud,
  Users,
  Video
} from 'lucide-react'
import './admin-studio.css'
import AdminStructureManager from './AdminStructureManager'
import AdminQuizEditor from './AdminQuizEditor'
import AdminUsersPanel from './AdminUsersPanel'

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

type ResourceType = 'VIDEO' | 'IMAGE' | 'PROJECT_ZIP' | 'EBOOK'

type LessonResource = {
  id: number
  type: ResourceType
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

type Props = {
  token: string
  userName: string
  courses: Course[]
  onCoursesChanged: (courses: Course[]) => void
  onExit: () => void
}

type AdminSection =
  | 'dashboard'
  | 'courses'
  | 'modules'
  | 'lessons'
  | 'materials'
  | 'quiz'
  | 'users'
  | 'analytics'
  | 'settings'

const typeMeta: Record<ResourceType, {
  label: string
  short: string
  accept: string
  hint: string
  icon: typeof Video
}> = {
  VIDEO: {
    label: 'Vídeo',
    short: 'VIDEO',
    accept: '.mp4,.mov,.avi,.webm',
    hint: 'MP4, MOV, AVI ou WEBM · máximo 500 MB',
    icon: Video
  },
  IMAGE: {
    label: 'Imagem',
    short: 'IMG',
    accept: '.png,.jpg,.jpeg,.webp',
    hint: 'PNG, JPG, JPEG ou WEBP',
    icon: ImageIcon
  },
  PROJECT_ZIP: {
    label: 'Projeto .zip',
    short: 'ZIP',
    accept: '.zip',
    hint: 'Projeto completo compactado em ZIP',
    icon: FileArchive
  },
  EBOOK: {
    label: 'E-book/PDF',
    short: 'PDF',
    accept: '.pdf,.epub',
    hint: 'PDF ou EPUB',
    icon: FileText
  }
}

async function request<T>(
  path: string,
  token: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Authorization', `Bearer ${token}`)

  if (!(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(path, { ...options, headers })

  if (!response.ok) {
    const raw = await response.text()
    let payload: { detail?: string; message?: string } | null = null

    try {
      payload = raw ? JSON.parse(raw) : null
    } catch {
      payload = null
    }

    throw new Error(
      payload?.detail ||
      payload?.message ||
      raw ||
      `HTTP ${response.status} — operação não concluída`
    )
  }

  if (response.status === 204) return undefined as T
  return response.json()
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export default function AdminStudio({
  token,
  userName,
  courses,
  onCoursesChanged,
  onExit
}: Props) {
  const [selectedCourseId, setSelectedCourseId] = useState<number | null>(
    courses[0]?.id ?? null
  )
  const [curriculum, setCurriculum] = useState<CourseCurriculum | null>(null)
  const [selectedModuleId, setSelectedModuleId] = useState<number | null>(null)
  const [selectedLessonId, setSelectedLessonId] = useState<number | null>(null)
  const [resourceType, setResourceType] = useState<ResourceType>('VIDEO')
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [message, setMessage] = useState('')
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [activeSection, setActiveSection] = useState<AdminSection>('dashboard')
  const [usersRefreshKey, setUsersRefreshKey] = useState(0)

  useEffect(() => {
    if (!selectedCourseId && courses.length > 0) {
      setSelectedCourseId(courses[0].id)
    }
  }, [courses, selectedCourseId])

  async function loadCurriculum(courseId: number, keepSelection = false) {
    setLoading(true)
    try {
      const data = await request<CourseCurriculum>(
        `/api/v1/courses/${courseId}/curriculum`,
        token
      )
      setCurriculum(data)

      const nextModule =
        keepSelection && data.modules.some(module => module.id === selectedModuleId)
          ? data.modules.find(module => module.id === selectedModuleId) ?? null
          : data.modules[0] ?? null

      setSelectedModuleId(nextModule?.id ?? null)

      const lessonStillExists =
        keepSelection &&
        nextModule?.lessons.some(lesson => lesson.id === selectedLessonId)

      setSelectedLessonId(
        lessonStillExists
          ? selectedLessonId
          : nextModule?.lessons[0]?.id ?? null
      )
    } catch (error) {
      setCurriculum(null)
      setSelectedModuleId(null)
      setSelectedLessonId(null)
      setMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível carregar a estrutura do curso.'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (selectedCourseId) {
      void loadCurriculum(selectedCourseId)
    }
  }, [selectedCourseId])

  const selectedCourse = useMemo(
    () => courses.find(course => course.id === selectedCourseId) ?? null,
    [courses, selectedCourseId]
  )

  const selectedModule = useMemo(
    () =>
      curriculum?.modules.find(module => module.id === selectedModuleId) ?? null,
    [curriculum, selectedModuleId]
  )

  const selectedLesson = useMemo(
    () =>
      selectedModule?.lessons.find(lesson => lesson.id === selectedLessonId) ?? null,
    [selectedModule, selectedLessonId]
  )

  const totalMaterials = useMemo(
    () =>
      curriculum?.modules.reduce(
        (total, module) =>
          total +
          module.lessons.reduce(
            (lessonTotal, lesson) => lessonTotal + lesson.resources.length,
            0
          ),
        0
      ) ?? 0,
    [curriculum]
  )

  const totalPlannedLessons = useMemo(
    () => courses.reduce((total, course) => total + course.totalLessons, 0),
    [courses]
  )

  useEffect(() => {
    let objectUrl: string | null = null
    let cancelled = false

    async function loadPreview() {
      setPreviewUrl(null)
      if (!selectedLesson) return

      const video = selectedLesson.resources.find(resource => resource.type === 'VIDEO')
      if (!video) return

      try {
        const response = await fetch(
          `/api/v1/learning/resources/${video.id}/download`,
          {
            headers: {
              Authorization: `Bearer ${token}`
            }
          }
        )

        if (!response.ok) return

        const blob = await response.blob()
        objectUrl = URL.createObjectURL(blob)

        if (!cancelled) setPreviewUrl(objectUrl)
      } catch {
        // Prévia é opcional. A listagem de materiais continua funcional.
      }
    }

    void loadPreview()

    return () => {
      cancelled = true
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [selectedLessonId, selectedLesson?.resources, token])

  async function refreshStructure() {
    const updatedCourses = await request<Course[]>('/api/v1/courses', token)
    onCoursesChanged(updatedCourses)

    if (updatedCourses.length === 0) {
      setSelectedCourseId(null)
      setSelectedModuleId(null)
      setSelectedLessonId(null)
      setCurriculum(null)
      return
    }

    const nextCourseId =
      selectedCourseId &&
      updatedCourses.some(course => course.id === selectedCourseId)
        ? selectedCourseId
        : updatedCourses[0].id

    if (nextCourseId !== selectedCourseId) {
      setSelectedCourseId(nextCourseId)
      return
    }

    await loadCurriculum(nextCourseId, false)
  }

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedLesson) {
      setMessage('Selecione uma aula antes de adicionar um material.')
      return
    }

    const form = event.currentTarget
    const data = new FormData(form)
    data.set('type', resourceType)

    setUploading(true)
    setMessage('')

    try {
      await request<LessonResource>(
        `/api/v1/admin/lessons/${selectedLesson.id}/resources`,
        token,
        {
          method: 'POST',
          body: data
        }
      )

      form.reset()
      await loadCurriculum(curriculum!.courseId, true)
      setMessage('Material publicado na aula com sucesso.')
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível enviar o material.'
      )
    } finally {
      setUploading(false)
    }
  }

  async function removeResource(resource: LessonResource) {
    const confirmed = window.confirm(
      `Remover "${resource.title}" desta aula?`
    )
    if (!confirmed || !curriculum) return

    try {
      await request<void>(
        `/api/v1/admin/resources/${resource.id}`,
        token,
        { method: 'DELETE' }
      )
      await loadCurriculum(curriculum.courseId, true)
      setMessage('Material removido.')
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível remover o material.'
      )
    }
  }

  async function downloadResource(resource: LessonResource) {
    try {
      const response = await fetch(
        `/api/v1/learning/resources/${resource.id}/download`,
        {
          headers: {
            Authorization: `Bearer ${token}`
          }
        }
      )

      if (!response.ok) throw new Error(`HTTP ${response.status}`)

      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = resource.fileName
      document.body.appendChild(anchor)
      anchor.click()
      anchor.remove()
      URL.revokeObjectURL(url)
    } catch {
      setMessage('Não foi possível baixar o material.')
    }
  }

  function navigateTo(section: AdminSection, targetId: string, notice?: string) {
    setActiveSection(section)
    if (section === 'users') setUsersRefreshKey(value => value + 1)
    if (notice) setMessage(notice)

    window.requestAnimationFrame(() => {
      document.getElementById(targetId)?.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      })
    })
  }

  function selectCourse(value: string) {
    setSelectedCourseId(Number(value))
    setSelectedModuleId(null)
    setSelectedLessonId(null)
    setMessage('')
  }

  function selectModule(value: string) {
    const id = Number(value)
    setSelectedModuleId(id)
    const module = curriculum?.modules.find(item => item.id === id)
    setSelectedLessonId(module?.lessons[0]?.id ?? null)
  }

  const ActiveIcon = typeMeta[resourceType].icon

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-brand">
          <img src="/techmind-logo.png" alt="TechMind AI Academy" />
          <div>
            <span>TECHMIND</span>
            <strong>AI Academy</strong>
          </div>
        </div>

        <nav>
          <button
            className={activeSection === 'dashboard' ? 'active' : ''}
            onClick={() => navigateTo('dashboard', 'admin-dashboard')}
          >
            <LayoutDashboard size={19} /> Dashboard
          </button>
          <button
            className={activeSection === 'courses' ? 'active' : ''}
            onClick={() => navigateTo('courses', 'admin-structure')}
          >
            <BookOpen size={19} /> Trilhas
          </button>
          <button
            className={activeSection === 'modules' ? 'active' : ''}
            onClick={() => navigateTo('modules', 'admin-structure')}
          >
            <ListVideo size={19} /> Módulos
          </button>
          <button
            className={activeSection === 'lessons' ? 'active' : ''}
            onClick={() => navigateTo('lessons', 'admin-lessons')}
          >
            <Play size={19} /> Aulas
          </button>
          <button
            className={activeSection === 'materials' ? 'active' : ''}
            onClick={() => navigateTo('materials', 'admin-materials')}
          >
            <FileText size={19} /> Materiais
          </button>
          <button
            className={activeSection === 'quiz' ? 'active' : ''}
            onClick={() => navigateTo('quiz', 'admin-quiz')}
          >
            <Sparkles size={19} /> Quiz
          </button>
          <button
            className={activeSection === 'users' ? 'active' : ''}
            onClick={() => navigateTo('users', 'admin-users')}
          >
            <Users size={19} /> Usuários
          </button>
          <button
            className={activeSection === 'analytics' ? 'active' : ''}
            onClick={() =>
              navigateTo(
                'analytics',
                'admin-analytics',
                'Analytics já possui a área preparada para os indicadores da plataforma.'
              )
            }
          >
            <BarChart3 size={19} /> Analytics
          </button>
          <button
            className={activeSection === 'settings' ? 'active' : ''}
            onClick={() =>
              navigateTo(
                'settings',
                'admin-settings',
                'Configurações da plataforma estão preparadas para a próxima etapa.'
              )
            }
          >
            <Settings size={19} /> Configurações
          </button>
        </nav>

        <div className="admin-plan">
          <Sparkles size={19} />
          <div>
            <strong>Admin Studio</strong>
            <span>Gestão do conteúdo da plataforma</span>
          </div>
        </div>
      </aside>

      <main className="admin-main">
        <header className="admin-header admin-scroll-target" id="admin-dashboard">
          <div>
            <span className="admin-kicker">TECHMIND CONTENT MANAGEMENT</span>
            <h1>Admin Studio</h1>
            <p>Gerencie vídeos, imagens, projetos e e-books das aulas.</p>
          </div>

          <div className="admin-header-actions">
            <button className="admin-preview-button" onClick={onExit}>
              <Eye size={17} /> Ver como aluno
            </button>
            <div className="admin-user">
              <span>{userName.slice(0, 2).toUpperCase()}</span>
              <div>
                <strong>{userName}</strong>
                <small>Administrador</small>
              </div>
            </div>
          </div>
        </header>

        <section className="admin-stats">
          <article>
            <BookOpen size={25} />
            <div><span>Trilhas</span><strong>{courses.length}</strong></div>
          </article>
          <article>
            <Play size={25} />
            <div><span>Aulas planejadas</span><strong>{totalPlannedLessons}</strong></div>
          </article>
          <article>
            <FileText size={25} />
            <div><span>Materiais no curso</span><strong>{totalMaterials}</strong></div>
          </article>
          <article>
            <ActiveIcon size={25} />
            <div><span>Tipo ativo</span><strong>{typeMeta[resourceType].label}</strong></div>
          </article>
        </section>

        {message && <div className="admin-notice">{message}</div>}

        <AdminStructureManager
          token={token}
          selectedCourse={selectedCourse}
          selectedModule={selectedModule}
          selectedLesson={selectedLesson}
          onChanged={refreshStructure}
          onMessage={setMessage}
        />

        <AdminQuizEditor
          token={token}
          lesson={selectedLesson}
          onMessage={setMessage}
        />

        <div className="admin-workspace">
          <div className="admin-left-column">
            <section className="admin-card admin-structure admin-scroll-target" id="admin-lessons">
              <div className="admin-step-title">
                <span>1</span>
                <div>
                  <h2>Selecione a aula</h2>
                  <p>Defina onde o conteúdo será adicionado.</p>
                </div>
              </div>

              <div className="admin-select-grid">
                <label>
                  <span>Trilha</span>
                  <select
                    value={selectedCourseId ?? ''}
                    onChange={event => selectCourse(event.target.value)}
                  >
                    {courses.map(course => (
                      <option value={course.id} key={course.id}>
                        {course.title}
                      </option>
                    ))}
                  </select>
                </label>

                <ChevronRight className="admin-chevron" size={20} />

                <label>
                  <span>Módulo</span>
                  <select
                    value={selectedModuleId ?? ''}
                    onChange={event => selectModule(event.target.value)}
                    disabled={loading || !curriculum}
                  >
                    {curriculum?.modules.map(module => (
                      <option value={module.id} key={module.id}>
                        {module.title}
                      </option>
                    ))}
                  </select>
                </label>

                <ChevronRight className="admin-chevron" size={20} />

                <label>
                  <span>Aula</span>
                  <select
                    value={selectedLessonId ?? ''}
                    onChange={event => setSelectedLessonId(Number(event.target.value))}
                    disabled={!selectedModule}
                  >
                    {selectedModule?.lessons.map(lesson => (
                      <option value={lesson.id} key={lesson.id}>
                        {lesson.title}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              {loading && (
                <div className="admin-loading">
                  <LoaderCircle size={18} /> Carregando estrutura...
                </div>
              )}
            </section>

            <section className="admin-card admin-editor admin-scroll-target" id="admin-materials">
              <div className="admin-step-title">
                <span>2</span>
                <div>
                  <h2>Adicionar conteúdo à aula</h2>
                  <p>Envie e configure o material disponível aos alunos.</p>
                </div>
              </div>

              <div className="admin-type-tabs">
                {(Object.keys(typeMeta) as ResourceType[]).map(type => {
                  const MetaIcon = typeMeta[type].icon
                  return (
                    <button
                      type="button"
                      className={resourceType === type ? 'active' : ''}
                      onClick={() => setResourceType(type)}
                      key={type}
                    >
                      <MetaIcon size={18} />
                      {typeMeta[type].label}
                    </button>
                  )
                })}
              </div>

              <form onSubmit={handleUpload}>
                <label className="admin-dropzone">
                  <UploadCloud size={34} />
                  <strong>
                    Arraste e solte {resourceType === 'VIDEO' ? 'o vídeo' : 'o arquivo'} aqui
                  </strong>
                  <span>ou clique para selecionar</span>
                  <small>{typeMeta[resourceType].hint}</small>
                  <input
                    name="file"
                    type="file"
                    accept={typeMeta[resourceType].accept}
                    required
                  />
                </label>

                <div className="admin-form-grid">
                  <label>
                    <span>Título *</span>
                    <input
                      name="title"
                      placeholder={
                        resourceType === 'VIDEO'
                          ? 'Ex.: Introdução ao Java 21'
                          : 'Ex.: Projeto completo da aula'
                      }
                      maxLength={200}
                      required
                    />
                  </label>

                  <label>
                    <span>Tipo</span>
                    <div className="admin-readonly-field">
                      <ActiveIcon size={17} /> {typeMeta[resourceType].label}
                    </div>
                  </label>
                </div>

                <label className="admin-description">
                  <span>Descrição</span>
                  <textarea
                    name="description"
                    placeholder="Explique o que o aluno encontrará neste material."
                    maxLength={500}
                    rows={4}
                  />
                </label>

                <div className="admin-editor-footer">
                  <span>
                    {selectedLesson
                      ? `Publicando em: ${selectedLesson.title}`
                      : 'Selecione uma aula'}
                  </span>

                  <button
                    className="admin-save"
                    disabled={uploading || !selectedLesson}
                  >
                    {uploading ? (
                      <><LoaderCircle className="spin" size={18} /> Enviando...</>
                    ) : (
                      <><Save size={18} /> Salvar material</>
                    )}
                  </button>
                </div>
              </form>
            </section>
          </div>

          <div className="admin-right-column">
            <section className="admin-card">
              <div className="admin-panel-title">
                <div className="admin-step-title compact">
                  <span>3</span>
                  <div>
                    <h2>Materiais da aula</h2>
                    <p>{selectedLesson?.resources.length ?? 0} conteúdo(s) disponíveis.</p>
                  </div>
                </div>
                <button className="admin-add-shortcut" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
                  <Plus size={16} /> Adicionar
                </button>
              </div>

              <div className="admin-material-list">
                {selectedLesson?.resources.length === 0 && (
                  <div className="admin-empty">
                    <UploadCloud size={28} />
                    <strong>Nenhum material anexado</strong>
                    <span>Use o formulário ao lado para publicar o primeiro conteúdo.</span>
                  </div>
                )}

                {selectedLesson?.resources.map(resource => {
                  const meta = typeMeta[resource.type]
                  const Icon = meta.icon

                  return (
                    <article className="admin-material-row" key={resource.id}>
                      <div className={`admin-resource-icon ${resource.type.toLowerCase()}`}>
                        <Icon size={20} />
                      </div>

                      <div className="admin-material-copy">
                        <strong>{resource.title}</strong>
                        <span>{meta.short} · {formatBytes(resource.sizeBytes)}</span>
                      </div>

                      <button
                        title="Baixar"
                        onClick={() => downloadResource(resource)}
                      >
                        <Download size={17} />
                      </button>
                      <button
                        className="danger"
                        title="Excluir"
                        onClick={() => removeResource(resource)}
                      >
                        <Trash2 size={17} />
                      </button>
                    </article>
                  )
                })}
              </div>
            </section>

            <section className="admin-card admin-preview">
              <div className="admin-panel-title">
                <div>
                  <span className="admin-kicker">PRÉVIA DA AULA</span>
                  <h2>{selectedLesson?.title ?? 'Selecione uma aula'}</h2>
                </div>
                <button className="admin-preview-button small" onClick={onExit}>
                  <Eye size={15} /> Abrir no aluno
                </button>
              </div>

              {previewUrl ? (
                <video controls src={previewUrl} className="admin-video-preview" />
              ) : (
                <div className="admin-video-placeholder">
                  <Play size={38} />
                  <strong>
                    {selectedLesson?.resources.some(resource => resource.type === 'VIDEO')
                      ? 'Carregando prévia do vídeo'
                      : 'Adicione um vídeo para visualizar a prévia'}
                  </strong>
                </div>
              )}

              <div className="admin-preview-copy">
                <strong>{selectedLesson?.title ?? 'Aula'}</strong>
                <p>
                  {selectedLesson?.summary ??
                    'O resumo da aula será exibido aqui para conferência.'}
                </p>
                <div>
                  <span>Conteúdo</span>
                  <span>Materiais ({selectedLesson?.resources.length ?? 0})</span>
                  <span>+{selectedLesson?.xpReward ?? 0} XP</span>
                </div>
              </div>
            </section>
          </div>
        </div>

        <AdminUsersPanel
          token={token}
          refreshKey={usersRefreshKey}
          onMessage={setMessage}
        />

        <section className="admin-module-grid admin-module-grid-secondary">
          <article className="admin-card admin-module-placeholder admin-scroll-target" id="admin-analytics">
            <BarChart3 size={24} />
            <div>
              <span className="admin-kicker">ANALYTICS</span>
              <h2>Indicadores da plataforma</h2>
              <p>Área preparada para progresso, aprovação em quizzes, XP e desempenho por trilha.</p>
            </div>
          </article>

          <article className="admin-card admin-module-placeholder admin-scroll-target" id="admin-settings">
            <Settings size={24} />
            <div>
              <span className="admin-kicker">CONFIGURAÇÕES</span>
              <h2>Configurações do ambiente</h2>
              <p>Área reservada para preferências, publicação, armazenamento e parâmetros da Academy.</p>
            </div>
          </article>
        </section>

        <button className="admin-mobile-exit" onClick={onExit}>
          <LogOut size={17} /> Voltar ao aluno
        </button>
      </main>
    </div>
  )
}
