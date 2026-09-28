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
) {

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
