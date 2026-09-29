package br.com.brunolopes.conciliacao.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;
import br.com.brunolopes.conciliacao.testutil.BancoTestePostgresql;

@EnabledIfEnvironmentVariable(
        named = "CONCILIACAO_TESTE_POSTGRESQL",
        matches = "true"
)
class RepositorioResultadoConciliacaoJdbcPostgresqlTests {

    private JdbcTemplate jdbcApp;
    private JdbcTemplate jdbcAdmin;

    private RepositorioExecucaoConciliacaoJdbc
            repositorioExecucao;

    private RepositorioResultadoConciliacaoJdbc
            repositorioResultado;

    @BeforeEach
    void preparar() {
        String urlApp =
                variavelObrigatoria(
                        "CONCILIACAO_DB_URL"
                );

        String usuarioApp =
                variavelObrigatoria(
                        "CONCILIACAO_DB_USER"
                );

        String senhaApp =
                variavelObrigatoria(
                        "CONCILIACAO_DB_PASSWORD"
                );

        String urlAdmin =
                variavelObrigatoria(
                        "CONCILIACAO_FLYWAY_URL"
                );

        String usuarioAdmin =
                variavelObrigatoria(
                        "CONCILIACAO_FLYWAY_USER"
                );

        String senhaAdmin =
                variavelObrigatoria(
                        "CONCILIACAO_FLYWAY_PASSWORD"
                );

        String bancoEsperado =
                variavelObrigatoria(
                        "CONCILIACAO_DB_TEST_NAME"
                );

        if ("conciliacao_pagamentos"
                .equals(bancoEsperado)) {

            throw new IllegalStateException(
                    "Teste PostgreSQL nao pode "
                            + "usar o banco principal."
            );
        }

        DataSource dataSourceApp =
                new DriverManagerDataSource(
                        urlApp,
                        usuarioApp,
                        senhaApp
                );

        DataSource dataSourceAdmin =
                new DriverManagerDataSource(
                        urlAdmin,
                        usuarioAdmin,
                        senhaAdmin
                );

        jdbcApp =
                new JdbcTemplate(
                        dataSourceApp
                );

        jdbcAdmin =
                new JdbcTemplate(
                        dataSourceAdmin
                );

        String bancoApp =
                jdbcApp.queryForObject(
                        "SELECT current_database()",
                        String.class
                );

        String bancoAdmin =
                jdbcAdmin.queryForObject(
                        "SELECT current_database()",
                        String.class
                );

        assertEquals(
                bancoEsperado,
                bancoApp
        );

        assertEquals(
                bancoEsperado,
                bancoAdmin
        );

        if ("conciliacao_pagamentos".equals(
                bancoApp
        )) {
            throw new IllegalStateException(
                    "Protecao acionada: "
                            + "conexao apontou "
                            + "para o banco principal."
            );
        }

        assertEquals(
                "conciliacao_app",
                jdbcApp.queryForObject(
                        "SELECT current_user",
                        String.class
                )
        );

        limparBancoTeste();

        repositorioExecucao =
                new RepositorioExecucaoConciliacaoJdbc(
                        jdbcApp
                );

        repositorioResultado =
                new RepositorioResultadoConciliacaoJdbc(
                        jdbcApp,
                        new DataSourceTransactionManager(
                                dataSourceApp
                        )
                );
    }

    @AfterEach
    void limpar() {
        if (jdbcAdmin != null) {
            limparBancoTeste();
        }
    }

    @Test
    void devePersistirResultadoEConcluirAtomicamente() {
        long conciliacaoId =
                criarExecucaoEmProcessamento();

        ResultadoConciliacao resultado =
                resultadoExemplo();

        repositorioResultado.persistirEConcluir(
                conciliacaoId,
                resultado
        );

        EstadoExecucao estado =
                consultarEstado(
                        conciliacaoId
                );

        assertEquals(
                "CONCLUIDA",
                estado.status()
        );

        assertEquals(
                1,
                estado.resultadoVersao()
        );

        assertNotNull(
                estado.iniciadaEm()
        );

        assertNotNull(
                estado.finalizadaEm()
        );

        assertTrue(
                !estado.finalizadaEm()
                        .isBefore(
                                estado.iniciadaEm()
                        )
        );

        Long quantidadeResumo =
                jdbcApp.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_resumos
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
                );

