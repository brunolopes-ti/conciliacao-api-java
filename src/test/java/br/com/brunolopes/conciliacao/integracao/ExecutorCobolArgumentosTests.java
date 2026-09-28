package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorCobolArgumentosTests {

    @TempDir
    Path diretorioTemporario;

    @Test
    void deveEnviarOsQuatroArgumentosAoProcesso()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "printf 'quantidade=%s\\n' \"$#\"\n"
                        + "printf '1=%s\\n' \"$1\"\n"
                        + "printf '2=%s\\n' \"$2\"\n"
                        + "printf '3=%s\\n' \"$3\"\n"
                        + "printf '4=%s\\n' \"$4\"\n"
                        + "exit 0\n");

        Path pasta =
                diretorioTemporario.resolve("minha execucao");

        Files.createDirectory(pasta);

        ArgumentosExecucaoCobol argumentos =
                new ArgumentosExecucaoCobol(
                        pasta.resolve("esperados.csv"),
                        pasta.resolve("recebidos.csv"),
                        pasta.resolve("relatorio.txt"),
                        pasta.resolve("resultado.tsv"));

        ExecutorCobol executor =
                new ExecutorCobol(executavel);

        ResultadoExecucaoProcesso resultado =
                executor.executar(argumentos);

        assertTrue(resultado.sucesso());

        assertTrue(
                resultado.saidaProcesso()
                        .contains("quantidade=4"));

        assertTrue(
                resultado.saidaProcesso()
                        .contains(
                                "1="
                                + pasta.resolve("esperados.csv")));

        assertTrue(
                resultado.saidaProcesso()
                        .contains(
                                "2="
                                + pasta.resolve("recebidos.csv")));

        assertTrue(
                resultado.saidaProcesso()
                        .contains(
                                "3="
                                + pasta.resolve("relatorio.txt")));

        assertTrue(
                resultado.saidaProcesso()
                        .contains(
                                "4="
                                + pasta.resolve("resultado.tsv")));
    }

    @Test
    void devePreservarCaminhosComEspacosComoUmArgumento()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "printf '%s\\n' \"$#\"\n"
                        + "exit 0\n");

        Path pasta =
                diretorioTemporario.resolve("pasta com espacos");

        Files.createDirectory(pasta);

        ArgumentosExecucaoCobol argumentos =
                new ArgumentosExecucaoCobol(
                        pasta.resolve("esperados.csv"),
                        pasta.resolve("recebidos.csv"),
                        pasta.resolve("relatorio.txt"),
                        pasta.resolve("resultado.tsv"));

        ExecutorCobol executor =
                new ExecutorCobol(executavel);

        ResultadoExecucaoProcesso resultado =
                executor.executar(argumentos);

        assertEquals(
                "4\n",
                resultado.saidaProcesso());
    }

    @Test
    void deveRejeitarArgumentosNulos() throws IOException {
        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "exit 0\n");

        ExecutorCobol executor =
                new ExecutorCobol(executavel);

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.executar(
                        (ArgumentosExecucaoCobol) null));
    }

    private Path criarScript(
            String conteudo
    ) throws IOException {

        Path script =
                diretorioTemporario.resolve(
                        "programa-teste.sh");

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
