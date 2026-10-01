package br.com.brunolopes.conciliacao.integracao;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import br.com.brunolopes.conciliacao.aplicacao.excecao.CapacidadeEsgotadaException;

import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;

public final class ProcessadorConciliacaoCobolLimitado
        implements ProcessadorConciliacaoCobol {

    private final ProcessadorConciliacaoCobol delegado;
    private final Semaphore limite;
    private final long esperaMaximaMs;

    public ProcessadorConciliacaoCobolLimitado(
            ProcessadorConciliacaoCobol delegado,
            int maximoSimultaneo
    ) {
        this(delegado, maximoSimultaneo, 1000);
    }

    public ProcessadorConciliacaoCobolLimitado(
            ProcessadorConciliacaoCobol delegado,
            int maximoSimultaneo,
            long esperaMaximaMs
    ) {
        if (esperaMaximaMs < 0) {
            throw new IllegalArgumentException("Espera maxima nao pode ser negativa.");
        }
        this.esperaMaximaMs = esperaMaximaMs;
        if (delegado == null) {
            throw new IllegalArgumentException(
                    "Processador COBOL delegado e obrigatorio."
            );
        }

        if (maximoSimultaneo <= 0) {
            throw new IllegalArgumentException(
                    "Limite de concorrencia deve ser maior que zero."
            );
        }

        this.delegado =
                delegado;

        this.limite =
                new Semaphore(
                        maximoSimultaneo,
                        true
                );
    }

    @Override
    public ResultadoConciliacao processar(
            SnapshotExecucaoCobol snapshot
    ) {
        boolean adquiriuPermissao =
                false;

        try {
            if (!limite.tryAcquire(esperaMaximaMs, TimeUnit.MILLISECONDS)) {
                throw new CapacidadeEsgotadaException();
            }

            adquiriuPermissao =
                    true;

            return delegado.processar(
                    snapshot
            );

        } catch (InterruptedException erro) {
            Thread.currentThread()
                    .interrupt();

            throw new IllegalStateException(
                    "Espera por vaga de execucao "
                            + "COBOL foi interrompida.",
                    erro
            );

        } finally {
            if (adquiriuPermissao) {
                limite.release();
            }
        }
    }
}
