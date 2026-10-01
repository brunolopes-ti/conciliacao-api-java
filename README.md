# API Java de conciliação de pagamentos

Projeto educacional de Bruno Ramos Lopes, com dados fictícios e código público.
Java 21, Spring Boot 4.1.1, PostgreSQL, Flyway e GnuCOBOL em Linux.

## Funcionalidades implementadas até o bloco 5

- `GET /api/status`: informa que a aplicação responde; não verifica banco ou motor.
- `POST /api/conciliacoes`: captura dados do banco, executa COBOL e persiste resultados.
- Snapshot persistente em transação `REPEATABLE READ`, ordenado pelo ID de origem.
- Geração de CSVs internos a partir desse snapshot; não há upload de arquivos.
- Execução externa com argumentos separados, stdin fechado e timeout.
- TSV validado internamente e contra os dados originais do snapshot.
- Persistência atômica do resumo, detalhes e estado `CONCLUIDA`.
- Registro de falhas por execução e tratamento HTTP sem divulgar detalhes internos.
- Diretório temporário exclusivo com permissão `0700`, removido após processamento.

O fluxo é PostgreSQL → snapshot → CSV → COBOL → TSV → validação → PostgreSQL.
O processamento do COBOL acontece depois do commit do snapshot e antes da
transação de persistência do resultado, sem manter uma transação JDBC aberta.

## Requisitos e execução

- Ubuntu/Linux, JDK 21 e acesso às dependências Maven na primeira execução.
- PostgreSQL preparado com as migrations V1 a V5; role `conciliacao_app` existente.
- Executável compilado do [motor COBOL](https://github.com/brunolopes-ti/cobol-conciliacao).
- Variáveis de banco e caminho absoluto do executável.

Consulte [execução local](docs/execucao-local.md) para configurar o ambiente e
[revisão do bloco 5](docs/correcao-bloco5.md) para aplicar a V5 com segurança.
As senhas ficam no ambiente; não devem ser versionadas.

Com o banco e as variáveis configurados:

```bash
bash ./mvnw spring-boot:run
```

Em outro terminal:

```bash
curl -i http://localhost:8080/api/status
curl -i -X POST http://localhost:8080/api/conciliacoes
```

O POST exige `CONCILIACAO_API_EXECUCAO_HABILITADA=true`, não aceita corpo nem
parâmetros e retorna 201 somente depois de persistir o resultado.
Veja [contrato HTTP](docs/contrato-http-conciliacao.md).

## Testes

```bash
bash ./mvnw test
```

Por padrão, os testes externos são ignorados. Um BUILD SUCCESS isolado não
comprova a execução contra PostgreSQL ou COBOL real: confira também `Skipped`.

```bash
bash testes/testar-cobol-real.sh "$HOME/projetos/cobol-conciliacao"
```

Esse script compila o COBOL em diretório temporário e habilita `CobolRealTests`.
Para as integrações PostgreSQL e HTTP reais, siga `docs/execucao-local.md`:
elas exigem um banco descartável já migrado e variáveis específicas.
As validações SQL estão em `testes/validar-esquema-postgresql.sql` e
`testes/validar-revisao-bloco5.sql`; usam transação com rollback dos dados.
Sequências podem avançar mesmo quando os dados são revertidos.

## Limites

- Até 1000 cobranças e 1000 pagamentos por execução.
- Valores individuais de 0.00 a 99999.99; totais até 99999990.00.
- TSV e relatório até 2 MiB; captura de saída do processo até 64 KiB.
- Timeout do motor: 30 segundos por padrão, mais o tempo de encerramento.
- Concorrência: um motor por JVM por padrão.
- Espera por vaga: até 1000 ms por padrão; configure
  `CONCILIACAO_COBOL_ESPERA_VAGA_MS`. Zero rejeita imediatamente quando ocupado.
- Espera esgotada retorna 503 `CAPACIDADE_ESGOTADA`. A execução e o snapshot
  já criados ficam registrados e a execução passa a `FALHOU`.

A espera por vaga não é um prazo global da requisição nem limita consultas SQL.
O semáforo é local à JVM; várias instâncias exigem coordenação adicional.
Não há quota de disco durante a execução nem isolamento por cgroups.
Encerramento de filhos já órfãos não é garantido. Use apenas motor confiável.

## Diagnóstico e relatórios

Resumo e detalhes são persistidos; os arquivos TXT/TSV são temporários.
O download futuro deverá reconstruir um relatório a partir dos dados persistidos.
Em falha com código de saída, até 2000 code points da saída capturada do motor,
sem controles, são preservados na exceção interna. O serviço registra a cadeia
limitada a 4000 caracteres em `erro_detalhe`, associada à execução.
Esse campo pode conter dados internos: não deve ser devolvido por endpoints de
consulta. O log geral registra ID, código e quantidade de falhas adicionais,
sem imprimir stdout. Não existe garantia de captura da saída em timeout.

## Pendências planejadas

Consultas HTTP e download de relatórios, autenticação, isolamento por usuário,
recuperação de execuções abandonadas, frontend e pipeline de CI.
A aplicação ainda não deve ser exposta publicamente sem controles de acesso.
Mainframe, z/OS, JCL, Db2 e CICS não fazem parte desta implementação local.
