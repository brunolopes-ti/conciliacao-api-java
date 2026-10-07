package br.com.brunolopes.conciliacao.aplicacao;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import br.com.brunolopes.conciliacao.aplicacao.excecao.CapacidadeEsgotadaException;

/**
 * Controla a admissao da conciliacao antes de qualquer gravacao de snapshot.
 *
 * O limite e local a JVM. Coordenacao entre varias instancias continua fora
 * do escopo desta versao.
 */
public final class ControleCapacidadeConciliacao {

    private final Semaphore limite;
    private final long esperaMaximaMs;
    private final boolean semLimite;

    public ControleCapacidadeConciliacao(
            int maximoSimultaneo,
            long esperaMaximaMs
    ) {
        if (maximoSimultaneo <= 0) {
            throw new IllegalArgumentException(
                    "Limite de concorrencia deve ser maior que zero."
            );
        }

        if (esperaMaximaMs < 0) {
            throw new IllegalArgumentException(
                    "Espera maxima nao pode ser negativa."
            );
        }

        this.limite = new Semaphore(maximoSimultaneo, true);
        this.esperaMaximaMs = esperaMaximaMs;
        this.semLimite = false;
    }

    private ControleCapacidadeConciliacao() {
        this.limite = null;
        this.esperaMaximaMs = 0;
        this.semLimite = true;
    }

    /**
     * Mantem compatibilidade com testes unitarios que instanciam o servico
     * diretamente e nao precisam exercitar concorrencia.
     */
    public static ControleCapacidadeConciliacao semLimite() {
        return new ControleCapacidadeConciliacao();
    }

    public Permissao adquirir() {
        if (semLimite) {
            return () -> { };
        }

        try {
            if (!limite.tryAcquire(
                    esperaMaximaMs,
                    TimeUnit.MILLISECONDS
            )) {
                throw new CapacidadeEsgotadaException();
            }

            return new PermissaoSemaforo(limite);

        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Espera por vaga de conciliacao foi interrompida.",
                    erro
            );
        }
    }

    public interface Permissao extends AutoCloseable {
        @Override
        void close();
    }

    private static final class PermissaoSemaforo
            implements Permissao {

        private final Semaphore limite;
        private final AtomicBoolean liberada = new AtomicBoolean(false);

        private PermissaoSemaforo(
                Semaphore limite
        ) {
            this.limite = limite;
        }

        @Override
        public void close() {
            if (liberada.compareAndSet(false, true)) {
                limite.release();
            }
        }
    }
}
