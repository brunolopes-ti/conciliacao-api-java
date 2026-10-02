package br.com.brunolopes.conciliacao.aplicacao;

import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import br.com.brunolopes.conciliacao.integracao.ExecutorCobol;
import br.com.brunolopes.conciliacao.integracao.IntegradorExecucaoCobol;
import br.com.brunolopes.conciliacao.integracao.ProcessadorConciliacaoCobolReal;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioExecucaoConciliacaoJdbc;
import br.com.brunolopes.conciliacao.persistencia.RepositorioResultadoConciliacaoJdbc;
import br.com.brunolopes.conciliacao.persistencia.RepositorioSnapshotConciliacaoJdbc;
import br.com.brunolopes.conciliacao.testutil.BancoTestePostgresql;

@EnabledIfEnvironmentVariable(
        named = "CONCILIACAO_TESTE_POSTGRESQL",
        matches = "true"
)
class ServicoConciliacaoRealPostgresqlCobolTests {

    private JdbcTemplate jdbcApp;
    private JdbcTemplate jdbcAdmin;

    private Path executavelCobol;

    @BeforeEach
    void preparar() {
        String caminhoExecutavel =
                System.getenv(
                        "COBOL_EXECUTAVEL_TESTE"
                );

        assumeTrue(
                caminhoExecutavel != null
                        && !caminhoExecutavel.isBlank(),
                "COBOL_EXECUTAVEL_TESTE nao configurado."
        );

        executavelCobol =
                Path.of(
                        caminhoExecutavel
                )
                        .toAbsolutePath()
                        .normalize();

        assumeTrue(
                Files.isRegularFile(
                        executavelCobol
                ),
                "Executavel COBOL de teste nao existe."
        );

        assumeTrue(
                Files.isExecutable(
                        executavelCobol
                ),
                "Executavel COBOL de teste nao possui permissao."
        );

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
                    "Teste ponta a ponta nao pode "
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

        if ("conciliacao_pagamentos".equals(
                bancoApp
        )
                || "conciliacao_pagamentos".equals(
                        bancoAdmin
                )) {

            throw new IllegalStateException(
                    "Protecao acionada: conexao "
                            + "apontou para o banco principal."
            );
        }

        assertEquals(
                bancoEsperado,
                bancoApp
        );

        assertEquals(
                bancoEsperado,
                bancoAdmin
        );

        assertEquals(
                "conciliacao_app",
                jdbcApp.queryForObject(
                        "SELECT current_user",
                        String.class
                )
        );

        limparBancoTeste();
    }

    @AfterEach
    void limpar() {
        if (jdbcAdmin != null) {
            limparBancoTeste();
        }
    }

    @Test
    void deveExecutarConciliacaoCompletaComPostgresqlECobolReal() {
        prepararDadosOrigem();

        DataSource dataSource =
                jdbcApp.getDataSource();

        assertNotNull(
                dataSource
        );

        DataSourceTransactionManager transactionManager =
                new DataSourceTransactionManager(
                        dataSource
                );

        RepositorioExecucaoConciliacaoJdbc
                repositorioExecucao =
                new RepositorioExecucaoConciliacaoJdbc(
                        jdbcApp
                );

        RepositorioSnapshotConciliacaoJdbc
                repositorioSnapshot =
                new RepositorioSnapshotConciliacaoJdbc(
                        jdbcApp,
                        transactionManager
                );

        RepositorioResultadoConciliacaoJdbc
                repositorioResultado =
                new RepositorioResultadoConciliacaoJdbc(
                        jdbcApp,
                        transactionManager
                );

        ExecutorCobol executor =
                new ExecutorCobol(
                        executavelCobol,
                        Duration.ofSeconds(10)
                );

        IntegradorExecucaoCobol integrador =
                new IntegradorExecucaoCobol(
                        executor
                );

        ProcessadorConciliacaoCobolReal processador =
                new ProcessadorConciliacaoCobolReal(
                        integrador
                );

        ServicoConciliacaoReal servico =
                new ServicoConciliacaoReal(
                        repositorioExecucao,
                        repositorioSnapshot,
                        repositorioResultado,
                        processador
                );

        ResultadoServicoConciliacao resposta =
                servico.executar();

        assertEquals(
                StatusExecucaoConciliacao.CONCLUIDA,
                resposta.status()
        );

        assertTrue(
                resposta.id() > 0
        );

        assertEquals(
                1,
                resposta.resultado().versao()
        );

        validarDetalhes(
                resposta
        );

        validarResumo(
                resposta
        );

        validarPersistenciaDefinitiva(
                resposta.id()
        );
    }

