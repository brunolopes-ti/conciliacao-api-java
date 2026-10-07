# API Java de conciliação de pagamentos

Projeto educacional de Bruno Ramos Lopes, com dados fictícios e código público.

Java 21, Spring Boot 4.1.1, PostgreSQL, Flyway e GnuCOBOL em Linux.

## Funcionalidades implementadas até o bloco 6

- `GET /api/status`: informa que a aplicação responde.
- `POST /api/conciliacoes`: captura dados do banco, executa COBOL e persiste resultados.
- `GET /api/conciliacoes`: consulta o histórico com paginação e filtro por status.
- `GET /api/conciliacoes/{id}`: consulta uma conciliação específica.
- `GET /api/conciliacoes/{id}/relatorio`: reconstrói e disponibiliza relatório TXT a partir dos dados persistidos.
- Snapshot persistente em transação `REPEATABLE READ`, ordenado pelo ID de origem.
- Geração de CSVs internos a partir do snapshot; não há upload de arquivos.
- Execução do COBOL com argumentos separados, stdin fechado, controle de concorrência e timeout.
- TSV validado internamente e contra os dados originais do snapshot.
- Persistência atômica do resumo, detalhes e estado da execução.
- Registro de falhas por execução.
- Paginação de histórico com tamanho máximo de 100 registros.
- Filtro de histórico pelos estados `CRIADA`, `EM_PROCESSAMENTO`, `CONCLUIDA` e `FALHOU`.
- Tratamento HTTP padronizado sem exposição de detalhes internos.
- Relatório TXT determinístico, UTF-8, reconstruído exclusivamente a partir do PostgreSQL.
- Diretório temporário exclusivo com permissão `0700`, removido após processamento.

O fluxo principal é:

```text
PostgreSQL
    ↓
snapshot
    ↓
CSV
    ↓
COBOL
    ↓
TSV
    ↓
validação
    ↓
PostgreSQL
```

O processamento COBOL acontece depois do commit do snapshot e antes da
transação de persistência do resultado, sem manter uma transação JDBC aberta
durante a execução externa.

As consultas do bloco 6 leem os dados já persistidos e não dependem de uma nova
execução do COBOL.

## Endpoints

### Status

```http
GET /api/status
```

### Executar conciliação

```http
POST /api/conciliacoes
```

Não aceita corpo nem parâmetros.

A execução depende de:

```text
CONCILIACAO_API_EXECUCAO_HABILITADA=true
```

### Histórico

```http
GET /api/conciliacoes
```

Parâmetros opcionais:

```text
pagina
tamanho
status
```

Valores padrão:

```text
pagina=0
tamanho=20
```

O tamanho máximo da página é `100`.

Exemplo:

```http
GET /api/conciliacoes?pagina=0&tamanho=20&status=CONCLUIDA
```

A ordenação é:

```text
criada_em DESC
id DESC
```

### Consultar por ID

```http
GET /api/conciliacoes/{id}
```

Exemplo:

```http
GET /api/conciliacoes/15
```

Uma conciliação concluída pode retornar resumo e detalhes.

Execuções ainda sem resultado ou que falharam não recebem dados inventados.

Uma conciliação inexistente retorna:

```text
404 CONCILIACAO_NAO_ENCONTRADA
```

### Relatório TXT

```http
GET /api/conciliacoes/{id}/relatorio
```

O relatório:

- é reconstruído a partir dos dados persistidos;
- não reutiliza arquivos temporários antigos;
- utiliza UTF-8;
- utiliza quebra de linha LF;
- mantém detalhes em ordem crescente;
- formata valores monetários com duas casas decimais;
- é disponibilizado como `conciliacao-{id}.txt`.

Somente conciliações `CONCLUIDA` possuem relatório.

Caso contrário:

```text
409 RELATORIO_INDISPONIVEL
```

## Arquitetura

A API utiliza separação entre aplicação, persistência, integração e camada HTTP.

Fluxo de execução:

```text
HTTP POST
    ↓
Controller
    ↓
Service
    ↓
Snapshot PostgreSQL
    ↓
COBOL
    ↓
Validação
    ↓
Persistência
```

Fluxo de consulta:

```text
HTTP GET
    ↓
Controller de consulta
    ↓
Service de consulta
    ↓
Repository JDBC
    ↓
PostgreSQL
```

O acesso ao PostgreSQL utiliza JDBC por meio do Spring `JdbcTemplate`, com SQL
explícito nos repositórios.

Não é utilizado JPA.

## Banco de dados

O PostgreSQL é versionado com Flyway.

Migrations atuais:

```text
V1
V2
V3
V4
V5
V6
```

As migrations aplicadas são tratadas como imutáveis.

A aplicação utiliza credenciais de execução separadas das credenciais de
migração.

A role de runtime não precisa de permissões DDL.

Entre as estruturas persistidas estão:

- execuções de conciliação;
- snapshots de cobranças;
- snapshots de pagamentos;
- resumos;
- detalhes da conciliação.

## Requisitos e execução