        Long quantidadeDetalhes =
                jdbcApp.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_detalhes
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
                );

        assertEquals(
                1L,
                quantidadeResumo
        );

        assertEquals(
                2L,
                quantidadeDetalhes
        );

        ResumoPersistido resumo =
                jdbcApp.queryForObject(
                        """
                        SELECT
                            versao,
                            conferidos,
                            acima,
                            abaixo,
                            duplicados,
                            sem_recebimento,
                            sem_previsao,
                            total_esperado,
                            total_recebido,
                            saldo_global
                        FROM public.conciliacao_resumos
                        WHERE conciliacao_id = ?
                        """,
                        (resultadoSql, numeroLinha) ->
                                new ResumoPersistido(
                                        resultadoSql.getInt(
                                                "versao"
                                        ),
                                        resultadoSql.getInt(
                                                "conferidos"
                                        ),
                                        resultadoSql.getInt(
                                                "acima"
                                        ),
                                        resultadoSql.getInt(
                                                "abaixo"
                                        ),
                                        resultadoSql.getInt(
                                                "duplicados"
                                        ),
                                        resultadoSql.getInt(
                                                "sem_recebimento"
                                        ),
                                        resultadoSql.getInt(
                                                "sem_previsao"
                                        ),
                                        resultadoSql.getBigDecimal(
                                                "total_esperado"
                                        ),
                                        resultadoSql.getBigDecimal(
                                                "total_recebido"
                                        ),
                                        resultadoSql.getBigDecimal(
                                                "saldo_global"
                                        )
                                ),
                        conciliacaoId
                );

        assertEquals(
                1,
                resumo.versao()
        );

        assertEquals(
                1,
                resumo.conferidos()
        );

        assertEquals(
                0,
                resumo.acima()
        );

        assertEquals(
                0,
                resumo.abaixo()
        );

        assertEquals(
                0,
                resumo.duplicados()
        );

        assertEquals(
                1,
                resumo.semRecebimento()
        );

        assertEquals(
                0,
                resumo.semPrevisao()
        );

        assertBigDecimal(
                "150.00",
                resumo.totalEsperado()
        );

        assertBigDecimal(
                "100.00",
                resumo.totalRecebido()
        );

        assertBigDecimal(
                "-50.00",
                resumo.saldoGlobal()
        );

        List<DetalhePersistido> detalhes =
                jdbcApp.query(
                        """
                        SELECT
                            ordem,
                            identificador,
                            valor_esperado,
                            valor_recebido,
                            diferenca,
                            status,
                            quantidade_recebimentos
                        FROM public.conciliacao_detalhes
                        WHERE conciliacao_id = ?
                        ORDER BY ordem
                        """,
                        (resultadoSql, numeroLinha) ->
                                new DetalhePersistido(
                                        resultadoSql.getInt(
                                                "ordem"
                                        ),
                                        resultadoSql.getString(
                                                "identificador"
                                        ),
                                        resultadoSql.getBigDecimal(
                                                "valor_esperado"
                                        ),
                                        resultadoSql.getBigDecimal(
                                                "valor_recebido"
                                        ),
                                        resultadoSql.getBigDecimal(
                                                "diferenca"
                                        ),
                                        resultadoSql.getString(
                                                "status"
                                        ),
                                        resultadoSql.getInt(
                                                "quantidade_recebimentos"
                                        )
                                ),
                        conciliacaoId
                );

        assertEquals(
                2,
                detalhes.size()
        );

        DetalhePersistido primeiro =
                detalhes.get(0);

        assertEquals(
                1,
                primeiro.ordem()
        );

        assertEquals(
                "COB001",
                primeiro.identificador()
        );

        assertBigDecimal(
                "100.00",
                primeiro.valorEsperado()
        );

        assertBigDecimal(
                "100.00",
                primeiro.valorRecebido()
        );

        assertBigDecimal(
                "0.00",
                primeiro.diferenca()
        );

        assertEquals(
                "CONFERIDO",
                primeiro.status()
        );

        assertEquals(
                1,
                primeiro.quantidadeRecebimentos()
        );

        DetalhePersistido segundo =
                detalhes.get(1);

        assertEquals(
                2,
                segundo.ordem()
        );

        assertEquals(
                "COB002",
                segundo.identificador()
        );

        assertBigDecimal(
                "50.00",
                segundo.valorEsperado()
        );

        assertEquals(
                null,
                segundo.valorRecebido()
        );

        assertEquals(
                null,
                segundo.diferenca()
        );

        assertEquals(
                "SEM_RECEBIMENTO",
                segundo.status()
        );

        assertEquals(
                0,
                segundo.quantidadeRecebimentos()
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        repositorioResultado
                                .persistirEConcluir(
                                        conciliacaoId,
                                        resultado
                                )
        );
    }

    @Test
    void deveFazerRollbackDeTudoSeDetalheFalhar() {
        long conciliacaoId =
                criarExecucaoEmProcessamento();

        jdbcAdmin.execute(
                """
                CREATE OR REPLACE FUNCTION
                    public.teste_falha_resultado_detalhe()
                RETURNS trigger
                LANGUAGE plpgsql
                AS $$
                BEGIN
                    RAISE EXCEPTION
                        'Falha proposital nos detalhes.';
                END;
                $$
                """
        );

        jdbcAdmin.execute(
                """
                CREATE TRIGGER
                    teste_falha_resultado_detalhe
                BEFORE INSERT
                ON public.conciliacao_detalhes
                FOR EACH ROW
                EXECUTE FUNCTION
                    public.teste_falha_resultado_detalhe()
                """
        );

        assertThrows(
                DataAccessException.class,
                () ->
                        repositorioResultado
                                .persistirEConcluir(
                                        conciliacaoId,
                                        resultadoExemplo()
                                )
        );

        Long resumos =
                jdbcApp.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_resumos
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
                );

        Long detalhes =
                jdbcApp.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_detalhes
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
                );

        assertEquals(
                0L,
                resumos
        );

        assertEquals(
                0L,
                detalhes
        );

        EstadoExecucao estado =
                consultarEstado(
                        conciliacaoId
                );

        assertEquals(
                "EM_PROCESSAMENTO",
                estado.status()
        );

        assertEquals(
                null,
                estado.finalizadaEm()
        );

        assertEquals(
                null,
                estado.resultadoVersao()
        );
    }

    private long criarExecucaoEmProcessamento() {
        long id =
                repositorioExecucao.criar();

        repositorioExecucao.iniciar(
                id
        );

        return id;
    }

    private ResultadoConciliacao resultadoExemplo() {
        return new ResultadoConciliacao(
                1,
                List.of(
                        new DetalheConciliacao(
                                "COB001",
                                new BigDecimal(
                                        "100.00"
                                ),
                                new BigDecimal(
                                        "100.00"
                                ),
                                new BigDecimal(
                                        "0.00"
                                ),
                                StatusConciliacao.CONFERIDO,
                                1
                        ),
                        new DetalheConciliacao(
                                "COB002",
                                new BigDecimal(
                                        "50.00"
                                ),
                                null,
                                null,
                                StatusConciliacao.SEM_RECEBIMENTO,
                                0
                        )
                ),
                new ResumoConciliacao(
                        1,
                        0,
                        0,
                        0,
                        1,
                        0,
                        new BigDecimal(
                                "150.00"
                        ),
                        new BigDecimal(
                                "100.00"
                        ),
                        new BigDecimal(
                                "-50.00"
                        )
                )
        );
    }

    private EstadoExecucao consultarEstado(
            long conciliacaoId
    ) {
        return jdbcApp.queryForObject(
                """
                SELECT
                    status,
                    iniciada_em,
                    finalizada_em,
                    resultado_versao
                FROM public.conciliacoes
                WHERE id = ?
                """,
                (resultado, numeroLinha) ->
                        new EstadoExecucao(
                                resultado.getString(
                                        "status"
                                ),
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
                                )
                        ),
                conciliacaoId
        );
    }

    private void assertBigDecimal(
            String esperado,
            BigDecimal atual
    ) {
        assertNotNull(
                atual
        );

        assertEquals(
                0,
                new BigDecimal(
                        esperado
                ).compareTo(
                        atual
                )
        );
    }

    private void limparBancoTeste() {
        BancoTestePostgresql.limpar(
                jdbcAdmin
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
            java.time.Instant iniciadaEm,
            java.time.Instant finalizadaEm,
            Integer resultadoVersao
    ) {
    }

    private record ResumoPersistido(
            int versao,
            int conferidos,
            int acima,
            int abaixo,
            int duplicados,
            int semRecebimento,
            int semPrevisao,
            BigDecimal totalEsperado,
            BigDecimal totalRecebido,
            BigDecimal saldoGlobal
    ) {
    }

    private record DetalhePersistido(
            int ordem,
            String identificador,
            BigDecimal valorEsperado,
            BigDecimal valorRecebido,
            BigDecimal diferenca,
            String status,
            int quantidadeRecebimentos
    ) {
    }
}
