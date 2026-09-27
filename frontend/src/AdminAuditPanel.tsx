import { useEffect, useState } from 'react'
import {
  Clock3,
  History,
  LoaderCircle,
  RefreshCw,
  ShieldCheck
} from 'lucide-react'
import './admin-audit-panel.css'

type AuditLog = {
  id: number
  actorEmail: string
  action: string
  entityType: string
  entityId: string | null
  summary: string
  createdAt: string
}

type Props = {
  token: string
  onMessage: (message: string) => void
}

const actionLabel: Record<string, string> = {
  CREATE: 'Criação',
  UPDATE: 'Alteração',
  DELETE: 'Exclusão',
  UPLOAD: 'Upload',
  ROLE_CHANGE: 'Permissão',
  SETTINGS_UPDATE: 'Configuração',
  TEMPLATE_INSTANTIATE: 'Template'
}

export default function AdminAuditPanel({ token, onMessage }: Props) {
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [loading, setLoading] = useState(false)

  async function load() {
    setLoading(true)

    try {
      const response = await fetch('/api/v1/admin/audit?limit=20', {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })

      if (!response.ok) {
        throw new Error(await response.text())
      }

      setLogs(await response.json())
    } catch (error) {
      onMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível carregar o histórico administrativo.'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [token])

  return (
    <section className="admin-card admin-audit-panel">
      <div className="audit-header">
        <div>
          <span className="admin-kicker">AUDITORIA</span>
          <h2>Últimas ações administrativas</h2>
          <p>Rastreabilidade das mudanças realizadas no Admin Studio.</p>
        </div>

        <button type="button" onClick={load} disabled={loading}>
          {loading
            ? <LoaderCircle className="spin" size={16} />
            : <RefreshCw size={16} />}
          Atualizar
        </button>
      </div>

      {loading && logs.length === 0 ? (
        <div className="audit-empty">
          <LoaderCircle className="spin" size={18} />
          Carregando auditoria...
        </div>
      ) : logs.length === 0 ? (
        <div className="audit-empty">
          <History size={18} />
          Nenhuma ação administrativa registrada ainda.
        </div>
      ) : (
        <div className="audit-list">
          {logs.map(log => (
            <article key={log.id}>
              <div className="audit-icon">
                <ShieldCheck size={16} />
              </div>

              <div className="audit-content">
                <div>
                  <strong>{actionLabel[log.action] ?? log.action}</strong>
                  <span>{log.entityType}</span>
                  {log.entityId && <span>#{log.entityId}</span>}
                </div>

                <p>{log.summary}</p>

                <small>
                  <span>{log.actorEmail}</span>
                  <span>
                    <Clock3 size={12} />
                    {new Date(log.createdAt).toLocaleString('pt-BR')}
                  </span>
                </small>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
