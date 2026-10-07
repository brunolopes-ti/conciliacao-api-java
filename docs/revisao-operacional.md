# Revisão operacional — concorrência, contrato e recuperação

Base revisada:

- `conciliacao-api-java`: `74ee34345d026a49153c1424f7f04e0c07cf1063`
- `cobol-conciliacao`: `4252edd9cc77927b25dab2e0a3418a8a0a926a1e`

Esta correção fecha os pontos encontrados na varredura dos blocos anteriores
sem reescrever as migrations V1–V5.

## Ajustes implementados

### Leitura consistente

`GET /api/conciliacoes/{id}` passa a ler cabeçalho e detalhes dentro de uma
transação `REPEATABLE_READ` com `REQUIRES_NEW`. Assim a consulta usa a mesma
visão do banco durante toda a montagem da resposta e não combina um cabeçalho
antigo com detalhes que apareceram no meio da leitura.

### Limites de banco

Foram separados:

- espera por conexão Hikari;
- espera por lock PostgreSQL;
- timeout das consultas executadas pelo `JdbcTemplate`.

Variáveis:

```text
CONCILIACAO_DB_LOCK_TIMEOUT_MS=5000
CONCILIACAO_DB_QUERY_TIMEOUT=15s
```

### Contrato de identificadores

O mesmo conjunto de espaços Unicode é usado por Java, PostgreSQL e COBOL:

```text
U+0020
U+1680
U+2000..U+200A
U+2028
U+2029
U+205F
U+3000
```

U+2007 está incluído. Bordas com esses caracteres são rejeitadas; um
identificador composto apenas por eles também é inválido. O interior não é
normalizado silenciosamente.

### Capacidade

A vaga é adquirida antes de criar a execução e antes de persistir snapshot.
Uma requisição recusada por capacidade retorna o erro existente de sobrecarga
sem gerar execução/snapshot que nunca serão processados.

### Integridade de banco

A V6:

- fecha o caso U+2007 nas constraints;
- impede INSERT de snapshot quando a execução não está `EM_PROCESSAMENTO`;
- exige, por trigger diferido, resumo e pelo menos um detalhe para uma execução
  permanecer `CONCLUIDA` ao final da transação.

O trigger de conclusão é diferido para continuar compatível com a persistência
atômica do resultado na mesma transação.

### Recuperação

Uma rotina periódica marca execuções antigas ainda em `EM_PROCESSAMENTO` como:

```text
FALHOU
EXECUCAO_INTERROMPIDA
```

O padrão considera abandonada uma execução com mais de 5 minutos.

Também são removidos diretórios `conciliacao-*` antigos no `java.io.tmpdir`
quando ultrapassam 30 minutos. Links simbólicos não são seguidos.

Configurações:

```text
CONCILIACAO_RECUPERACAO_HABILITADA=true
CONCILIACAO_RECUPERACAO_IDADE_MINIMA_MS=300000
CONCILIACAO_RECUPERACAO_INTERVALO_MS=60000
CONCILIACAO_RECUPERACAO_ATRASO_INICIAL_MS=10000
CONCILIACAO_RECUPERACAO_TEMPORARIOS_IDADE_MINIMA_MS=1800000
```

### Política de repetição

O POST continua representando **uma nova execução**. Ele não é idempotente.
Uma repetição pode produzir uma nova conciliação histórica. Retry automático
não é recomendado; `Idempotency-Key` não faz parte do contrato desta versão.

### CI

Foram adicionados workflows para:

- executar as 10 suítes COBOL;
- usar Java 21;
- levantar PostgreSQL 18 no CI;
- aplicar migrations;
- compilar o motor COBOL;
- executar a suíte Java com PostgreSQL e COBOL reais;
- validar as constraints da revisão operacional.

## Validação local antes do commit

No COBOL:

```bash
bash testes/testar-tudo.sh
```

Na API Java:

```bash
bash ./mvnw test
```

A V6 deve ser validada primeiro em banco descartável pelo Flyway. Depois, com
a role `conciliacao_app`:

```bash
psql ... -f testes/validar-revisao-operacional.sql
```

O script SQL usa transação e `ROLLBACK`.

## Banco principal

O pacote aplicador **não executa migration e não altera banco algum**.

Depois da validação em banco descartável, faça backup do banco principal e
aplique a V6 pelo Flyway usando as credenciais administrativas já adotadas no
projeto.
