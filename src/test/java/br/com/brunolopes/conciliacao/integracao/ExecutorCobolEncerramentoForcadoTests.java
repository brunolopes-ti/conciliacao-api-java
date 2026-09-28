package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorCobolEncerramentoForcadoTests {

    @TempDir
    Path diretorioTemporario;

    @Test
    void deveForcarEncerramentoQuandoProcessoIgnorarTerm()
            throws IOException {

        Path arquivoPid =
                diretorioTemporario.resolve(
                        "processo.pid");

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "trap '' TERM\n"
                        + "printf '%s' \"$$\" > \"$1\"\n"
                        + "while true\n"
                        + "do\n"
                        + "    sleep 1\n"
                        + "done\n");

        ExecutorCobol executor =
                new ExecutorCobol(
                        executavel,
                        Duration.ofMillis(100));

        assertThrows(
                IllegalStateException.class,
                () -> executor.executar(
                        List.of(
                                arquivoPid.toString())));

        assertTrue(
                Files.exists(arquivoPid));

        long pid =
                Long.parseLong(
                        Files.readString(
                                arquivoPid,
                                StandardCharsets.UTF_8));

        boolean processoAindaEstaVivo =
                ProcessHandle.of(pid)
                        .map(ProcessHandle::isAlive)
                        .orElse(false);

        assertFalse(
                processoAindaEstaVivo);
    }

    private Path criarScript(
            String conteudo
    ) throws IOException {

        Path script =
                diretorioTemporario.resolve(
                        "programa-resistente.sh");

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
