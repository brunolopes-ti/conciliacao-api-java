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

class ExecutorCobolTimeoutTests {

    @TempDir
    Path diretorioTemporario;

    @Test
    void deveInterromperProcessoQuandoUltrapassarTimeout()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                                + "sleep 2\n"
                                + "exit 0\n"
                );

        ExecutorCobol executor =
                new ExecutorCobol(
                        executavel,
                        Duration.ofMillis(100)
                );

        assertThrows(
                TimeoutExecucaoCobolException.class,
                () -> executor.executar(
                        List.of()
                )
        );
    }

    @Test
    void deveRejeitarTimeoutInvalido()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                                + "exit 0\n"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ExecutorCobol(
                        executavel,
                        Duration.ZERO
                )
        );
    }

    private Path criarScript(
            String conteudo
    ) throws IOException {

        Path script =
                diretorioTemporario.resolve(
                        "programa-timeout.sh"
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
