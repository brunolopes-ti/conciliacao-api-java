package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExecutorCobolTests {

    @TempDir
    Path diretorioTemporario;

    @Test
    void deveGuardarCaminhoAbsolutoDoExecutavel()
            throws IOException {

        Path arquivo =
                Files.createFile(
                        diretorioTemporario.resolve("conciliacao"));

        ExecutorCobol executor =
                new ExecutorCobol(arquivo);

        assertEquals(
                arquivo.toAbsolutePath().normalize(),
                executor.executavel());
    }

    @Test
    void deveAceitarExecutavelValido()
            throws IOException {

        Path arquivo =
                criarExecutavel();

        ExecutorCobol executor =
                new ExecutorCobol(arquivo);

        executor.validarExecutavel();
    }

    @Test
    void deveRejeitarCaminhoNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ExecutorCobol(null));
    }

    @Test
    void deveRejeitarExecutavelInexistente() {
        Path arquivo =
                diretorioTemporario.resolve(
                        "nao-existe");

        ExecutorCobol executor =
                new ExecutorCobol(arquivo);

        assertThrows(
                IllegalArgumentException.class,
                executor::validarExecutavel);
    }

    @Test
    void deveRejeitarDiretorioComoExecutavel()
            throws IOException {

        Path diretorio =
                Files.createDirectory(
                        diretorioTemporario.resolve("programa"));

        ExecutorCobol executor =
                new ExecutorCobol(diretorio);

        assertThrows(
                IllegalArgumentException.class,
                executor::validarExecutavel);
    }

    @Test
    void deveRejeitarArquivoSemPermissaoDeExecucao()
            throws IOException {

        Path arquivo =
                Files.createFile(
                        diretorioTemporario.resolve("conciliacao"));

        Files.setPosixFilePermissions(
                arquivo,
                PosixFilePermissions.fromString("rw-------"));

        ExecutorCobol executor =
                new ExecutorCobol(arquivo);

        assertThrows(
                IllegalArgumentException.class,
                executor::validarExecutavel);
    }

    private Path criarExecutavel()
            throws IOException {

        Path arquivo =
                Files.createFile(
                        diretorioTemporario.resolve("conciliacao"));

        Files.setPosixFilePermissions(
                arquivo,
                PosixFilePermissions.fromString("rwx------"));

        return arquivo;
    }
}
