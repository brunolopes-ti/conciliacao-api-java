package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorCobolProcessoTests {

    @TempDir
    Path diretorioTemporario;

    @Test
    void deveExecutarProcessoComSucesso()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "printf 'processo executado\\n'\n"
                        + "exit 0\n");

        ExecutorCobol executor =
                new ExecutorCobol(executavel);

        ResultadoExecucaoProcesso resultado =
                executor.executar(List.of());

        assertEquals(0, resultado.codigoSaida());
        assertTrue(resultado.sucesso());
        assertEquals(
                "processo executado\n",
                resultado.saidaProcesso());
    }

    @Test
    void devePassarArgumentosSeparadamente()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "printf 'primeiro=%s segundo=%s\\n' \"$1\" \"$2\"\n"
                        + "exit 0\n");

        ExecutorCobol executor =
                new ExecutorCobol(executavel);

        ResultadoExecucaoProcesso resultado =
                executor.executar(
                        List.of(
                                "arquivo esperado.csv",
                                "arquivo recebido.csv"));

        assertEquals(
                "primeiro=arquivo esperado.csv "
                + "segundo=arquivo recebido.csv\n",
                resultado.saidaProcesso());
    }

    @Test
    void deveCapturarCodigoDeFalha()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "printf 'erro controlado\\n'\n"
                        + "exit 2\n");

        ExecutorCobol executor =
                new ExecutorCobol(executavel);

        ResultadoExecucaoProcesso resultado =
                executor.executar(List.of());

        assertEquals(2, resultado.codigoSaida());
        assertFalse(resultado.sucesso());
    }

    @Test
    void deveCapturarSaidaDeErroJuntoComSaidaPadrao()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "printf 'saida normal\\n'\n"
                        + "printf 'saida de erro\\n' >&2\n"
                        + "exit 0\n");

        ExecutorCobol executor =
                new ExecutorCobol(executavel);

        ResultadoExecucaoProcesso resultado =
                executor.executar(List.of());

        assertTrue(
                resultado.saidaProcesso()
                        .contains("saida normal"));

        assertTrue(
                resultado.saidaProcesso()
                        .contains("saida de erro"));
    }

    private Path criarScript(
            String conteudo
    ) throws IOException {

        Path script =
                diretorioTemporario.resolve("programa-teste.sh");

        Files.writeString(
                script,
                conteudo,
                StandardCharsets.UTF_8);

        Files.setPosixFilePermissions(
                script,
                PosixFilePermissions.fromString("rwx------"));

        return script;
    }
}
