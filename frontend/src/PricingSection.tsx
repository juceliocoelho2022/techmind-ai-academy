import {
  BriefcaseBusiness,
  Building2,
  Check,
  Crown,
  GraduationCap,
  Rocket,
  Sparkles
} from 'lucide-react'
import { useMemo, useState } from 'react'
import './pricing-section.css'

export type PlanCode = 'FREE' | 'PRO' | 'CAREER' | 'EDUCATION'

type BillingPeriod = 'MONTHLY' | 'ANNUAL'

type Props = {
  supportEmail: string | null
  onSelectPlan: (plan: PlanCode) => void
}

type Plan = {
  code: Exclude<PlanCode, 'EDUCATION'>
  name: string
  description: string
  monthlyPrice: number
  annualPrice: number
  badge?: string
  icon: typeof Rocket
  features: string[]
}

const plans: Plan[] = [
  {
    code: 'FREE',
    name: 'Free',
    description: 'Para conhecer a TechMind e iniciar sua jornada.',
    monthlyPrice: 0,
    annualPrice: 0,
    icon: Rocket,
    features: [
      'Cadastro gratuito',
      'Aulas introdutórias',
      'Quiz básico',
      'XP e progresso',
      'Acesso às primeiras trilhas'
    ]
  },
  {
    code: 'PRO',
    name: 'Pro',
    description: 'A experiência completa para aprender e construir projetos reais.',
    monthlyPrice: 49.9,
    annualPrice: 479,
    badge: 'Mais escolhido',
    icon: Crown,
    features: [
      'Todas as trilhas',
      'Vídeos, e-books e projetos ZIP',
      'Quizzes completos',
      'Projetos para portfólio',
      'Dashboard de evolução',
      'Certificados de conclusão'
    ]
  },
  {
    code: 'CAREER',
    name: 'Career',
    description: 'Para transformar estudo em preparação profissional.',
    monthlyPrice: 89.9,
    annualPrice: 849,
    badge: 'Foco em carreira',
    icon: BriefcaseBusiness,
    features: [
      'Tudo do plano Pro',
      'Simulados de entrevista',
      'Desafios técnicos',
      'Trilhas orientadas a vagas',
      'Preparação de currículo e LinkedIn',
      'Roadmap profissional',
      'Mentor IA e Job Ready evolutivos'
    ]
  }
]

function money(value: number) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2
  }).format(value)
}

export default function PricingSection({ supportEmail, onSelectPlan }: Props) {
  const [period, setPeriod] = useState<BillingPeriod>('MONTHLY')

  const annualSavings = useMemo(() => {
    return plans.reduce<Record<string, number>>((acc, plan) => {
      if (plan.monthlyPrice <= 0) {
        acc[plan.code] = 0
        return acc
      }

      acc[plan.code] = Math.max(
        0,
        Math.round(
          ((plan.monthlyPrice * 12 - plan.annualPrice) /
            (plan.monthlyPrice * 12)) *
            100
        )
      )

      return acc
    }, {})
  }, [])

  return (
    <section className="pricing-section" id="plans">
      <div className="pricing-heading">
        <div>
          <span className="eyebrow">PLANOS</span>
          <h3>Escolha como você quer evoluir.</h3>
          <p>
            Comece grátis e avance quando quiser. Os planos pagos ampliam acesso,
            projetos, certificação e preparação profissional.
          </p>
        </div>

        <div className="billing-toggle" role="group" aria-label="Período de cobrança">
          <button
            type="button"
            className={period === 'MONTHLY' ? 'active' : ''}
            onClick={() => setPeriod('MONTHLY')}
          >
            Mensal
          </button>
          <button
            type="button"
            className={period === 'ANNUAL' ? 'active' : ''}
            onClick={() => setPeriod('ANNUAL')}
          >
            Anual
            <span>economize</span>
          </button>
        </div>
      </div>

      <div className="pricing-grid">
        {plans.map(plan => {
          const Icon = plan.icon
          const annual = period === 'ANNUAL'
          const currentPrice = annual ? plan.annualPrice : plan.monthlyPrice
          const monthlyEquivalent =
            annual && plan.annualPrice > 0 ? plan.annualPrice / 12 : currentPrice

          return (
            <article
              className={[
                'pricing-card',
                plan.code === 'PRO' ? 'featured' : '',
                plan.code === 'CAREER' ? 'career' : ''
              ]
                .filter(Boolean)
                .join(' ')}
              key={plan.code}
            >
              {plan.badge && <span className="pricing-badge">{plan.badge}</span>}

              <div className="pricing-icon">
                <Icon size={23} />
              </div>

              <div className="pricing-plan-head">
                <div>
                  <h4>{plan.name}</h4>
                  <p>{plan.description}</p>
                </div>
              </div>

              <div className="pricing-price">
                {plan.code === 'FREE' ? (
                  <>
                    <strong>R$ 0</strong>
                    <span>para começar</span>
                  </>
                ) : annual ? (
                  <>
                    <strong>{money(monthlyEquivalent)}</strong>
                    <span>/mês equivalente</span>
                    <small>
                      {money(plan.annualPrice)}/ano · economize {annualSavings[plan.code]}%
                    </small>
                  </>
                ) : (
                  <>
                    <strong>{money(currentPrice)}</strong>
                    <span>/mês</span>
                  </>
                )}
              </div>

              <ul>
                {plan.features.map(feature => (
                  <li key={feature}>
                    <Check size={16} />
                    <span>{feature}</span>
                  </li>
                ))}
              </ul>

              <button
                type="button"
                className={plan.code === 'PRO' ? 'primary pricing-cta' : 'pricing-cta'}
                onClick={() => onSelectPlan(plan.code)}
              >
                {plan.code === 'FREE'
                  ? 'Começar grátis'
                  : plan.code === 'PRO'
                    ? 'Escolher Pro'
                    : 'Escolher Career'}
              </button>
            </article>
          )
        })}
      </div>

      <div className="pricing-b2b">
        <div className="pricing-b2b-icon">
          <Building2 size={24} />
        </div>

        <div className="pricing-b2b-copy">
          <span className="eyebrow">ESCOLAS E EMPRESAS</span>
          <h4>TechMind Education & Business</h4>
          <p>
            Gestão de turmas, trilhas personalizadas, usuários, quizzes,
            relatórios e Analytics para acompanhar aprendizagem em escala.
          </p>

          <div className="pricing-b2b-tags">
            <span>
              <GraduationCap size={14} />
              Education a partir de R$ 299/mês
            </span>
            <span>
              <BriefcaseBusiness size={14} />
              Business sob consulta
            </span>
          </div>
        </div>

        <button
          type="button"
          className="pricing-b2b-button"
          onClick={() => onSelectPlan('EDUCATION')}
        >
          <Sparkles size={16} />
          {supportEmail ? 'Falar com a TechMind' : 'Tenho interesse'}
        </button>
      </div>

      <p className="pricing-note">
        Os valores exibidos são os planos comerciais atuais da TechMind. O
        checkout e a cobrança recorrente serão conectados ao módulo de
        assinaturas.
      </p>
    </section>
  )
}
