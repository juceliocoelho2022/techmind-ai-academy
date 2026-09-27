import { FormEvent, useEffect, useState } from 'react'
import {
  BookOpen,
  FilePenLine,
  Layers3,
  Plus,
  Save,
  Trash2,
  X
} from 'lucide-react'
import './admin-structure-manager.css'

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

type Lesson = {
  id: number
  slug: string
  title: string
  summary: string
  position: number
  xpReward: number
}

type LearningModule = {
  id: number
  title: string
  description: string
  position: number
  lessons: Lesson[]
}

type EntityType = 'COURSE' | 'MODULE' | 'LESSON'
type FormMode = 'CREATE' | 'EDIT'

type Props = {
  token: string
  selectedCourse: Course | null
  selectedModule: LearningModule | null
  selectedLesson: Lesson | null
  defaultLessonXp: number
  onChanged: () => Promise<void>
  onMessage: (message: string) => void
}

async function adminRequest<T>(
  path: string,
  token: string,
  options: RequestInit
): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Authorization', `Bearer ${token}`)
  headers.set('Content-Type', 'application/json')

  const response = await fetch(path, { ...options, headers })

  if (!response.ok) {
    const raw = await response.text()
    let payload: { detail?: string; message?: string; title?: string } | null = null

    try {
      payload = raw ? JSON.parse(raw) : null
    } catch {
      payload = null
    }

    throw new Error(
      payload?.detail ||
      payload?.message ||
      payload?.title ||
      raw ||
      `HTTP ${response.status} — operação não concluída`
    )
  }

  if (response.status === 204) return undefined as T
  return response.json()
}

