package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

public record DiretorioExecucaoCobol(
        Path diretorio,
        Path esperados,
        Path recebidos,
        Path relatorio,
        Path resultado,
        Path log
) implements AutoCloseable {

    /** Chamar somente apos terminar o processo e consumir/persistir o relatorio. */
    @Override
    public void close() throws IOException {
        if (Files.isSymbolicLink(diretorio)) {
            throw new IOException("Diretorio de execucao nao pode ser link simbolico.");
        }
        if (!Files.exists(diretorio, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        // walkFileTree nao segue links: remove o link, preservando seu destino.
        Files.walkFileTree(diretorio, new java.nio.file.SimpleFileVisitor<Path>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(
                    Path arquivo, java.nio.file.attribute.BasicFileAttributes atributos)
                    throws IOException {
                Files.delete(arquivo);
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path pasta, IOException erro)
                    throws IOException {
                if (erro != null) {
                    throw erro;
                }
                Files.delete(pasta);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }

    public static DiretorioExecucaoCobol criar() {
        try {
            var permissoes =
                    PosixFilePermissions.asFileAttribute(
                            PosixFilePermissions.fromString(
                                    "rwx------"));

            Path diretorio =
                    Files.createTempDirectory(
                            "conciliacao-",
                            permissoes)
                            .toAbsolutePath()
                            .normalize();

            return new DiretorioExecucaoCobol(
                    diretorio,
                    diretorio.resolve("esperados.csv"),
                    diretorio.resolve("recebidos.csv"),
                    diretorio.resolve("relatorio.txt"),
                    diretorio.resolve("resultado.tsv"),
                    diretorio.resolve("processo.log"));

        } catch (IOException erro) {
            throw new IllegalStateException(
                    "Nao foi possivel criar "
                    + "o diretorio temporario da conciliacao.",
                    erro);
        }
    }
}
