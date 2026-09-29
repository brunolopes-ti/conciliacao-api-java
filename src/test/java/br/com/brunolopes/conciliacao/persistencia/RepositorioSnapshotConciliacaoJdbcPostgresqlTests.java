package br.com.brunolopes.conciliacao.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import br.com.brunolopes.conciliacao.aplicacao.excecao.ConflitoDadosException;
import br.com.brunolopes.conciliacao.integracao.SnapshotExecucaoCobol;
import br.com.brunolopes.conciliacao.testutil.BancoTestePostgresql;

@EnabledIfEnvironmentVariable(
        named = "CONCILIACAO_TESTE_POSTGRESQL",
        matches = "true"
)
class RepositorioSnapshotConciliacaoJdbcPostgresqlTests {

    private DataSource dataSourceApp;
    private DataSource dataSourceAdmin;

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
                    "Teste PostgreSQL nao pode usar o banco principal."
            );
        }

        dataSourceApp = new DriverManagerDataSource(
                urlApp,
                usuarioApp,
                senhaApp
        );

        dataSourceAdmin = new DriverManagerDataSource(
                urlAdmin,
                usuarioAdmin,
                senhaAdmin
        );

        jdbcApp = new JdbcTemplate(dataSourceApp);
        jdbcAdmin = new JdbcTemplate(dataSourceAdmin);

        String bancoApp = jdbcApp.queryForObject(
                "SELECT current_database()",
                String.class
        );

        String bancoAdmin = jdbcAdmin.queryForObject(
                "SELECT current_database()",
                String.class
        );

        String usuarioAtual = jdbcApp.queryForObject(
                "SELECT current_user",
                String.class
        );

        assertEquals(
                bancoEsperado,
                bancoApp,
                "Aplicacao conectou ao banco incorreto."
        );

        assertEquals(
                bancoEsperado,
                bancoAdmin,
                "Administrador conectou ao banco incorreto."
        );

        assertEquals(
                "conciliacao_app",
                usuarioAtual,
                "Repositorio deve ser testado com o usuario da aplicacao."
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
    void devePreservarOrdemEOrigemDoSnapshot() {
        long cobrancaZ = inserirCobranca(
                "COB-Z",
                "200.00"
        );

        long cobrancaA = inserirCobranca(
                "COB-A",
                "100.50"
        );

        long pagamentoA1 = inserirPagamento(
                "COB-A",
                "100.50"
        );

        long pagamentoZ = inserirPagamento(
                "COB-Z",
                "180.00"
        );

        long pagamentoA2 = inserirPagamento(
                "COB-A",
                "5.00"
        );

        long conciliacaoId =
                criarExecucaoEmProcessamento();

        SnapshotExecucaoCobol snapshot =
                repositorioSnapshot.capturarEPersistir(
                        conciliacaoId
                );

        assertEquals(
                List.of(
                        "COB-Z",
                        "COB-A"
                ),
                snapshot.cobrancas()
                        .stream()
                        .map(
                                SnapshotExecucaoCobol.Item
                                        ::identificador
                        )
                        .toList()
        );

        assertEquals(
                List.of(
                        "COB-A",
                        "COB-Z",
                        "COB-A"
                ),
                snapshot.pagamentos()
                        .stream()
                        .map(
                                SnapshotExecucaoCobol.Item
                                        ::identificador
                        )
                        .toList()
        );

        List<RegistroSnapshot> cobrancasPersistidas =
                jdbcApp.query(
                        """
                        SELECT
                            ordem,
                            origem_id,
                            identificador,
                            valor_esperado
                        FROM public.conciliacao_snapshot_cobrancas
                        WHERE conciliacao_id = ?
                        ORDER BY ordem
                        """,
                        (resultado, numeroLinha) ->
                                new RegistroSnapshot(
                                        resultado.getInt(
                                                "ordem"
                                        ),
                                        resultado.getLong(
                                                "origem_id"
                                        ),
                                        resultado.getString(
                                                "identificador"
                                        ),
                                        resultado.getBigDecimal(
                                                "valor_esperado"
                                        )
                                ),
                        conciliacaoId
                );

        assertEquals(
                List.of(
                        new RegistroSnapshot(
                                1,
                                cobrancaZ,
                                "COB-Z",
                                new BigDecimal("200.00")
                        ),
                        new RegistroSnapshot(
                                2,
                                cobrancaA,
                                "COB-A",
                                new BigDecimal("100.50")
                        )
                ),
                cobrancasPersistidas
        );

        List<RegistroSnapshot> pagamentosPersistidos =
                jdbcApp.query(
                        """
                        SELECT
                            ordem,
                            origem_id,
                            identificador_cobranca,
                            valor_pago
                        FROM public.conciliacao_snapshot_pagamentos
                        WHERE conciliacao_id = ?
                        ORDER BY ordem
                        """,
                        (resultado, numeroLinha) ->
                                new RegistroSnapshot(
                                        resultado.getInt(
                                                "ordem"
                                        ),
                                        resultado.getLong(
                                                "origem_id"
                                        ),
                                        resultado.getString(
                                                "identificador_cobranca"
                                        ),
                                        resultado.getBigDecimal(
                                                "valor_pago"
                                        )
                                ),
                        conciliacaoId
                );

        assertEquals(
                List.of(
                        new RegistroSnapshot(
                                1,
                                pagamentoA1,
                                "COB-A",
                                new BigDecimal("100.50")
                        ),
                        new RegistroSnapshot(
                                2,
                                pagamentoZ,
                                "COB-Z",
                                new BigDecimal("180.00")
                        ),
                        new RegistroSnapshot(
                                3,
                                pagamentoA2,
                                "COB-A",
                                new BigDecimal("5.00")
                        )
                ),
                pagamentosPersistidos
        );
    }

    @Test
    void deveRejeitarMaisDeMilPagamentos() {
        inserirCobranca(
                "COB-LIMITE",
                "100.00"
        );

        int inseridos = jdbcApp.update(
                """
                INSERT INTO public.pagamentos (
                    identificador_cobranca,
                    valor_pago
                )
                SELECT
                    'COB-LIMITE',
                    1.00
                FROM generate_series(1, 1001)
                """
        );

        assertEquals(
                1001,
                inseridos
        );

        long conciliacaoId =
                criarExecucaoEmProcessamento();

        assertThrows(
                ConflitoDadosException.class,
                () ->
                        repositorioSnapshot
                                .capturarEPersistir(
                                        conciliacaoId
                                )
        );

        assertEquals(
                0,
                contarSnapshotCobrancas(
                        conciliacaoId
                )
        );

        assertEquals(
                0,
                contarSnapshotPagamentos(
                        conciliacaoId
                )
        );
    }

    @Test
    void deveFazerRollbackSePersistenciaFalharNoMeio() {
        inserirCobranca(
                "COB-ROLLBACK",
                "100.00"
        );

        inserirPagamento(
                "COB-ROLLBACK",
                "100.00"
        );

        long conciliacaoId =
                criarExecucaoEmProcessamento();

        jdbcAdmin.execute(
                """
                CREATE OR REPLACE FUNCTION
                    public.teste_falha_snapshot_pagamento()
                RETURNS trigger
                LANGUAGE plpgsql
                AS $$
                BEGIN
                    RAISE EXCEPTION
                        'Falha proposital do teste.';
                END;
                $$
                """
        );

        jdbcAdmin.execute(
                """
                CREATE TRIGGER teste_falha_snapshot_pagamento
                BEFORE INSERT
                ON public.conciliacao_snapshot_pagamentos
                FOR EACH ROW
                EXECUTE FUNCTION
                    public.teste_falha_snapshot_pagamento()
                """
        );

        assertThrows(
                DataAccessException.class,
                () ->
                        repositorioSnapshot
                                .capturarEPersistir(
                                        conciliacaoId
                                )
        );

        assertEquals(
                0,
                contarSnapshotCobrancas(
                        conciliacaoId
                )
        );

        assertEquals(
                0,
                contarSnapshotPagamentos(
                        conciliacaoId
                )
        );

        String status = jdbcApp.queryForObject(
                """
                SELECT status
                FROM public.conciliacoes
                WHERE id = ?
                """,
                String.class,
                conciliacaoId
        );

        assertEquals(
                "EM_PROCESSAMENTO",
                status
        );
    }

    @Test
    void deveUsarSnapshotRepeatableRead() throws Exception {
        inserirCobranca(
                "COB-ISOLAMENTO",
                "100.00"
        );

        inserirPagamento(
                "COB-ISOLAMENTO",
                "100.00"
        );

        long conciliacaoId =
                criarExecucaoEmProcessamento();

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        Future<SnapshotExecucaoCobol> execucao =
                null;

        Connection bloqueio =
                dataSourceAdmin.getConnection();

        try {
            bloqueio.setAutoCommit(false);

            try (Statement comando =
                         bloqueio.createStatement()) {

                comando.execute(
                        """
                        LOCK TABLE public.cobrancas
                        IN ACCESS EXCLUSIVE MODE
                        """
                );
            }

            execucao = executor.submit(
                    () ->
                            repositorioSnapshot
                                    .capturarEPersistir(
                                            conciliacaoId
                                    )
            );

            aguardarLeituraBloqueada();

            inserirPagamento(
                    "COB-ISOLAMENTO",
                    "25.00"
            );

            bloqueio.commit();

            SnapshotExecucaoCobol snapshot =
                    execucao.get(
                            10,
                            TimeUnit.SECONDS
                    );

            assertEquals(
                    1,
                    snapshot.pagamentos().size(),
                    """
                    Pagamento confirmado depois do inicio
                    da transacao nao deveria entrar no snapshot.
                    """
            );

            assertEquals(
                    new BigDecimal("100.00"),
                    snapshot.pagamentos()
                            .getFirst()
                            .valor()
            );

            Long pagamentosOrigem =
                    jdbcApp.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM public.pagamentos
                            WHERE identificador_cobranca =
                                'COB-ISOLAMENTO'
                            """,
                            Long.class
                    );

            assertEquals(
                    2L,
                    pagamentosOrigem
            );

            assertEquals(
                    1,
                    contarSnapshotPagamentos(
                            conciliacaoId
                    )
            );

        } finally {
            try {
                bloqueio.rollback();
            } finally {
                bloqueio.close();
            }

            if (execucao != null
                    && !execucao.isDone()) {
                execucao.cancel(true);
            }

            executor.shutdownNow();
        }
    }

    private void aguardarLeituraBloqueada()
            throws InterruptedException {

        long limite =
                System.nanoTime()
                        + TimeUnit.SECONDS.toNanos(5);

        while (System.nanoTime() < limite) {
            Long bloqueadas = jdbcAdmin.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM pg_stat_activity
                    WHERE datname = current_database()
                      AND usename = 'conciliacao_app'
                      AND state = 'active'
                      AND wait_event_type = 'Lock'
                      AND query ILIKE
                          '%FROM public.cobrancas%'
                    """,
                    Long.class
            );

            if (bloqueadas != null
                    && bloqueadas > 0) {
                return;
            }

            Thread.sleep(50);
        }

        fail(
                """
                Repositorio nao chegou ao ponto
                esperado de bloqueio na leitura
                de cobrancas.
                """
        );
    }

    private long criarExecucaoEmProcessamento() {
        long id =
                repositorioExecucao.criar();

        repositorioExecucao.iniciar(id);

        return id;
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

    private int contarSnapshotCobrancas(
            long conciliacaoId
    ) {
        Long quantidade =
                jdbcApp.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_snapshot_cobrancas
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
                );

        return quantidade == null
                ? 0
                : quantidade.intValue();
    }

    private int contarSnapshotPagamentos(
            long conciliacaoId
    ) {
        Long quantidade =
                jdbcApp.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_snapshot_pagamentos
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
                );

        return quantidade == null
                ? 0
                : quantidade.intValue();
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

    private record RegistroSnapshot(
            int ordem,
            long origemId,
            String identificador,
            BigDecimal valor
    ) {
    }
}
