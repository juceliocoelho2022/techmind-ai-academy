-- Restores the learning catalog in environments where seed rows were removed
-- while preserving existing users, enrollments and schema history.

INSERT INTO courses (slug, title, description, total_lessons) VALUES
('java-backend', 'Java Backend', 'Java 21, orientação a objetos, APIs REST, testes e boas práticas.', 20),
('spring-boot', 'Spring Boot', 'Controllers, Services, JPA, validação, segurança, observabilidade e resiliência.', 18),
('aws-cloud', 'AWS Cloud', 'Fundamentos AWS, arquitetura, containers, bancos, observabilidade e custos.', 16),
('data-ai', 'Dados & IA', 'Python, SQL, engenharia de dados, RAG, agentes e aplicações com IA.', 22)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    total_lessons = EXCLUDED.total_lessons;

INSERT INTO course_modules (course_id, title, description, position) VALUES
((SELECT id FROM courses WHERE slug = 'java-backend'), 'Fundamentos de Java', 'Base sólida em Java 21, orientação a objetos e coleções.', 1),
((SELECT id FROM courses WHERE slug = 'java-backend'), 'APIs Backend', 'Construção de APIs REST e práticas de backend profissional.', 2),
((SELECT id FROM courses WHERE slug = 'spring-boot'), 'Fundamentos Spring', 'Injeção de dependência, camadas e construção de APIs.', 1),
((SELECT id FROM courses WHERE slug = 'spring-boot'), 'Persistência e Segurança', 'JPA, validação, transações e segurança de APIs.', 2),
((SELECT id FROM courses WHERE slug = 'aws-cloud'), 'Fundamentos AWS', 'Serviços essenciais e conceitos de arquitetura em nuvem.', 1),
((SELECT id FROM courses WHERE slug = 'aws-cloud'), 'Containers e Operação', 'Execução, observabilidade e operação de workloads.', 2),
((SELECT id FROM courses WHERE slug = 'data-ai'), 'Dados para IA', 'SQL, preparação de dados e fundamentos de pipelines.', 1),
((SELECT id FROM courses WHERE slug = 'data-ai'), 'IA Aplicada', 'RAG, agentes, prompts e aplicações com modelos de linguagem.', 2)
ON CONFLICT (course_id, position) DO UPDATE SET
    title = EXCLUDED.title,
    description = EXCLUDED.description;

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
((SELECT m.id FROM course_modules m JOIN courses c ON c.id = m.course_id WHERE c.slug = 'data-ai' AND m.position = 2), 'ai-evals-guardrails', 'Evals e Guardrails', 'Qualidade, segurança e avaliação de sistemas com IA.', 3, 25)
ON CONFLICT (module_id, slug) DO UPDATE SET
    title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    position = EXCLUDED.position,
    xp_reward = EXCLUDED.xp_reward;
