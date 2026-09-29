package br.com.brunolopes.conciliacao.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.brunolopes.conciliacao.aplicacao.excecao.FalhaCobolException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.ResultadoInvalidoException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.TimeoutCobolException;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

class ProcessadorConciliacaoCobolRealTests {

    @TempDir
    Path diretorioTemporario;

    @Test
    void deveProcessarResultadoValido()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                                + "printf 'Relatorio valido\\n' > \"$3\"\n"
                                + "printf 'VERSAO\\t1\\n"
                                + "DETALHE\\tP001\\t100.00\\t100.00\\t0.00"
                                + "\\tCONFERIDO\\t1\\n"
                                + "RESUMO\\t1\\t0\\t0\\t0\\t0\\t0"
                                + "\\t100.00\\t100.00\\t0.00\\n' > \"$4\"\n"
                                + "exit 0\n"
                );

        ProcessadorConciliacaoCobolReal processador =
                criarProcessador(
                        executavel,
                        Duration.ofSeconds(5)
                );

        var resultado =
                processador.processar(
                        snapshotExemplo()
                );

        assertEquals(
                1,
                resultado.versao()
        );

        assertEquals(
                1,
                resultado.detalhes().size()
        );

        assertEquals(
                StatusConciliacao.CONFERIDO,
                resultado.detalhes()
                        .getFirst()
                        .status()
        );
    }

    @Test
    void deveConverterTimeoutParaExcecaoDaAplicacao()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                                + "sleep 2\n"
                                + "exit 0\n"
                );

        ProcessadorConciliacaoCobolReal processador =
                criarProcessador(
                        executavel,
                        Duration.ofMillis(100)
                );

        assertThrows(
                TimeoutCobolException.class,
                () -> processador.processar(
                        snapshotExemplo()
                )
        );
    }

    @Test
    void deveConverterFalhaDoMotorParaExcecaoDaAplicacao()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                                + "exit 1\n"
                );

        ProcessadorConciliacaoCobolReal processador =
                criarProcessador(
                        executavel,
                        Duration.ofSeconds(5)
                );

        assertThrows(
                FalhaCobolException.class,
                () -> processador.processar(
                        snapshotExemplo()
                )
        );
    }

    @Test
    void deveConverterResultadoInvalidoParaExcecaoDaAplicacao()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                                + "printf 'Relatorio valido\\n' > \"$3\"\n"
                                + "printf 'resultado invalido\\n' > \"$4\"\n"
                                + "exit 0\n"
                );

        ProcessadorConciliacaoCobolReal processador =
                criarProcessador(
                        executavel,
                        Duration.ofSeconds(5)
                );

        assertThrows(
                ResultadoInvalidoException.class,
                () -> processador.processar(
                        snapshotExemplo()
                )
        );
    }

    private ProcessadorConciliacaoCobolReal criarProcessador(
            Path executavel,
            Duration timeout
    ) {
        ExecutorCobol executor =
                new ExecutorCobol(
                        executavel,
                        timeout
                );

        IntegradorExecucaoCobol integrador =
                new IntegradorExecucaoCobol(
                        executor
                );

        return new ProcessadorConciliacaoCobolReal(
                integrador
        );
    }

    private SnapshotExecucaoCobol snapshotExemplo() {
        return new SnapshotExecucaoCobol(
                List.of(
                        new SnapshotExecucaoCobol.Item(
                                "P001",
                                new BigDecimal("100.00")
                        )
                ),
                List.of(
                        new SnapshotExecucaoCobol.Item(
                                "P001",
                                new BigDecimal("100.00")
                        )
                )
        );
    }

    private Path criarScript(
            String conteudo
    ) throws IOException {

        Path script =
                diretorioTemporario.resolve(
                        "cobol-processador-teste.sh"
                );

        Files.writeString(
                script,
                conteudo,
                StandardCharsets.UTF_8
        );

        Files.setPosixFilePermissions(
                script,
                PosixFilePermissions.fromString(
                        "rwx------"
                )
        );

        return script;
    }
}
