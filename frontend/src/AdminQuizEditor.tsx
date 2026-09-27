import { FormEvent, useEffect, useState } from 'react'
import {
  CheckCircle2,
  CirclePlus,
  ClipboardCheck,
  Plus,
  Save,
  Trash2,
  X
} from 'lucide-react'
import './admin-quiz-editor.css'

type Lesson = {
  id: number
  title: string
}

type OptionDraft = {
  clientId: string
  text: string
  correct: boolean
}

type QuestionDraft = {
  clientId: string
  prompt: string
  options: OptionDraft[]
}

type QuizDraft = {
  title: string
  description: string
  passingScore: number
  xpReward: number
  active: boolean
  questions: QuestionDraft[]
}

type Props = {
  token: string
  lesson: Lesson | null
  onMessage: (message: string) => void
}

function id() {
  return crypto.randomUUID()
}

function newQuestion(): QuestionDraft {
  return {
    clientId: id(),
    prompt: '',
    options: [
      { clientId: id(), text: '', correct: true },
      { clientId: id(), text: '', correct: false }
    ]
  }
}

function emptyQuiz(lesson?: Lesson | null): QuizDraft {
  return {
    title: lesson ? `Quiz — ${lesson.title}` : 'Quiz da aula',
    description: '',
    passingScore: 70,
    xpReward: 50,
    active: true,
    questions: [newQuestion()]
  }
}

