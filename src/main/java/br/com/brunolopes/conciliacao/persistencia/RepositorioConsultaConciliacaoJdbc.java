package br.com.brunolopes.conciliacao.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;
import br.com.brunolopes.conciliacao.modelo.consulta.ConciliacaoConsultada;
import br.com.brunolopes.conciliacao.modelo.consulta.DetalheConciliacaoConsultado;
import br.com.brunolopes.conciliacao.modelo.consulta.ItemHistoricoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;

@Repository
@ConditionalOnProperty(
        name = "conciliacao.persistencia.habilitada",
        havingValue = "true",
        matchIfMissing = true
)
public class RepositorioConsultaConciliacaoJdbc
        implements RepositorioConsultaConciliacao {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public RepositorioConsultaConciliacaoJdbc(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = Objects.requireNonNull(
                jdbcTemplate,
                "JdbcTemplate e obrigatorio."
        );

        this.transactionTemplate =
                new TransactionTemplate(
                        Objects.requireNonNull(
                                transactionManager,
                                "TransactionManager e obrigatorio."
                        )
                );

        this.transactionTemplate.setReadOnly(true);
        this.transactionTemplate.setIsolationLevel(
                TransactionDefinition.ISOLATION_REPEATABLE_READ
        );
        this.transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }

    /* Construtor de apoio para testes diretos existentes. */
    public RepositorioConsultaConciliacaoJdbc(
            JdbcTemplate jdbcTemplate
    ) {
        this(
                jdbcTemplate,
                new DataSourceTransactionManager(
                        Objects.requireNonNull(
                                jdbcTemplate.getDataSource(),
                                "DataSource e obrigatorio."
                        )
                )
        );
    }

    @Override
    public List<ItemHistoricoConciliacao> listar(
            int limite,
            long deslocamento,
            StatusExecucaoConciliacao status
    ) {
        validarPaginacao(
                limite,
                deslocamento
        );

        if (status == null) {
            return jdbcTemplate.query(
                    """
                    SELECT
                        c.id,
                        c.status,
                        c.criada_em,
                        c.iniciada_em,
                        c.finalizada_em,
                        c.resultado_versao,
                        c.erro_codigo,
                        r.conciliacao_id AS resumo_id,
                        r.conferidos,
                        r.acima,
                        r.abaixo,
                        r.duplicados,
                        r.sem_recebimento,
                        r.sem_previsao,
                        r.total_esperado,
                        r.total_recebido,
                        r.saldo_global
                    FROM public.conciliacoes c
                    LEFT JOIN public.conciliacao_resumos r
                        ON r.conciliacao_id = c.id
                    ORDER BY
                        c.criada_em DESC,
                        c.id DESC
                    LIMIT ?
                    OFFSET ?
                    """,
                    this::mapearItemHistorico,
                    limite,
                    deslocamento
            );
        }

        return jdbcTemplate.query(
                """
                SELECT
                    c.id,
                    c.status,
                    c.criada_em,
                    c.iniciada_em,
                    c.finalizada_em,
                    c.resultado_versao,
                    c.erro_codigo,
                    r.conciliacao_id AS resumo_id,
                    r.conferidos,
                    r.acima,
                    r.abaixo,
                    r.duplicados,
                    r.sem_recebimento,
                    r.sem_previsao,
                    r.total_esperado,
                    r.total_recebido,
                    r.saldo_global
                FROM public.conciliacoes c
                LEFT JOIN public.conciliacao_resumos r
                    ON r.conciliacao_id = c.id
                WHERE c.status = ?
                ORDER BY
                    c.criada_em DESC,
                    c.id DESC
                LIMIT ?
                OFFSET ?
                """,
                this::mapearItemHistorico,
                status.name(),
                limite,
                deslocamento
        );
    }

    @Override
    public long contar(
            StatusExecucaoConciliacao status
    ) {
        Long quantidade;

        if (status == null) {
            quantidade = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM public.conciliacoes
                    """,
                    Long.class
            );

        } else {
            quantidade = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM public.conciliacoes
                    WHERE status = ?
                    """,
                    Long.class,
                    status.name()
            );
        }

        if (quantidade == null) {
            throw new IllegalStateException(
                    "Banco nao retornou a quantidade de conciliacoes."
            );
        }

        return quantidade;
    }

    @Override
    public Optional<ConciliacaoConsultada> buscarPorId(
            long conciliacaoId
    ) {
        validarId(
                conciliacaoId
        );

        Optional<ConciliacaoConsultada> resultado =
                transactionTemplate.execute(
                        status -> buscarPorIdConsistente(
                                conciliacaoId
                        )
                );

        if (resultado == null) {
            throw new IllegalStateException(
                    "Transacao de leitura nao retornou resultado."
            );
        }

        return resultado;
    }

    private Optional<ConciliacaoConsultada> buscarPorIdConsistente(
            long conciliacaoId
    ) {
        Optional<CabecalhoConsulta> cabecalho =
                buscarCabecalho(
                        conciliacaoId
                );

        if (cabecalho.isEmpty()) {
            return Optional.empty();
        }

        List<DetalheConciliacaoConsultado> detalhes =
                buscarDetalhes(
                        conciliacaoId
                );

        CabecalhoConsulta dados =
                cabecalho.get();

        validarConsistencia(
                dados,
                detalhes
        );

        return Optional.of(
                new ConciliacaoConsultada(
                        dados.id(),
                        dados.status(),
                        dados.criadaEm(),
                        dados.iniciadaEm(),
                        dados.finalizadaEm(),
                        dados.resultadoVersao(),
                        dados.erroCodigo(),
                        dados.resumo(),
                        detalhes
                )
        );
    }

    private Optional<CabecalhoConsulta> buscarCabecalho(
            long conciliacaoId
    ) {
        List<CabecalhoConsulta> resultados =
                jdbcTemplate.query(
                        """
                        SELECT
                            c.id,
                            c.status,
                            c.criada_em,
                            c.iniciada_em,
                            c.finalizada_em,
                            c.resultado_versao,
                            c.erro_codigo,
                            r.conciliacao_id AS resumo_id,
                            r.conferidos,
                            r.acima,
                            r.abaixo,
                            r.duplicados,
                            r.sem_recebimento,
                            r.sem_previsao,
                            r.total_esperado,
                            r.total_recebido,
                            r.saldo_global
                        FROM public.conciliacoes c
                        LEFT JOIN public.conciliacao_resumos r
                            ON r.conciliacao_id = c.id
                        WHERE c.id = ?
                        """,
                        this::mapearCabecalho,
                        conciliacaoId
                );

        if (resultados.isEmpty()) {
            return Optional.empty();
        }

        if (resultados.size() != 1) {
            throw new IllegalStateException(
                    "Consulta retornou mais de uma conciliacao para o mesmo ID."
            );
        }

        return Optional.of(
                resultados.get(0)
        );
    }

    private List<DetalheConciliacaoConsultado> buscarDetalhes(
            long conciliacaoId
    ) {
        return jdbcTemplate.query(
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
                ORDER BY ordem ASC
                """,
                this::mapearDetalhe,
                conciliacaoId
        );
    }

    private ItemHistoricoConciliacao mapearItemHistorico(
            ResultSet resultado,
            int numeroLinha
    ) throws SQLException {

        StatusExecucaoConciliacao status =
                lerStatusExecucao(
                        resultado
                                .getString("status")
                );

        Integer resultadoVersao =
                resultado.getObject(
                        "resultado_versao",
                        Integer.class
                );

        ResumoConciliacao resumo =
                mapearResumoSeExistir(
                        resultado
                );

        validarResumoComStatus(
                status,
                resultadoVersao,
                resumo
        );

        return new ItemHistoricoConciliacao(
                resultado.getLong("id"),
                status,
                lerData(
                        resultado,
                        "criada_em"
                ),
                lerData(
                        resultado,
                        "iniciada_em"
                ),
                lerData(
                        resultado,
                        "finalizada_em"
                ),
                resultadoVersao,
                resultado.getString("erro_codigo"),
                resumo
        );
    }

    private CabecalhoConsulta mapearCabecalho(
            ResultSet resultado,
            int numeroLinha
    ) throws SQLException {

        StatusExecucaoConciliacao status =
                lerStatusExecucao(
                        resultado
                                .getString("status")
                );

        Integer resultadoVersao =
                resultado.getObject(
                        "resultado_versao",
                        Integer.class
                );

        ResumoConciliacao resumo =
                mapearResumoSeExistir(
                        resultado
                );

        validarResumoComStatus(
                status,
                resultadoVersao,
                resumo
        );

        return new CabecalhoConsulta(
                resultado.getLong("id"),
                status,
                lerData(
                        resultado,
                        "criada_em"
                ),
                lerData(
                        resultado,
                        "iniciada_em"
                ),
                lerData(
                        resultado,
                        "finalizada_em"
                ),
                resultadoVersao,
                resultado.getString("erro_codigo"),
                resumo
        );
    }

    private DetalheConciliacaoConsultado mapearDetalhe(
            ResultSet resultado,
            int numeroLinha
    ) throws SQLException {

        StatusConciliacao status;

        try {
            status = StatusConciliacao.valueOf(
                    resultado.getString("status")
            );

        } catch (IllegalArgumentException erro) {
            throw new IllegalStateException(
                    "Banco retornou status de detalhe desconhecido.",
                    erro
            );
        }

        return new DetalheConciliacaoConsultado(
                resultado.getInt("ordem"),
                resultado.getString("identificador"),
                resultado.getBigDecimal("valor_esperado"),
                resultado.getBigDecimal("valor_recebido"),
                resultado.getBigDecimal("diferenca"),
                status,
                resultado.getInt(
                        "quantidade_recebimentos"
                )
        );
    }

    private ResumoConciliacao mapearResumoSeExistir(
            ResultSet resultado
    ) throws SQLException {

        Long resumoId =
                resultado.getObject(
                        "resumo_id",
                        Long.class
                );

        if (resumoId == null) {
            return null;
        }

        return new ResumoConciliacao(
                resultado.getInt("conferidos"),
                resultado.getInt("acima"),
                resultado.getInt("abaixo"),
                resultado.getInt("duplicados"),
                resultado.getInt("sem_recebimento"),
                resultado.getInt("sem_previsao"),
                resultado.getBigDecimal(
                        "total_esperado"
                ),
                resultado.getBigDecimal(
                        "total_recebido"
                ),
                resultado.getBigDecimal(
                        "saldo_global"
                )
        );
    }

    private StatusExecucaoConciliacao lerStatusExecucao(
            String valor
    ) {
        try {
            return StatusExecucaoConciliacao.valueOf(
                    valor
            );

        } catch (IllegalArgumentException
                | NullPointerException erro) {

            throw new IllegalStateException(
                    "Banco retornou status de execucao desconhecido.",
                    erro
            );
        }
    }

    private OffsetDateTime lerData(
            ResultSet resultado,
            String coluna
    ) throws SQLException {

        return resultado.getObject(
                coluna,
                OffsetDateTime.class
        );
    }

    private void validarResumoComStatus(
            StatusExecucaoConciliacao status,
            Integer resultadoVersao,
            ResumoConciliacao resumo
    ) {
        if (status == StatusExecucaoConciliacao.CONCLUIDA) {

            if (resultadoVersao == null
                    || resultadoVersao != 1
                    || resumo == null) {

                throw new IllegalStateException(
                        "Conciliacao concluida possui resultado inconsistente."
                );
            }

            return;
        }

        if (resultadoVersao != null
                || resumo != null) {

            throw new IllegalStateException(
                    "Conciliacao nao concluida possui resultado persistido."
            );
        }
    }

    private void validarConsistencia(
            CabecalhoConsulta cabecalho,
            List<DetalheConciliacaoConsultado> detalhes
    ) {
        if (cabecalho.status()
                == StatusExecucaoConciliacao.CONCLUIDA) {

            if (detalhes.isEmpty()) {
                throw new IllegalStateException(
                        "Conciliacao concluida nao possui detalhes."
                );
            }

            return;
        }

        if (!detalhes.isEmpty()) {
            throw new IllegalStateException(
                    "Conciliacao nao concluida possui detalhes persistidos."
            );
        }
    }

    private void validarPaginacao(
            int limite,
            long deslocamento
    ) {
        if (limite < 1
                || limite > TAMANHO_MAXIMO_PAGINA) {

            throw new IllegalArgumentException(
                    "Limite deve estar entre 1 e "
                            + TAMANHO_MAXIMO_PAGINA
                            + "."
            );
        }

        if (deslocamento < 0) {
            throw new IllegalArgumentException(
                    "Deslocamento nao pode ser negativo."
            );
        }
    }

    private void validarId(
            long conciliacaoId
    ) {
        if (conciliacaoId <= 0) {
            throw new IllegalArgumentException(
                    "Identificador da conciliacao deve ser positivo."
            );
        }
    }

    private record CabecalhoConsulta(
            long id,
            StatusExecucaoConciliacao status,
            OffsetDateTime criadaEm,
            OffsetDateTime iniciadaEm,
            OffsetDateTime finalizadaEm,
            Integer resultadoVersao,
            String erroCodigo,
            ResumoConciliacao resumo
    ) {
    }
}
