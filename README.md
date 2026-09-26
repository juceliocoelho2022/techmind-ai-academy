# TechMind AI Academy

<p align="center">
  <img src="frontend/public/techmind-logo.png" alt="TechMind AI Academy" width="240" />
</p>

<h3 align="center">Trilhas inteligentes para aprender, praticar e evoluir.</h3>

<p align="center">
  Plataforma full stack de aprendizagem em tecnologia, com autenticação segura, trilhas de estudo, progresso persistido, XP e preparação para entrevistas técnicas.
</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.5.5-6DB33F?logo=springboot&logoColor=white">
  <img alt="React" src="https://img.shields.io/badge/React-19.1.1-61DAFB?logo=react&logoColor=111827">
  <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-5.9.2-3178C6?logo=typescript&logoColor=white">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white">
  <img alt="Docker" src="https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white">
  <img alt="Status" src="https://img.shields.io/badge/status-MVP%20v0.2-7C3AED">
  <a href="https://github.com/juceliocoelho2022/techmind-ai-academy/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/juceliocoelho2022/techmind-ai-academy/actions/workflows/ci.yml/badge.svg"></a>
</p>

---

## Sobre o projeto

A **TechMind AI Academy** é uma plataforma de aprendizagem prática criada para transformar conteúdo técnico em uma jornada orientada a evolução:

**aprender → praticar → construir → medir progresso → receber feedback → evoluir**

O MVP atual já oferece uma base full stack funcional para cadastro e autenticação de alunos, catálogo de trilhas, matrículas, progresso por curso, XP e preparação para entrevistas técnicas.

A arquitetura foi mantida como **monólito modular**, priorizando simplicidade operacional, separação de responsabilidades e evolução incremental antes de qualquer migração prematura para microsserviços.

> **Status atual:** v0.3 em desenvolvimento — Learning Engine com módulos, aulas, conclusão idempotente e XP calculado no servidor.

---

## Índice

