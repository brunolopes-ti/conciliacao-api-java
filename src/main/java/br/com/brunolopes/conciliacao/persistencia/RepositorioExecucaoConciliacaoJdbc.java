package br.com.brunolopes.conciliacao.persistencia;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
        name = "conciliacao.persistencia.habilitada",
        havingValue = "true",
        matchIfMissing = true
)
public class RepositorioExecucaoConciliacaoJdbc
        implements RepositorioExecucaoConciliacao {

    private static final int LIMITE_DETALHE_ERRO = 4000;

    private final JdbcTemplate jdbcTemplate;

    public RepositorioExecucaoConciliacaoJdbc(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long criar() {
        Long id = jdbcTemplate.queryForObject(
                """
                INSERT INTO public.conciliacoes (
                    status
                )
                VALUES (
                    'CRIADA'
                )
                RETURNING id
                """,
                Long.class
        );

        return exigirId(
                id
        );
    }

    @Override
    public void iniciar(long conciliacaoId) {
        validarId(
                conciliacaoId
        );

        int linhasAlteradas = jdbcTemplate.update(
                """
                UPDATE public.conciliacoes
                SET
                    status = 'EM_PROCESSAMENTO',
                    iniciada_em = clock_timestamp()
                WHERE id = ?
                  AND status = 'CRIADA'
                """,
                conciliacaoId
        );

        exigirUmaLinha(
                linhasAlteradas,
                "Nao foi possivel iniciar a conciliacao."
        );
    }

    @Override
    public long criarEmProcessamento() {
        Long id = jdbcTemplate.queryForObject(
                """
                INSERT INTO public.conciliacoes (
                    status,
                    iniciada_em
                )
                VALUES (
                    'EM_PROCESSAMENTO',
                    clock_timestamp()
                )
                RETURNING id
                """,
                Long.class
        );

        return exigirId(
                id
        );
    }

    @Override
    public void falhar(
            long conciliacaoId,
            String codigoErro,
            String detalheErro
    ) {
        validarId(
                conciliacaoId
        );

        String codigoNormalizado =
                validarCodigoErro(
                        codigoErro
                );

        String detalheNormalizado =
                normalizarDetalheErro(
                        detalheErro
                );

        int linhasAlteradas = jdbcTemplate.update(
                """
                UPDATE public.conciliacoes
                SET
                    status = 'FALHOU',
                    finalizada_em = GREATEST(
                        clock_timestamp(),
                        iniciada_em
                    ),
                    erro_codigo = ?,
                    erro_detalhe = ?
                WHERE id = ?
                  AND status = 'EM_PROCESSAMENTO'
                """,
                codigoNormalizado,
                detalheNormalizado,
                conciliacaoId
        );

        exigirUmaLinha(
                linhasAlteradas,
                "Nao foi possivel registrar a falha da conciliacao."
        );
    }

    @Override
    public int falharAbandonadas(
            long idadeMinimaMs
    ) {
        if (idadeMinimaMs <= 0) {
            throw new IllegalArgumentException(
                    "Idade minima deve ser maior que zero."
            );
        }

        return jdbcTemplate.update(
                """
                UPDATE public.conciliacoes
                SET
                    status = 'FALHOU',
                    finalizada_em = GREATEST(
                        clock_timestamp(),
                        iniciada_em
                    ),
                    erro_codigo = 'EXECUCAO_INTERROMPIDA',
                    erro_detalhe =
                        'Execucao abandonada recuperada automaticamente.'
                WHERE status = 'EM_PROCESSAMENTO'
                  AND iniciada_em <
                      clock_timestamp()
                      - (? * interval '1 millisecond')
                """,
                idadeMinimaMs
        );
    }

    private long exigirId(
            Long id
    ) {
        if (id == null) {
            throw new IllegalStateException(
                    "Banco nao retornou o identificador da conciliacao."
            );
        }

        return id;
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

    private String validarCodigoErro(
            String codigoErro
    ) {
        if (codigoErro == null
                || codigoErro.isBlank()
                || !codigoErro.equals(
                        codigoErro.strip()
                )
                || codigoErro.length() > 50) {

            throw new IllegalArgumentException(
                    "Codigo de erro invalido."
            );
        }

        return codigoErro;
    }

    private String normalizarDetalheErro(
            String detalheErro
    ) {
        if (detalheErro == null) {
            return null;
        }

        if (detalheErro.length()
                <= LIMITE_DETALHE_ERRO) {

            return detalheErro;
        }

        return detalheErro.substring(
                0,
                LIMITE_DETALHE_ERRO
        );
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
