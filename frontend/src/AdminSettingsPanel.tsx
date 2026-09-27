import { FormEvent, useEffect, useState } from 'react'
import {
  BookOpenCheck,
  Mail,
  MessageCircle,
  Save,
  Settings2,
  ShieldCheck,
  Sparkles,
  UserPlus
} from 'lucide-react'
import './admin-settings-panel.css'

export type PlatformSettings = {
  academyName: string
  tagline: string
  supportEmail: string | null
  whatsappNumber: string | null
  registrationEnabled: boolean
  defaultLessonXp: number
  defaultQuizPassingScore: number
  defaultQuizXp: number
  updatedAt: string
}

type Props = {
  token: string
  onChanged: (settings: PlatformSettings) => void
  onMessage: (message: string) => void
}

async function settingsRequest<T>(
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
      // mantém resposta textual
    }

    throw new Error(detail || `HTTP ${response.status}`)
  }

  return response.json()
}

export default function AdminSettingsPanel({
  token,
  onChanged,
  onMessage
}: Props) {
  const [settings, setSettings] = useState<PlatformSettings | null>(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)

  async function loadSettings() {
    setLoading(true)

    try {
      const data = await settingsRequest<PlatformSettings>(
        '/api/v1/admin/settings',
        token
      )
      setSettings(data)
      onChanged(data)
    } catch (error) {
      onMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível carregar as configurações.'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadSettings()
  }, [token])

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!settings) return

    setSaving(true)

    try {
      const updated = await settingsRequest<PlatformSettings>(
        '/api/v1/admin/settings',
        token,
        {
          method: 'PUT',
          body: JSON.stringify({
            academyName: settings.academyName,
            tagline: settings.tagline,
            supportEmail: settings.supportEmail || null,
            whatsappNumber: settings.whatsappNumber || null,
            registrationEnabled: settings.registrationEnabled,
            defaultLessonXp: settings.defaultLessonXp,
            defaultQuizPassingScore: settings.defaultQuizPassingScore,
            defaultQuizXp: settings.defaultQuizXp
          })
        }
      )

      setSettings(updated)
      onChanged(updated)
      onMessage('Configurações da plataforma salvas com sucesso.')
    } catch (error) {
      onMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível salvar as configurações.'
      )
    } finally {
      setSaving(false)
    }
  }

  return (
    <section
      className="admin-card admin-settings-panel admin-scroll-target"
      id="admin-settings"
    >
      <div className="settings-header">
        <div>
          <span className="admin-kicker">CONFIGURAÇÕES</span>
          <h2>Configurações da plataforma</h2>
          <p>
            Controle identidade, acesso e padrões acadêmicos da Academy.
          </p>
        </div>

        <Settings2 size={28} />
      </div>

      {loading && !settings ? (
        <div className="settings-loading">Carregando configurações...</div>
      ) : settings ? (
        <form onSubmit={save}>
          <div className="settings-section">
            <div className="settings-section-title">
              <Sparkles size={20} />
              <div>
                <strong>Identidade da Academy</strong>
                <span>Nome, mensagem principal e contato.</span>
              </div>
            </div>

            <div className="settings-grid">
              <label>
                <span>Nome da plataforma *</span>
                <input
                  value={settings.academyName}
                  maxLength={120}
                  required
                  onChange={event =>
                    setSettings(current =>
                      current
                        ? { ...current, academyName: event.target.value }
                        : current
                    )
                  }
                />
              </label>

              <label>
                <span>E-mail de suporte</span>
                <div className="settings-input-with-icon">
                  <Mail size={16} />
                  <input
                    type="email"
                    value={settings.supportEmail ?? ''}
                    maxLength={200}
                    placeholder="suporte@techmind.com"
                    onChange={event =>
                      setSettings(current =>
                        current
                          ? {
                              ...current,
                              supportEmail: event.target.value || null
                            }
                          : current
                      )
                    }
                  />
                </div>
              </label>

              <label>
                <span>WhatsApp de suporte</span>
                <div className="settings-input-with-icon">
                  <MessageCircle size={16} />
                  <input
                    type="tel"
                    value={settings.whatsappNumber ?? ''}
                    maxLength={30}
                    placeholder="+5511911625945"
                    onChange={event =>
                      setSettings(current =>
                        current
                          ? {
                              ...current,
                              whatsappNumber: event.target.value || null
                            }
                          : current
                      )
                    }
                  />
                </div>
              </label>

              <label className="wide">
                <span>Tagline *</span>
                <input
                  value={settings.tagline}
                  maxLength={240}
                  required
                  onChange={event =>
                    setSettings(current =>
                      current
                        ? { ...current, tagline: event.target.value }
                        : current
                    )
                  }
                />
              </label>
            </div>
          </div>

          <div className="settings-section">
            <div className="settings-section-title">
              <ShieldCheck size={20} />
              <div>
                <strong>Acesso</strong>
                <span>Controle a entrada de novos usuários.</span>
              </div>
            </div>

            <label className="settings-toggle-card">
              <div>
                <UserPlus size={19} />
                <div>
                  <strong>Permitir novos cadastros</strong>
                  <span>
                    Quando desativado, novos usuários não conseguem criar conta.
                  </span>
                </div>
              </div>

              <input
                type="checkbox"
                checked={settings.registrationEnabled}
                onChange={event =>
                  setSettings(current =>
                    current
                      ? {
                          ...current,
                          registrationEnabled: event.target.checked
                        }
                      : current
                  )
                }
              />
            </label>
          </div>

          <div className="settings-section">
            <div className="settings-section-title">
              <BookOpenCheck size={20} />
              <div>
                <strong>Padrões acadêmicos</strong>
                <span>
                  Valores usados ao criar novas aulas e quizzes.
                </span>
              </div>
            </div>

            <div className="settings-grid academic">
              <label>
                <span>XP padrão por aula</span>
                <input
                  type="number"
                  min={0}
                  value={settings.defaultLessonXp}
                  onChange={event =>
                    setSettings(current =>
                      current
                        ? {
                            ...current,
                            defaultLessonXp: Number(event.target.value)
                          }
                        : current
                    )
                  }
                />
              </label>

              <label>
                <span>Nota mínima padrão do quiz (%)</span>
                <input
                  type="number"
                  min={0}
                  max={100}
                  value={settings.defaultQuizPassingScore}
                  onChange={event =>
                    setSettings(current =>
                      current
                        ? {
                            ...current,
                            defaultQuizPassingScore: Number(event.target.value)
                          }
                        : current
                    )
                  }
                />
              </label>

              <label>
                <span>XP padrão por aprovação no quiz</span>
                <input
                  type="number"
                  min={0}
                  value={settings.defaultQuizXp}
                  onChange={event =>
                    setSettings(current =>
                      current
                        ? {
                            ...current,
                            defaultQuizXp: Number(event.target.value)
                          }
                        : current
                    )
                  }
                />
              </label>
            </div>
          </div>

          <div className="settings-footer">
            <div>
              <span>Status</span>
              <strong>
                {settings.registrationEnabled
                  ? 'Cadastros abertos'
                  : 'Cadastros fechados'}
              </strong>
              <small>
                Última atualização:{' '}
                {new Date(settings.updatedAt).toLocaleString('pt-BR')}
              </small>
            </div>

            <button className="admin-save" disabled={saving}>
              <Save size={17} />
              {saving ? 'Salvando...' : 'Salvar configurações'}
            </button>
          </div>
        </form>
      ) : (
        <div className="settings-loading">
          Configurações indisponíveis.
        </div>
      )}
    </section>
  )
}
