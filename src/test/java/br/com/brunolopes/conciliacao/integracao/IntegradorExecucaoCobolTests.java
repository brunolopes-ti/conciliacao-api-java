package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegradorExecucaoCobolTests {

    @TempDir
    Path diretorioTemporario;

    @Test
    void deveExecutarEInterpretarResultadoValido()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "printf 'Relatorio da conciliacao\\n' > \"$3\"\n"
                        + "printf 'VERSAO\\t1\\n"
                        + "DETALHE\\tP001\\t100.00\\t100.00\\t0.00"
                        + "\\tCONFERIDO\\t1\\n"
                        + "RESUMO\\t1\\t0\\t0\\t0\\t0\\t0"
                        + "\\t100.00\\t100.00\\t0.00\\n' > \"$4\"\n"
                        + "exit 0\n");

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        ExecutorCobol executor =
                new ExecutorCobol(
                        executavel,
                        Duration.ofSeconds(5));

        IntegradorExecucaoCobol integrador =
                new IntegradorExecucaoCobol(
                        executor);

        ResultadoExecucaoCobol resultado =
                integrador.executar(
                        execucao);

        assertTrue(
                resultado.processo().sucesso());

        assertEquals(
                1,
                resultado.resultado().versao());

        assertEquals(
                1,
                resultado.resultado()
                        .detalhes()
                        .size());

        assertEquals(
                StatusConciliacao.CONFERIDO,
                resultado.resultado()
                        .detalhes()
                        .get(0)
                        .status());

        assertEquals(
                execucao.relatorio(),
                resultado.relatorio());
    }

    @Test
    void deveRejeitarCodigoUm()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "exit 1\n");

        IntegradorExecucaoCobol integrador =
                criarIntegrador(
                        executavel);

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        assertThrows(
                IllegalStateException.class,
                () -> integrador.executar(
                        execucao));
    }

    @Test
    void deveRejeitarCodigoDois()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "exit 2\n");

        IntegradorExecucaoCobol integrador =
                criarIntegrador(
                        executavel);

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        assertThrows(
                IllegalStateException.class,
                () -> integrador.executar(
                        execucao));
    }

    @Test
    void deveRejeitarCodigoDesconhecido()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "exit 7\n");

        IntegradorExecucaoCobol integrador =
                criarIntegrador(
                        executavel);

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        assertThrows(
                IllegalStateException.class,
                () -> integrador.executar(
                        execucao));
    }

    @Test
    void deveRejeitarResultadoTsvInvalido()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "printf 'Relatorio valido\\n' > \"$3\"\n"
                        + "printf 'resultado invalido\\n' > \"$4\"\n"
                        + "exit 0\n");

        IntegradorExecucaoCobol integrador =
                criarIntegrador(
                        executavel);

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        assertThrows(
                IllegalArgumentException.class,
                () -> integrador.executar(
                        execucao));
    }

    @Test
    void deveRejeitarRelatorioAusenteMesmoComCodigoZero()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "printf 'VERSAO\\t1\\n"
                        + "DETALHE\\tP001\\t100.00\\t100.00\\t0.00"
                        + "\\tCONFERIDO\\t1\\n"
                        + "RESUMO\\t1\\t0\\t0\\t0\\t0\\t0"
                        + "\\t100.00\\t100.00\\t0.00\\n' > \"$4\"\n"
                        + "exit 0\n");

        IntegradorExecucaoCobol integrador =
                criarIntegrador(
                        executavel);

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        assertThrows(
                IllegalStateException.class,
                () -> integrador.executar(
                        execucao));
    }

    @Test
    void deveAplicarTimeoutNaExecucaoIntegrada()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "sleep 2\n"
                        + "exit 0\n");

        ExecutorCobol executor =
                new ExecutorCobol(
                        executavel,
                        Duration.ofMillis(100));

        IntegradorExecucaoCobol integrador =
                new IntegradorExecucaoCobol(
                        executor);

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        assertThrows(
                IllegalStateException.class,
                () -> integrador.executar(
                        execucao));
    }

    private IntegradorExecucaoCobol criarIntegrador(
            Path executavel
    ) {
        ExecutorCobol executor =
                new ExecutorCobol(
                        executavel,
                        Duration.ofSeconds(5));

        return new IntegradorExecucaoCobol(
                executor);
    }

    private Path criarScript(
            String conteudo
    ) throws IOException {

        Path script =
                diretorioTemporario.resolve(
                        "programa-cobol-teste.sh");

        Files.writeString(
                script,
                conteudo,
                StandardCharsets.UTF_8);

        Files.setPosixFilePermissions(
                script,
                PosixFilePermissions.fromString(
                        "rwx------"));

        return script;
    }
}
