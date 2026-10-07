package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

/** Remove somente diretorios conciliacao-* antigos no java.io.tmpdir. */
public final class LimpadorDiretoriosTemporariosCobol {

    private final Path diretorioTemporario;
    private final long idadeMinimaMs;

    public LimpadorDiretoriosTemporariosCobol(
            long idadeMinimaMs
    ) {
        if (idadeMinimaMs <= 0) {
            throw new IllegalArgumentException(
                    "Idade minima dos temporarios deve ser maior que zero."
            );
        }

        this.diretorioTemporario =
                Path.of(System.getProperty("java.io.tmpdir"))
                        .toAbsolutePath()
                        .normalize();
        this.idadeMinimaMs = idadeMinimaMs;
    }

    public int limpar() {
        Instant limite =
                Instant.now().minusMillis(idadeMinimaMs);

        int removidos = 0;

        try (var entradas = Files.list(diretorioTemporario)) {
            for (Path entrada : entradas.toList()) {
                if (podeRemover(entrada, limite)) {
                    removerArvore(entrada);
                    removidos++;
                }
            }

            return removidos;

        } catch (IOException erro) {
            throw new IllegalStateException(
                    "Nao foi possivel varrer diretorios temporarios COBOL.",
                    erro
            );
        }
    }

    private boolean podeRemover(
            Path entrada,
            Instant limite
    ) throws IOException {
        Path normalizado = entrada.toAbsolutePath().normalize();

        if (!diretorioTemporario.equals(normalizado.getParent())) {
            return false;
        }

        if (!normalizado.getFileName().toString().startsWith("conciliacao-")) {
            return false;
        }

        if (Files.isSymbolicLink(normalizado)
                || !Files.isDirectory(normalizado, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }

        Instant alteradoEm =
                Files.getLastModifiedTime(
                        normalizado,
                        LinkOption.NOFOLLOW_LINKS
                ).toInstant();

        return alteradoEm.isBefore(limite);
    }

    private void removerArvore(
            Path diretorio
    ) throws IOException {
        Files.walkFileTree(
                diretorio,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(
                            Path arquivo,
                            BasicFileAttributes atributos
                    ) throws IOException {
                        Files.delete(arquivo);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(
                            Path pasta,
                            IOException erro
                    ) throws IOException {
                        if (erro != null) {
                            throw erro;
                        }

                        Files.delete(pasta);
                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }
}
