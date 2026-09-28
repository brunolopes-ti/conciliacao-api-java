# Revisão da integração — 28/09/2026

Base revisada: `8196cf639e832e99b1c8e77a5c08ab0ef622d408`.

## Correções

1. Limite de 2 MiB para relatório.
2. Leitura do TSV limitada a 2 MiB + um byte de detecção, sem `readAllBytes`.
3. Fechamento imediato de stdin do processo.
4. Interrupção tenta encerrar a árvore conhecida antes de restaurar o sinal.
5. Coleta não bloqueante em bytes disponíveis enquanto o processo está ativo;
   não depende de EOF de um filho órfão após o encerramento do principal.
6. Limpeza explícita da pasta de execução, sem seguir links.
7. Snapshot imutável em memória e validação cruzada adicional.
8. Teste com o motor real e dados dos seis status.

O teste de pipe herdado foi atualizado para a nova semântica: terminar a
captura junto do processo principal, sem esperar saída posterior de órfãos.
A limitação de não garantir encerramento desses órfãos continua explícita.

## Evidência local desta revisão

- Camada Java de integração/modelos compilada com o compilador disponível,
  OpenJDK 17. O projeto continua configurado para Java 21.
- 10 cenários de `CenariosRevisao.main` passaram, incluindo GnuCOBOL real.
- Motor compilado a partir do commit COBOL
  `4252edd9cc77927b25dab2e0a3418a8a0a926a1e`.
- Verificação adicional confirmou encerramento de filho resistente a TERM.
- Não foi possível executar Maven/Spring Boot neste ambiente de revisão:
  Maven e dependências não estavam disponíveis e o download estava inacessível.

A validação completa do pacote é `bash testes/testar-cobol-real.sh`, no Ubuntu
com Java 21. Não afirmar que a suíte Maven completa passou antes dessa execução.

## Responsabilidades ainda futuras

Snapshot no PostgreSQL, geração de CSV a partir do snapshot, persistência,
serviço proprietário do ciclo de vida, recuperação de execuções interrompidas
e endpoint HTTP continuam nos próximos blocos. O overload sem snapshot serve
à integração de baixo nível e não deve ser usado sozinho para concluir uma
operação de negócio.