export default function AdminStructureManager({
  token,
  selectedCourse,
  selectedModule,
  selectedLesson,
  defaultLessonXp,
  onChanged,
  onMessage
}: Props) {
  const [entity, setEntity] = useState<EntityType | null>(null)
  const [mode, setMode] = useState<FormMode>('CREATE')
  const [busy, setBusy] = useState(false)

  function openForm(nextEntity: EntityType, nextMode: FormMode) {
    setEntity(nextEntity)
    setMode(nextMode)
  }

  function closeForm() {
    setEntity(null)
    setMode('CREATE')
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!entity) return

    const form = event.currentTarget
    const data = new FormData(form)
    setBusy(true)

    try {
      if (entity === 'COURSE') {
        const payload = {
          slug: String(data.get('slug') ?? ''),
          title: String(data.get('title') ?? ''),
          description: String(data.get('description') ?? ''),
          category: String(data.get('category') ?? ''),
          technology: String(data.get('technology') ?? ''),
          level: String(data.get('level') ?? 'INTERMEDIATE'),
          totalLessons: Number(data.get('totalLessons') ?? 0)
        }

        const path =
          mode === 'CREATE'
            ? '/api/v1/admin/content/courses'
            : `/api/v1/admin/content/courses/${selectedCourse?.id}`

        await adminRequest(path, token, {
          method: mode === 'CREATE' ? 'POST' : 'PUT',
          body: JSON.stringify(payload)
        })
      }

      if (entity === 'MODULE') {
        if (!selectedCourse) throw new Error('Selecione uma trilha primeiro.')

        const positionValue = String(data.get('position') ?? '').trim()
        const payload = {
          title: String(data.get('title') ?? ''),
          description: String(data.get('description') ?? ''),
          position: positionValue ? Number(positionValue) : null
        }

        const path =
          mode === 'CREATE'
            ? `/api/v1/admin/content/courses/${selectedCourse.id}/modules`
            : `/api/v1/admin/content/modules/${selectedModule?.id}`

        await adminRequest(path, token, {
          method: mode === 'CREATE' ? 'POST' : 'PUT',
          body: JSON.stringify(payload)
        })
      }

      if (entity === 'LESSON') {
        if (!selectedModule) throw new Error('Selecione um módulo primeiro.')

        const positionValue = String(data.get('position') ?? '').trim()
        const xpValue = String(data.get('xpReward') ?? '').trim()

        const payload = {
          slug: String(data.get('slug') ?? ''),
          title: String(data.get('title') ?? ''),
          summary: String(data.get('summary') ?? ''),
          position: positionValue ? Number(positionValue) : null,
          xpReward: xpValue ? Number(xpValue) : defaultLessonXp
        }

        const path =
          mode === 'CREATE'
            ? `/api/v1/admin/content/modules/${selectedModule.id}/lessons`
            : `/api/v1/admin/content/lessons/${selectedLesson?.id}`

        await adminRequest(path, token, {
          method: mode === 'CREATE' ? 'POST' : 'PUT',
          body: JSON.stringify(payload)
        })
      }

      await onChanged()
      onMessage(
        mode === 'CREATE'
          ? 'Estrutura criada com sucesso.'
          : 'Estrutura atualizada com sucesso.'
      )
      closeForm()
    } catch (error) {
      onMessage(
        error instanceof Error ? error.message : 'Não foi possível salvar a estrutura.'
      )
    } finally {
      setBusy(false)
    }
  }

  async function remove(nextEntity: EntityType) {
    const target =
      nextEntity === 'COURSE'
        ? selectedCourse
        : nextEntity === 'MODULE'
          ? selectedModule
          : selectedLesson

    if (!target) {
      onMessage('Selecione um item antes de excluir.')
      return
    }

    const label =
      nextEntity === 'COURSE'
        ? selectedCourse?.title
        : nextEntity === 'MODULE'
          ? selectedModule?.title
          : selectedLesson?.title

    if (!window.confirm(`Excluir "${label}"? Esta ação não pode ser desfeita.`)) {
      return
    }

    const path =
      nextEntity === 'COURSE'
        ? `/api/v1/admin/content/courses/${selectedCourse!.id}`
        : nextEntity === 'MODULE'
          ? `/api/v1/admin/content/modules/${selectedModule!.id}`
          : `/api/v1/admin/content/lessons/${selectedLesson!.id}`

    setBusy(true)

    try {
      await adminRequest<void>(path, token, { method: 'DELETE' })
      await onChanged()
      onMessage('Item excluído com sucesso.')
      closeForm()
    } catch (error) {
      onMessage(
        error instanceof Error ? error.message : 'Não foi possível excluir o item.'
      )
    } finally {
      setBusy(false)
    }
  }

  const title =
    entity === 'COURSE'
      ? mode === 'CREATE'
        ? 'Nova trilha'
        : 'Editar trilha'
      : entity === 'MODULE'
        ? mode === 'CREATE'
          ? 'Novo módulo'
          : 'Editar módulo'
        : mode === 'CREATE'
          ? 'Nova aula'
          : 'Editar aula'

  return (
    <section className="admin-card structure-manager" id="admin-structure">
      <div className="structure-manager-heading">
        <div>
          <span className="admin-kicker">ESTRUTURA ACADÊMICA</span>
          <h2>Gerencie trilhas, módulos e aulas</h2>
          <p>Crie e organize o conteúdo sem alterar migrations ou código.</p>
        </div>
      </div>

      <div className="structure-entities">
        <article>
          <div className="structure-entity-title">
            <BookOpen size={19} />
            <div>
              <span>Trilha</span>
              <strong>{selectedCourse?.title ?? 'Nenhuma selecionada'}</strong>
            </div>
          </div>
          <div className="structure-actions">
            <button onClick={() => openForm('COURSE', 'CREATE')}>
              <Plus size={14} /> Nova
            </button>
            <button
              disabled={!selectedCourse}
              onClick={() => openForm('COURSE', 'EDIT')}
            >
              <FilePenLine size={14} /> Editar
            </button>
            <button
              className="danger"
              disabled={!selectedCourse || busy}
              onClick={() => remove('COURSE')}
            >
              <Trash2 size={14} />
            </button>
          </div>
        </article>

        <article>
          <div className="structure-entity-title">
            <Layers3 size={19} />
            <div>
              <span>Módulo</span>
              <strong>{selectedModule?.title ?? 'Nenhum selecionado'}</strong>
            </div>
          </div>
          <div className="structure-actions">
            <button
              disabled={!selectedCourse}
              onClick={() => openForm('MODULE', 'CREATE')}
            >
              <Plus size={14} /> Novo
            </button>
            <button
              disabled={!selectedModule}
              onClick={() => openForm('MODULE', 'EDIT')}
            >
              <FilePenLine size={14} /> Editar
            </button>
            <button
              className="danger"
              disabled={!selectedModule || busy}
              onClick={() => remove('MODULE')}
            >
              <Trash2 size={14} />
            </button>
          </div>
        </article>

        <article>
          <div className="structure-entity-title">
            <PlayLessonIcon />
            <div>
              <span>Aula</span>
              <strong>{selectedLesson?.title ?? 'Nenhuma selecionada'}</strong>
            </div>
          </div>
          <div className="structure-actions">
            <button
              disabled={!selectedModule}
              onClick={() => openForm('LESSON', 'CREATE')}
            >
              <Plus size={14} /> Nova
            </button>
            <button
              disabled={!selectedLesson}
              onClick={() => openForm('LESSON', 'EDIT')}
            >
              <FilePenLine size={14} /> Editar
            </button>
            <button
              className="danger"
              disabled={!selectedLesson || busy}
              onClick={() => remove('LESSON')}
            >
              <Trash2 size={14} />
            </button>
          </div>
        </article>
      </div>

      {entity && (
        <div className="structure-form-panel">
          <div className="structure-form-title">
            <div>
              <span>{mode === 'CREATE' ? 'CADASTRO' : 'EDIÇÃO'}</span>
              <h3>{title}</h3>
            </div>
            <button type="button" onClick={closeForm}>
              <X size={18} />
            </button>
          </div>

          <StructureForm
            key={`${entity}-${mode}-${selectedCourse?.id}-${selectedModule?.id}-${selectedLesson?.id}`}
            entity={entity}
            mode={mode}
            course={selectedCourse}
            module={selectedModule}
            lesson={selectedLesson}
            defaultLessonXp={defaultLessonXp}
            busy={busy}
            onSubmit={submit}
          />
        </div>
      )}
    </section>
  )
}

