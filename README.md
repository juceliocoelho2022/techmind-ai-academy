# TechMind AI Academy

<p align="center">
  <img src="frontend/public/techmind-logo.png" alt="TechMind AI Academy" width="220" />
</p>

**TechMind AI Academy** é uma plataforma de aprendizagem prática para tecnologia, combinando trilhas, projetos, progresso, gamificação e preparação para entrevistas técnicas com IA.

> **Visão do produto:** transformar conteúdo técnico em uma experiência interativa: aprender → praticar → construir → receber feedback → evoluir.

## Status

**MVP v0.2 — autenticação e jornada do aluno**

Já implementado:

- Java 21 + Spring Boot 3.5.5
- React + TypeScript + Vite
- PostgreSQL 17
- Flyway
- Spring Security
- autenticação stateless com JWT HS256
- senha protegida com BCrypt
- cadastro e login
- perfil autenticado
- catálogo público de trilhas
- matrícula do aluno
- progresso persistido por trilha
- XP e nível agregado
- simulador de entrevista local preparado para futura integração com LLM
- Docker / Docker Compose
- Actuator
- testes unitários de autenticação e entrevista

## Arquitetura do MVP

```text
┌──────────────────────────┐
│ React + TypeScript       │
│ Dashboard / Auth / UX    │
└─────────────┬────────────┘
              │ REST + JWT
              ▼
┌──────────────────────────┐
│ Spring Boot 3.5.5        │
│                          │
│ auth                     │
│ user                     │
│ course                   │
│ enrollment               │
│ progress                 │
│ interview                │
└─────────────┬────────────┘
              │ JPA / Flyway
              ▼
┌──────────────────────────┐
│ PostgreSQL 17            │
└──────────────────────────┘
```

O projeto permanece como **monólito modular** no MVP. Isso mantém baixo acoplamento entre módulos sem introduzir a complexidade operacional de microsserviços antes de existir necessidade real.

## Principais endpoints

### Públicos

| Método | Endpoint | Função |
|---|---|---|
| `POST` | `/api/v1/auth/register` | criar conta |
| `POST` | `/api/v1/auth/login` | autenticar e receber JWT |
| `GET` | `/api/v1/courses` | listar trilhas |
| `GET` | `/api/v1/courses/{id}` | consultar trilha |
| `GET` | `/actuator/health` | health check |

### Autenticados

| Método | Endpoint | Função |
|---|---|---|
| `GET` | `/api/v1/users/me` | perfil do aluno |
| `POST` | `/api/v1/enrollments/courses/{courseId}` | matricular em uma trilha |
| `GET` | `/api/v1/enrollments/me` | listar matrículas |
| `PATCH` | `/api/v1/enrollments/courses/{courseId}/progress` | atualizar progresso/XP |
| `GET` | `/api/v1/progress/me` | progresso agregado |
| `POST` | `/api/v1/interviews/evaluate` | avaliar resposta de entrevista |

Há exemplos prontos em [`docs/api.http`](docs/api.http).

## Banco de dados

Flyway cria automaticamente:

```text
courses
app_users
enrollments
```

Migrações:

```text
V1__create_courses.sql
V2__create_users_and_enrollments.sql
```

## Executar localmente

### 1. PostgreSQL

```powershell
cd C:\Projetos\techmind-ai-academy
docker compose up -d postgres
```

### 2. Backend

```powershell
cd backend
$env:JWT_SECRET="techmind-local-secret-super-seguro-2026"
mvn spring-boot:run
```

Backend:

```text
http://localhost:8080
```

### 3. Frontend

Em outro PowerShell:

```powershell
cd frontend
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

### Executar tudo com Docker

```powershell
docker compose up --build
```

Frontend Docker:

```text
http://localhost:3000
```

## Segurança

A aplicação usa JWT assinado com HMAC SHA-256 e sessão stateless. Em desenvolvimento existe um secret padrão para facilitar o bootstrap; **em produção, `JWT_SECRET` deve sempre ser definido por variável de ambiente e nunca versionado**.

O arquivo `.env.example` documenta as variáveis esperadas.

## Próximas etapas

### v0.3 — Conteúdo e gamificação

- módulos e aulas
- conclusão de aula
- desafios de código
- badges/conquistas
- ranking opcional
- dashboard detalhado

### v0.4 — Mentor IA

- integração com LLM
- prompts versionados
- avaliação estruturada de entrevistas
- geração de plano de estudo
- histórico de sessões
- RAG com material das trilhas

### v0.5 — Produto

- painel administrativo
- certificados
- pagamentos/assinaturas
- observabilidade
- CI/CD
- deploy cloud

## Stack

`Java 21 · Spring Boot · Spring Security · JWT · JPA/Hibernate · PostgreSQL · Flyway · React · TypeScript · Vite · Docker`

---

**TechMind AI Academy — Trilhas inteligentes para aprender, praticar e evoluir.**
