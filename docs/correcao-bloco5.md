# Correções da revisão do bloco 5

Base: main 94ba339d4564a7e4a0c9ac680b7892976cdc3c2a.

## Alterações

- V5 adiciona exigência de versão em CONCLUIDA e de diferença nos três status
  de comparação, sem alterar V1–V4.
- Espaços Unicode removidos por Java strip() são rejeitados nas bordas dos IDs
  também no banco. Entrada incompatível no snapshot vira conflito de dados.
- Espera pelo semáforo limitada e HTTP 503 para capacidade esgotada.
- Diagnóstico do motor limitado, preservado internamente e associado à execução.
- README e contrato HTTP atualizados para o estado real do bloco 5.

## Validação na VM

Execute na pasta `~/projetos/conciliacao-api-java`:

```bash
bash ./mvnw test
bash testes/testar-cobol-real.sh "$HOME/projetos/cobol-conciliacao"
```

Depois configure CONCILIACAO_DB_URL, CONCILIACAO_DB_USER,
CONCILIACAO_DB_PASSWORD, CONCILIACAO_FLYWAY_URL, CONCILIACAO_FLYWAY_USER e
CONCILIACAO_FLYWAY_PASSWORD apontando para o banco DESCARTÁVEL de testes.
Confira o banco antes de habilitar qualquer teste externo.

Para aplicar migrations pelo Flyway, sem iniciar servidor HTTP:

```bash
CONCILIACAO_FLYWAY_ENABLED=true CONCILIACAO_API_EXECUCAO_HABILITADA=false \
  bash ./mvnw spring-boot:run \
  -Dspring-boot.run.arguments=--spring.main.web-application-type=none
```

Não execute a V5 manualmente por psql: o histórico deve continuar gerenciado
pelo Flyway. Não habilite baseline-on-migrate para contornar erros da V5.
Se a migration rejeitar dados históricos, interrompa e investigue as linhas;
não apague dados nem altere migrations antigas para forçar sucesso.

Conectado pelo psql ao banco descartável já migrado, execute:

```sql
\i testes/validar-esquema-postgresql.sql
\i testes/validar-revisao-bloco5.sql
```

Os caminhos acima pressupõem que o psql foi aberto na pasta do projeto.
O segundo script verifica 64 rejeições e cinco detalhes válidos, com rollback.

Habilite depois os testes reais conforme `execucao-local.md`, confirmando
CONCILIACAO_DB_TEST_NAME e as duas URLs no mesmo banco descartável.
Somente após aprovação aplique a V5 no banco da aplicação usando o Flyway,
com as credenciais correspondentes. Guarde backup antes de migrar esse banco.

## Limites da validação da entrega

O ambiente do assistente dispõe de Java 17, sem a distribuição Java 21/Maven
completa. Foram executados testes locais do subconjunto Java independente de
Spring e SQL no PGlite. Isso não substitui os testes Java 21 e PostgreSQL
reais na VM. Não foram alterados bancos do usuário nem executados commit/push.