function PlayLessonIcon() {
  return (
    <div className="structure-play-icon" aria-hidden="true">
      ▶
    </div>
  )
}

type StructureFormProps = {
  entity: EntityType
  mode: FormMode
  course: Course | null
  module: LearningModule | null
  lesson: Lesson | null
  defaultLessonXp: number
  busy: boolean
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

function StructureForm({
  entity,
  mode,
  course,
  module,
  lesson,
  defaultLessonXp,
  busy,
  onSubmit
}: StructureFormProps) {
  useEffect(() => {}, [])

  if (entity === 'COURSE') {
    return (
      <form className="structure-form" onSubmit={onSubmit}>
        <label>
          <span>Slug *</span>
          <input
            name="slug"
            defaultValue={mode === 'EDIT' ? course?.slug : ''}
            placeholder="ex.: java-backend"
            pattern="[a-z0-9]+(-[a-z0-9]+)*"
            required
          />
        </label>
        <label>
          <span>Título *</span>
          <input
            name="title"
            defaultValue={mode === 'EDIT' ? course?.title : ''}
            placeholder="Ex.: Java Backend"
            maxLength={200}
            required
          />
        </label>
        <label className="wide">
          <span>Descrição *</span>
          <textarea
            name="description"
            defaultValue={mode === 'EDIT' ? course?.description : ''}
            maxLength={500}
            rows={3}
            required
          />
        </label>
        <label>
          <span>Categoria *</span>
          <input
            name="category"
            defaultValue={mode === 'EDIT' ? course?.category : ''}
            placeholder="Ex.: Backend"
            maxLength={80}
            required
          />
        </label>
        <label>
          <span>Tecnologia *</span>
          <input
            name="technology"
            defaultValue={mode === 'EDIT' ? course?.technology : ''}
            placeholder="Ex.: Java"
            maxLength={80}
            required
          />
        </label>
        <label>
          <span>Nível *</span>
          <select
            name="level"
            defaultValue={mode === 'EDIT' ? course?.level ?? 'INTERMEDIATE' : 'BEGINNER'}
            required
          >
            <option value="BEGINNER">Iniciante</option>
            <option value="INTERMEDIATE">Intermediário</option>
            <option value="ADVANCED">Avançado</option>
          </select>
        </label>
        <label>
          <span>Total planejado de aulas</span>
          <input
            name="totalLessons"
            type="number"
            min={0}
            defaultValue={mode === 'EDIT' ? course?.totalLessons ?? 0 : 0}
          />
        </label>
        <StructureSaveButton busy={busy} />
      </form>
    )
  }

  if (entity === 'MODULE') {
    return (
      <form className="structure-form" onSubmit={onSubmit}>
        <label>
          <span>Título *</span>
          <input
            name="title"
            defaultValue={mode === 'EDIT' ? module?.title : ''}
            placeholder="Ex.: Fundamentos de Java"
            maxLength={200}
            required
          />
        </label>
        <label>
          <span>Posição</span>
          <input
            name="position"
            type="number"
            min={1}
            defaultValue={mode === 'EDIT' ? module?.position : ''}
            placeholder="Automática"
          />
        </label>
        <label className="wide">
          <span>Descrição *</span>
          <textarea
            name="description"
            defaultValue={mode === 'EDIT' ? module?.description : ''}
            maxLength={500}
            rows={3}
            required
          />
        </label>
        <StructureSaveButton busy={busy} />
      </form>
    )
  }

  return (
    <form className="structure-form" onSubmit={onSubmit}>
      <label>
        <span>Slug *</span>
        <input
          name="slug"
          defaultValue={mode === 'EDIT' ? lesson?.slug : ''}
          placeholder="ex.: collections-streams"
          pattern="[a-z0-9]+(-[a-z0-9]+)*"
          required
        />
      </label>
      <label>
        <span>Título *</span>
        <input
          name="title"
          defaultValue={mode === 'EDIT' ? lesson?.title : ''}
          placeholder="Ex.: Collections e Streams"
          maxLength={200}
          required
        />
      </label>
      <label className="wide">
        <span>Resumo *</span>
        <textarea
          name="summary"
          defaultValue={mode === 'EDIT' ? lesson?.summary : ''}
          maxLength={500}
          rows={3}
          required
        />
      </label>
      <label>
        <span>Posição</span>
        <input
          name="position"
          type="number"
          min={1}
          defaultValue={mode === 'EDIT' ? lesson?.position : ''}
          placeholder="Automática"
        />
      </label>
      <label>
        <span>XP da aula</span>
        <input
          name="xpReward"
          type="number"
          min={0}
          defaultValue={mode === 'EDIT' ? lesson?.xpReward ?? 10 : 10}
        />
      </label>
      <StructureSaveButton busy={busy} />
    </form>
  )
}

function StructureSaveButton({ busy }: { busy: boolean }) {
  return (
    <div className="structure-form-actions">
      <button className="admin-save" disabled={busy}>
        <Save size={17} />
        {busy ? 'Salvando...' : 'Salvar'}
      </button>
    </div>
  )
}
