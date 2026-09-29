package br.com.brunolopes.conciliacao.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(
        named = "CONCILIACAO_TESTE_POSTGRESQL",
        matches = "true"
)
class RepositorioExecucaoConciliacaoJdbcPostgresqlTests {

    private JdbcTemplate jdbcTemplate;
    private RepositorioExecucaoConciliacaoJdbc repositorio;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void preparar() {
        String url = variavelObrigatoria(
                "CONCILIACAO_DB_URL"
        );

        String usuario = variavelObrigatoria(
                "CONCILIACAO_DB_USER"
        );

        String senha = variavelObrigatoria(
                "CONCILIACAO_DB_PASSWORD"
        );

        String bancoEsperado = variavelObrigatoria(
                "CONCILIACAO_DB_TEST_NAME"
        );

        if ("conciliacao_pagamentos".equals(bancoEsperado)) {
            throw new IllegalStateException(
                    "Teste PostgreSQL nao pode usar o banco principal."
            );
        }

        DataSource dataSource = new DriverManagerDataSource(
                url,
                usuario,
                senha
        );

        jdbcTemplate = new JdbcTemplate(
                dataSource
        );

        String bancoAtual =
                jdbcTemplate.queryForObject(
                        "SELECT current_database()",
                        String.class
                );

        String usuarioAtual =
                jdbcTemplate.queryForObject(
                        "SELECT current_user",
                        String.class
                );

        assertEquals(
                bancoEsperado,
                bancoAtual,
                "Conexao apontou para banco diferente do esperado."
        );

        assertEquals(
                "conciliacao_app",
                usuarioAtual,
                "Teste deve executar com o usuario da aplicacao."
        );

        repositorio =
                new RepositorioExecucaoConciliacaoJdbc(
                        jdbcTemplate
                );

        transactionTemplate =
                new TransactionTemplate(
                        new DataSourceTransactionManager(
                                dataSource
                        )
                );
    }

    @Test
    void deveCriarEIniciarConciliacao() {
        transactionTemplate.executeWithoutResult(
                statusTransacao -> {

                    long id =
                            repositorio.criar();

                    EstadoExecucao criada =
                            consultar(
                                    id
                            );

                    assertEquals(
                            "CRIADA",
                            criada.status()
                    );

                    assertNotNull(
                            criada.criadaEm()
                    );

                    assertEquals(
                            null,
                            criada.iniciadaEm()
                    );

                    assertEquals(
                            null,
                            criada.finalizadaEm()
                    );

                    assertEquals(
                            null,
                            criada.resultadoVersao()
                    );

                    repositorio.iniciar(
                            id
                    );

                    EstadoExecucao emProcessamento =
                            consultar(
                                    id
                            );

                    assertEquals(
                            "EM_PROCESSAMENTO",
                            emProcessamento.status()
                    );

                    assertNotNull(
                            emProcessamento.iniciadaEm()
                    );

                    assertEquals(
                            null,
                            emProcessamento.finalizadaEm()
                    );

                    assertEquals(
                            null,
                            emProcessamento.resultadoVersao()
                    );

                    assertThrows(
                            IllegalStateException.class,
                            () ->
                                    repositorio.iniciar(
                                            id
                                    )
                    );

                    statusTransacao.setRollbackOnly();
                }
        );
    }

    @Test
    void deveCriarIniciarERegistrarFalha() {
        transactionTemplate.executeWithoutResult(
                statusTransacao -> {

                    long id =
                            repositorio.criar();

                    repositorio.iniciar(
                            id
                    );

                    repositorio.falhar(
                            id,
                            "ERRO_TESTE",
                            "Falha controlada do teste PostgreSQL."
                    );

                    EstadoExecucao falhou =
                            consultar(
                                    id
                            );

                    assertEquals(
                            "FALHOU",
                            falhou.status()
                    );

                    assertNotNull(
                            falhou.iniciadaEm()
                    );

                    assertNotNull(
                            falhou.finalizadaEm()
                    );

                    assertEquals(
                            "ERRO_TESTE",
                            falhou.erroCodigo()
                    );

                    assertEquals(
                            "Falha controlada do teste PostgreSQL.",
                            falhou.erroDetalhe()
                    );

                    assertEquals(
                            null,
                            falhou.resultadoVersao()
                    );

                    statusTransacao.setRollbackOnly();
                }
        );
    }

    private EstadoExecucao consultar(
            long id
    ) {
        return jdbcTemplate.queryForObject(
                """
                SELECT
                    status,
                    criada_em,
                    iniciada_em,
                    finalizada_em,
                    resultado_versao,
                    erro_codigo,
                    erro_detalhe
                FROM public.conciliacoes
                WHERE id = ?
                """,
                (resultado, numeroLinha) ->
                        new EstadoExecucao(
                                resultado.getString(
                                        "status"
                                ),
                                resultado.getTimestamp(
                                        "criada_em"
                                ).toInstant(),
                                resultado.getTimestamp(
                                        "iniciada_em"
                                ) == null
                                        ? null
                                        : resultado
                                                .getTimestamp(
                                                        "iniciada_em"
                                                )
                                                .toInstant(),
                                resultado.getTimestamp(
                                        "finalizada_em"
                                ) == null
                                        ? null
                                        : resultado
                                                .getTimestamp(
                                                        "finalizada_em"
                                                )
                                                .toInstant(),
                                resultado.getObject(
                                        "resultado_versao",
                                        Integer.class
                                ),
                                resultado.getString(
                                        "erro_codigo"
                                ),
                                resultado.getString(
                                        "erro_detalhe"
                                )
                        ),
                id
        );
    }

    private String variavelObrigatoria(
            String nome
    ) {
        String valor =
                System.getenv(
                        nome
                );

        if (valor == null
                || valor.isBlank()) {

            throw new IllegalStateException(
                    "Variavel obrigatoria ausente: "
                            + nome
            );
        }

        return valor;
    }

    private record EstadoExecucao(
            String status,
            java.time.Instant criadaEm,
            java.time.Instant iniciadaEm,
            java.time.Instant finalizadaEm,
            Integer resultadoVersao,
            String erroCodigo,
            String erroDetalhe
    ) {
    }
}
