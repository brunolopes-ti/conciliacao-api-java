# Execução local — PostgreSQL + COBOL

Este documento descreve como executar localmente a API de conciliação com PostgreSQL e o motor COBOL real.

## Arquitetura desta etapa

O fluxo implementado é:

```text
POST /api/conciliacoes
        |
        v
ServicoConciliacaoReal
        |
        v
PostgreSQL
  - cria execução EM_PROCESSAMENTO
  - captura snapshot
        |
        v
CSV temporários
        |
        v
COBOL
        |
        v
resultado.tsv
        |
        v
Validação Java
        |
        v
PostgreSQL
  - resumo
  - detalhes
  - status CONCLUIDA
```

A captura do snapshot termina sua transação antes da execução do COBOL.

O processamento COBOL não mantém transação JDBC aberta.

A persistência de resumo, detalhes e mudança da execução para `CONCLUIDA` ocorre em uma única transação.

## PostgreSQL

A aplicação utiliza duas categorias de credenciais:

- credenciais da aplicação, com permissões limitadas;
- credenciais administrativas para migrações Flyway e preparação dos testes.

O usuário da aplicação utilizado no ambiente local é:

```text
conciliacao_app
```

As senhas não devem ser gravadas no repositório.

A role `conciliacao_app` deve existir no PostgreSQL antes da execução das migrations, pois as migrations concedem permissões explicitamente para essa role.

## PostgreSQL do Windows acessado pelo Ubuntu

No ambiente atual, o Ubuntu executa em uma VM VirtualBox usando NAT.

O endereço estável utilizado pela VM para alcançar o host Windows é:

```text
10.0.2.2
```

Esse endereço evita depender do IP dinâmico recebido pelo Windows na rede local.

Teste de conectividade:

```bash
nc -vz 10.0.2.2 5432
```

Exemplo de URL JDBC:

```text
jdbc:postgresql://10.0.2.2:5432/conciliacao_migracao_teste
```

Não gravar esse endereço diretamente no código Java. A aplicação recebe a URL por variável de ambiente.

## Variáveis principais

Exemplo para o banco de testes:

```bash
export CONCILIACAO_DB_URL="jdbc:postgresql://10.0.2.2:5432/conciliacao_migracao_teste"
export CONCILIACAO_DB_USER="conciliacao_app"

export CONCILIACAO_FLYWAY_URL="jdbc:postgresql://10.0.2.2:5432/conciliacao_migracao_teste"
export CONCILIACAO_FLYWAY_USER="postgres"

export CONCILIACAO_DB_TEST_NAME="conciliacao_migracao_teste"
```

As senhas devem existir somente no ambiente:

```text
CONCILIACAO_DB_PASSWORD
CONCILIACAO_FLYWAY_PASSWORD
```

Configuração do COBOL:

```bash
export CONCILIACAO_COBOL_EXECUTAVEL="/tmp/conciliacao-cobol-e2e/conciliacao"
export CONCILIACAO_COBOL_TIMEOUT_MS="30000"
export CONCILIACAO_COBOL_MAX_CONCORRENCIA="1"
```

Para habilitar o endpoint real:

```bash
export CONCILIACAO_API_EXECUCAO_HABILITADA="true"
export CONCILIACAO_PERSISTENCIA_HABILITADA="true"
```

## Limite de concorrência COBOL

A quantidade máxima de processamentos COBOL simultâneos é configurada por:

```text
CONCILIACAO_COBOL_MAX_CONCORRENCIA
```

O padrão é:

```text
1
```

O controle utiliza um `Semaphore` dentro da aplicação Java.

Esse limite é local à JVM. Caso futuramente existam várias instâncias da aplicação, cada instância possuirá seu próprio limite e será necessário um mecanismo distribuído.

## Flyway

As migrações estão em:

```text
src/main/resources/db/migration
```

As versões existentes nesta etapa são:

```text
V1__estrutura_base.sql
V2__persistencia_conciliacao.sql
V3__endurece_esquema_legado.sql
V4__remove_views_legadas.sql
V5__corrige_nulos_e_identificadores.sql
```

Migrações já aplicadas não devem ser editadas.

Quando uma alteração futura de banco for necessária, deve ser criada uma nova versão, por exemplo:

```text
V6__nome_da_alteracao.sql
```

O Flyway fica desabilitado por padrão na execução normal:

```text
spring.flyway.enabled=false
```

A execução de migrações deve utilizar as credenciais administrativas, separadas das credenciais da aplicação.

## Endpoint

