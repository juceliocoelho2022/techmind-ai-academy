# TechMind AI Academy — Checkout & Payments

## Escopo da v1.1

A TechMind usa o Mercado Pago Checkout Pro para pagamentos de planos PRO e CAREER.

Fluxo:

1. o usuário escolhe PRO ou CAREER;
2. o backend calcula o preço — o frontend nunca define o valor cobrado;
3. o backend cria uma ordem local em `payment_orders`;
4. o backend cria uma preferência no Mercado Pago;
5. o navegador é redirecionado para o checkout do provedor;
6. no retorno, a TechMind consulta o pagamento diretamente na API do Mercado Pago;
7. em produção, o webhook assinado também reconcilia o pagamento;
8. somente pagamento com status `approved`, valor esperado, moeda BRL e `external_reference` válida ativa o plano.

## Preços protegidos no backend

| Plano | Mensal | Anual |
| --- | ---: | ---: |
| PRO | R$ 49,90 | R$ 479,00 |
| CAREER | R$ 89,90 | R$ 849,00 |

Alterar somente o texto do frontend não altera o valor cobrado.

## Período de acesso

Nesta versão, o checkout compra um período de acesso:

- MONTHLY: 1 mês;
- ANNUAL: 1 ano.

A renovação automática recorrente não está habilitada na v1.1. Quando o período termina, o entitlement efetivo volta para FREE até nova compra ou renovação futura.

## Variáveis de ambiente

Copie `.env.example` para `.env` e configure:

```env
APP_PUBLIC_URL=http://localhost:3000

MERCADO_PAGO_ACCESS_TOKEN=
MERCADO_PAGO_WEBHOOK_SECRET=
MERCADO_PAGO_SANDBOX=true

# Produção: URL HTTPS pública da API, sem barra final.
PAYMENT_WEBHOOK_BASE_URL=
```

Nunca coloque `MERCADO_PAGO_ACCESS_TOKEN` ou `MERCADO_PAGO_WEBHOOK_SECRET` no React, em commits, screenshots ou documentação pública.

## Teste local

O retorno do Checkout Pro inclui `payment_id`. Por isso, mesmo sem uma URL pública de webhook, a aplicação consegue reconciliar o retorno local:

```text
Mercado Pago
    ↓
http://localhost:3000/?payment_result=success&payment_id=...
    ↓
POST /api/v1/payments/reconcile/{paymentId}
    ↓
GET Mercado Pago /v1/payments/{id}
    ↓
validação server-side
    ↓
assinatura ativada
```

Se o Mercado Pago não estiver configurado, a UI preserva o fluxo manual de upgrade como contingência.

## Webhook de produção

Endpoint:

```text
POST /api/v1/payments/webhooks/mercado-pago
```

O endpoint não usa JWT porque é chamado pelo Mercado Pago. Em compensação, exige validação da assinatura `x-signature` com a chave secreta configurada no backend.

Configure `PAYMENT_WEBHOOK_BASE_URL` com o domínio HTTPS público da API, por exemplo:

```env
PAYMENT_WEBHOOK_BASE_URL=https://api.seudominio.com
```

A URL enviada ao provedor será:

```text
https://api.seudominio.com/api/v1/payments/webhooks/mercado-pago
```

Não use `localhost` como URL de webhook de produção.

## Estados da ordem

```text
CREATED
  ↓
CHECKOUT_CREATED
  ↓
PENDING
  ├── PAID
  ├── FAILED
  └── CANCELED
```

`PAID` é idempotente: notificações repetidas não estendem o plano duas vezes.

## Proteções aplicadas

Antes de ativar qualquer plano, a TechMind valida:

- pagamento consultado diretamente no provedor;
- assinatura do webhook;
- `external_reference`;
- usuário dono da ordem no retorno autenticado;
- moeda BRL;
- valor igual ao calculado no backend;
- status do provedor igual a `approved`;
- idempotência da liquidação.

## Banco

Migration:

```text
V13__create_payment_orders.sql
```

Consulta operacional:

```sql
SELECT
    id,
    external_reference,
    user_id,
    plan_code,
    billing_period,
    amount,
    status,
    provider_status,
    provider_payment_id,
    created_at,
    paid_at
FROM payment_orders
ORDER BY id DESC;
```

## Produção

Antes de habilitar pagamento real:

1. configure credenciais de produção no ambiente do backend;
2. defina `MERCADO_PAGO_SANDBOX=false`;
3. use HTTPS;
4. configure o webhook e a chave secreta;
5. execute uma compra de valor controlado;
6. confirme ordem `PAID`;
7. confirme `user_subscriptions.source = PAYMENT`;
8. confirme `ends_at`;
9. valide o Audit Log;
10. mantenha backup antes da primeira venda real.

## Próxima evolução

A v1.2 pode adicionar:

- recorrência automática;
- cancelamento pelo aluno;
- histórico de pagamentos;
- reembolso;
- cobrança vencida;
- e-mails de confirmação;
- recibos;
- cupons;
- preço fundador;
- métricas de MRR/ARR.
