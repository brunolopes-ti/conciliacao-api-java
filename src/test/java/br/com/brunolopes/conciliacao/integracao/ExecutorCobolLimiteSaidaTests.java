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

class ExecutorCobolLimiteSaidaTests {

    @TempDir
    Path diretorioTemporario;

    @Test
    void deveManterSaidaCompletaQuandoDentroDoLimite()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "printf 'saida curta\\n'\n"
                        + "exit 0\n");

        ExecutorCobol executor =
                new ExecutorCobol(executavel);

        ResultadoExecucaoProcesso resultado =
                executor.executar(List.of());

        assertEquals(
                "saida curta\n",
                resultado.saidaProcesso());

        assertFalse(
                resultado.saidaTruncada());
    }

    @Test
    void deveLimitarSaidaMuitoGrande()
            throws IOException {

        Path executavel =
                criarScript(
                        "#!/bin/sh\n"
                        + "printf '%*s' 70000 ''\n"
                        + "exit 0\n");

        ExecutorCobol executor =
                new ExecutorCobol(executavel);

        ResultadoExecucaoProcesso resultado =
                executor.executar(List.of());

        int quantidadeBytes =
                resultado.saidaProcesso()
                        .getBytes(StandardCharsets.UTF_8)
                        .length;

        assertEquals(
                ExecutorCobol.LIMITE_SAIDA_BYTES,
                quantidadeBytes);

        assertTrue(
                resultado.saidaTruncada());

        assertTrue(
                resultado.sucesso());
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
