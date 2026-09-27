import {
  CalendarClock,
  CheckCircle2,
  CreditCard,
  History,
  ShieldCheck,
  XCircle
} from 'lucide-react'
import { useEffect, useState } from 'react'
import './billing-center.css'

export type BillingSubscription = {
  userId: number
  plan: 'FREE' | 'PRO' | 'CAREER'
  status: 'ACTIVE' | 'CANCELED' | 'PAST_DUE'
  source: 'FREE' | 'MANUAL' | 'PAYMENT'
  billingPeriod: 'MONTHLY' | 'ANNUAL' | null
  startedAt: string
  endsAt: string | null
  canceledAt: string | null
  pendingUpgrade: {
    id: number
    requestedPlan: 'PRO' | 'CAREER'
    billingPeriod: 'MONTHLY' | 'ANNUAL'
    status: 'PENDING' | 'APPROVED' | 'REJECTED'
    createdAt: string
  } | null
}

type PaymentHistory = {
  id: number
  externalReference: string
  plan: 'PRO' | 'CAREER'
  billingPeriod: 'MONTHLY' | 'ANNUAL'
  amount: number
  currency: string
  status: 'CREATED' | 'CHECKOUT_CREATED' | 'PENDING' | 'PAID' | 'FAILED' | 'CANCELED'
  provider: 'MERCADO_PAGO'
  providerStatus: string | null
  createdAt: string
  paidAt: string | null
}

type Props = {
  token: string
  subscription: BillingSubscription
  onChanged: (subscription: BillingSubscription) => void
  onMessage: (message: string) => void
}

function money(value: number) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(value)
}

function date(value: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleString('pt-BR')
}

const statusLabel: Record<PaymentHistory['status'], string> = {
  CREATED: 'Criado',
  CHECKOUT_CREATED: 'Checkout aberto',
  PENDING: 'Pendente',
  PAID: 'Pago',
  FAILED: 'Falhou',
  CANCELED: 'Cancelado'
}

export default function BillingCenter({
  token,
  subscription,
  onChanged,
  onMessage
}: Props) {
  const [payments, setPayments] = useState<PaymentHistory[]>([])
  const [loading, setLoading] = useState(false)
  const [canceling, setCanceling] = useState(false)

  async function loadPayments() {
    setLoading(true)

    try {
      const response = await fetch('/api/v1/payments/me', {
        headers: { Authorization: `Bearer ${token}` }
      })

      if (!response.ok) {
        const raw = await response.text()
        throw new Error(raw || `HTTP ${response.status}`)
      }

      setPayments(await response.json())
    } catch (error) {
      onMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível carregar o histórico financeiro.'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadPayments()
  }, [token])

  async function cancelSubscription() {
    const confirmed = window.confirm(
      'Encerrar o plano agora? O acesso premium será interrompido imediatamente. Esta ação não processa estorno automático.'
    )

    if (!confirmed) return

    setCanceling(true)

    try {
      const response = await fetch('/api/v1/subscriptions/cancel', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ confirmImmediate: true })
      })

      if (!response.ok) {
        const raw = await response.text()
        let detail = raw

        try {
          const payload = JSON.parse(raw)
          detail = payload.detail || payload.message || raw
        } catch {
          // mantém texto original
        }

        throw new Error(detail || `HTTP ${response.status}`)
      }

      const updated = (await response.json()) as BillingSubscription
      onChanged(updated)
      onMessage('Plano premium encerrado. Seu acesso efetivo voltou para FREE.')
    } catch (error) {
      onMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível cancelar a assinatura.'
      )
    } finally {
      setCanceling(false)
    }
  }

  const canCancel =
    subscription.status === 'ACTIVE' && subscription.plan !== 'FREE'

  return (
    <section className="billing-center" id="billing-center">
      <div className="billing-center-heading">
        <div>
          <span className="eyebrow">MINHA ASSINATURA</span>
          <h3>Plano e pagamentos</h3>
          <p>
            Acompanhe seu acesso, validade e histórico financeiro em um único lugar.
          </p>
        </div>

        <div className={`billing-current-plan ${subscription.plan.toLowerCase()}`}>
          <ShieldCheck size={18} />
          <div>
            <span>Plano</span>
            <strong>{subscription.status === 'ACTIVE' ? subscription.plan : 'FREE'}</strong>
          </div>
        </div>
      </div>

      <div className="billing-account-grid">
        <article>
          <CreditCard size={20} />
          <div>
            <span>Status</span>
            <strong>{subscription.status}</strong>
            <small>
              {subscription.source === 'PAYMENT'
                ? 'Pagamento confirmado'
                : subscription.source === 'MANUAL'
                  ? 'Ativação administrativa'
                  : 'Plano gratuito'}
            </small>
          </div>
        </article>

        <article>
          <CalendarClock size={20} />
          <div>
            <span>Período</span>
            <strong>
              {subscription.billingPeriod === 'ANNUAL'
                ? 'Anual'
                : subscription.billingPeriod === 'MONTHLY'
                  ? 'Mensal'
                  : 'Sem cobrança'}
            </strong>
            <small>
              {subscription.endsAt
                ? `Válido até ${date(subscription.endsAt)}`
                : 'Sem vencimento de plano gratuito'}
            </small>
          </div>
        </article>

        <article>
          {subscription.status === 'ACTIVE' ? (
            <CheckCircle2 size={20} />
          ) : (
            <XCircle size={20} />
          )}
          <div>
            <span>Acesso efetivo</span>
            <strong>
              {subscription.status === 'ACTIVE' ? subscription.plan : 'FREE'}
            </strong>
            <small>
              {subscription.canceledAt
                ? `Cancelado em ${date(subscription.canceledAt)}`
                : 'Entitlements atualizados automaticamente'}
            </small>
          </div>
        </article>
      </div>

      {canCancel && (
        <div className="billing-cancel-row">
          <div>
            <strong>Encerrar acesso premium</strong>
            <span>
              Como a renovação automática ainda não está ativa, este botão encerra
              o acesso imediatamente.
            </span>
          </div>

          <button
            type="button"
            disabled={canceling}
            onClick={() => void cancelSubscription()}
          >
            <XCircle size={16} />
            {canceling ? 'Cancelando...' : 'Encerrar plano agora'}
          </button>
        </div>
      )}

      <div className="billing-history">
        <div className="billing-history-title">
          <History size={18} />
          <div>
            <strong>Histórico de pagamentos</strong>
            <span>{loading ? 'Atualizando...' : `${payments.length} registro(s)`}</span>
          </div>
        </div>

        {payments.length === 0 ? (
          <div className="billing-empty">
            Nenhum pagamento registrado ainda.
          </div>
        ) : (
          <div className="billing-table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Plano</th>
                  <th>Período</th>
                  <th>Valor</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {payments.slice(0, 10).map(payment => (
                  <tr key={payment.id}>
                    <td>{date(payment.paidAt ?? payment.createdAt)}</td>
                    <td>{payment.plan}</td>
                    <td>
                      {payment.billingPeriod === 'ANNUAL' ? 'Anual' : 'Mensal'}
                    </td>
                    <td>{money(payment.amount)}</td>
                    <td>
                      <span className={`billing-status ${payment.status.toLowerCase()}`}>
                        {statusLabel[payment.status]}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  )
}
