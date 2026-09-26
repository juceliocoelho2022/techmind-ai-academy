CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(500) NOT NULL,
    total_lessons INTEGER NOT NULL
);

INSERT INTO courses (slug, title, description, total_lessons) VALUES
('java-backend', 'Java Backend', 'Java 21, orientação a objetos, APIs REST, testes e boas práticas.', 20),
('spring-boot', 'Spring Boot', 'Controllers, Services, JPA, validação, segurança, observabilidade e resiliência.', 18),
('aws-cloud', 'AWS Cloud', 'Fundamentos AWS, arquitetura, containers, bancos, observabilidade e custos.', 16),
('data-ai', 'Dados & IA', 'Python, SQL, engenharia de dados, RAG, agentes e aplicações com IA.', 22);
