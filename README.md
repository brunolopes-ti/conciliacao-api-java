# API Java de conciliação de pagamentos

Projeto educacional de Bruno Ramos Lopes, com dados fictícios, para integrar
Spring Boot ao motor GnuCOBOL e, em etapas posteriores, ao PostgreSQL.

## Estado atual

- Java 21, Spring Boot 4.1.1 e Maven Wrapper.
- `GET /api/status` retorna JSON com `aplicacao` e `status`.
- Modelos de detalhes, resumo e resultado com `BigDecimal`.
- Leitura e validação do `resultado.tsv` versão 1.
- Execução do motor como processo externo, com argumentos separados e timeout.
- Diretório exclusivo com permissões `rwx------`.
- Conferência adicional contra um snapshot imutável em memória.
- Testes com processos simulados e teste opcional com o GnuCOBOL real.

A API ainda não expõe uma rota para executar conciliações. Não há acesso ao
PostgreSQL, snapshot persistente, autenticação ou histórico implementados.
O status `OK` informa apenas que a rota responde.

## Requisitos

- Ubuntu/Linux, JDK 21.
- Acesso às dependências Maven na primeira execução.
- Para o teste real: GnuCOBOL, compilador C e o repositório
  https://github.com/brunolopes-ti/cobol-conciliacao.

## Executar

```bash
bash ./mvnw spring-boot:run
```

Em outro terminal no mesmo Ubuntu:

```bash
curl -i http://localhost:8080/api/status
```

Resposta:

```json
{"aplicacao":"conciliacao-api-java","status":"OK"}
```

## Testar

```bash
bash ./mvnw test
```

O teste `CobolRealTests` é ignorado quando `COBOL_EXECUTAVEL_TESTE` não está
definido. Os demais testes usam arquivos e processos locais, sem banco.

Para compilar o motor em uma pasta temporária e executar a suíte incluindo o
motor real:

```bash
bash testes/testar-cobol-real.sh "$HOME/projetos/cobol-conciliacao"
```

O teste real usa entradas próprias, não modifica `dados/` do repositório COBOL
e cobre os seis status, pagamentos adicionais e pagamentos sem cobrança
repetidos. A comparação usa as entradas ordenadas do snapshot.

## Integração

`IntegradorExecucaoCobol.executar(execucao)` verifica processo, arquivos e
consistência interna do TSV. Essa variante não verifica correspondência com
as entradas e não basta para concluir uma conciliação de negócio.

`IntegradorExecucaoCobol.executar(execucao, snapshot)` acrescenta a comparação
com cobranças e pagamentos do snapshot, incluindo ordem, primeiro recebimento,
quantidades, ocorrências sem previsão e totais brutos. Na integração futura,
os CSV deverão ser gerados desse mesmo snapshot.

A persistência do snapshot em uma transação PostgreSQL consistente ainda será
implementada. As listas em memória não substituem essa transação.

Valores individuais: `0.00` a `99999.99`. Totais: até `99999990.00`.
Campos não aplicáveis ficam vazios no TSV e são representados por `null`.
Um detalhe `DUPLICADO` contém apenas o primeiro recebimento; o total bruto
inclui todos os pagamentos. Por isso ele não pode ser reconstituído somando
somente os detalhes.

## Limites e ciclo de vida

- TSV: até 2 MiB, com leitura limitada mesmo se o arquivo crescer.
- Relatório: até 2 MiB na validação após o processo.
- Saída capturada: 64 KiB; o excedente é consumido e descartado.
- Timeout padrão: 30 segundos, mais períodos limitados de encerramento.
- Coleta da saída acompanha o processo principal e termina após consumir os
  bytes disponíveis quando ele encerra. Saída posterior de filhos órfãos não
  faz parte do log capturado.
- A implementação não oferece isolamento de processos como cgroups. Um filho
  já órfão e não identificado não tem encerramento garantido.
- Limites verificados após o processo não constituem quota de disco durante
  a execução; o executável é uma configuração interna confiável.

`DiretorioExecucaoCobol` implementa `AutoCloseable`. O chamador deve encerrar o
processo e consumir/persistir o relatório antes de chamar `close()`. A limpeza
não segue links simbólicos. Uma falha de encerramento exige preservar a pasta
para recuperação; não se deve apagá-la enquanto um processo ainda a utiliza.

O integrador não apaga automaticamente a pasta, pois devolve o caminho do
relatório. O futuro serviço que coordena persistência e execução será dono
desse ciclo de vida. Falha de limpeza depois da persistência deverá ser
registrada sem mudar uma execução já concluída para falha.

## Próximas etapas

Definir o contrato HTTP, conectar PostgreSQL, criar snapshot persistente,
gerar CSV a partir dele, validar e persistir os resultados. O bloco 4 não
foi iniciado por esta revisão.
