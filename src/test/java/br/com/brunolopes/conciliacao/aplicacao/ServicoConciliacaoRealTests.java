package br.com.brunolopes.conciliacao.aplicacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.brunolopes.conciliacao.aplicacao.excecao.ConflitoDadosException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.FalhaCobolException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.TimeoutCobolException;
import br.com.brunolopes.conciliacao.integracao.ProcessadorConciliacaoCobol;
import br.com.brunolopes.conciliacao.integracao.SnapshotExecucaoCobol;
import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioExecucaoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioResultadoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioSnapshotConciliacao;

class ServicoConciliacaoRealTests {

    @Test
    void deveExecutarFluxoCompletoNaOrdemCorreta() {
        List<String> eventos =
                new ArrayList<>();

        SnapshotExecucaoCobol snapshot =
                snapshotExemplo();

        ResultadoConciliacao resultado =
                resultadoExemplo();

        RepositorioExecucaoFake repositorioExecucao =
                new RepositorioExecucaoFake(
                        eventos
                );

        RepositorioSnapshotFake repositorioSnapshot =
                new RepositorioSnapshotFake(
                        eventos,
                        snapshot
                );

        RepositorioResultadoFake repositorioResultado =
                new RepositorioResultadoFake(
                        eventos
                );

        ProcessadorFake processador =
                new ProcessadorFake(
                        eventos,
                        resultado
                );

        ServicoConciliacaoReal servico =
                new ServicoConciliacaoReal(
                        repositorioExecucao,
                        repositorioSnapshot,
                        repositorioResultado,
                        processador
                );

        ResultadoServicoConciliacao resposta =
                servico.executar();

        assertEquals(
                42L,
                resposta.id()
        );

        assertEquals(
                StatusExecucaoConciliacao.CONCLUIDA,
                resposta.status()
        );

        assertSame(
                resultado,
                resposta.resultado()
        );

        assertEquals(
                List.of(
                        "CRIAR",
                        "INICIAR:42",
                        "SNAPSHOT:42",
                        "COBOL",
                        "RESULTADO:42:1"
                ),
                eventos
        );
    }

    @Test
    void deveRegistrarFalhaQuandoCobolFalhar() {
        List<String> eventos =
                new ArrayList<>();

        FalhaCobolException erroEsperado =
                new FalhaCobolException(
                        "Falha controlada do COBOL."
                );

        RepositorioExecucaoFake repositorioExecucao =
                new RepositorioExecucaoFake(
                        eventos
                );

        RepositorioSnapshotFake repositorioSnapshot =
                new RepositorioSnapshotFake(
                        eventos,
                        snapshotExemplo()
                );

        RepositorioResultadoFake repositorioResultado =
                new RepositorioResultadoFake(
                        eventos
                );

        ProcessadorFake processador =
                new ProcessadorFake(
                        eventos,
                        erroEsperado
                );

        ServicoConciliacaoReal servico =
                new ServicoConciliacaoReal(
                        repositorioExecucao,
                        repositorioSnapshot,
                        repositorioResultado,
                        processador
                );

        FalhaCobolException erroObtido =
                assertThrows(
                        FalhaCobolException.class,
                        servico::executar
                );

        assertSame(
                erroEsperado,
                erroObtido
        );

        assertEquals(
                List.of(
                        "CRIAR",
                        "INICIAR:42",
                        "SNAPSHOT:42",
                        "COBOL",
                        "FALHAR:42:FALHA_COBOL"
                ),
                eventos
        );
    }