- [Principais funcionalidades](#principais-funcionalidades)
- [Trilhas disponíveis](#trilhas-disponíveis)
- [Arquitetura](#arquitetura)
- [Módulos do backend](#módulos-do-backend)
- [Stack](#stack)
- [Modelo de dados](#modelo-de-dados)
- [API REST](#api-rest)
- [Segurança](#segurança)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Como executar](#como-executar)
- [Testes](#testes)
- [Decisões de engenharia](#decisões-de-engenharia)
- [Roadmap](#roadmap)
- [Autor](#autor)

---

## Principais funcionalidades

### Implementado no MVP

- cadastro de usuário
- login com JWT
- autenticação stateless
- senhas protegidas com BCrypt
- validação de issuer do JWT
- assinatura HMAC SHA-256
- perfil do aluno autenticado
- catálogo público de trilhas
- matrícula do aluno em trilhas
- progresso persistido por curso
- XP acumulado
- visão agregada de progresso
- simulador local de entrevista técnica
- banco versionado com Flyway
- PostgreSQL 17
- health check com Spring Boot Actuator
- frontend React + TypeScript
- execução local ou com Docker Compose
- testes de autenticação e entrevista
- currículo estruturado em módulos e aulas
- conclusão idempotente de aulas
- XP calculado exclusivamente pelo backend
- lock transacional para atualização consistente de progresso

### Preparado para evolução

- módulos e aulas
- desafios de código
- badges e conquistas
- mentor com LLM
- geração de plano de estudos
- RAG sobre conteúdo das trilhas
- certificados
- painel administrativo
- observabilidade
- CI/CD e deploy cloud

---

## Trilhas disponíveis

O seed inicial do banco cria quatro trilhas:

| Trilha | Conteúdo | Aulas |
|---|---|---:|
| **Java Backend** | Java 21, orientação a objetos, APIs REST, testes e boas práticas | 20 |
| **Spring Boot** | Controllers, Services, JPA, validação, segurança, observabilidade e resiliência | 18 |
| **AWS Cloud** | fundamentos AWS, arquitetura, containers, bancos, observabilidade e custos | 16 |
| **Dados & IA** | Python, SQL, engenharia de dados, RAG, agentes e aplicações com IA | 22 |

---

## Arquitetura

> Documentação técnica detalhada: **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**

~~~mermaid
flowchart LR
    U[Aluno] --> F[React + TypeScript]
    F -->|REST / JSON| API[Spring Boot API]
    API --> SEC[Spring Security + JWT]
    API --> AUTH[Auth]
    API --> COURSE[Courses]
    API --> ENR[Enrollments]
    API --> PROG[Progress]
    API --> INT[Interview]
    AUTH --> DB[(PostgreSQL 17)]
    COURSE --> DB
    ENR --> DB
    PROG --> DB
    API --> FLY[Flyway]
    FLY --> DB
~~~

### Fluxo de autenticação

~~~text
Usuário
   │
   ├── POST /api/v1/auth/register
   │
   └── POST /api/v1/auth/login
                │
                ▼
          JWT Bearer Token
                │
                ▼
      Spring Security Resource Server
                │
                ▼
         Endpoints protegidos
~~~

---

## Módulos do backend

O backend está organizado por domínio:

~~~text
br.com.techmind.academy
├── auth
├── course
├── enrollment
├── interview
├── learning
├── progress
├── security
└── user
~~~

| Módulo | Responsabilidade |
|---|---|
| <code>auth</code> | cadastro, login e emissão de token |
| <code>security</code> | Spring Security, JWT, BCrypt e CORS |
| <code>user</code> | domínio e perfil do aluno |
| <code>course</code> | catálogo de trilhas |
| <code>enrollment</code> | matrícula e evolução por trilha |
| <code>progress</code> | consolidação de XP e progresso |
| <code>interview</code> | avaliação local de respostas para entrevistas |
| <code>learning</code> | módulos, aulas, conclusão, XP e progresso detalhado |

---

## Stack

### Backend

| Tecnologia | Uso |
|---|---|
| Java 21 | linguagem principal |
| Spring Boot 3.5.5 | aplicação e API REST |
| Spring Web | camada HTTP |
| Spring Data JPA | persistência |
| Hibernate | ORM |
| Spring Security | autenticação e autorização |
| OAuth2 Resource Server | validação de Bearer JWT |
| BCrypt | hash de senhas |
| Flyway | migrations |
| PostgreSQL | banco relacional |
| Actuator | health e métricas básicas |
| Maven | build e dependências |

### Frontend

| Tecnologia | Versão / uso |
|---|---|
| React | 19.1.1 |
| TypeScript | 5.9.2 |
| Vite | 7.1.7 |
| Lucide React | ícones |
| CSS | identidade visual responsiva |

### Infraestrutura local

- Docker
- Docker Compose
- Nginx para servir o build do frontend
- PostgreSQL 17

---

## Modelo de dados

~~~mermaid
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
~~~

### Migrations

~~~text
V1__create_courses.sql
V2__create_users_and_enrollments.sql
V3__create_learning_engine.sql
~~~

O Hibernate está configurado com <code>ddl-auto: validate</code>, deixando a evolução do schema sob responsabilidade explícita do Flyway.

---

## API REST

### Endpoints públicos

| Método | Endpoint | Descrição |
|---|---|---|
| POST | <code>/api/v1/auth/register</code> | cria uma conta |
| POST | <code>/api/v1/auth/login</code> | autentica e retorna JWT |
| GET | <code>/api/v1/courses</code> | lista as trilhas |
| GET | <code>/api/v1/courses/{id}</code> | consulta uma trilha |
| GET | <code>/api/v1/courses/{id}/curriculum</code> | retorna módulos e aulas disponíveis |
| GET | <code>/actuator/health</code> | health check |

### Endpoints autenticados

| Método | Endpoint | Descrição |
|---|---|---|
| GET | <code>/api/v1/users/me</code> | retorna o perfil autenticado |
| POST | <code>/api/v1/enrollments/courses/{courseId}</code> | realiza matrícula |
| GET | <code>/api/v1/enrollments/me</code> | lista matrículas do usuário |
| GET | <code>/api/v1/learning/courses/{courseId}/progress</code> | retorna aulas concluídas e XP da trilha |
| POST | <code>/api/v1/learning/lessons/{lessonId}/complete</code> | conclui uma aula e credita XP de forma idempotente |
| GET | <code>/api/v1/progress/me</code> | retorna progresso agregado |
| POST | <code>/api/v1/interviews/evaluate</code> | avalia resposta de entrevista |

Exemplos de chamadas estão em **[docs/api.http](docs/api.http)**.

### Exemplo de cadastro

~~~http
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "Aluno TechMind",
  "email": "aluno@techmind.dev",
  "password": "senha-segura"
}
~~~

### Exemplo de endpoint protegido

~~~http
GET /api/v1/users/me
Authorization: Bearer <TOKEN>
~~~

---

## Segurança

A API utiliza:

- Spring Security
- sessão <code>STATELESS</code>
- Bearer Token
- JWT assinado com **HS256**
- issuer <code>techmind-ai-academy</code>
- validação de expiração
- BCrypt para armazenamento de senhas
- CORS configurável por variável de ambiente
- endpoints públicos explicitamente permitidos
- demais endpoints autenticados por padrão

### Segredos

**Nenhuma senha ou secret deve ser versionado.**

Use o arquivo <code>.env.example</code> apenas como referência e crie localmente um arquivo <code>.env</code> com suas credenciais.

~~~bash
cp .env.example .env
~~~

No Windows PowerShell:

~~~powershell
Copy-Item .env.example .env
~~~

O arquivo <code>.env</code> está ignorado pelo Git.

---

## Estrutura do projeto

~~~text
techmind-ai-academy/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/br/com/techmind/academy/
│   │   │   │   ├── auth/
│   │   │   │   ├── course/
│   │   │   │   ├── enrollment/
│   │   │   │   ├── interview/
│   │   │   │   ├── learning/
│   │   │   │   ├── progress/
│   │   │   │   ├── security/
│   │   │   │   └── user/
│   │   │   └── resources/
│   │   │       ├── db/migration/
│   │   │       └── application.yml
│   │   └── test/
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── public/
│   │   └── techmind-logo.png
│   ├── src/
│   │   ├── App.tsx
│   │   ├── main.tsx
│   │   └── styles.css
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── docs/
│   ├── api.http
│   └── ARCHITECTURE.md
├── .env.example
├── .gitignore
├── docker-compose.yml
└── README.md
~~~

---

## Como executar

### Pré-requisitos

Para desenvolvimento local:

- Java 21
- Maven 3.9+
- Node.js 22+
- npm
- Docker Desktop / Docker Engine
- Docker Compose

### Opção 1 — stack completa com Docker

1. Clone o repositório:

~~~bash
git clone https://github.com/juceliocoelho2022/techmind-ai-academy.git
cd techmind-ai-academy
~~~

2. Crie o arquivo local de ambiente:

~~~powershell
Copy-Item .env.example .env
~~~

3. Preencha no <code>.env</code> pelo menos:

~~~env
POSTGRES_DB=techmind
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me

DB_URL=jdbc:postgresql://postgres:5432/techmind
DB_USER=postgres
DB_PASSWORD=change-me

JWT_SECRET=change-me-with-at-least-32-bytes
JWT_EXPIRATION_MINUTES=120

CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
~~~

4. Suba a stack:

~~~bash
docker compose up --build
~~~

Acessos:

| Serviço | URL |
|---|---|
| Frontend Docker | http://localhost:3000 |
| Backend | http://localhost:8080 |
| Health | http://localhost:8080/actuator/health |
| PostgreSQL | localhost:5432 |

### Opção 2 — desenvolvimento híbrido

Suba apenas o PostgreSQL:

~~~powershell
docker compose up -d postgres
~~~

No terminal do backend, defina as variáveis necessárias e execute:

~~~powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/techmind"
$env:DB_USER="postgres"
$env:DB_PASSWORD="<sua-senha-local>"
$env:JWT_SECRET="<secret-local-com-pelo-menos-32-bytes>"

cd backend
mvn spring-boot:run
~~~

Em outro terminal:

~~~powershell
cd frontend
npm install
npm run dev
~~~

Frontend de desenvolvimento:

~~~text
http://localhost:5173
~~~

O Vite encaminha as chamadas <code>/api</code> para o backend local.

---

## Testes

### Backend

~~~bash
cd backend
mvn test
~~~

Atualmente há testes nos módulos de:

- autenticação
- entrevista técnica

### Frontend

Validação do build:

~~~bash
cd frontend
npm install
npm run build
~~~

---

## Decisões de engenharia

### Monólito modular

O MVP utiliza monólito modular em vez de microsserviços para:

- reduzir complexidade operacional
- manter deploy simples
- preservar separação por domínio
- acelerar validação do produto
- permitir futura extração de serviços quando houver necessidade real

### Flyway como fonte de verdade do schema

As alterações estruturais do banco são versionadas por migrations. O Hibernate apenas valida o schema existente.

### JWT stateless

A autenticação não depende de sessão no servidor. Isso facilita escalabilidade horizontal e desacopla o frontend do estado da aplicação.

### Variáveis de ambiente

Credenciais de banco, segredo JWT e configurações de CORS são externalizadas e não devem fazer parte do código-fonte.

---

## Roadmap

### v0.3 — Learning Engine

- [x] módulos
- [x] aulas
- [x] conclusão idempotente de aula
- [ ] quizzes
- [ ] desafios de código
- [x] XP por aula calculado no servidor
- [ ] badges
- [ ] dashboard detalhado de evolução

### v0.4 — Mentor IA

- [ ] integração real com LLM
- [ ] avaliação estruturada de entrevistas
- [ ] plano de estudos personalizado
- [ ] prompts versionados
- [ ] histórico de sessões
- [ ] RAG sobre materiais das trilhas

### v0.5 — Produto

- [ ] painel administrativo
- [ ] gestão de conteúdo
- [ ] certificados
- [ ] assinatura/pagamentos
- [ ] observabilidade
- [x] pipeline CI de build/test
- [ ] pipeline CD
- [ ] deploy cloud
- [ ] métricas de produto e aprendizagem

---

## Visão de evolução

~~~text
MVP v0.2
Autenticação + Trilhas + Matrículas + Progresso
                         │
                         ▼
v0.3 Learning Engine
Módulos + Aulas + Desafios + XP + Badges
                         │
                         ▼
v0.4 Mentor IA
LLM + Entrevistas + Plano de Estudo + RAG
                         │
                         ▼
v0.5 Produto
Admin + Certificados + Observabilidade + Cloud
~~~

---

## Autor

Projeto mantido por **[@juceliocoelho2022](https://github.com/juceliocoelho2022)**.

Este projeto faz parte da evolução da **TechMind AI Academy** como plataforma de educação tecnológica orientada a prática, projetos e desenvolvimento profissional.

---

<p align="center">
  <strong>TechMind AI Academy</strong><br>
  Aprenda. Pratique. Construa. Evolua.
</p>
