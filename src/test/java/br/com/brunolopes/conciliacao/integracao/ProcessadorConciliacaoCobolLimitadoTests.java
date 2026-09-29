package br.com.brunolopes.conciliacao.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

class ProcessadorConciliacaoCobolLimitadoTests {

    @Test
    void deveLimitarExecucaoSimultanea()
            throws Exception {

        CountDownLatch primeiraEntrou =
                new CountDownLatch(1);

        CountDownLatch liberarPrimeira =
                new CountDownLatch(1);

        AtomicInteger emExecucao =
                new AtomicInteger();

        AtomicInteger maximoObservado =
                new AtomicInteger();

        ProcessadorConciliacaoCobol delegado =
                snapshot -> {
                    int atual =
                            emExecucao.incrementAndGet();

                    maximoObservado.updateAndGet(
                            anterior ->
                                    Math.max(
                                            anterior,
                                            atual
                                    )
                    );

                    try {
                        primeiraEntrou.countDown();

                        liberarPrimeira.await(
                                5,
                                TimeUnit.SECONDS
                        );

                        return resultadoExemplo();

                    } catch (InterruptedException erro) {
                        Thread.currentThread()
                                .interrupt();

                        throw new IllegalStateException(
                                "Teste interrompido.",
                                erro
                        );

                    } finally {
                        emExecucao.decrementAndGet();
                    }
                };

        ProcessadorConciliacaoCobolLimitado processador =
                new ProcessadorConciliacaoCobolLimitado(
                        delegado,
                        1
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        2
                );

        try {
            Future<ResultadoConciliacao> primeira =
                    executor.submit(
                            () ->
                                    processador.processar(
                                            snapshotExemplo()
                                    )
                    );

            assertTrue(
                    primeiraEntrou.await(
                            2,
                            TimeUnit.SECONDS
                    )
            );

            Future<ResultadoConciliacao> segunda =
                    executor.submit(
                            () ->
                                    processador.processar(
                                            snapshotExemplo()
                                    )
                    );

            Thread.sleep(
                    200
            );

            assertFalse(
                    segunda.isDone()
            );

            assertEquals(
                    1,
                    maximoObservado.get()
            );

            liberarPrimeira.countDown();

            primeira.get(
                    5,
                    TimeUnit.SECONDS
            );

            segunda.get(
                    5,
                    TimeUnit.SECONDS
            );

            assertEquals(
                    1,
                    maximoObservado.get()
            );

        } finally {
            liberarPrimeira.countDown();
            executor.shutdownNow();
        }
    }

    private SnapshotExecucaoCobol snapshotExemplo() {
        return new SnapshotExecucaoCobol(
                List.of(
                        new SnapshotExecucaoCobol.Item(
                                "COB001",
                                new BigDecimal(
                                        "100.00"
                                )
                        )
                ),
                List.of(
                        new SnapshotExecucaoCobol.Item(
                                "COB001",
                                new BigDecimal(
                                        "100.00"
                                )
                        )
                )
        );
    }

    private ResultadoConciliacao resultadoExemplo() {
        return new ResultadoConciliacao(
                1,
                List.of(
                        new DetalheConciliacao(
                                "COB001",
                                new BigDecimal(
                                        "100.00"
                                ),
                                new BigDecimal(
                                        "100.00"
                                ),
                                new BigDecimal(
                                        "0.00"
                                ),
                                StatusConciliacao.CONFERIDO,
                                1
                        )
                ),
                new ResumoConciliacao(
                        1,
                        0,
                        0,
                        0,
                        0,
                        0,
                        new BigDecimal(
                                "100.00"
                        ),
                        new BigDecimal(
                                "100.00"
                        ),
                        new BigDecimal(
                                "0.00"
                        )
                )
        );
    }
}
