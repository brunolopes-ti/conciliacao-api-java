package br.com.brunolopes.conciliacao.persistencia;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;

@Repository
@ConditionalOnProperty(
        name = "conciliacao.persistencia.habilitada",
        havingValue = "true",
        matchIfMissing = true
)
public class RepositorioResultadoConciliacaoJdbc
        implements RepositorioResultadoConciliacao {

    private static final int LIMITE_DETALHES = 2000;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public RepositorioResultadoConciliacaoJdbc(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = jdbcTemplate;

        this.transactionTemplate =
                new TransactionTemplate(
                        transactionManager
                );
    }

    @Override
    public void persistirEConcluir(
            long conciliacaoId,
            ResultadoConciliacao resultado
    ) {
        validarEntrada(
                conciliacaoId,
                resultado
        );

        transactionTemplate.executeWithoutResult(
                status ->
                        persistirDentroDaTransacao(
                                conciliacaoId,
                                resultado
                        )
        );
    }

    private void persistirDentroDaTransacao(
            long conciliacaoId,
            ResultadoConciliacao resultado
    ) {
        bloquearEValidarExecucao(
                conciliacaoId
        );

        validarResultadoAindaNaoPersistido(
                conciliacaoId
        );

        persistirResumo(
                conciliacaoId,
                resultado.resumo(),
                resultado.versao()
        );

        persistirDetalhes(
                conciliacaoId,
                resultado.detalhes()
        );

        concluirExecucao(
                conciliacaoId,
                resultado.versao()
        );
    }

    private void bloquearEValidarExecucao(
            long conciliacaoId
    ) {
        String statusAtual;

        try {
            statusAtual =
                    jdbcTemplate.queryForObject(
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

        if (!"EM_PROCESSAMENTO".equals(
                statusAtual
        )) {
            throw new IllegalStateException(
                    "Resultado somente pode ser persistido "
                            + "para conciliacao EM_PROCESSAMENTO."
            );
        }
    }

    private void validarResultadoAindaNaoPersistido(
            long conciliacaoId
    ) {
        Long quantidadeResumo =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_resumos
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
                );

        Long quantidadeDetalhes =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_detalhes
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
                );

        if (quantidadeResumo == null
                || quantidadeDetalhes == null) {

            throw new IllegalStateException(
                    "Banco nao retornou a quantidade "
                            + "dos resultados persistidos."
            );
        }

        if (quantidadeResumo > 0
                || quantidadeDetalhes > 0) {

            throw new IllegalStateException(
                    "Resultado da conciliacao "
                            + "ja foi persistido."
            );
        }
    }

    private void persistirResumo(
            long conciliacaoId,
            ResumoConciliacao resumo,
            int versao
    ) {
        int linhas =
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
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        conciliacaoId,
                        versao,
                        resumo.conferidos(),
                        resumo.acima(),
                        resumo.abaixo(),
                        resumo.duplicados(),
                        resumo.semRecebimento(),
                        resumo.semPrevisao(),
                        resumo.totalEsperado(),
                        resumo.totalRecebido(),
                        resumo.saldoGlobal()
                );

        exigirUmaLinha(
                linhas,
                "Nao foi possivel persistir "
                        + "o resumo da conciliacao."
        );
    }

    private void persistirDetalhes(
            long conciliacaoId,
            List<DetalheConciliacao> detalhes
    ) {
        jdbcTemplate.batchUpdate(
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
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(
                            PreparedStatement comando,
                            int indice
                    ) throws SQLException {

                        DetalheConciliacao detalhe =
                                detalhes.get(
                                        indice
                                );

                        comando.setLong(
                                1,
                                conciliacaoId
                        );

                        comando.setInt(
                                2,
                                indice + 1
                        );

                        comando.setString(
                                3,
                                detalhe.identificador()
                        );

                        definirDecimal(
                                comando,
                                4,
                                detalhe.valorEsperado()
                        );

                        definirDecimal(
                                comando,
                                5,
                                detalhe.valorRecebido()
                        );

                        definirDecimal(
                                comando,
                                6,
                                detalhe.diferenca()
                        );

                        comando.setString(
                                7,
                                detalhe.status()
                                        .name()
                        );

                        comando.setInt(
                                8,
                                detalhe
                                        .quantidadeRecebimentos()
                        );
                    }

                    @Override
                    public int getBatchSize() {
                        return detalhes.size();
                    }
                }
        );
    }

    private void concluirExecucao(
            long conciliacaoId,
            int versao
    ) {
        int linhasAlteradas =
                jdbcTemplate.update(
                        """
                        UPDATE public.conciliacoes
                        SET
                            status = 'CONCLUIDA',
                            finalizada_em = GREATEST(
                                clock_timestamp(),
                                iniciada_em
                            ),
                            resultado_versao = ?
                        WHERE id = ?
                          AND status = 'EM_PROCESSAMENTO'
                        """,
                        versao,
                        conciliacaoId
                );

        exigirUmaLinha(
                linhasAlteradas,
                "Nao foi possivel concluir "
                        + "a conciliacao."
        );
    }

    private void definirDecimal(
            PreparedStatement comando,
            int indice,
            java.math.BigDecimal valor
    ) throws SQLException {

        if (valor == null) {
            comando.setNull(
                    indice,
                    Types.NUMERIC
            );

            return;
        }

        comando.setBigDecimal(
                indice,
                valor
        );
    }

    private void validarEntrada(
            long conciliacaoId,
            ResultadoConciliacao resultado
    ) {
        if (conciliacaoId <= 0) {
            throw new IllegalArgumentException(
                    "Identificador da conciliacao "
                            + "deve ser positivo."
            );
        }

        if (resultado == null) {
            throw new IllegalArgumentException(
                    "Resultado da conciliacao "
                            + "e obrigatorio."
            );
        }

        if (resultado.versao() != 1) {
            throw new IllegalArgumentException(
                    "Versao de resultado "
                            + "nao suportada."
            );
        }

        if (resultado.resumo() == null) {
            throw new IllegalArgumentException(
                    "Resumo da conciliacao "
                            + "e obrigatorio."
            );
        }

        if (resultado.detalhes() == null
                || resultado.detalhes().isEmpty()
                || resultado.detalhes().size()
                        > LIMITE_DETALHES) {

            throw new IllegalArgumentException(
                    "Resultado deve possuir entre "
                            + "1 e "
                            + LIMITE_DETALHES
                            + " detalhes."
            );
        }
    }

    private void exigirUmaLinha(
            int linhasAlteradas,
            String mensagem
    ) {
        if (linhasAlteradas != 1) {
            throw new IllegalStateException(
                    mensagem
            );
        }
    }
}
