package br.com.brunolopes.conciliacao.aplicacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import br.com.brunolopes.conciliacao.integracao.SnapshotExecucaoCobol;
import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioExecucaoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioResultadoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioSnapshotConciliacao;

class ServicoConciliacaoRealPersistenciaTests {

    @Test
    void deveClassificarFalhaDeBancoComoErroPersistencia() {
        AtomicReference<String> codigoRegistrado =
                new AtomicReference<>();

        RepositorioExecucaoConciliacao repositorioExecucao =
                new RepositorioExecucaoConciliacao() {

                    @Override
                    public long criar() {
                        return 77L;
                    }

                    @Override
                    public void iniciar(
                            long conciliacaoId
                    ) {
                    }

                    @Override
                    public long criarEmProcessamento() {
                        return 77L;
                    }

                    @Override
                    public void falhar(
                            long conciliacaoId,
                            String codigoErro,
                            String detalheErro
                    ) {
                        codigoRegistrado.set(
                                codigoErro
                        );
                    }
                };

        RepositorioSnapshotConciliacao repositorioSnapshot =
                conciliacaoId ->
                        snapshotExemplo();

        RepositorioResultadoConciliacao repositorioResultado =
                (conciliacaoId, resultado) -> {
                    throw new DataAccessResourceFailureException(
                            "Banco indisponivel."
                    );
                };

        ServicoConciliacaoReal servico =
                new ServicoConciliacaoReal(
                        repositorioExecucao,
                        repositorioSnapshot,
                        repositorioResultado,
                        snapshot ->
                                resultadoExemplo()
                );

        assertThrows(
                DataAccessResourceFailureException.class,
                servico::executar
        );

        assertEquals(
                "ERRO_PERSISTENCIA",
                codigoRegistrado.get()
        );
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