export default function AdminQuizEditor({ token, lesson, onMessage }: Props) {
  const [quiz, setQuiz] = useState<QuizDraft>(() => emptyQuiz(lesson))
  const [exists, setExists] = useState(false)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!lesson) {
      setQuiz(emptyQuiz(null))
      setExists(false)
      return
    }

    void loadQuiz(lesson)
  }, [lesson?.id])

  async function loadQuiz(currentLesson: Lesson) {
    setLoading(true)

    try {
      const response = await fetch(
        `/api/v1/admin/lessons/${currentLesson.id}/quiz`,
        {
          headers: { Authorization: `Bearer ${token}` }
        }
      )

      if (response.status === 404) {
        setQuiz(emptyQuiz(currentLesson))
        setExists(false)
        return
      }

      if (!response.ok) {
        throw new Error(await response.text())
      }

      const data = await response.json()

      setQuiz({
        title: data.title,
        description: data.description ?? '',
        passingScore: data.passingScore,
        xpReward: data.xpReward,
        active: data.active,
        questions: data.questions.map((question: any) => ({
          clientId: id(),
          prompt: question.prompt,
          options: question.options.map((option: any) => ({
            clientId: id(),
            text: option.text,
            correct: option.correct
          }))
        }))
      })
      setExists(true)
    } catch (error) {
      onMessage(
        error instanceof Error ? error.message : 'Não foi possível carregar o quiz.'
      )
    } finally {
      setLoading(false)
    }
  }

  function updateQuestion(index: number, prompt: string) {
    setQuiz(current => ({
      ...current,
      questions: current.questions.map((question, questionIndex) =>
        questionIndex === index ? { ...question, prompt } : question
      )
    }))
  }

  function addQuestion() {
    setQuiz(current => ({
      ...current,
      questions: [...current.questions, newQuestion()]
    }))
  }

  function removeQuestion(index: number) {
    setQuiz(current => ({
      ...current,
      questions: current.questions.filter((_, questionIndex) => questionIndex !== index)
    }))
  }

  function addOption(questionIndex: number) {
    setQuiz(current => ({
      ...current,
      questions: current.questions.map((question, index) =>
        index === questionIndex
          ? {
              ...question,
              options: [
                ...question.options,
                { clientId: id(), text: '', correct: false }
              ]
            }
          : question
      )
    }))
  }

  function updateOption(questionIndex: number, optionIndex: number, text: string) {
    setQuiz(current => ({
      ...current,
      questions: current.questions.map((question, index) =>
        index === questionIndex
          ? {
              ...question,
              options: question.options.map((option, currentOptionIndex) =>
                currentOptionIndex === optionIndex ? { ...option, text } : option
              )
            }
          : question
      )
    }))
  }

  function markCorrect(questionIndex: number, optionIndex: number) {
    setQuiz(current => ({
      ...current,
      questions: current.questions.map((question, index) =>
        index === questionIndex
          ? {
              ...question,
              options: question.options.map((option, currentOptionIndex) => ({
                ...option,
                correct: currentOptionIndex === optionIndex
              }))
            }
          : question
      )
    }))
  }

  function removeOption(questionIndex: number, optionIndex: number) {
    setQuiz(current => ({
      ...current,
      questions: current.questions.map((question, index) =>
        index === questionIndex
          ? {
              ...question,
              options: question.options.filter(
                (_, currentOptionIndex) => currentOptionIndex !== optionIndex
              )
            }
          : question
      )
    }))
  }

  async function save(event: FormEvent) {
    event.preventDefault()
    if (!lesson) return

    if (quiz.questions.length === 0) {
      onMessage('Adicione pelo menos uma questão.')
      return
    }

    const invalid = quiz.questions.find(
      question =>
        !question.prompt.trim() ||
        question.options.length < 2 ||
        question.options.some(option => !option.text.trim()) ||
        question.options.filter(option => option.correct).length !== 1
    )

    if (invalid) {
      onMessage('Cada questão precisa de enunciado, duas alternativas e exatamente uma correta.')
      return
    }

    setSaving(true)

    try {
      const response = await fetch(
        `/api/v1/admin/lessons/${lesson.id}/quiz`,
        {
          method: 'PUT',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            title: quiz.title,
            description: quiz.description || null,
            passingScore: quiz.passingScore,
            xpReward: quiz.xpReward,
            active: quiz.active,
            questions: quiz.questions.map(question => ({
              prompt: question.prompt,
              options: question.options.map(option => ({
                text: option.text,
                correct: option.correct
              }))
            }))
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

      setExists(true)
      onMessage('Quiz salvo e disponível para a aula.')
      await loadQuiz(lesson)
    } catch (error) {
      onMessage(
        error instanceof Error ? error.message : 'Não foi possível salvar o quiz.'
      )
    } finally {
      setSaving(false)
    }
  }

  async function removeQuiz() {
    if (!lesson || !exists) return
    if (!window.confirm('Excluir este quiz?')) return

    try {
      const response = await fetch(
        `/api/v1/admin/lessons/${lesson.id}/quiz`,
        {
          method: 'DELETE',
          headers: { Authorization: `Bearer ${token}` }
        }
      )

      if (!response.ok) {
        throw new Error(await response.text())
      }

      setExists(false)
      setQuiz(emptyQuiz(lesson))
      onMessage('Quiz removido.')
    } catch (error) {
      onMessage(
        error instanceof Error ? error.message : 'Não foi possível remover o quiz.'
      )
    }
  }

  return (
    <section className="admin-card quiz-admin" id="admin-quiz">
      <div className="quiz-admin-header">
        <div>
          <span className="admin-kicker">QUIZ ENGINE</span>
          <h2>Quiz da aula</h2>
          <p>
            {lesson
              ? `Avaliação vinculada a: ${lesson.title}`
              : 'Selecione uma aula para configurar a avaliação.'}
          </p>
        </div>
        <div className={`quiz-status ${exists ? 'published' : ''}`}>
          <ClipboardCheck size={16} />
          {exists ? 'Configurado' : 'Novo'}
        </div>
      </div>

      {!lesson ? (
        <div className="quiz-admin-empty">Selecione uma aula acima.</div>
      ) : loading ? (
        <div className="quiz-admin-empty">Carregando quiz...</div>
      ) : (
        <form onSubmit={save}>
          <div className="quiz-config-grid">
            <label>
              <span>Título *</span>
              <input
                value={quiz.title}
                onChange={event =>
                  setQuiz(current => ({ ...current, title: event.target.value }))
                }
                maxLength={200}
                required
              />
            </label>
            <label>
              <span>Nota mínima (%)</span>
              <input
                type="number"
                min={0}
                max={100}
                value={quiz.passingScore}
                onChange={event =>
                  setQuiz(current => ({
                    ...current,
                    passingScore: Number(event.target.value)
                  }))
                }
              />
            </label>
            <label>
              <span>XP por aprovação</span>
              <input
                type="number"
                min={0}
                value={quiz.xpReward}
                onChange={event =>
                  setQuiz(current => ({
                    ...current,
                    xpReward: Number(event.target.value)
                  }))
                }
              />
            </label>
            <label className="quiz-active-toggle">
              <input
                type="checkbox"
                checked={quiz.active}
                onChange={event =>
                  setQuiz(current => ({ ...current, active: event.target.checked }))
                }
              />
              <span>Quiz ativo para alunos</span>
            </label>
            <label className="wide">
              <span>Descrição</span>
              <textarea
                value={quiz.description}
                onChange={event =>
                  setQuiz(current => ({ ...current, description: event.target.value }))
                }
                rows={3}
                maxLength={500}
              />
            </label>
          </div>

          <div className="quiz-question-list">
            {quiz.questions.map((question, questionIndex) => (
              <article className="quiz-question-editor" key={question.clientId}>
                <div className="quiz-question-heading">
                  <strong>Questão {questionIndex + 1}</strong>
                  <button
                    type="button"
                    title="Remover questão"
                    disabled={quiz.questions.length === 1}
                    onClick={() => removeQuestion(questionIndex)}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>

                <textarea
                  className="quiz-prompt"
                  value={question.prompt}
                  onChange={event =>
                    updateQuestion(questionIndex, event.target.value)
                  }
                  placeholder="Digite o enunciado da questão"
                  rows={3}
                  maxLength={1000}
                  required
                />

                <div className="quiz-option-list">
                  {question.options.map((option, optionIndex) => (
                    <div className="quiz-option-editor" key={option.clientId}>
                      <button
                        className={option.correct ? 'correct' : ''}
                        type="button"
                        title="Marcar como resposta correta"
                        onClick={() => markCorrect(questionIndex, optionIndex)}
                      >
                        <CheckCircle2 size={18} />
                      </button>
                      <input
                        value={option.text}
                        onChange={event =>
                          updateOption(questionIndex, optionIndex, event.target.value)
                        }
                        placeholder={`Alternativa ${optionIndex + 1}`}
                        maxLength={500}
                        required
                      />
                      <button
                        type="button"
                        title="Remover alternativa"
                        disabled={question.options.length <= 2}
                        onClick={() => removeOption(questionIndex, optionIndex)}
                      >
                        <X size={16} />
                      </button>
                    </div>
                  ))}
                </div>

                <button
                  className="quiz-add-option"
                  type="button"
                  onClick={() => addOption(questionIndex)}
                >
                  <CirclePlus size={15} /> Adicionar alternativa
                </button>
              </article>
            ))}
          </div>

          <div className="quiz-admin-footer">
            <button className="quiz-add-question" type="button" onClick={addQuestion}>
              <Plus size={16} /> Nova questão
            </button>
            <div>
              {exists && (
                <button
                  className="quiz-delete"
                  type="button"
                  onClick={removeQuiz}
                >
                  <Trash2 size={16} /> Excluir quiz
                </button>
              )}
              <button className="admin-save" disabled={saving}>
                <Save size={17} />
                {saving ? 'Salvando...' : 'Salvar quiz'}
              </button>
            </div>
          </div>
        </form>
      )}
    </section>
  )
}