    private void prepararDadosOrigem() {
        inserirCobranca(
                "COB001",
                "100.50"
        );

        inserirCobranca(
                "COB002",
                "200.00"
        );

        inserirCobranca(
                "COB003",
                "75.25"
        );

        inserirCobranca(
                "COB004",
                "90.00"
        );

        inserirPagamento(
                "COB001",
                "100.50"
        );

        inserirPagamento(
                "COB002",
                "180.00"
        );

        inserirPagamento(
                "COB999",
                "50.00"
        );

        inserirPagamento(
                "COB001",
                "100.50"
        );

        inserirPagamento(
                "COB003",
                "75.25"
        );
    }

    private void validarDetalhes(
            ResultadoServicoConciliacao resposta
    ) {
        var detalhes =
                resposta.resultado()
                        .detalhes();

        assertEquals(
                5,
                detalhes.size()
        );

        assertEquals(
                List.of(
                        StatusConciliacao.DUPLICADO,
                        StatusConciliacao.ABAIXO_DO_ESPERADO,
                        StatusConciliacao.CONFERIDO,
                        StatusConciliacao.SEM_RECEBIMENTO,
                        StatusConciliacao.SEM_PREVISAO
                ),
                detalhes.stream()
                        .map(
                                detalhe ->
                                        detalhe.status()
                        )
                        .toList()
        );

        assertEquals(
                List.of(
                        "COB001",
                        "COB002",
                        "COB003",
                        "COB004",
                        "COB999"
                ),
                detalhes.stream()
                        .map(
                                detalhe ->
                                        detalhe.identificador()
                        )
                        .toList()
        );
    }

    private void validarResumo(
            ResultadoServicoConciliacao resposta
    ) {
        var resumo =
                resposta.resultado()
                        .resumo();

        assertEquals(
                1,
                resumo.conferidos()
        );

        assertEquals(
                0,
                resumo.acima()
        );

        assertEquals(
                1,
                resumo.abaixo()
        );

        assertEquals(
                1,
                resumo.duplicados()
        );

        assertEquals(
                1,
                resumo.semRecebimento()
        );

        assertEquals(
                1,
                resumo.semPrevisao()
        );

        assertBigDecimal(
                "465.75",
                resumo.totalEsperado()
        );

        assertBigDecimal(
                "506.25",
                resumo.totalRecebido()
        );

        assertBigDecimal(
                "40.50",
                resumo.saldoGlobal()
        );
    }

    private void validarPersistenciaDefinitiva(
            long conciliacaoId
    ) {
        String status =
                jdbcApp.queryForObject(
                        """
                        SELECT status
                        FROM public.conciliacoes
                        WHERE id = ?
                        """,
                        String.class,
                        conciliacaoId
                );

        Integer versao =
                jdbcApp.queryForObject(
                        """
                        SELECT resultado_versao
                        FROM public.conciliacoes
                        WHERE id = ?
                        """,
                        Integer.class,
                        conciliacaoId
                );

        Long cobrancasSnapshot =
                jdbcApp.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_snapshot_cobrancas
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
                );

        Long pagamentosSnapshot =
                jdbcApp.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_snapshot_pagamentos
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
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
                "CONCLUIDA",
                status
        );

        assertEquals(
                1,
                versao
        );

        assertEquals(
                4L,
                cobrancasSnapshot
        );

        assertEquals(
                5L,
                pagamentosSnapshot
        );

        assertEquals(
                1L,
                resumos
        );

        assertEquals(
                5L,
                detalhes
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

    private void inserirCobranca(
            String identificador,
            String valor
    ) {
        int linhas =
                jdbcApp.update(
                        """
                        INSERT INTO public.cobrancas (
                            identificador,
                            valor_esperado
                        )
                        VALUES (?, ?)
                        """,
                        identificador,
                        new BigDecimal(valor)
                );

        assertEquals(
                1,
                linhas
        );
    }

    private void inserirPagamento(
            String identificador,
            String valor
    ) {
        int linhas =
                jdbcApp.update(
                        """
                        INSERT INTO public.pagamentos (
                            identificador_cobranca,
                            valor_pago
                        )
                        VALUES (?, ?)
                        """,
                        identificador,
                        new BigDecimal(valor)
                );

        assertEquals(
                1,
                linhas
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
}
