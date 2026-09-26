CREATE TABLE course_modules (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(500) NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT uk_course_module_position UNIQUE (course_id, position),
    CONSTRAINT ck_course_module_position_positive CHECK (position > 0)
);

CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,
    module_id BIGINT NOT NULL REFERENCES course_modules(id) ON DELETE CASCADE,
    slug VARCHAR(120) NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    position INTEGER NOT NULL,
    xp_reward INTEGER NOT NULL DEFAULT 10,
    CONSTRAINT uk_lesson_module_slug UNIQUE (module_id, slug),
    CONSTRAINT uk_lesson_module_position UNIQUE (module_id, position),
    CONSTRAINT ck_lesson_position_positive CHECK (position > 0),
    CONSTRAINT ck_lesson_xp_nonnegative CHECK (xp_reward >= 0)
);

CREATE TABLE lesson_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    xp_awarded INTEGER NOT NULL,
    CONSTRAINT uk_lesson_progress_user_lesson UNIQUE (user_id, lesson_id),
    CONSTRAINT ck_lesson_progress_xp_nonnegative CHECK (xp_awarded >= 0)
);

CREATE INDEX idx_course_modules_course_id ON course_modules(course_id);
CREATE INDEX idx_lessons_module_id ON lessons(module_id);
CREATE INDEX idx_lesson_progress_user_id ON lesson_progress(user_id);
CREATE INDEX idx_lesson_progress_lesson_id ON lesson_progress(lesson_id);

INSERT INTO course_modules (course_id, title, description, position) VALUES
((SELECT id FROM courses WHERE slug = 'java-backend'), 'Fundamentos de Java', 'Base sólida em Java 21, orientação a objetos e coleções.', 1),
((SELECT id FROM courses WHERE slug = 'java-backend'), 'APIs Backend', 'Construção de APIs REST e práticas de backend profissional.', 2),
((SELECT id FROM courses WHERE slug = 'spring-boot'), 'Fundamentos Spring', 'Injeção de dependência, camadas e construção de APIs.', 1),
((SELECT id FROM courses WHERE slug = 'spring-boot'), 'Persistência e Segurança', 'JPA, validação, transações e segurança de APIs.', 2),
((SELECT id FROM courses WHERE slug = 'aws-cloud'), 'Fundamentos AWS', 'Serviços essenciais e conceitos de arquitetura em nuvem.', 1),
((SELECT id FROM courses WHERE slug = 'aws-cloud'), 'Containers e Operação', 'Execução, observabilidade e operação de workloads.', 2),
((SELECT id FROM courses WHERE slug = 'data-ai'), 'Dados para IA', 'SQL, preparação de dados e fundamentos de pipelines.', 1),
((SELECT id FROM courses WHERE slug = 'data-ai'), 'IA Aplicada', 'RAG, agentes, prompts e aplicações com modelos de linguagem.', 2);

INSERT INTO lessons (module_id, slug, title, summary, position, xp_reward) VALUES
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'java-backend' AND m.position = 1), 'java-21-visao-geral', 'Java 21 na prática', 'Recursos modernos da linguagem e organização de um projeto Java.', 1, 10),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'java-backend' AND m.position = 1), 'poo-e-interfaces', 'POO, interfaces e abstrações', 'Modelagem orientada a objetos com foco em código sustentável.', 2, 15),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'java-backend' AND m.position = 1), 'collections-streams', 'Collections e Streams', 'Manipulação de dados com Collections API e Streams.', 3, 20),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'java-backend' AND m.position = 2), 'rest-fundamentos', 'Fundamentos de REST', 'Recursos, verbos HTTP, status codes e contratos de API.', 1, 15),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'java-backend' AND m.position = 2), 'dto-validation', 'DTOs e validação', 'Contratos de entrada e saída com validações explícitas.', 2, 20),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'java-backend' AND m.position = 2), 'testes-backend', 'Testes de backend', 'Estratégias unitárias e de integração para APIs Java.', 3, 25),

((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'spring-boot' AND m.position = 1), 'spring-di', 'Dependency Injection', 'IoC, beans e injeção de dependências no Spring.', 1, 10),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'spring-boot' AND m.position = 1), 'spring-layers', 'Controllers, Services e Repositories', 'Separação de responsabilidades em aplicações Spring Boot.', 2, 15),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'spring-boot' AND m.position = 1), 'spring-errors', 'Tratamento de erros', 'Problem Details e tratamento centralizado de exceções.', 3, 20),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'spring-boot' AND m.position = 2), 'spring-jpa', 'JPA e Hibernate', 'Mapeamento relacional, repositories e consultas.', 1, 15),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'spring-boot' AND m.position = 2), 'spring-transactions', 'Transações', 'Consistência e limites transacionais com @Transactional.', 2, 20),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'spring-boot' AND m.position = 2), 'spring-security-jwt', 'Spring Security e JWT', 'Proteção de endpoints com autenticação stateless.', 3, 25),

((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'aws-cloud' AND m.position = 1), 'aws-regions-az', 'Regions e Availability Zones', 'Fundamentos de infraestrutura global da AWS.', 1, 10),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'aws-cloud' AND m.position = 1), 'aws-iam', 'IAM e segurança', 'Usuários, roles, policies e princípio do menor privilégio.', 2, 15),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'aws-cloud' AND m.position = 1), 'aws-networking', 'VPC e redes', 'Subnets, rotas, security groups e conectividade.', 3, 20),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'aws-cloud' AND m.position = 2), 'aws-containers', 'Containers na AWS', 'ECR, ECS e fundamentos de workloads conteinerizados.', 1, 15),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'aws-cloud' AND m.position = 2), 'aws-observability', 'Observabilidade', 'Logs, métricas e alarmes com serviços AWS.', 2, 20),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'aws-cloud' AND m.position = 2), 'aws-costs', 'Custos e arquitetura', 'Trade-offs de arquitetura e fundamentos de otimização de custos.', 3, 25),

((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'data-ai' AND m.position = 1), 'sql-data', 'SQL para dados', 'Consultas, joins, agregações e preparação de datasets.', 1, 10),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'data-ai' AND m.position = 1), 'data-pipelines', 'Pipelines de dados', 'Ingestão, transformação e qualidade de dados.', 2, 15),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'data-ai' AND m.position = 1), 'data-quality', 'Qualidade de dados', 'Validação, consistência e observabilidade de dados.', 3, 20),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'data-ai' AND m.position = 2), 'rag-fundamentos', 'Fundamentos de RAG', 'Recuperação de contexto para aplicações com LLMs.', 1, 15),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'data-ai' AND m.position = 2), 'ai-agents', 'Agentes de IA', 'Tool calling, planejamento e execução orientada a objetivos.', 2, 20),
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'data-ai' AND m.position = 2), 'ai-evals-guardrails', 'Evals e Guardrails', 'Qualidade, segurança e avaliação de sistemas com IA.', 3, 25);