    @Test
    void deveRegistrarFalhaQuandoSnapshotEncontrarConflito() {
        List<String> eventos =
                new ArrayList<>();

        ConflitoDadosException erroEsperado =
                new ConflitoDadosException(
                        "Nao existem dados para conciliacao."
                );

        RepositorioExecucaoFake repositorioExecucao =
                new RepositorioExecucaoFake(
                        eventos
                );

        RepositorioSnapshotFake repositorioSnapshot =
                new RepositorioSnapshotFake(
                        eventos,
                        erroEsperado
                );

        RepositorioResultadoFake repositorioResultado =
                new RepositorioResultadoFake(
                        eventos
                );

        ProcessadorFake processador =
                new ProcessadorFake(
                        eventos,
                        resultadoExemplo()
                );

        ServicoConciliacaoReal servico =
                new ServicoConciliacaoReal(
                        repositorioExecucao,
                        repositorioSnapshot,
                        repositorioResultado,
                        processador
                );

        ConflitoDadosException erroObtido =
                assertThrows(
                        ConflitoDadosException.class,
                        servico::executar
                );

        assertSame(
                erroEsperado,
                erroObtido
        );

        assertEquals(
                List.of(
                        "CRIAR",
                        "INICIAR:42",
                        "SNAPSHOT:42",
                        "FALHAR:42:CONFLITO_DADOS"
                ),
                eventos
        );
    }

    @Test
    void deveRegistrarFalhaQuandoPersistenciaDoResultadoFalhar() {
        List<String> eventos =
                new ArrayList<>();

        IllegalStateException erroEsperado =
                new IllegalStateException(
                        "Falha controlada na persistencia do resultado."
                );

        RepositorioExecucaoFake repositorioExecucao =
                new RepositorioExecucaoFake(
                        eventos
                );

        RepositorioSnapshotFake repositorioSnapshot =
                new RepositorioSnapshotFake(
                        eventos,
                        snapshotExemplo()
                );

        RepositorioResultadoFake repositorioResultado =
                new RepositorioResultadoFake(
                        eventos,
                        erroEsperado
                );

        ProcessadorFake processador =
                new ProcessadorFake(
                        eventos,
                        resultadoExemplo()
                );

        ServicoConciliacaoReal servico =
                new ServicoConciliacaoReal(
                        repositorioExecucao,
                        repositorioSnapshot,
                        repositorioResultado,
                        processador
                );

        IllegalStateException erroObtido =
                assertThrows(
                        IllegalStateException.class,
                        servico::executar
                );

        assertSame(
                erroEsperado,
                erroObtido
        );

        assertEquals(
                List.of(
                        "CRIAR",
                        "INICIAR:42",
                        "SNAPSHOT:42",
                        "COBOL",
                        "RESULTADO:42:1",
                        "FALHAR:42:ERRO_INTERNO"
                ),
                eventos
        );
    }

    @Test
    void naoDeveMascararErroOriginalSeRegistroDaFalhaTambemFalhar() {
        List<String> eventos =
                new ArrayList<>();

        TimeoutCobolException erroOriginal =
                new TimeoutCobolException(
                        "Timeout controlado."
                );

        RepositorioExecucaoFake repositorioExecucao =
                new RepositorioExecucaoFake(
                        eventos
                );

        repositorioExecucao.falharAoRegistrarFalha =
                true;

        RepositorioSnapshotFake repositorioSnapshot =
                new RepositorioSnapshotFake(
                        eventos,
                        snapshotExemplo()
                );

        RepositorioResultadoFake repositorioResultado =
                new RepositorioResultadoFake(
                        eventos
                );

        ProcessadorFake processador =
                new ProcessadorFake(
                        eventos,
                        erroOriginal
                );

        ServicoConciliacaoReal servico =
                new ServicoConciliacaoReal(
                        repositorioExecucao,
                        repositorioSnapshot,
                        repositorioResultado,
                        processador
                );

        TimeoutCobolException erroObtido =
                assertThrows(
                        TimeoutCobolException.class,
                        servico::executar
                );

        assertSame(
                erroOriginal,
                erroObtido
        );

        assertEquals(
                1,
                erroObtido.getSuppressed().length
        );

        assertEquals(
                "Falha proposital ao registrar FALHOU.",
                erroObtido.getSuppressed()[0].getMessage()
        );

        assertEquals(
                List.of(
                        "CRIAR",
                        "INICIAR:42",
                        "SNAPSHOT:42",
                        "COBOL",
                        "FALHAR:42:TIMEOUT_COBOL"
                ),
                eventos
        );
    }

