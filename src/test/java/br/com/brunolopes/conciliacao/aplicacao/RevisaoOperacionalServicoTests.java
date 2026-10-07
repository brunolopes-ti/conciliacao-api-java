package br.com.brunolopes.conciliacao.aplicacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import br.com.brunolopes.conciliacao.aplicacao.excecao.CapacidadeEsgotadaException;
import br.com.brunolopes.conciliacao.integracao.SnapshotExecucaoCobol;
import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioExecucaoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioResultadoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioSnapshotConciliacao;

class RevisaoOperacionalServicoTests {

    @Test
    void deveRecusarAntesDeCriarExecucaoQuandoCapacidadeEstaOcupada()
            throws Exception {

        ControleCapacidadeConciliacao controle =
                new ControleCapacidadeConciliacao(1, 0);

        AtomicInteger criadas = new AtomicInteger();
        CountDownLatch entrouCobol = new CountDownLatch(1);
        CountDownLatch liberarCobol = new CountDownLatch(1);

        RepositorioExecucaoConciliacao execucoes =
                new RepositorioExecucaoConciliacao() {
                    @Override
                    public long criar() {
                        return criarEmProcessamento();
                    }

                    @Override
                    public void iniciar(long conciliacaoId) { }

                    @Override
                    public long criarEmProcessamento() {
                        return criadas.incrementAndGet();
                    }

                    @Override
                    public void falhar(
                            long conciliacaoId,
                            String codigoErro,
                            String detalheErro
                    ) { }
                };

        RepositorioSnapshotConciliacao snapshots =
                conciliacaoId -> snapshotExemplo();

        RepositorioResultadoConciliacao resultados =
                (conciliacaoId, resultado) -> { };

        ServicoConciliacaoReal servico =
                new ServicoConciliacaoReal(
                        execucoes,
                        snapshots,
                        resultados,
                        snapshot -> {
                            entrouCobol.countDown();
                            try {
                                liberarCobol.await(5, TimeUnit.SECONDS);
                            } catch (InterruptedException erro) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(erro);
                            }
                            return resultadoExemplo();
                        },
                        controle
                );

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> primeira = executor.submit(servico::executar);
            entrouCobol.await(2, TimeUnit.SECONDS);

            assertThrows(
                    CapacidadeEsgotadaException.class,
                    servico::executar
            );

            assertEquals(
                    1,
                    criadas.get(),
                    "Requisicao recusada nao deve criar execucao nem snapshot."
            );

            liberarCobol.countDown();
            primeira.get(5, TimeUnit.SECONDS);

        } finally {
            liberarCobol.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void deveDelegarRecuperacaoDeExecucoesAbandonadas() {
        AtomicInteger chamadas = new AtomicInteger();

        RepositorioExecucaoConciliacao repositorio =
                new RepositorioExecucaoConciliacao() {
                    @Override
                    public long criar() { return 1; }

                    @Override
                    public void iniciar(long conciliacaoId) { }

                    @Override
                    public void falhar(
                            long conciliacaoId,
                            String codigoErro,
                            String detalheErro
                    ) { }

                    @Override
                    public int falharAbandonadas(long idadeMinimaMs) {
                        chamadas.incrementAndGet();
                        assertEquals(300000L, idadeMinimaMs);
                        return 2;
                    }
                };

        RecuperadorExecucoesAbandonadas recuperador =
                new RecuperadorExecucoesAbandonadas(
                        repositorio,
                        300000
                );

        assertEquals(2, recuperador.recuperar());
        assertEquals(1, chamadas.get());
    }

    private SnapshotExecucaoCobol snapshotExemplo() {
        return new SnapshotExecucaoCobol(
                List.of(new SnapshotExecucaoCobol.Item(
                        "P001", new BigDecimal("1.00"))),
                List.of(new SnapshotExecucaoCobol.Item(
                        "P001", new BigDecimal("1.00")))
        );
    }

    private ResultadoConciliacao resultadoExemplo() {
        return new ResultadoConciliacao(
                1,
                List.of(new DetalheConciliacao(
                        "P001",
                        new BigDecimal("1.00"),
                        new BigDecimal("1.00"),
                        new BigDecimal("0.00"),
                        StatusConciliacao.CONFERIDO,
                        1
                )),
                new ResumoConciliacao(
                        1, 0, 0, 0, 0, 0,
                        new BigDecimal("1.00"),
                        new BigDecimal("1.00"),
                        new BigDecimal("0.00")
                )
        );
    }
}