Com a execução real habilitada:

```http
POST /api/conciliacoes
```

O endpoint não recebe:

- arquivos enviados pelo cliente;
- caminhos locais;
- registros financeiros no corpo da requisição;
- caminho do executável COBOL.

Os dados são obtidos do PostgreSQL por meio de snapshot.

Em caso de sucesso, a resposta é:

```text
HTTP 201 Created
```

com status:

```text
CONCLUIDA
```

## Persistência do resultado

O resultado interpretado do COBOL é persistido de forma normalizada nas tabelas:

```text
conciliacao_resumos
conciliacao_detalhes
```

A aplicação não mantém `resultado.tsv` nem `relatorio.txt` como armazenamento definitivo.

Esses arquivos pertencem apenas ao diretório temporário da execução.

## Estados da execução

Os estados atualmente utilizados são:

```text
CRIADA
EM_PROCESSAMENTO
CONCLUIDA
FALHOU
```

O caminho real da aplicação cria a execução diretamente como:

```text
EM_PROCESSAMENTO
```

Isso evita uma janela entre criação e início que poderia deixar uma execução real abandonada em `CRIADA`.

Uma execução somente pode chegar a `CONCLUIDA` por meio da persistência atômica do resultado.

Não existe mais um método separado capaz de marcar uma execução como concluída sem resumo e detalhes.

## Códigos internos de falha

Entre os códigos persistidos estão:

```text
CONFLITO_DADOS
TIMEOUT_COBOL
FALHA_COBOL
RESULTADO_INVALIDO
ERRO_PERSISTENCIA
ERRO_INTERNO
```

Falhas de acesso ao PostgreSQL são classificadas como:

```text
ERRO_PERSISTENCIA
```

## Testes normais

Os testes normais não dependem do PostgreSQL do Windows nem do COBOL real.

Executar:

```bash
./mvnw test
```

Os testes que dependem de infraestrutura externa permanecem desabilitados quando suas variáveis específicas não estão presentes.

## Testes reais PostgreSQL + COBOL

Utilizar exclusivamente um banco descartável de testes.

Nunca utilizar:

```text
conciliacao_pagamentos
```

como `CONCILIACAO_DB_TEST_NAME`.

Habilitar:

```bash
export CONCILIACAO_TESTE_POSTGRESQL="true"

export CONCILIACAO_COBOL_EXECUTAVEL="/tmp/conciliacao-cobol-e2e/conciliacao"
export COBOL_EXECUTAVEL_TESTE="/tmp/conciliacao-cobol-e2e/conciliacao"
```

Depois:

```bash
./mvnw test
```

Os testes destrutivos possuem uma proteção centralizada que valida o banco conectado antes de executar operações como:

```text
TRUNCATE
DROP TRIGGER
DROP FUNCTION
```

Após os testes reais:

```bash
unset CONCILIACAO_TESTE_POSTGRESQL
unset CONCILIACAO_COBOL_EXECUTAVEL
unset COBOL_EXECUTAVEL_TESTE
```

## Limitação conhecida — recuperação após queda

Nesta versão ainda não existe recuperação automática de uma conciliação que permaneça em:

```text
EM_PROCESSAMENTO
```

caso a JVM, o sistema operacional ou a máquina sejam encerrados abruptamente durante o processamento.

Esse comportamento não será resolvido silenciosamente por reprocessamento automático, pois isso exige uma política explícita para determinar:

- quando uma execução pode ser considerada abandonada;
- se ela deve apenas ser marcada como falha;
- se pode ser reprocessada;
- como evitar execução duplicada;
- como tratar várias instâncias da aplicação.

A recuperação de execuções abandonadas fica registrada para a etapa de consolidação operacional do projeto.

## Escopo

Esta etapa utiliza:

```text
Java
Spring Boot
PostgreSQL
Flyway
GnuCOBOL
```

Mainframe, z/OS, JCL, Db2 e CICS não fazem parte da execução local atual.


## Correções do bloco 5

A V5 fecha brechas de nulos e rejeita espaços Unicode nas extremidades dos
identificadores. Registros históricos inválidos impedem a migração; não há
correção automática nem exclusão de dados. Consulte `correcao-bloco5.md`.

`CONCILIACAO_COBOL_ESPERA_VAGA_MS` limita a espera pelo semáforo (padrão 1000).
Saturação retorna 503 `CAPACIDADE_ESGOTADA` e registra a execução como FALHOU.
O limite é do processamento COBOL, não da entrada de todas as requisições HTTP.