    private SnapshotExecucaoCobol snapshotExemplo() {
        return new SnapshotExecucaoCobol(
                List.of(
                        new SnapshotExecucaoCobol.Item(
                                "COB001",
                                new BigDecimal("100.00")
                        )
                ),
                List.of(
                        new SnapshotExecucaoCobol.Item(
                                "COB001",
                                new BigDecimal("100.00")
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
                                new BigDecimal("100.00"),
                                new BigDecimal("100.00"),
                                new BigDecimal("0.00"),
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
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("0.00")
                )
        );
    }

    private static final class RepositorioExecucaoFake
            implements RepositorioExecucaoConciliacao {

        private final List<String> eventos;

        private boolean falharAoRegistrarFalha;

        private RepositorioExecucaoFake(
                List<String> eventos
        ) {
            this.eventos = eventos;
        }

        @Override
        public long criar() {
            eventos.add(
                    "CRIAR"
            );

            return 42L;
        }

        @Override
        public void iniciar(
                long conciliacaoId
        ) {
            eventos.add(
                    "INICIAR:"
                            + conciliacaoId
            );
        }

        @Override
        public void falhar(
                long conciliacaoId,
                String codigoErro,
                String detalheErro
        ) {
            eventos.add(
                    "FALHAR:"
                            + conciliacaoId
                            + ":"
                            + codigoErro
            );

            if (falharAoRegistrarFalha) {
                throw new IllegalStateException(
                        "Falha proposital ao registrar FALHOU."
                );
            }
        }
    }

    private static final class RepositorioSnapshotFake
            implements RepositorioSnapshotConciliacao {

        private final List<String> eventos;

        private final SnapshotExecucaoCobol snapshot;

        private final RuntimeException erro;

        private RepositorioSnapshotFake(
                List<String> eventos,
                SnapshotExecucaoCobol snapshot
        ) {
            this.eventos = eventos;
            this.snapshot = snapshot;
            this.erro = null;
        }

        private RepositorioSnapshotFake(
                List<String> eventos,
                RuntimeException erro
        ) {
            this.eventos = eventos;
            this.snapshot = null;
            this.erro = erro;
        }

        @Override
        public SnapshotExecucaoCobol capturarEPersistir(
                long conciliacaoId
        ) {
            eventos.add(
                    "SNAPSHOT:"
                            + conciliacaoId
            );

            if (erro != null) {
                throw erro;
            }

            return snapshot;
        }
    }

    private static final class RepositorioResultadoFake
            implements RepositorioResultadoConciliacao {

        private final List<String> eventos;

        private final RuntimeException erro;

        private RepositorioResultadoFake(
                List<String> eventos
        ) {
            this.eventos = eventos;
            this.erro = null;
        }

        private RepositorioResultadoFake(
                List<String> eventos,
                RuntimeException erro
        ) {
            this.eventos = eventos;
            this.erro = erro;
        }

        @Override
        public void persistirEConcluir(
                long conciliacaoId,
                ResultadoConciliacao resultado
        ) {
            eventos.add(
                    "RESULTADO:"
                            + conciliacaoId
                            + ":"
                            + resultado.versao()
            );

            if (erro != null) {
                throw erro;
            }
        }
    }

    private static final class ProcessadorFake
            implements ProcessadorConciliacaoCobol {

        private final List<String> eventos;

        private final ResultadoConciliacao resultado;

        private final RuntimeException erro;

        private ProcessadorFake(
                List<String> eventos,
                ResultadoConciliacao resultado
        ) {
            this.eventos = eventos;
            this.resultado = resultado;
            this.erro = null;
        }

        private ProcessadorFake(
                List<String> eventos,
                RuntimeException erro
        ) {
            this.eventos = eventos;
            this.resultado = null;
            this.erro = erro;
        }

        @Override
        public ResultadoConciliacao processar(
                SnapshotExecucaoCobol snapshot
        ) {
            eventos.add(
                    "COBOL"
            );

            if (erro != null) {
                throw erro;
            }

            return resultado;
        }
    }
}
