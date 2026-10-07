package br.com.brunolopes.conciliacao.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.brunolopes.conciliacao.modelo.consulta.ConciliacaoConsultada;
import br.com.brunolopes.conciliacao.modelo.consulta.ItemHistoricoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;

@EnabledIfEnvironmentVariable(
        named = "CONCILIACAO_TESTE_POSTGRESQL",
        matches = "true"
)
class RepositorioConsultaConciliacaoJdbcPostgresqlTests {

    private JdbcTemplate jdbcTemplate;
    private RepositorioConsultaConciliacaoJdbc repositorio;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void preparar() {
        String url =
                variavelObrigatoria(
                        "CONCILIACAO_DB_URL"
                );

        String usuario =
                variavelObrigatoria(
                        "CONCILIACAO_DB_USER"
                );

        String senha =
                variavelObrigatoria(
                        "CONCILIACAO_DB_PASSWORD"
                );

        String bancoEsperado =
                variavelObrigatoria(
                        "CONCILIACAO_DB_TEST_NAME"
                );

        if ("conciliacao_pagamentos".equals(
                bancoEsperado
        )) {
            throw new IllegalStateException(
                    "Teste PostgreSQL nao pode usar o banco principal."
            );
        }

        DataSource dataSource =
                new DriverManagerDataSource(
                        url,
                        usuario,
                        senha
                );

        jdbcTemplate =
                new JdbcTemplate(
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
                new RepositorioConsultaConciliacaoJdbc(
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
    void deveListarComPaginacaoEOrdenacao() {
        transactionTemplate.executeWithoutResult(
                statusTransacao -> {

                    long quantidadeAntes =
                            repositorio.contar(
                                    null
                            );

                    long idMaisAntigo =
                            inserirCriada(
                                    "2099-01-01 10:00:00+00"
                            );

                    long idIntermediario =
                            inserirCriada(
                                    "2099-01-01 11:00:00+00"
                            );

                    long idMaisRecente =
                            inserirCriada(
                                    "2099-01-01 12:00:00+00"
                            );

                    assertEquals(
                            quantidadeAntes + 3,
                            repositorio.contar(
                                    null
                            )
                    );

                    List<ItemHistoricoConciliacao>
                            primeiraPagina =
                                    repositorio.listar(
                                            2,
                                            0,
                                            null
                                    );

                    assertEquals(
                            2,
                            primeiraPagina.size()
                    );

                    assertEquals(
                            idMaisRecente,
                            primeiraPagina.get(0).id()
                    );

                    assertEquals(
                            idIntermediario,
                            primeiraPagina.get(1).id()
                    );

                    List<ItemHistoricoConciliacao>
                            segundaPagina =
                                    repositorio.listar(
                                            1,
                                            2,
                                            null
                                    );

                    assertEquals(
                            1,
                            segundaPagina.size()
                    );

                    assertEquals(
                            idMaisAntigo,
                            segundaPagina.get(0).id()
                    );

                    statusTransacao.setRollbackOnly();
                }
        );
    }

    @Test
    void deveFiltrarEContarPorStatus() {
        transactionTemplate.executeWithoutResult(
                statusTransacao -> {

                    long falhasAntes =
                            repositorio.contar(
                                    StatusExecucaoConciliacao.FALHOU
                            );

                    long idFalhou =
                            inserirFalhou(
                                    "2099-02-01 12:00:00+00"
                            );

                    long idCriada =
                            inserirCriada(
                                    "2099-02-02 12:00:00+00"
                            );

                    assertEquals(
                            falhasAntes + 1,
                            repositorio.contar(
                                    StatusExecucaoConciliacao.FALHOU
                            )
                    );

                    List<ItemHistoricoConciliacao>
                            falhas =
                                    repositorio.listar(
                                            1,
                                            0,
                                            StatusExecucaoConciliacao.FALHOU
                                    );

                    assertEquals(
                            1,
                            falhas.size()
                    );

                    assertEquals(
                            idFalhou,
                            falhas.get(0).id()
                    );

                    assertEquals(
                            StatusExecucaoConciliacao.FALHOU,
                            falhas.get(0).status()
                    );

                    assertFalse(
                            falhas.stream()
                                    .anyMatch(
                                            item ->
                                                    item.id()
                                                            == idCriada
                                    )
                    );

                    statusTransacao.setRollbackOnly();
                }
        );
    }

    @Test
    void deveBuscarConciliacaoConcluidaComResumoEDetalhes() {
        /*
         * buscarPorId usa REQUIRES_NEW. A massa precisa estar commitada
         * antes da leitura para representar o que outra transacao enxerga.
         */
        Long idCriado =
                transactionTemplate.execute(
                        statusTransacao -> {
                            long id =
                                    inserirConcluida();

                            inserirResumo(
                                    id
                            );

                            inserirDetalhes(
                                    id
                            );

                            return id;
                        }
                );

        long id =
                exigirId(
                        idCriado
                );

        try {
            Optional<ConciliacaoConsultada>
                    resultado =
                            repositorio.buscarPorId(
                                    id
                            );

            assertTrue(
                    resultado.isPresent()
            );

            ConciliacaoConsultada conciliacao =
                    resultado.orElseThrow();

            assertEquals(
                    id,
                    conciliacao.id()
            );

            assertEquals(
                    StatusExecucaoConciliacao.CONCLUIDA,
                    conciliacao.status()
            );

            assertEquals(
                    1,
                    conciliacao.resultadoVersao()
            );

            assertNull(
                    conciliacao.erroCodigo()
            );

            assertEquals(
                    1,
                    conciliacao
                            .resumo()
                            .conferidos()
            );

            assertEquals(
                    1,
                    conciliacao
                            .resumo()
                            .semRecebimento()
            );

            assertEquals(
                    new BigDecimal("300.00"),
                    conciliacao
                            .resumo()
                            .totalEsperado()
            );

            assertEquals(
                    new BigDecimal("100.00"),
                    conciliacao
                            .resumo()
                            .totalRecebido()
            );

            assertEquals(
                    new BigDecimal("-200.00"),
                    conciliacao
                            .resumo()
                            .saldoGlobal()
            );

            assertEquals(
                    2,
                    conciliacao.detalhes().size()
            );

            assertEquals(
                    1,
                    conciliacao
                            .detalhes()
                            .get(0)
                            .ordem()
            );

            assertEquals(
                    "P001",
                    conciliacao
                            .detalhes()
                            .get(0)
                            .identificador()
            );

            assertEquals(
                    2,
                    conciliacao
                            .detalhes()
                            .get(1)
                            .ordem()
            );

            assertEquals(
                    "P002",
                    conciliacao
                            .detalhes()
                            .get(1)
                            .identificador()
            );

            assertNull(
                    conciliacao
                            .detalhes()
                            .get(1)
                            .valorRecebido()
            );

        } finally {
            excluirConciliacaoTeste(
                    id
            );
        }
    }
    @Test
    void deveBuscarExecucaoFalhaSemResultado() {
        /*
         * buscarPorId abre REQUIRES_NEW; por isso a execucao de teste e
         * commitada antes da consulta.
         */
        Long idCriado =
                transactionTemplate.execute(
                        statusTransacao ->
                                inserirFalhou(
                                        "2099-03-01 12:00:00+00"
                                )
                );

        long id =
                exigirId(
                        idCriado
                );

        try {
            ConciliacaoConsultada conciliacao =
                    repositorio.buscarPorId(
                                    id
                            )
                            .orElseThrow();

            assertEquals(
                    StatusExecucaoConciliacao.FALHOU,
                    conciliacao.status()
            );

            assertEquals(
                    "ERRO_TESTE",
                    conciliacao.erroCodigo()
            );

            assertNull(
                    conciliacao.resumo()
            );

            assertTrue(
                    conciliacao.detalhes().isEmpty()
            );

        } finally {
            excluirConciliacaoTeste(
                    id
            );
        }
    }
    @Test
    void deveRetornarVazioParaConciliacaoInexistente() {
        Optional<ConciliacaoConsultada> resultado =
                repositorio.buscarPorId(
                        Long.MAX_VALUE
                );

        assertTrue(
                resultado.isEmpty()
        );
    }

    @Test
    void deveRejeitarPaginacaoEIdInvalidos() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        repositorio.listar(
                                0,
                                0,
                                null
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        repositorio.listar(
                                101,
                                0,
                                null
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        repositorio.listar(
                                20,
                                -1,
                                null
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        repositorio.buscarPorId(
                                0
                        )
        );
    }

    private long inserirCriada(
            String criadaEm
    ) {
        Long id =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO public.conciliacoes (
                            status,
                            criada_em
                        )
                        VALUES (
                            'CRIADA',
                            ?::timestamptz
                        )
                        RETURNING id
                        """,
                        Long.class,
                        criadaEm
                );

        return exigirId(
                id
        );
    }

    private long inserirFalhou(
            String criadaEm
    ) {
        Long id =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO public.conciliacoes (
                            status,
                            criada_em,
                            iniciada_em,
                            finalizada_em,
                            erro_codigo
                        )
                        VALUES (
                            'FALHOU',
                            ?::timestamptz,
                            ?::timestamptz,
                            ?::timestamptz,
                            'ERRO_TESTE'
                        )
                        RETURNING id
                        """,
                        Long.class,
                        criadaEm,
                        criadaEm,
                        criadaEm
                );

        return exigirId(
                id
        );
    }

    private long inserirConcluida() {
        Long id =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO public.conciliacoes (
                            status,
                            criada_em,
                            iniciada_em,
                            finalizada_em,
                            resultado_versao
                        )
                        VALUES (
                            'CONCLUIDA',
                            '2099-04-01 10:00:00+00'::timestamptz,
                            '2099-04-01 10:00:01+00'::timestamptz,
                            '2099-04-01 10:00:02+00'::timestamptz,
                            1
                        )
                        RETURNING id
                        """,
                        Long.class
                );

        return exigirId(
                id
        );
    }

    private void inserirResumo(
            long conciliacaoId
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO public.conciliacao_resumos (
                    conciliacao_id,
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
                )
                VALUES (
                    ?,
                    1,
                    1,
                    0,
                    0,
                    0,
                    1,
                    0,
                    300.00,
                    100.00,
                    -200.00
                )
                """,
                conciliacaoId
        );
    }

    private void inserirDetalhes(
            long conciliacaoId
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO public.conciliacao_detalhes (
                    conciliacao_id,
                    ordem,
                    identificador,
                    valor_esperado,
                    valor_recebido,
                    diferenca,
                    status,
                    quantidade_recebimentos
                )
                VALUES (
                    ?,
                    1,
                    'P001',
                    100.00,
                    100.00,
                    0.00,
                    'CONFERIDO',
                    1
                )
                """,
                conciliacaoId
        );

        jdbcTemplate.update(
                """
                INSERT INTO public.conciliacao_detalhes (
                    conciliacao_id,
                    ordem,
                    identificador,
                    valor_esperado,
                    valor_recebido,
                    diferenca,
                    status,
                    quantidade_recebimentos
                )
                VALUES (
                    ?,
                    2,
                    'P002',
                    200.00,
                    NULL,
                    NULL,
                    'SEM_RECEBIMENTO',
                    0
                )
                """,
                conciliacaoId
        );
    }

    private void excluirConciliacaoTeste(
            long conciliacaoId
    ) {
        transactionTemplate.executeWithoutResult(
                statusTransacao -> {
                    jdbcTemplate.update(
                            "DELETE FROM public.conciliacao_detalhes WHERE conciliacao_id = ?",
                            conciliacaoId
                    );

                    jdbcTemplate.update(
                            "DELETE FROM public.conciliacao_resumos WHERE conciliacao_id = ?",
                            conciliacaoId
                    );

                    jdbcTemplate.update(
                            "DELETE FROM public.conciliacao_snapshot_pagamentos WHERE conciliacao_id = ?",
                            conciliacaoId
                    );

                    jdbcTemplate.update(
                            "DELETE FROM public.conciliacao_snapshot_cobrancas WHERE conciliacao_id = ?",
                            conciliacaoId
                    );

                    jdbcTemplate.update(
                            "DELETE FROM public.conciliacoes WHERE id = ?",
                            conciliacaoId
                    );
                }
        );
    }

    private long exigirId(
            Long id
    ) {
        if (id == null) {
            throw new IllegalStateException(
                    "Banco nao retornou o ID criado pelo teste."
            );
        }

        return id;
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
