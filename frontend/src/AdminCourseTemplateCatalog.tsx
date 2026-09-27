import { FormEvent, useEffect, useMemo, useState } from 'react'
import {
  BookOpen,
  BrainCircuit,
  CheckCircle2,
  Cloud,
  Code2,
  Database,
  Layers3,
  Search,
  Sparkles,
  X
} from 'lucide-react'
import './admin-course-template-catalog.css'

type CourseLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
type SubscriptionPlan = 'FREE' | 'PRO' | 'CAREER'

type Course = {
  id: number
  slug: string
  title: string
  description: string
  category: string
  technology: string
  level: CourseLevel
  requiredPlan: SubscriptionPlan
  totalLessons: number
}

type LessonTemplate = {
  slug: string
  title: string
  summary: string
  position: number
  xpReward: number
}

type ModuleTemplate = {
  title: string
  description: string
  position: number
  lessons: LessonTemplate[]
}

type CourseTemplate = {
  key: string
  category: string
  technology: string
  level: CourseLevel
  requiredPlan: SubscriptionPlan
  title: string
  slug: string
  description: string
  totalLessons: number
  modules: ModuleTemplate[]
}

type Props = {
  token: string
  onCreated: (course: Course) => Promise<void>
  onMessage: (message: string) => void
}

const levelLabel: Record<CourseLevel, string> = {
  BEGINNER: 'Iniciante',
  INTERMEDIATE: 'Intermediário',
  ADVANCED: 'Avançado'
}

function categoryIcon(category: string) {
  if (category === 'Banco de Dados') return Database
  if (category === 'Cloud') return Cloud
  if (category === 'Dados & IA') return BrainCircuit
  if (category === 'Frontend') return Code2
  if (category === 'Backend') return Code2
  return BookOpen
}

