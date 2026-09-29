package br.com.brunolopes.conciliacao.persistencia;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.brunolopes.conciliacao.aplicacao.excecao.ConflitoDadosException;
import br.com.brunolopes.conciliacao.integracao.SnapshotExecucaoCobol;

@Repository
@ConditionalOnProperty(
        name = "conciliacao.persistencia.habilitada",
        havingValue = "true",
        matchIfMissing = true
)
public class RepositorioSnapshotConciliacaoJdbc
        implements RepositorioSnapshotConciliacao {

    private static final int LIMITE_ITENS = 1000;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public RepositorioSnapshotConciliacaoJdbc(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = jdbcTemplate;

        this.transactionTemplate =
                new TransactionTemplate(transactionManager);

        this.transactionTemplate.setIsolationLevel(
                TransactionDefinition.ISOLATION_REPEATABLE_READ
        );
    }

    @Override
    public SnapshotExecucaoCobol capturarEPersistir(
            long conciliacaoId
    ) {
        validarId(conciliacaoId);

        SnapshotExecucaoCobol snapshot =
                transactionTemplate.execute(
                        status -> capturarDentroDaTransacao(
                                conciliacaoId
                        )
                );

        if (snapshot == null) {
            throw new IllegalStateException(
                    "Transacao nao retornou o snapshot."
            );
        }

        return snapshot;
    }

    private SnapshotExecucaoCobol capturarDentroDaTransacao(
            long conciliacaoId
    ) {
        validarExecucaoEmProcessamento(conciliacaoId);
        validarSnapshotAindaNaoExiste(conciliacaoId);

        List<ItemOrigem> cobrancas =
                carregarCobrancas();

        List<ItemOrigem> pagamentos =
                carregarPagamentos();

        validarQuantidade(
                cobrancas,
                "cobrancas"
        );

        validarQuantidade(
                pagamentos,
                "pagamentos"
        );

        SnapshotExecucaoCobol snapshot =
                criarSnapshot(
                        cobrancas,
                        pagamentos
                );

        persistirCobrancas(
                conciliacaoId,
                cobrancas
        );

        persistirPagamentos(
                conciliacaoId,
                pagamentos
        );

        return snapshot;
    }

    private void validarExecucaoEmProcessamento(
            long conciliacaoId
    ) {
        String statusAtual;

        try {
            statusAtual = jdbcTemplate.queryForObject(
                    """
                    SELECT status
                    FROM public.conciliacoes
                    WHERE id = ?
                    FOR UPDATE
                    """,
                    String.class,
                    conciliacaoId
            );
        } catch (EmptyResultDataAccessException erro) {
            throw new IllegalStateException(
                    "Conciliacao nao encontrada.",
                    erro
            );
        }

        if (!"EM_PROCESSAMENTO".equals(statusAtual)) {
            throw new IllegalStateException(
                    "Snapshot exige conciliacao EM_PROCESSAMENTO."
            );
        }
    }

    private void validarSnapshotAindaNaoExiste(
            long conciliacaoId
    ) {
        Integer quantidade = jdbcTemplate.queryForObject(
                """
                SELECT
                    (
                        SELECT COUNT(*)
                        FROM public.conciliacao_snapshot_cobrancas
                        WHERE conciliacao_id = ?
                    )
                    +
                    (
                        SELECT COUNT(*)
                        FROM public.conciliacao_snapshot_pagamentos
                        WHERE conciliacao_id = ?
                    )
                """,
                Integer.class,
                conciliacaoId,
                conciliacaoId
        );

        if (quantidade == null) {
            throw new IllegalStateException(
                    "Banco nao retornou a quantidade do snapshot."
            );
        }

        if (quantidade > 0) {
            throw new IllegalStateException(
                    "Snapshot da conciliacao ja foi persistido."
            );
        }
    }

    private List<ItemOrigem> carregarCobrancas() {
        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    identificador,
                    valor_esperado
                FROM public.cobrancas
                ORDER BY id
                LIMIT 1001
                """,
                (resultado, numeroLinha) ->
                        new ItemOrigem(
                                resultado.getLong("id"),
                                resultado.getString(
                                        "identificador"
                                ),
                                resultado.getBigDecimal(
                                        "valor_esperado"
                                )
                        )
        );
    }

    private List<ItemOrigem> carregarPagamentos() {
        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    identificador_cobranca,
                    valor_pago
                FROM public.pagamentos
                ORDER BY id
                LIMIT 1001
                """,
                (resultado, numeroLinha) ->
                        new ItemOrigem(
                                resultado.getLong("id"),
                                resultado.getString(
                                        "identificador_cobranca"
                                ),
                                resultado.getBigDecimal(
                                        "valor_pago"
                                )
                        )
        );
    }

    private void validarQuantidade(
            List<ItemOrigem> itens,
            String nomeConjunto
    ) {
        if (itens.isEmpty()) {
            throw new ConflitoDadosException(
                    "Nao existem "
                            + nomeConjunto
                            + " para conciliacao."
            );
        }

        if (itens.size() > LIMITE_ITENS) {
            throw new ConflitoDadosException(
                    "Quantidade de "
                            + nomeConjunto
                            + " excede o limite de "
                            + LIMITE_ITENS
                            + "."
            );
        }
    }

    private SnapshotExecucaoCobol criarSnapshot(
            List<ItemOrigem> cobrancas,
            List<ItemOrigem> pagamentos
    ) {
        List<SnapshotExecucaoCobol.Item>
                cobrancasSnapshot =
                cobrancas.stream()
                        .map(this::converterItem)
                        .toList();

        List<SnapshotExecucaoCobol.Item>
                pagamentosSnapshot =
                pagamentos.stream()
                        .map(this::converterItem)
                        .toList();

        return new SnapshotExecucaoCobol(
                cobrancasSnapshot,
                pagamentosSnapshot
        );
    }

    private SnapshotExecucaoCobol.Item converterItem(
            ItemOrigem item
    ) {
        return new SnapshotExecucaoCobol.Item(
                item.identificador(),
                item.valor()
        );
    }

    private void persistirCobrancas(
            long conciliacaoId,
            List<ItemOrigem> cobrancas
    ) {
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO public.conciliacao_snapshot_cobrancas (
                    conciliacao_id,
                    ordem,
                    origem_id,
                    identificador,
                    valor_esperado
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(
                            PreparedStatement comando,
                            int indice
                    ) throws SQLException {
                        ItemOrigem item =
                                cobrancas.get(indice);

                        comando.setLong(
                                1,
                                conciliacaoId
                        );

                        comando.setInt(
                                2,
                                indice + 1
                        );

                        comando.setLong(
                                3,
                                item.origemId()
                        );

                        comando.setString(
                                4,
                                item.identificador()
                        );

                        comando.setBigDecimal(
                                5,
                                item.valor()
                        );
                    }

                    @Override
                    public int getBatchSize() {
                        return cobrancas.size();
                    }
                }
        );
    }

    private void persistirPagamentos(
            long conciliacaoId,
            List<ItemOrigem> pagamentos
    ) {
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO public.conciliacao_snapshot_pagamentos (
                    conciliacao_id,
                    ordem,
                    origem_id,
                    identificador_cobranca,
                    valor_pago
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(
                            PreparedStatement comando,
                            int indice
                    ) throws SQLException {
                        ItemOrigem item =
                                pagamentos.get(indice);

                        comando.setLong(
                                1,
                                conciliacaoId
                        );

                        comando.setInt(
                                2,
                                indice + 1
                        );

                        comando.setLong(
                                3,
                                item.origemId()
                        );

                        comando.setString(
                                4,
                                item.identificador()
                        );

                        comando.setBigDecimal(
                                5,
                                item.valor()
                        );
                    }

                    @Override
                    public int getBatchSize() {
                        return pagamentos.size();
                    }
                }
        );
    }

    private void validarId(long conciliacaoId) {
        if (conciliacaoId <= 0) {
            throw new IllegalArgumentException(
                    "Identificador da conciliacao deve ser positivo."
            );
        }
    }

    private record ItemOrigem(
            long origemId,
            String identificador,
            BigDecimal valor
    ) {
    }
}
