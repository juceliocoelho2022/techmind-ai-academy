# Arquitetura — TechMind AI Academy

## 1. Visão geral

A TechMind AI Academy adota, no MVP, uma arquitetura de **monólito modular full stack**. A escolha mantém a operação simples e, ao mesmo tempo, organiza o backend por domínios que podem evoluir com baixo acoplamento.

O objetivo arquitetural atual é sustentar a jornada:

```text
Aluno → autenticação → trilha → matrícula → progresso → XP → feedback
```

## 2. Contexto do sistema

```mermaid
flowchart LR
    Student[Aluno] --> Browser[Browser]
    Browser --> Web[React + TypeScript]
    Web -->|REST / JSON| API[Spring Boot API]
    API -->|JPA| DB[(PostgreSQL 17)]
    API --> Security[Spring Security + JWT]
    API --> Health[Actuator]
    Flyway[Flyway] --> DB
```

## 3. Containers

```mermaid
flowchart TB
    Browser[Browser]

    subgraph Docker Compose
      Frontend[Nginx + React build]
      Backend[Spring Boot · Java 21]
      Postgres[(PostgreSQL 17)]
    end

    Browser -->|:3000| Frontend
    Frontend -->|/api| Backend
    Backend -->|JDBC :5432| Postgres
    Browser -->|dev :5173| Vite[Vite Dev Server]
    Vite -->|proxy /api → :8080| Backend
```

## 4. Backend por domínio

```text
br.com.techmind.academy
├── auth
├── course
├── enrollment
├── interview
├── progress
├── security
└── user
```

### auth
Responsável por cadastro, login, normalização de e-mail, hash de senha e emissão do JWT.

### security
Configura autenticação stateless, Resource Server JWT, HS256, issuer, BCrypt e CORS.

### user
Representa a identidade do aluno e seu perfil autenticado.

### course
Mantém o catálogo de trilhas de aprendizagem.

### enrollment
Relaciona aluno e trilha, armazenando aulas concluídas e XP.

### progress
Consolida indicadores de evolução do aluno.

### interview
Implementa a base de avaliação de respostas para entrevistas técnicas e prepara a futura integração com LLM.

## 5. Fluxo de autenticação

```mermaid
sequenceDiagram
    actor Student as Aluno
    participant Web as React
    participant Auth as AuthController
    participant Service as AuthService
    participant DB as PostgreSQL
    participant JWT as JwtService

    Student->>Web: e-mail + senha
    Web->>Auth: POST /api/v1/auth/login
    Auth->>Service: login()
    Service->>DB: busca usuário
    DB-->>Service: usuário + password hash
    Service->>Service: BCrypt.matches()
    Service->>JWT: generate(user)
    JWT-->>Service: JWT HS256
    Service-->>Auth: AuthResponse
    Auth-->>Web: Bearer token + user
```

O token contém subject, id do usuário, nome e role, possui expiração configurável e issuer `techmind-ai-academy`.

## 6. Persistência

```mermaid
erDiagram
    APP_USERS ||--o{ ENROLLMENTS : possui
    COURSES ||--o{ ENROLLMENTS : recebe

    APP_USERS {
        BIGINT id PK
        VARCHAR name
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR role
        TIMESTAMPTZ created_at
    }

    COURSES {
        BIGINT id PK
        VARCHAR slug UK
        VARCHAR title
        VARCHAR description
        INTEGER total_lessons
    }

    ENROLLMENTS {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT course_id FK
        INTEGER completed_lessons
        INTEGER xp
        TIMESTAMPTZ started_at
        TIMESTAMPTZ updated_at
    }
```

O Flyway é a fonte de verdade do schema. O Hibernate utiliza `ddl-auto: validate`, portanto valida o modelo sem alterar automaticamente a estrutura.

## 7. Segurança

Princípios atuais:

- autenticação stateless;
- BCrypt para senhas;
- JWT HS256;
- secret mínimo de 32 bytes;
- validação de issuer;
- variáveis de ambiente para credenciais;
- CORS configurável;
- endpoints privados por padrão;
- `.env` fora do versionamento.

## 8. CI

O workflow `.github/workflows/ci.yml` valida três aspectos independentes:

1. **Backend:** Java 21 + Maven + testes.
2. **Frontend:** Node 22 + `npm ci` + build TypeScript/Vite.
3. **Docker:** validação do Compose e build das imagens do backend e frontend.

O job Docker só inicia depois dos jobs de backend e frontend concluírem com sucesso.

## 9. Decisões arquiteturais

### Monólito modular antes de microsserviços
O sistema ainda não exige a complexidade operacional de múltiplos deploys, service discovery, observabilidade distribuída ou comunicação entre serviços.

### REST + JSON
É suficiente para a interação atual SPA/API e mantém baixo custo de integração.

### PostgreSQL
Adequado ao domínio relacional de usuários, cursos e matrículas, além de oferecer consistência transacional e boa evolução futura.

### Flyway
Garante histórico explícito e reprodutível de schema.

### JWT stateless
Permite escalar instâncias da API sem replicar sessão no servidor.

## 10. Evolução prevista

```mermaid
flowchart LR
    V02[v0.2<br/>Auth + Courses + Enrollments] -->
    V03[v0.3<br/>Learning Engine] -->
    V04[v0.4<br/>AI Mentor + RAG] -->
    V05[v0.5<br/>Admin + Observability + Cloud]
```

A migração para microsserviços só deve ocorrer quando métricas de produto, escala ou autonomia de times justificarem a separação de componentes.
