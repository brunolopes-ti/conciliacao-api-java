package br.com.brunolopes.conciliacao.integracao;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import br.com.brunolopes.conciliacao.persistencia.RepositorioExecucaoConciliacaoJdbc;
import br.com.brunolopes.conciliacao.persistencia.RepositorioSnapshotConciliacaoJdbc;
import br.com.brunolopes.conciliacao.testutil.BancoTestePostgresql;

@EnabledIfEnvironmentVariable(
        named = "CONCILIACAO_TESTE_POSTGRESQL",
        matches = "true"
)
class FluxoSnapshotArquivosPostgresqlTests {

    private JdbcTemplate jdbcApp;
    private JdbcTemplate jdbcAdmin;

    private RepositorioExecucaoConciliacaoJdbc repositorioExecucao;
    private RepositorioSnapshotConciliacaoJdbc repositorioSnapshot;

    @BeforeEach
    void preparar() {
        String urlApp = variavelObrigatoria(
                "CONCILIACAO_DB_URL"
        );

        String usuarioApp = variavelObrigatoria(
                "CONCILIACAO_DB_USER"
        );

        String senhaApp = variavelObrigatoria(
                "CONCILIACAO_DB_PASSWORD"
        );

        String urlAdmin = variavelObrigatoria(
                "CONCILIACAO_FLYWAY_URL"
        );

        String usuarioAdmin = variavelObrigatoria(
                "CONCILIACAO_FLYWAY_USER"
        );

        String senhaAdmin = variavelObrigatoria(
                "CONCILIACAO_FLYWAY_PASSWORD"
        );

        String bancoEsperado = variavelObrigatoria(
                "CONCILIACAO_DB_TEST_NAME"
        );

        if ("conciliacao_pagamentos".equals(bancoEsperado)) {
            throw new IllegalStateException(
                    "Teste integrado nao pode usar o banco principal."
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

        jdbcApp = new JdbcTemplate(
                dataSourceApp
        );

        jdbcAdmin = new JdbcTemplate(
                dataSourceAdmin
        );

        assertEquals(
                bancoEsperado,
                jdbcApp.queryForObject(
                        "SELECT current_database()",
                        String.class
                )
        );

        assertEquals(
                bancoEsperado,
                jdbcAdmin.queryForObject(
                        "SELECT current_database()",
                        String.class
                )
        );

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

        repositorioSnapshot =
                new RepositorioSnapshotConciliacaoJdbc(
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
    void deveGerarArquivosAPartirDoSnapshotDoBanco()
            throws Exception {

        inserirCobranca(
                "COB003",
                "75.25"
        );

        inserirCobranca(
                "COB001",
                "100.50"
        );

        inserirCobranca(
                "COB002",
                "200.00"
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
                "COB001",
                "100.50"
        );

        inserirPagamento(
                "COB999",
                "50.00"
        );

        long conciliacaoId =
                repositorioExecucao.criar();

        repositorioExecucao.iniciar(
                conciliacaoId
        );

        SnapshotExecucaoCobol snapshot =
                repositorioSnapshot.capturarEPersistir(
                        conciliacaoId
                );

        try (DiretorioExecucaoCobol execucao =
                     DiretorioExecucaoCobol.criar()) {

            GeradorArquivosEntradaCobol.gerar(
                    execucao,
                    snapshot
            );

            String esperados =
                    Files.readString(
                            execucao.esperados(),
                            UTF_8
                    );

            String recebidos =
                    Files.readString(
                            execucao.recebidos(),
                            UTF_8
                    );

            assertEquals(
                    """
                    COB003;75.25
                    COB001;100.50
                    COB002;200.00
                    """,
                    esperados
            );

            assertEquals(
                    """
                    COB001;100.50
                    COB002;180.00
                    COB001;100.50
                    COB999;50.00
                    """,
                    recebidos
            );

            assertFalse(
                    esperados.contains("\r")
            );

            assertFalse(
                    recebidos.contains("\r")
            );

            validarSnapshotPersistido(
                    conciliacaoId
            );
        }
    }

    private void validarSnapshotPersistido(
            long conciliacaoId
    ) {
        List<String> cobrancas =
                jdbcApp.query(
                        """
                        SELECT
                            ordem
                            || ':'
                            || identificador
                            || ':'
                            || valor_esperado
                        FROM public.conciliacao_snapshot_cobrancas
                        WHERE conciliacao_id = ?
                        ORDER BY ordem
                        """,
                        (resultado, numeroLinha) ->
                                resultado.getString(1),
                        conciliacaoId
                );

        assertEquals(
                List.of(
                        "1:COB003:75.25",
                        "2:COB001:100.50",
                        "3:COB002:200.00"
                ),
                cobrancas
        );

        List<String> pagamentos =
                jdbcApp.query(
                        """
                        SELECT
                            ordem
                            || ':'
                            || identificador_cobranca
                            || ':'
                            || valor_pago
                        FROM public.conciliacao_snapshot_pagamentos
                        WHERE conciliacao_id = ?
                        ORDER BY ordem
                        """,
                        (resultado, numeroLinha) ->
                                resultado.getString(1),
                        conciliacaoId
                );

        assertEquals(
                List.of(
                        "1:COB001:100.50",
                        "2:COB002:180.00",
                        "3:COB001:100.50",
                        "4:COB999:50.00"
                ),
                pagamentos
        );
    }

    private long inserirCobranca(
            String identificador,
            String valor
    ) {
        Long id = jdbcApp.queryForObject(
                """
                INSERT INTO public.cobrancas (
                    identificador,
                    valor_esperado
                )
                VALUES (?, ?)
                RETURNING id
                """,
                Long.class,
                identificador,
                new BigDecimal(valor)
        );

        if (id == null) {
            throw new IllegalStateException(
                    "Banco nao retornou id da cobranca."
            );
        }

        return id;
    }

    private long inserirPagamento(
            String identificador,
            String valor
    ) {
        Long id = jdbcApp.queryForObject(
                """
                INSERT INTO public.pagamentos (
                    identificador_cobranca,
                    valor_pago
                )
                VALUES (?, ?)
                RETURNING id
                """,
                Long.class,
                identificador,
                new BigDecimal(valor)
        );

        if (id == null) {
            throw new IllegalStateException(
                    "Banco nao retornou id do pagamento."
            );
        }

        return id;
    }

    private void limparBancoTeste() {
        BancoTestePostgresql.limpar(
                jdbcAdmin
        );
    }

    private String variavelObrigatoria(
            String nome
    ) {
        String valor = System.getenv(nome);

        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Variavel obrigatoria ausente: "
                            + nome
            );
        }

        return valor;
    }
}
