package br.com.techmind.academy.learning;

import br.com.techmind.academy.course.Course;
import br.com.techmind.academy.course.CourseLevel;
import br.com.techmind.academy.course.CourseRepository;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class CourseTemplateCatalogService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    public CourseTemplateCatalogService(
            CourseRepository courseRepository,
            CourseModuleRepository moduleRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository
    ) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseTemplateResponse> catalog(String email) {
        requireAdmin(email);
        return templates();
    }

    @Transactional
    public AdminCourseResponse instantiate(
            String email,
            String templateKey,
            CourseTemplateInstantiateRequest request
    ) {
        requireAdmin(email);

        var template = templates().stream()
                .filter(item -> item.key().equals(templateKey))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Template de curso não encontrado"
                ));

        var slug = normalizeSlug(request.slug());
        courseRepository.findBySlug(slug).ifPresent(existing -> {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe uma trilha com este slug"
            );
        });

        var course = Course.builder()
                .slug(slug)
                .title(request.title().trim())
                .description(request.description().trim())
                .category(template.category())
                .technology(template.technology())
                .level(request.level())
                .totalLessons(template.totalLessons())
                .build();

        var savedCourse = courseRepository.save(course);

        if (Boolean.TRUE.equals(request.createStructure())) {
            for (var moduleTemplate : template.modules()) {
                var module = CourseModule.builder()
                        .course(savedCourse)
                        .title(moduleTemplate.title())
                        .description(moduleTemplate.description())
                        .position(moduleTemplate.position())
                        .build();

                var savedModule = moduleRepository.save(module);

                var lessons = moduleTemplate.lessons().stream()
                        .map(lessonTemplate -> Lesson.builder()
                                .module(savedModule)
                                .slug(lessonTemplate.slug())
                                .title(lessonTemplate.title())
                                .summary(lessonTemplate.summary())
                                .position(lessonTemplate.position())
                                .xpReward(lessonTemplate.xpReward())
                                .build())
                        .toList();

                lessonRepository.saveAll(lessons);
            }
        }

        return AdminCourseResponse.from(savedCourse);
    }

    private List<CourseTemplateResponse> templates() {
        return List.of(
                template(
                        "java-backend",
                        "Backend",
                        "Java",
                        CourseLevel.INTERMEDIATE,
                        "Java Backend",
                        "java-backend",
                        "Java 21, orientação a objetos, APIs REST, persistência, testes e práticas de backend.",
                        module(
                                1,
                                "Fundamentos de Java",
                                "Base moderna da linguagem e orientação a objetos.",
                                lesson(1, "java-21", "Java 21 na prática", "Sintaxe moderna, tipos, records e recursos atuais.", 10),
                                lesson(2, "poo", "Orientação a Objetos", "Classes, interfaces, encapsulamento e polimorfismo.", 15)
                        ),
                        module(
                                2,
                                "APIs e Spring",
                                "Construção de APIs REST profissionais.",
                                lesson(1, "spring-boot-intro", "Spring Boot Essencial", "IoC, DI, camadas e configuração.", 15),
                                lesson(2, "rest-api", "APIs REST", "Controllers, DTOs, validação e status HTTP.", 20)
                        ),
                        module(
                                3,
                                "Persistência",
                                "Integração com bancos relacionais.",
                                lesson(1, "jpa-hibernate", "JPA e Hibernate", "Entidades, relacionamentos e transações.", 20),
                                lesson(2, "postgresql", "PostgreSQL para Backend", "Modelagem, consultas e integração com Spring.", 20)
                        ),
                        module(
                                4,
                                "Qualidade e Deploy",
                                "Testes e empacotamento da aplicação.",
                                lesson(1, "junit-mockito", "JUnit e Mockito", "Testes unitários e isolamento de dependências.", 20),
                                lesson(2, "docker-backend", "Docker para Java", "Containerização e execução da API.", 20)
                        )
                ),
                template(
                        "spring-boot",
                        "Backend",
                        "Spring Boot",
                        CourseLevel.INTERMEDIATE,
                        "Spring Boot",
                        "spring-boot-profissional",
                        "Spring Boot com arquitetura em camadas, JPA, segurança, testes, observabilidade e resiliência.",
                        module(
                                1,
                                "Arquitetura Spring",
                                "Estrutura profissional de aplicações Spring.",
                                lesson(1, "camadas-spring", "Camadas e Responsabilidades", "Controller, Service, Repository e DTO.", 15),
                                lesson(2, "validation-problemdetail", "Validation e ProblemDetail", "Validação e tratamento padronizado de erros.", 20)
                        ),
                        module(
                                2,
                                "Dados e Transações",
                                "Persistência consistente com Spring Data.",
                                lesson(1, "spring-data-jpa", "Spring Data JPA", "Repositories, queries e paginação.", 20),
                                lesson(2, "transactions", "Transações", "Atomicidade e uso correto de @Transactional.", 20)
                        ),
                        module(
                                3,
                                "Segurança e Resiliência",
                                "Proteção e tolerância a falhas.",
                                lesson(1, "spring-security-jwt", "Spring Security e JWT", "Autenticação stateless e autorização.", 25),
                                lesson(2, "resilience4j", "Resilience4j", "Retry, Circuit Breaker e Rate Limiter.", 25)
                        ),
                        module(
                                4,
                                "Produção",
                                "Qualidade e observabilidade.",
                                lesson(1, "spring-tests", "Testes com Spring Boot", "MockMvc, integração e cobertura.", 20),
                                lesson(2, "actuator-observability", "Actuator e Observabilidade", "Health, métricas e sinais da aplicação.", 20)
                        )
                ),
                template(
                        "react-js",
                        "Frontend",
                        "React.js",
                        CourseLevel.BEGINNER,
                        "React.js",
                        "react-js",
                        "React moderno com JavaScript/TypeScript, componentes, estado, consumo de APIs e projeto final.",
                        module(
                                1,
                                "Base Web Moderna",
                                "Fundamentos necessários para trabalhar com React.",
                                lesson(1, "javascript-moderno", "JavaScript Moderno", "ES Modules, arrays, objetos e funções.", 10),
                                lesson(2, "typescript-base", "TypeScript Essencial", "Tipos, interfaces e tipagem segura.", 15)
                        ),
                        module(
                                2,
                                "Fundamentos React",
                                "Componentização e composição de interfaces.",
                                lesson(1, "componentes-props", "Componentes e Props", "Componentes reutilizáveis e composição.", 15),
                                lesson(2, "state-hooks", "State e Hooks", "useState, useEffect e fluxo de dados.", 20)
                        ),
                        module(
                                3,
                                "Integração",
                                "Dados externos e navegação.",
                                lesson(1, "react-rest-api", "Consumo de APIs REST", "Fetch, loading, erros e autenticação.", 20),
                                lesson(2, "react-router", "Rotas no Frontend", "Navegação e páginas com React Router.", 20)
                        ),
                        module(
                                4,
                                "Projeto",
                                "Entrega de uma aplicação completa.",
                                lesson(1, "vite-build", "Build com Vite", "Ambiente, variáveis e build de produção.", 15),
                                lesson(2, "projeto-react", "Projeto React Completo", "Aplicação integrada a uma API real.", 30)
                        )
                ),
                template(
                        "database-sql",
                        "Banco de Dados",
                        "SQL",
                        CourseLevel.BEGINNER,
                        "Banco de Dados & SQL",
                        "banco-de-dados-sql",
                        "Modelagem relacional, SQL, joins, transações, índices e projeto de banco de dados.",
                        module(
                                1,
                                "Modelagem",
                                "Estrutura e desenho de bancos relacionais.",
                                lesson(1, "modelo-relacional", "Modelo Relacional", "Tabelas, chaves e relacionamentos.", 10),
                                lesson(2, "normalizacao", "Normalização", "1FN, 2FN, 3FN e integridade.", 15)
                        ),
                        module(
                                2,
                                "SQL",
                                "Consultas e manipulação de dados.",
                                lesson(1, "select-joins", "SELECT e JOINs", "Filtros, agrupamentos e relacionamentos.", 20),
                                lesson(2, "dml", "INSERT, UPDATE e DELETE", "Manipulação segura de dados.", 15)
                        ),
                        module(
                                3,
                                "Confiabilidade",
                                "Consistência e desempenho.",
                                lesson(1, "transactions-sql", "Transações", "Commit, rollback e ACID.", 20),
                                lesson(2, "indexes", "Índices", "Estratégias de indexação e performance.", 20)
                        ),
                        module(
                                4,
                                "Projeto",
                                "Aplicação prática da modelagem e SQL.",
                                lesson(1, "views-procedures", "Views e Procedures", "Reuso e encapsulamento no banco.", 20),
                                lesson(2, "projeto-banco", "Projeto de Banco de Dados", "Modelagem, criação e consultas completas.", 30)
                        )
                ),
                template(
                        "postgresql",
                        "Banco de Dados",
                        "PostgreSQL",
                        CourseLevel.INTERMEDIATE,
                        "PostgreSQL",
                        "postgresql",
                        "PostgreSQL com modelagem, SQL avançado, desempenho, segurança, backup e operação.",
                        module(
                                1,
                                "Fundamentos PostgreSQL",
                                "Ambiente e arquitetura básica.",
                                lesson(1, "postgres-install", "Ambiente PostgreSQL", "Instalação, bancos, schemas e usuários.", 10),
                                lesson(2, "postgres-types", "Tipos e Constraints", "Tipos, PK, FK, UNIQUE e CHECK.", 15)
                        ),
                        module(
                                2,
                                "SQL Avançado",
                                "Recursos poderosos para consulta.",
                                lesson(1, "cte-window", "CTEs e Window Functions", "Consultas analíticas e legíveis.", 20),
                                lesson(2, "jsonb", "JSONB", "Dados semiestruturados no PostgreSQL.", 20)
                        ),
                        module(
                                3,
                                "Performance",
                                "Diagnóstico e otimização.",
                                lesson(1, "explain-analyze", "EXPLAIN ANALYZE", "Leitura de planos de execução.", 25),
                                lesson(2, "postgres-indexes", "Índices PostgreSQL", "B-tree, GIN e estratégias.", 25)
                        ),
                        module(
                                4,
                                "Operação",
                                "Segurança e continuidade.",
                                lesson(1, "roles-security", "Roles e Segurança", "Privilégios e segregação de acesso.", 20),
                                lesson(2, "backup-restore", "Backup e Restore", "pg_dump, pg_restore e estratégia.", 20)
                        )
                ),
                template(
                        "aws-cloud",
                        "Cloud",
                        "AWS",
                        CourseLevel.BEGINNER,
                        "AWS Cloud",
                        "aws-cloud-profissional",
                        "Fundamentos AWS, redes, compute, storage, bancos, containers, observabilidade e custos.",
                        module(
                                1,
                                "Fundamentos AWS",
                                "Serviços e modelo de responsabilidade.",
                                lesson(1, "iam", "IAM", "Usuários, roles, policies e least privilege.", 15),
                                lesson(2, "aws-regions", "Regiões e AZs", "Resiliência e distribuição de workloads.", 10)
                        ),
                        module(
                                2,
                                "Compute e Rede",
                                "Infraestrutura para aplicações.",
                                lesson(1, "ec2", "Amazon EC2", "Instâncias, AMIs e Security Groups.", 20),
                                lesson(2, "vpc", "Amazon VPC", "Subnets, rotas, IGW e NAT.", 25)
                        ),
                        module(
                                3,
                                "Dados",
                                "Armazenamento e bancos gerenciados.",
                                lesson(1, "s3", "Amazon S3", "Buckets, políticas e versionamento.", 20),
                                lesson(2, "rds", "Amazon RDS", "Bancos relacionais gerenciados.", 20)
                        ),
                        module(
                                4,
                                "Aplicações",
                                "Containers e observabilidade.",
                                lesson(1, "ecs-ecr", "ECR e ECS", "Imagens e execução de containers.", 25),
                                lesson(2, "cloudwatch", "CloudWatch", "Logs, métricas e alarmes.", 20)
                        )
                ),
                template(
                        "devops",
                        "DevOps",
                        "DevOps",
                        CourseLevel.INTERMEDIATE,
                        "DevOps",
                        "devops",
                        "Git, Linux, Docker, CI/CD, Kubernetes, infraestrutura como código e observabilidade.",
                        module(
                                1,
                                "Fundamentos",
                                "Base para automação e entrega.",
                                lesson(1, "git-github", "Git e GitHub", "Branches, pull requests e fluxo de trabalho.", 15),
                                lesson(2, "linux-shell", "Linux e Shell", "Comandos, processos e automação.", 15)
                        ),
                        module(
                                2,
                                "Containers",
                                "Empacotamento e execução consistente.",
                                lesson(1, "docker", "Docker", "Images, containers e volumes.", 20),
                                lesson(2, "docker-compose", "Docker Compose", "Ambientes multi-serviço.", 20)
                        ),
                        module(
                                3,
                                "Entrega Contínua",
                                "Automação de build, teste e deploy.",
                                lesson(1, "github-actions", "GitHub Actions", "Pipelines de CI/CD.", 25),
                                lesson(2, "terraform", "Terraform", "Infraestrutura como código.", 25)
                        ),
                        module(
                                4,
                                "Orquestração",
                                "Operação de aplicações distribuídas.",
                                lesson(1, "kubernetes", "Kubernetes", "Pods, Deployments e Services.", 25),
                                lesson(2, "observability", "Observabilidade", "Métricas, logs e traces.", 25)
                        )
                ),
                template(
                        "java-programming",
                        "Programação",
                        "Java",
                        CourseLevel.BEGINNER,
                        "Java",
                        "java-fundamentos",
                        "Java do zero com sintaxe, orientação a objetos, collections, exceções, lambdas, streams e projeto prático.",
                        module(
                                1,
                                "Fundamentos da Linguagem",
                                "Primeiros passos com Java moderno e lógica de programação.",
                                lesson(1, "java-primeiros-passos", "Java 21 — Primeiros Passos", "JDK, estrutura de um programa, variáveis, tipos e operadores.", 10),
                                lesson(2, "controle-fluxo-metodos", "Controle de Fluxo e Métodos", "Condições, loops, métodos, parâmetros e retorno.", 15)
                        ),
                        module(
                                2,
                                "Orientação a Objetos",
                                "Modelagem de software com os fundamentos de OOP.",
                                lesson(1, "classes-objetos", "Classes, Objetos e Encapsulamento", "Atributos, construtores, métodos e modificadores de acesso.", 20),
                                lesson(2, "heranca-polimorfismo", "Herança, Interfaces e Polimorfismo", "Reuso, abstração, contratos e comportamento polimórfico.", 20)
                        ),
                        module(
                                3,
                                "Java Essencial",
                                "Estruturas e tratamento seguro de dados.",
                                lesson(1, "collections-java", "Collections", "List, Set, Map, generics e escolha da estrutura adequada.", 20),
                                lesson(2, "exceptions-optional", "Exceptions e Optional", "Tratamento de erros, exceções customizadas e ausência de valores.", 20)
                        ),
                        module(
                                4,
                                "Java Moderno e Projeto",
                                "Programação funcional, qualidade e aplicação prática.",
                                lesson(1, "lambdas-streams", "Lambdas e Streams", "Filter, map, reduce, method references e processamento declarativo.", 25),
                                lesson(2, "projeto-java-console", "Projeto Final em Java", "Aplicação orientada a objetos com collections, testes e organização em camadas simples.", 30)
                        )
                ),
                template(
                        "python",
                        "Programação",
                        "Python",
                        CourseLevel.BEGINNER,
                        "Python",
                        "python",
                        "Python do zero a aplicações práticas, APIs, automação, testes e manipulação de dados.",
                        module(
                                1,
                                "Fundamentos Python",
                                "Sintaxe e estruturas básicas.",
                                lesson(1, "python-syntax", "Sintaxe e Tipos", "Variáveis, tipos e operadores.", 10),
                                lesson(2, "python-control", "Controle de Fluxo", "Condições, loops e funções.", 10)
                        ),
                        module(
                                2,
                                "Estruturas e OOP",
                                "Organização de dados e código.",
                                lesson(1, "python-collections", "Collections", "Listas, dicionários, sets e tuples.", 15),
                                lesson(2, "python-oop", "Orientação a Objetos", "Classes, herança e composição.", 20)
                        ),
                        module(
                                3,
                                "Aplicações",
                                "APIs e automação.",
                                lesson(1, "fastapi", "FastAPI", "Construção de APIs REST com Python.", 20),
                                lesson(2, "python-automation", "Automação", "Scripts para tarefas repetitivas.", 20)
                        ),
                        module(
                                4,
                                "Qualidade",
                                "Testes e projeto final.",
                                lesson(1, "pytest", "Pytest", "Testes automatizados e fixtures.", 20),
                                lesson(2, "python-project", "Projeto Python", "Aplicação completa e organizada.", 30)
                        )
                ),
                template(
                        "generative-ai",
                        "Dados & IA",
                        "IA Generativa",
                        CourseLevel.INTERMEDIATE,
                        "IA Generativa",
                        "ia-generativa",
                        "LLMs, prompt engineering, RAG, agentes, tool calling, guardrails e avaliação de aplicações de IA.",
                        module(
                                1,
                                "Fundamentos de LLMs",
                                "Como modelos de linguagem funcionam em aplicações.",
                                lesson(1, "llm-basics", "LLMs na prática", "Tokens, contexto e geração.", 15),
                                lesson(2, "prompt-engineering", "Prompt Engineering", "Estrutura, contexto e padrões.", 20)
                        ),
                        module(
                                2,
                                "RAG",
                                "Aplicações baseadas em recuperação.",
                                lesson(1, "embeddings", "Embeddings e Busca", "Representação vetorial e recuperação.", 20),
                                lesson(2, "rag-pipeline", "Pipeline RAG", "Retrieve, context e geração.", 25)
                        ),
                        module(
                                3,
                                "Agentes",
                                "Automação com ferramentas.",
                                lesson(1, "tool-calling", "Tool Calling", "Ferramentas e ações externas.", 25),
                                lesson(2, "ai-agents", "Agentes de IA", "Planejamento e execução orientada a objetivos.", 25)
                        ),
                        module(
                                4,
                                "Qualidade e Segurança",
                                "Confiabilidade de aplicações com IA.",
                                lesson(1, "guardrails", "Guardrails", "Limites e validações de segurança.", 20),
                                lesson(2, "evals", "Evals", "Avaliação sistemática de respostas.", 25)
                        )
                ),
                template(
                        "android-kotlin",
                        "Mobile",
                        "Kotlin / Android",
                        CourseLevel.INTERMEDIATE,
                        "Android com Kotlin",
                        "android-kotlin",
                        "Kotlin, Jetpack Compose, arquitetura, navegação, APIs e testes para Android moderno.",
                        module(
                                1,
                                "Kotlin",
                                "Fundamentos modernos da linguagem.",
                                lesson(1, "kotlin-basics", "Kotlin Essencial", "Null safety, funções e data classes.", 15),
                                lesson(2, "coroutines", "Coroutines", "Concorrência e programação assíncrona.", 20)
                        ),
                        module(
                                2,
                                "Jetpack Compose",
                                "Interfaces declarativas modernas.",
                                lesson(1, "compose-ui", "UI com Compose", "Composables, state e layouts.", 20),
                                lesson(2, "compose-navigation", "Navigation", "Fluxo entre telas e parâmetros.", 20)
                        ),
                        module(
                                3,
                                "Arquitetura",
                                "Organização do app.",
                                lesson(1, "mvvm", "MVVM", "ViewModel, estado e separação de responsabilidades.", 20),
                                lesson(2, "android-api", "Consumo de APIs", "HTTP, DTOs e tratamento de erros.", 20)
                        ),
                        module(
                                4,
                                "Qualidade",
                                "Persistência e testes.",
                                lesson(1, "room", "Room Database", "Persistência local estruturada.", 20),
                                lesson(2, "android-tests", "Testes Android", "Testes unitários e de UI.", 25)
                        )
                )
        );
    }

    private CourseTemplateResponse template(
            String key,
            String category,
            String technology,
            CourseLevel level,
            String title,
            String slug,
            String description,
            CourseTemplateResponse.ModuleTemplate... modules
    ) {
        var moduleList = List.of(modules);
        int totalLessons = moduleList.stream()
                .mapToInt(module -> module.lessons().size())
                .sum();

        return new CourseTemplateResponse(
                key,
                category,
                technology,
                level,
                title,
                slug,
                description,
                totalLessons,
                moduleList
        );
    }

    private CourseTemplateResponse.ModuleTemplate module(
            int position,
            String title,
            String description,
            CourseTemplateResponse.LessonTemplate... lessons
    ) {
        return new CourseTemplateResponse.ModuleTemplate(
                title,
                description,
                position,
                List.of(lessons)
        );
    }

    private CourseTemplateResponse.LessonTemplate lesson(
            int position,
            String slug,
            String title,
            String summary,
            int xpReward
    ) {
        return new CourseTemplateResponse.LessonTemplate(
                slug,
                title,
                summary,
                position,
                xpReward
        );
    }

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase(Locale.ROOT);
    }

    private void requireAdmin(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));

        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acesso restrito ao administrador"
            );
        }
    }
}
