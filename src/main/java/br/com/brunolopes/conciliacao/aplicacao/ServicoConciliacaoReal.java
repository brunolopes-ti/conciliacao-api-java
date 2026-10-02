package br.com.brunolopes.conciliacao.aplicacao;

import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.brunolopes.conciliacao.aplicacao.excecao.CapacidadeEsgotadaException;
import br.com.brunolopes.conciliacao.integracao.FalhaExecucaoCobolException;

import br.com.brunolopes.conciliacao.aplicacao.excecao.ConflitoDadosException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.FalhaCobolException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.ResultadoInvalidoException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.TimeoutCobolException;
import br.com.brunolopes.conciliacao.integracao.ProcessadorConciliacaoCobol;
import br.com.brunolopes.conciliacao.integracao.SnapshotExecucaoCobol;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioExecucaoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioResultadoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioSnapshotConciliacao;

public final class ServicoConciliacaoReal
        implements ServicoConciliacao {

    private static final Logger LOG = LoggerFactory.getLogger(ServicoConciliacaoReal.class);

    private final RepositorioExecucaoConciliacao
            repositorioExecucao;

    private final RepositorioSnapshotConciliacao
            repositorioSnapshot;

    private final RepositorioResultadoConciliacao
            repositorioResultado;

    private final ProcessadorConciliacaoCobol
            processadorCobol;

    public ServicoConciliacaoReal(
            RepositorioExecucaoConciliacao repositorioExecucao,
            RepositorioSnapshotConciliacao repositorioSnapshot,
            RepositorioResultadoConciliacao repositorioResultado,
            ProcessadorConciliacaoCobol processadorCobol
    ) {
        if (repositorioExecucao == null
                || repositorioSnapshot == null
                || repositorioResultado == null
                || processadorCobol == null) {

            throw new IllegalArgumentException(
                    "Dependencias do servico "
                            + "de conciliacao sao obrigatorias."
            );
        }

        this.repositorioExecucao =
                repositorioExecucao;

        this.repositorioSnapshot =
                repositorioSnapshot;

        this.repositorioResultado =
                repositorioResultado;

        this.processadorCobol =
                processadorCobol;
    }

    @Override
    public ResultadoServicoConciliacao executar() {
        long conciliacaoId =
                repositorioExecucao
                        .criarEmProcessamento();

        try {
            SnapshotExecucaoCobol snapshot =
                    repositorioSnapshot
                            .capturarEPersistir(
                                    conciliacaoId
                            );

            /*
             * A transacao do snapshot ja terminou.
             * O COBOL roda sem transacao JDBC aberta.
             */
            ResultadoConciliacao resultado =
                    processadorCobol.processar(
                            snapshot
                    );

            /*
             * Resumo, detalhes e mudanca para
             * CONCLUIDA acontecem na mesma transacao.
             */
            repositorioResultado.persistirEConcluir(
                    conciliacaoId,
                    resultado
            );

            return new ResultadoServicoConciliacao(
                    conciliacaoId,
                    StatusExecucaoConciliacao.CONCLUIDA,
                    resultado
            );

        } catch (RuntimeException erro) {
            registrarFalhaSemMascarar(
                    conciliacaoId,
                    erro
            );

            // Nao registra stdout nem dados financeiros no log geral.
            // Diagnostico limitado fica no registro interno da execucao.
            LOG.error("Conciliacao {} falhou: {}; falhas adicionais: {}",
                    conciliacaoId, identificarCodigoErro(erro), erro.getSuppressed().length);
            throw erro;
        }
    }

    private void registrarFalhaSemMascarar(
            long conciliacaoId,
            RuntimeException erroOriginal
    ) {
        try {
            repositorioExecucao.falhar(
                    conciliacaoId,
                    identificarCodigoErro(
                            erroOriginal
                    ),
                    montarDetalheErro(
                            erroOriginal
                    )
            );

        } catch (RuntimeException erroAoRegistrarFalha) {
            erroOriginal.addSuppressed(
                    erroAoRegistrarFalha
            );
        }
    }

    private String identificarCodigoErro(
            RuntimeException erro
    ) {
        if (erro instanceof CapacidadeEsgotadaException) {
            return "CAPACIDADE_ESGOTADA";
        }
        if (erro instanceof ConflitoDadosException) {
            return "CONFLITO_DADOS";
        }

        if (erro instanceof TimeoutCobolException) {
            return "TIMEOUT_COBOL";
        }

        if (erro instanceof FalhaCobolException) {
            return "FALHA_COBOL";
        }

        if (erro instanceof ResultadoInvalidoException) {
            return "RESULTADO_INVALIDO";
        }

        if (possuiCausaDePersistencia(
                erro
        )) {
            return "ERRO_PERSISTENCIA";
        }

        return "ERRO_INTERNO";
    }

    private boolean possuiCausaDePersistencia(
            Throwable erro
    ) {
        Throwable atual =
                erro;

        while (atual != null) {
            if (atual instanceof DataAccessException) {
                return true;
            }

            Throwable causa =
                    atual.getCause();

            if (causa == atual) {
                break;
            }

            atual =
                    causa;
        }

        return false;
    }

    private String montarDetalheErro(RuntimeException erro) {
        StringBuilder detalhe = new StringBuilder();
        var vistos = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<Throwable, Boolean>());
        Throwable atual = erro;
        while (atual != null && vistos.add(atual) && detalhe.length() < 4000) {
            if (!detalhe.isEmpty()) detalhe.append(" | ");
            detalhe.append(atual.getClass().getSimpleName());
            if (atual.getMessage() != null) detalhe.append(": ").append(atual.getMessage());
            if (atual instanceof FalhaExecucaoCobolException falha
                    && !falha.diagnostico().isBlank()) {
                detalhe.append("; motor: ").append(falha.diagnostico());
            }
            atual = atual.getCause();
        }
        String texto = detalhe.toString();
        int fim = Math.min(4000, texto.length());
        if (fim > 0 && Character.isHighSurrogate(texto.charAt(fim - 1))) fim--;
        return texto.substring(0, fim);
    }
}
