import { useEffect, useState } from 'react'
import {
  CalendarClock,
  CreditCard,
  DollarSign,
  RefreshCw,
  TrendingUp,
  Users
} from 'lucide-react'
import './admin-billing-panel.css'

type RecentPayment = {
  id: number
  externalReference: string
  userId: number
  userName: string
  userEmail: string
  plan: 'PRO' | 'CAREER'
  billingPeriod: 'MONTHLY' | 'ANNUAL'
  amount: number
  currency: string
  status: 'CREATED' | 'CHECKOUT_CREATED' | 'PENDING' | 'PAID' | 'FAILED' | 'CANCELED'
  createdAt: string
  paidAt: string | null
}

type UpcomingExpiration = {
  subscriptionId: number
  userId: number
  userName: string
  userEmail: string
  plan: 'PRO' | 'CAREER'
  billingPeriod: 'MONTHLY' | 'ANNUAL' | null
  source: 'FREE' | 'MANUAL' | 'PAYMENT'
  endsAt: string
}

type BillingResponse = {
  generatedAt: string
  grossPaidRevenue: number
  paidRevenue30Days: number
  mrrEquivalent: number
  arrEquivalent: number
  paidOrders: number
  activePremiumAccesses: number
  activePaidSubscriptions: number
  proActive: number
  careerActive: number
  expiring7Days: number
  expiring30Days: number
  recentPayments: RecentPayment[]
  upcomingExpirations: UpcomingExpiration[]
}

type Props = {
  token: string
  refreshKey: number
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

export default function AdminBillingPanel({
  token,
  refreshKey,
  onMessage
}: Props) {
  const [billing, setBilling] = useState<BillingResponse | null>(null)
  const [loading, setLoading] = useState(false)

  async function loadBilling() {
    setLoading(true)

    try {
      const response = await fetch('/api/v1/admin/billing', {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })

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

      setBilling(await response.json())
    } catch (error) {
      onMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível carregar o financeiro.'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadBilling()
  }, [token, refreshKey])

  return (
    <section
      className="admin-card admin-billing-panel admin-scroll-target"
      id="admin-billing"
    >
      <div className="admin-billing-header">
        <div>
          <span className="admin-kicker">FINANCEIRO</span>
          <h2>Billing Center</h2>
          <p>
            Receita confirmada, assinaturas premium, vencimentos e histórico de pagamentos.
          </p>
        </div>

        <button type="button" disabled={loading} onClick={loadBilling}>
          <RefreshCw size={16} className={loading ? 'spin' : ''} />
          Atualizar
        </button>
      </div>

      {billing && (
        <>
          <div className="admin-billing-grid">
            <article>
              <DollarSign size={21} />
              <div>
                <span>Receita bruta paga</span>
                <strong>{money(billing.grossPaidRevenue)}</strong>
                <small>{billing.paidOrders} pagamento(s) PAID</small>
              </div>
            </article>

            <article>
              <TrendingUp size={21} />
              <div>
                <span>Receita últimos 30 dias</span>
                <strong>{money(billing.paidRevenue30Days)}</strong>
                <small>somente ordens confirmadas</small>
              </div>
            </article>

            <article>
              <CreditCard size={21} />
              <div>
                <span>MRR equivalente</span>
                <strong>{money(billing.mrrEquivalent)}</strong>
                <small>não representa recorrência contratada</small>
              </div>
            </article>

            <article>
              <TrendingUp size={21} />
              <div>
                <span>ARR equivalente</span>
                <strong>{money(billing.arrEquivalent)}</strong>
                <small>MRR equivalente × 12</small>
              </div>
            </article>

            <article>
              <Users size={21} />
              <div>
                <span>Acessos premium ativos</span>
                <strong>{billing.activePremiumAccesses}</strong>
                <small>
                  {billing.proActive} PRO · {billing.careerActive} CAREER
                </small>
              </div>
            </article>

            <article>
              <CalendarClock size={21} />
              <div>
                <span>Vencimentos</span>
                <strong>{billing.expiring7Days}</strong>
                <small>{billing.expiring30Days} nos próximos 30 dias</small>
              </div>
            </article>
          </div>

          <div className="admin-billing-columns">
            <div className="admin-billing-section">
              <div className="admin-billing-section-title">
                <CreditCard size={18} />
                <div>
                  <strong>Pagamentos recentes</strong>
                  <span>Últimos 50 pedidos registrados</span>
                </div>
              </div>

              {billing.recentPayments.length === 0 ? (
                <div className="admin-billing-empty">
                  Nenhum pagamento registrado ainda.
                </div>
              ) : (
                <div className="admin-billing-table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Aluno</th>
                        <th>Plano</th>
                        <th>Valor</th>
                        <th>Status</th>
                        <th>Data</th>
                      </tr>
                    </thead>
                    <tbody>
                      {billing.recentPayments.map(payment => (
                        <tr key={payment.id}>
                          <td>
                            <strong>{payment.userName}</strong>
                            <small>{payment.userEmail}</small>
                          </td>
                          <td>
                            {payment.plan} · {payment.billingPeriod === 'ANNUAL' ? 'Anual' : 'Mensal'}
                          </td>
                          <td>{money(payment.amount)}</td>
                          <td>
                            <span className={`admin-billing-status ${payment.status.toLowerCase()}`}>
                              {payment.status}
                            </span>
                          </td>
                          <td>{date(payment.paidAt ?? payment.createdAt)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className="admin-billing-section">
              <div className="admin-billing-section-title">
                <CalendarClock size={18} />
                <div>
                  <strong>Próximos vencimentos</strong>
                  <span>Até 20 acessos ativos nos próximos 30 dias</span>
                </div>
              </div>

              {billing.upcomingExpirations.length === 0 ? (
                <div className="admin-billing-empty">
                  Nenhum vencimento nos próximos 30 dias.
                </div>
              ) : (
                <div className="admin-expiration-list">
                  {billing.upcomingExpirations.map(item => (
                    <article key={item.subscriptionId}>
                      <div>
                        <strong>{item.userName}</strong>
                        <span>{item.userEmail}</span>
                      </div>

                      <div>
                        <strong>{item.plan}</strong>
                        <span>
                          {item.billingPeriod === 'ANNUAL' ? 'Anual' : 'Mensal'} · {item.source}
                        </span>
                      </div>

                      <time>{date(item.endsAt)}</time>
                    </article>
                  ))}
                </div>
              )}
            </div>
          </div>

          <p className="admin-billing-disclaimer">
            MRR/ARR são equivalentes calculados a partir de assinaturas pagas ativas.
            A TechMind v1.2 ainda não possui renovação automática recorrente.
          </p>
        </>
      )}

      {loading && !billing && (
        <div className="admin-billing-empty">Calculando indicadores financeiros...</div>
      )}
    </section>
  )
}
