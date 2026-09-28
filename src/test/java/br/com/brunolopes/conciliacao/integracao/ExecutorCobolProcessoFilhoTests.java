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

class ExecutorCobolProcessoFilhoTests {

    @TempDir
    Path diretorioTemporario;

    @Test
    void deveEncerrarProcessoFilhoQuandoOcorrerTimeout()
            throws IOException {

        Path arquivoPidFilho =
                diretorioTemporario.resolve(
                        "filho.pid");

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "sh -c '"
                        + "trap \"\" TERM; "
                        + "while true; do sleep 1; done"
                        + "' &\n"
                        + "printf '%s' \"$!\" > \"$1\"\n"
                        + "wait\n");

        ExecutorCobol executor =
                new ExecutorCobol(
                        executavel,
                        Duration.ofMillis(300));

        assertThrows(
                IllegalStateException.class,
                () -> executor.executar(
                        List.of(
                                arquivoPidFilho.toString())));

        assertTrue(
                Files.exists(
                        arquivoPidFilho));

        long pidFilho =
                Long.parseLong(
                        Files.readString(
                                arquivoPidFilho,
                                StandardCharsets.UTF_8));

        boolean filhoAindaEstaVivo =
                ProcessHandle.of(pidFilho)
                        .map(ProcessHandle::isAlive)
                        .orElse(false);

        assertFalse(
                filhoAindaEstaVivo);
    }

    private Path criarScript(
            String conteudo
    ) throws IOException {

        Path script =
                diretorioTemporario.resolve(
                        "programa-com-filho.sh");

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
