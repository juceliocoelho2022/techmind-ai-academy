import { useEffect, useState } from 'react'
import {
  CheckCircle2,
  Clock3,
  CreditCard,
  RefreshCw,
  XCircle
} from 'lucide-react'
import './admin-subscription-requests.css'

type UpgradeRequest = {
  id: number
  userId: number
  userName: string
  userEmail: string
  currentPlan: 'FREE' | 'PRO' | 'CAREER'
  requestedPlan: 'PRO' | 'CAREER'
  billingPeriod: 'MONTHLY' | 'ANNUAL'
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  createdAt: string
  resolvedAt: string | null
  resolvedByEmail: string | null
}

type Props = {
  token: string
  refreshKey: number
  onChanged: () => void
  onMessage: (message: string) => void
}

async function subscriptionRequest<T>(
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

export default function AdminSubscriptionRequestsPanel({
  token,
  refreshKey,
  onChanged,
  onMessage
}: Props) {
  const [requests, setRequests] = useState<UpgradeRequest[]>([])
  const [loading, setLoading] = useState(false)
  const [processingId, setProcessingId] = useState<number | null>(null)

  async function loadRequests() {
    setLoading(true)

    try {
      const data = await subscriptionRequest<UpgradeRequest[]>(
        '/api/v1/admin/subscriptions/upgrade-requests?status=PENDING',
        token
      )
      setRequests(data)
    } catch (error) {
      onMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível carregar as solicitações de upgrade.'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadRequests()
  }, [token, refreshKey])

  async function process(
    request: UpgradeRequest,
    action: 'approve' | 'reject'
  ) {
    const verb = action === 'approve' ? 'aprovar' : 'rejeitar'
    if (
      !window.confirm(
        `Deseja ${verb} o upgrade de ${request.userEmail} para ${request.requestedPlan}?`
      )
    ) {
      return
    }

    setProcessingId(request.id)

    try {
      await subscriptionRequest<UpgradeRequest>(
        `/api/v1/admin/subscriptions/upgrade-requests/${request.id}/${action}`,
        token,
        { method: 'POST' }
      )

      setRequests(current =>
        current.filter(item => item.id !== request.id)
      )
      onChanged()
      onMessage(
        action === 'approve'
          ? `Plano ${request.requestedPlan} ativado para ${request.userEmail}.`
          : `Solicitação de ${request.userEmail} rejeitada.`
      )
    } catch (error) {
      onMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível processar a solicitação.'
      )
    } finally {
      setProcessingId(null)
    }
  }

  return (
    <section className="admin-card subscription-requests-panel">
      <div className="subscription-requests-header">
        <div>
          <span className="admin-kicker">ASSINATURAS</span>
          <h2>Solicitações de upgrade</h2>
          <p>
            Aprovação manual temporária até a integração com o gateway de pagamento.
          </p>
        </div>

        <button type="button" onClick={loadRequests} disabled={loading}>
          <RefreshCw size={16} />
          Atualizar
        </button>
      </div>

      {requests.length === 0 ? (
        <div className="subscription-empty">
          <CheckCircle2 size={20} />
          <div>
            <strong>Nenhum upgrade pendente</strong>
            <span>
              Novos pedidos de Pro ou Career aparecerão aqui.
            </span>
          </div>
        </div>
      ) : (
        <div className="subscription-request-list">
          {requests.map(request => (
            <article key={request.id}>
              <div className="subscription-request-user">
                <CreditCard size={20} />
                <div>
                  <strong>{request.userName}</strong>
                  <span>{request.userEmail}</span>
                </div>
              </div>

              <div className="subscription-request-plan">
                <span>{request.currentPlan}</span>
                <strong>→</strong>
                <span className="target">{request.requestedPlan}</span>
              </div>

              <div className="subscription-request-meta">
                <span>
                  <Clock3 size={13} />
                  {request.billingPeriod === 'ANNUAL' ? 'Anual' : 'Mensal'}
                </span>
                <span>
                  {new Date(request.createdAt).toLocaleString('pt-BR')}
                </span>
              </div>

              <div className="subscription-request-actions">
                <button
                  className="reject"
                  type="button"
                  disabled={processingId === request.id}
                  onClick={() => void process(request, 'reject')}
                >
                  <XCircle size={15} />
                  Rejeitar
                </button>
                <button
                  className="approve"
                  type="button"
                  disabled={processingId === request.id}
                  onClick={() => void process(request, 'approve')}
                >
                  <CheckCircle2 size={15} />
                  Aprovar
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
