import { Headphones, Mail, MessageCircle } from 'lucide-react'
import './support-contact-section.css'

type Props = {
  supportEmail: string | null
  whatsappNumber: string | null
}

function gmailComposeHref(email: string) {
  const params = new URLSearchParams({
    view: 'cm',
    fs: '1',
    to: email,
    su: 'Suporte TechMind AI Academy',
    body: 'Olá! Preciso de ajuda com a TechMind AI Academy.'
  })

  return `https://mail.google.com/mail/?${params.toString()}`
}

function whatsappHref(number: string) {
  const digits = number.replace(/\D/g, '')
  const message = encodeURIComponent(
    'Olá! Preciso de ajuda com a TechMind AI Academy.'
  )

  return `https://wa.me/${digits}?text=${message}`
}

function formatWhatsapp(number: string) {
  const digits = number.replace(/\D/g, '')

  if (digits.length === 13 && digits.startsWith('55')) {
    const ddd = digits.slice(2, 4)
    const first = digits.slice(4, 9)
    const second = digits.slice(9)

    return `(${ddd}) ${first}-${second}`
  }

  return number
}

export default function SupportContactSection({
  supportEmail,
  whatsappNumber
}: Props) {
  if (!supportEmail && !whatsappNumber) return null

  return (
    <section className="support-contact-section" id="support">
      <div className="support-contact-copy">
        <div className="support-contact-icon">
          <Headphones size={25} />
        </div>

        <div>
          <span className="eyebrow">SUPORTE TECHMIND</span>
          <h3>Precisa de ajuda? Fale com a gente.</h3>
          <p>
            Dúvidas sobre acesso, planos, aulas ou pagamentos? Use um dos nossos
            canais oficiais de atendimento.
          </p>
        </div>
      </div>

      <div className="support-contact-actions">
        {supportEmail && (
          <a
            className="support-contact-card"
            href={gmailComposeHref(supportEmail)}
            target="_blank"
            rel="noreferrer"
          >
            <Mail size={21} />
            <div>
              <span>E-mail</span>
              <strong>{supportEmail}</strong>
            </div>
          </a>
        )}

        {whatsappNumber && (
          <a
            className="support-contact-card whatsapp"
            href={whatsappHref(whatsappNumber)}
            target="_blank"
            rel="noreferrer"
          >
            <MessageCircle size={21} />
            <div>
              <span>WhatsApp</span>
              <strong>{formatWhatsapp(whatsappNumber)}</strong>
            </div>
          </a>
        )}
      </div>
    </section>
  )
}
