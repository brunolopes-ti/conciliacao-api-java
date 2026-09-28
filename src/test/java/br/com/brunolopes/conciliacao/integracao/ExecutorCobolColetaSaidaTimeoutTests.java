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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorCobolColetaSaidaTimeoutTests {

    @TempDir
    Path diretorioTemporario;

    @Test
    void naoDeveEsperarIndefinidamenteQuandoFilhoMantiverSaidaAberta()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "sh -c 'sleep 4' &\n"
                        + "exit 0\n");

        ExecutorCobol executor =
                new ExecutorCobol(
                        executavel,
                        Duration.ofSeconds(5));

        long inicio =
                System.nanoTime();

        assertThrows(
                IllegalStateException.class,
                () -> executor.executar(
                        List.of()));

        long duracaoMillis =
                Duration.ofNanos(
                        System.nanoTime() - inicio)
                        .toMillis();

        assertTrue(
                duracaoMillis < 3500,
                "A coleta de saida demorou alem do limite esperado.");
    }

    private Path criarScript(
            String conteudo
    ) throws IOException {

        Path script =
                diretorioTemporario.resolve(
                        "programa-saida-aberta.sh");

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
