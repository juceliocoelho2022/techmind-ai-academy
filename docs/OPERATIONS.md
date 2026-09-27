# TechMind AI Academy — Operação, Backup e Recuperação

Este documento descreve os procedimentos mínimos para operar a TechMind como ferramenta de trabalho sem depender apenas do ambiente local.

## Regra principal

Nunca use:

~~~powershell
docker compose down -v
~~~

O parâmetro `-v` remove os volumes Docker. Na TechMind isso pode apagar:

- PostgreSQL;
- usuários;
- matrículas;
- progresso;
- quizzes e tentativas;
- configurações;
- audit log;
- materiais enviados às aulas.

Para parar a stack, use:

~~~powershell
docker compose down
~~~

## Backup completo

O script `scripts/backup.ps1` cria uma pasta versionada por data/hora contendo:

~~~text
backups/
└── techmind-YYYYMMDD-HHMMSS/
    ├── database.dump
    ├── learning-resources.tar.gz
    └── manifest.json
~~~

O backup inclui:

- dump PostgreSQL em formato custom do `pg_dump`;
- materiais armazenados no volume do backend;
- checksums SHA-256 para detectar corrupção.

### Executar

Na raiz do projeto:

~~~powershell
.\scripts\backup.ps1
~~~

Ou escolher outro destino:

~~~powershell
.\scripts\backup.ps1 -Destination "D:\TechMindBackups"
~~~

## Restore

O restore substitui o estado atual da aplicação.

Execute:

~~~powershell
.\scripts\restore.ps1 -BackupPath ".\backups\techmind-20260927-120000"
~~~

O script exige confirmação digitando:

~~~text
RESTORE
~~~

Para automação controlada:

~~~powershell
.\scripts\restore.ps1 -BackupPath ".\backups\techmind-20260927-120000" -Force
~~~

Depois valide:

~~~powershell
docker compose ps
Invoke-RestMethod "http://localhost:8080/actuator/health"
~~~

## Política recomendada

Enquanto a aplicação estiver em uso real:

- backup diário do banco e materiais;
- manter pelo menos 7 backups diários;
- manter 4 backups semanais;
- copiar pelo menos uma cópia para armazenamento fora da máquina principal;
- testar restore periodicamente;
- nunca considerar um backup confiável sem teste de recuperação.

## Audit Log

A migration `V10__create_admin_audit_log.sql` cria um histórico de operações administrativas importantes.

Endpoint:

~~~http
GET /api/v1/admin/audit?limit=100
Authorization: Bearer <ADMIN_TOKEN>
~~~

O log registra:

- administrador responsável;
- ação;
- tipo do objeto;
- identificador;
- resumo sem senha/token;
- data e hora.

Exemplos de ações:

- CREATE / UPDATE / DELETE de trilhas, módulos e aulas;
- upload e exclusão de materiais;
- criação/edição/exclusão de quiz;
- mudança de papel de usuário;
- alteração de configurações;
- criação de trilha via template.

## Checklist antes de uso real

1. Fazer backup completo.
2. Validar restore em ambiente de teste.
3. Usar senha forte do PostgreSQL.
4. Usar `JWT_SECRET` longo e exclusivo.
5. Configurar CORS apenas para o domínio oficial.
6. Não publicar o PostgreSQL diretamente na internet.
7. Usar HTTPS em produção.
8. Manter CI verde antes de deploy.
9. Revisar o Audit Log após mudanças administrativas importantes.
10. Monitorar espaço em disco dos materiais e backups.