export default function AdminCourseTemplateCatalog({
  token,
  onCreated,
  onMessage
}: Props) {
  const [templates, setTemplates] = useState<CourseTemplate[]>([])
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('ALL')
  const [level, setLevel] = useState<'ALL' | CourseLevel>('ALL')
  const [selected, setSelected] = useState<CourseTemplate | null>(null)
  const [title, setTitle] = useState('')
  const [slug, setSlug] = useState('')
  const [description, setDescription] = useState('')
  const [selectedLevel, setSelectedLevel] = useState<CourseLevel>('BEGINNER')
  const [selectedRequiredPlan, setSelectedRequiredPlan] =
    useState<SubscriptionPlan>('FREE')
  const [createStructure, setCreateStructure] = useState(true)
  const [loading, setLoading] = useState(false)
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    void loadTemplates()
  }, [token])

  async function loadTemplates() {
    setLoading(true)

    try {
      const response = await fetch('/api/v1/admin/course-templates', {
        headers: { Authorization: `Bearer ${token}` }
      })

      if (!response.ok) {
        throw new Error(await response.text())
      }

      setTemplates(await response.json())
    } catch (error) {
      onMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível carregar o catálogo de templates.'
      )
    } finally {
      setLoading(false)
    }
  }

  const categories = useMemo(
    () => Array.from(new Set(templates.map(template => template.category))).sort(),
    [templates]
  )

  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase()

    return templates.filter(template => {
      const matchesQuery =
        !normalized ||
        template.title.toLowerCase().includes(normalized) ||
        template.technology.toLowerCase().includes(normalized) ||
        template.category.toLowerCase().includes(normalized)

      const matchesCategory =
        category === 'ALL' || template.category === category

      const matchesLevel =
        level === 'ALL' || template.level === level

      return matchesQuery && matchesCategory && matchesLevel
    })
  }, [templates, query, category, level])

  function choose(template: CourseTemplate) {
    setSelected(template)
    setTitle(template.title)
    setSlug(template.slug)
    setDescription(template.description)
    setSelectedLevel(template.level)
    setSelectedRequiredPlan(template.requiredPlan)
    setCreateStructure(true)
  }

  function closeConfigurator() {
    setSelected(null)
  }

  async function createFromTemplate(event: FormEvent) {
    event.preventDefault()
    if (!selected) return

    setCreating(true)

    try {
      const response = await fetch(
        `/api/v1/admin/course-templates/${selected.key}/instantiate`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            title,
            slug,
            description,
            level: selectedLevel,
            requiredPlan: selectedRequiredPlan,
            createStructure
          })
        }
      )

      if (!response.ok) {
        const raw = await response.text()
        let detail = raw
        try {
          const payload = JSON.parse(raw)
          detail = payload.detail || payload.message || payload.title || raw
        } catch {
          // mantém resposta textual
        }
        throw new Error(detail)
      }

      const course: Course = await response.json()
      await onCreated(course)
      onMessage(
        createStructure
          ? `Trilha "${course.title}" criada com módulos e aulas iniciais.`
          : `Trilha "${course.title}" criada a partir do template.`
      )
      closeConfigurator()
    } catch (error) {
      onMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível criar a trilha pelo template.'
      )
    } finally {
      setCreating(false)
    }
  }

  function openCustomCourse() {
    document.getElementById('admin-structure')?.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    })
    onMessage('Para uma trilha totalmente personalizada, clique em "Nova" na Estrutura Acadêmica.')
  }

  return (
    <section
      className="admin-card course-template-catalog admin-scroll-target"
      id="admin-course-catalog"
    >
      <div className="course-template-header">
        <div>
          <span className="admin-kicker">CATÁLOGO DE TRILHAS</span>
          <h2>Crie um curso a partir de um template profissional</h2>
          <p>
            Escolha a tecnologia, personalize os dados e gere a estrutura inicial automaticamente.
          </p>
        </div>

        <button className="course-custom-button" type="button" onClick={openCustomCourse}>
          <Layers3 size={16} /> Curso personalizado
        </button>
      </div>

      <div className="course-template-toolbar">
        <label className="course-template-search">
          <Search size={16} />
          <input
            value={query}
            onChange={event => setQuery(event.target.value)}
            placeholder="Buscar Java, React, PostgreSQL, AWS..."
          />
        </label>

        <select value={category} onChange={event => setCategory(event.target.value)}>
          <option value="ALL">Todas as categorias</option>
          {categories.map(item => (
            <option value={item} key={item}>{item}</option>
          ))}
        </select>

        <select
          value={level}
          onChange={event =>
            setLevel(event.target.value as 'ALL' | CourseLevel)
          }
        >
          <option value="ALL">Todos os níveis</option>
          <option value="BEGINNER">Iniciante</option>
          <option value="INTERMEDIATE">Intermediário</option>
          <option value="ADVANCED">Avançado</option>
        </select>
      </div>

      {loading ? (
        <div className="course-template-empty">Carregando catálogo...</div>
      ) : (
        <div className="course-template-grid">
          {filtered.map(template => {
            const Icon = categoryIcon(template.category)

            return (
              <button
                className="course-template-card"
                type="button"
                key={template.key}
                onClick={() => choose(template)}
              >
                <div className="course-template-icon">
                  <Icon size={22} />
                </div>

                <div className="course-template-card-head">
                  <span>{template.category}</span>
                  <em>{levelLabel[template.level]}</em>
                </div>

                <strong>{template.title}</strong>
                <p>{template.description}</p>

                <div className="course-template-meta">
                  <span>{template.technology}</span>
                  <span>Plano {template.requiredPlan}</span>
                  <span>{template.modules.length} módulos</span>
                  <span>{template.totalLessons} aulas</span>
                </div>
              </button>
            )
          })}

          {filtered.length === 0 && (
            <div className="course-template-empty">
              Nenhum template encontrado com esses filtros.
            </div>
          )}
        </div>
      )}

      {selected && (
        <div className="course-template-config">
          <div className="course-template-config-head">
            <div>
              <span className="admin-kicker">CONFIGURAR TEMPLATE</span>
              <h3>{selected.title}</h3>
              <p>
                {selected.category} · {selected.technology} · {selected.modules.length} módulos
              </p>
            </div>

            <button type="button" onClick={closeConfigurator}>
              <X size={18} />
            </button>
          </div>

          <form onSubmit={createFromTemplate}>
            <div className="course-template-form-grid">
              <label>
                <span>Título *</span>
                <input
                  value={title}
                  onChange={event => setTitle(event.target.value)}
                  maxLength={200}
                  required
                />
              </label>

              <label>
                <span>Slug *</span>
                <input
                  value={slug}
                  onChange={event => setSlug(event.target.value)}
                  pattern="[a-z0-9]+(-[a-z0-9]+)*"
                  maxLength={120}
                  required
                />
              </label>

              <label>
                <span>Nível *</span>
                <select
                  value={selectedLevel}
                  onChange={event =>
                    setSelectedLevel(event.target.value as CourseLevel)
                  }
                >
                  <option value="BEGINNER">Iniciante</option>
                  <option value="INTERMEDIATE">Intermediário</option>
                  <option value="ADVANCED">Avançado</option>
                </select>
              </label>

              <label>
                <span>Plano mínimo *</span>
                <select
                  value={selectedRequiredPlan}
                  onChange={event =>
                    setSelectedRequiredPlan(event.target.value as SubscriptionPlan)
                  }
                >
                  <option value="FREE">FREE</option>
                  <option value="PRO">PRO</option>
                  <option value="CAREER">CAREER</option>
                </select>
              </label>

              <label className="wide">
                <span>Descrição *</span>
                <textarea
                  value={description}
                  onChange={event => setDescription(event.target.value)}
                  rows={3}
                  maxLength={500}
                  required
                />
              </label>
            </div>

            <div className="course-template-structure-preview">
              <div className="course-template-structure-title">
                <div>
                  <Sparkles size={18} />
                  <div>
                    <strong>Estrutura inicial</strong>
                    <span>
                      {selected.modules.length} módulos · {selected.totalLessons} aulas
                    </span>
                  </div>
                </div>

                <label>
                  <input
                    type="checkbox"
                    checked={createStructure}
                    onChange={event => setCreateStructure(event.target.checked)}
                  />
                  Criar automaticamente
                </label>
              </div>

              {createStructure && (
                <div className="course-template-module-preview">
                  {selected.modules.map(module => (
                    <article key={module.position}>
                      <div>
                        <span>{module.position}</span>
                        <strong>{module.title}</strong>
                      </div>
                      <small>
                        {module.lessons.map(lesson => lesson.title).join(' · ')}
                      </small>
                    </article>
                  ))}
                </div>
              )}
            </div>

            <div className="course-template-actions">
              <div>
                <CheckCircle2 size={16} />
                Categoria, tecnologia e plano mínimo serão vinculados à trilha.
              </div>

              <button className="admin-save" disabled={creating}>
                <Sparkles size={17} />
                {creating ? 'Criando trilha...' : 'Criar trilha com template'}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  )
}