- Ubuntu/Linux.
- JDK 21.
- PostgreSQL.
- GnuCOBOL.
- acesso às dependências Maven na primeira execução.
- banco preparado pelas migrations.
- role `conciliacao_app`.
- executável compilado do motor COBOL.

Motor COBOL:

https://github.com/brunolopes-ti/cobol-conciliacao

Consulte:

- `docs/execucao-local.md`
- `docs/contrato-http-conciliacao.md`
- `docs/contrato-consultas-relatorios.md`
- `docs/correcao-bloco5.md`

As senhas ficam em variáveis de ambiente e não devem ser versionadas.

## Exemplos

Com a aplicação iniciada:

```bash
curl -i http://localhost:8080/api/status
```

Histórico:

```bash
curl -i http://localhost:8080/api/conciliacoes
```

Histórico filtrado:

```bash
curl -i \
"http://localhost:8080/api/conciliacoes?pagina=0&tamanho=20&status=CONCLUIDA"
```

Consulta individual:

```bash
curl -i http://localhost:8080/api/conciliacoes/1
```

Relatório:

```bash
curl -i http://localhost:8080/api/conciliacoes/1/relatorio
```

Execução:

```bash
curl -i -X POST http://localhost:8080/api/conciliacoes
```

## Testes

Execute:

```bash
bash ./mvnw test
```

A suíte cobre, entre outros pontos:

- interpretação do resultado COBOL;
- execução externa;
- validações;
- persistência;
- PostgreSQL real;
- regras de negócio;
- paginação;
- filtros;
- consultas por ID;
- tratamento HTTP;
- códigos de erro;
- relatório TXT.

Testes PostgreSQL utilizam banco descartável e possuem proteção para evitar
execução destrutiva contra o banco principal.

Para integrações reais e configuração das variáveis consulte:

```text
docs/execucao-local.md
```

As validações SQL estão em:

```text
testes/validar-esquema-postgresql.sql
testes/validar-revisao-bloco5.sql
```

## Limites

- Até 1000 cobranças e 1000 pagamentos por execução.
- Valores individuais de `0.00` a `99999.99`.
- Totais até `99999990.00`.
- TSV e relatório de execução COBOL limitados a 2 MiB.
- Captura de saída do processo limitada a 64 KiB.
- Timeout COBOL de 30 segundos por padrão.
- Concorrência padrão de um motor COBOL por JVM.
- Espera por vaga de 1000 ms por padrão.
- Página de histórico limitada a 100 registros.

Configure a espera por vaga com:

```text
CONCILIACAO_COBOL_ESPERA_VAGA_MS
```

Espera esgotada retorna:

```text
503 CAPACIDADE_ESGOTADA
```

O semáforo de concorrência é local à JVM. Múltiplas instâncias exigem
coordenação adicional.

## Segurança e diagnóstico

Detalhes internos de execução são persistidos somente para diagnóstico e não
são expostos pelos endpoints de consulta.

O campo interno `erro_detalhe` não faz parte das respostas públicas.

Relatórios públicos são reconstruídos a partir do resultado normalizado
persistido.

Não são versionadas senhas de banco.

A aplicação ainda não possui autenticação e não deve ser exposta publicamente
sem os controles planejados para os próximos blocos.

## Próximos blocos

- usuários;
- autenticação;
- isolamento por usuário;
- recuperação de execuções abandonadas;
- consolidação do backend;
- CI/CD;
- frontend Angular;
- integração end-to-end.

Mainframe, z/OS, JCL, Db2 e CICS não fazem parte desta implementação local.

## Revisao operacional 2026-10

A revisao operacional adicionou:

- leitura consistente de cabecalho e detalhes em `REPEATABLE_READ`;
- admissao de capacidade antes de criar execucao e snapshot;
- `lock_timeout` e timeout de consulta JDBC configuraveis;
- contrato de identificadores alinhado entre Java, PostgreSQL e COBOL;
- V6 com protecao de snapshot apos o fim do processamento e validacao diferida de `CONCLUIDA`;
- recuperacao periodica de execucoes antigas em `EM_PROCESSAMENTO`;
- CI com Java 21, PostgreSQL real e motor COBOL real.

O `POST /api/conciliacoes` continua criando uma nova execucao a cada chamada.
Ele nao e idempotente: uma repeticao apos perda da resposta pode gerar outra
execucao historica. Clientes nao devem fazer retry automatico desse POST nesta
versao. Um mecanismo `Idempotency-Key` fica fora do contrato atual.

Configuracoes novas:

```text
CONCILIACAO_DB_LOCK_TIMEOUT_MS=5000
CONCILIACAO_DB_QUERY_TIMEOUT=15s
CONCILIACAO_RECUPERACAO_HABILITADA=true
CONCILIACAO_RECUPERACAO_IDADE_MINIMA_MS=300000
CONCILIACAO_RECUPERACAO_INTERVALO_MS=60000
CONCILIACAO_RECUPERACAO_TEMPORARIOS_IDADE_MINIMA_MS=1800000
```

Detalhes e validacao: `docs/revisao-operacional.md`.
