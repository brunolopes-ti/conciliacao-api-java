package br.com.brunolopes.conciliacao.integracao;

import java.util.concurrent.Semaphore;

import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;

public final class ProcessadorConciliacaoCobolLimitado
        implements ProcessadorConciliacaoCobol {

    private final ProcessadorConciliacaoCobol delegado;
    private final Semaphore limite;

    public ProcessadorConciliacaoCobolLimitado(
            ProcessadorConciliacaoCobol delegado,
            int maximoSimultaneo
    ) {
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
            limite.acquire();

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
