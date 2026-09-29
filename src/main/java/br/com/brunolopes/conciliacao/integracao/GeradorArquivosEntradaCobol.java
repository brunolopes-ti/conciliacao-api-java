package br.com.brunolopes.conciliacao.integracao;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GeradorArquivosEntradaCobol {

    private GeradorArquivosEntradaCobol() {
    }

    public static void gerar(
            DiretorioExecucaoCobol execucao,
            SnapshotExecucaoCobol snapshot
    ) {
        if (execucao == null) {
            throw new IllegalArgumentException(
                    "Diretorio da execucao COBOL e obrigatorio."
            );
        }

        if (snapshot == null) {
            throw new IllegalArgumentException(
                    "Snapshot e obrigatorio."
            );
        }

        validarDiretorio(execucao.diretorio());

        validarDestino(
                execucao.diretorio(),
                execucao.esperados()
        );

        validarDestino(
                execucao.diretorio(),
                execucao.recebidos()
        );

        try {
            escrever(
                    execucao.esperados(),
                    snapshot.cobrancas()
            );

            escrever(
                    execucao.recebidos(),
                    snapshot.pagamentos()
            );

        } catch (IOException erro) {
            removerArquivoCriado(
                    execucao.esperados()
            );

            removerArquivoCriado(
                    execucao.recebidos()
            );

            throw new IllegalStateException(
                    "Nao foi possivel gerar "
                            + "os arquivos de entrada do COBOL.",
                    erro
            );
        }
    }

    private static void escrever(
            Path arquivo,
            List<SnapshotExecucaoCobol.Item> itens
    ) throws IOException {

        try (BufferedWriter writer =
                     Files.newBufferedWriter(
                             arquivo,
                             UTF_8,
                             CREATE_NEW,
                             WRITE
                     )) {

            for (SnapshotExecucaoCobol.Item item : itens) {
                writer.write(
                        item.identificador()
                );

                writer.write(';');

                writer.write(
                        item.valor().toPlainString()
                );

                /*
                 * Nao usamos writer.newLine().
                 *
                 * O contrato exige LF de forma explicita,
                 * independentemente do sistema operacional.
                 */
                writer.write('\n');
            }
        }
    }

    private static void validarDiretorio(
            Path diretorio
    ) {
        if (Files.isSymbolicLink(diretorio)
                || !Files.isDirectory(
                        diretorio,
                        NOFOLLOW_LINKS
                )) {

            throw new IllegalArgumentException(
                    "Diretorio de execucao COBOL invalido."
            );
        }
    }

    private static void validarDestino(
            Path diretorio,
            Path arquivo
    ) {
        Path diretorioNormalizado =
                diretorio.toAbsolutePath()
                        .normalize();

        Path arquivoNormalizado =
                arquivo.toAbsolutePath()
                        .normalize();

        Path pastaArquivo =
                arquivoNormalizado.getParent();

        if (!diretorioNormalizado.equals(
                pastaArquivo
        )) {
            throw new IllegalArgumentException(
                    "Arquivo de entrada deve permanecer "
                            + "no diretorio da execucao COBOL."
            );
        }

        if (Files.exists(
                arquivoNormalizado,
                NOFOLLOW_LINKS
        )) {
            throw new IllegalStateException(
                    "Arquivo de entrada ja existe: "
                            + arquivoNormalizado.getFileName()
            );
        }
    }

    private static void removerArquivoCriado(
            Path arquivo
    ) {
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException ignorado) {
            /*
             * A excecao original de geracao
             * continua sendo a causa principal.
             */
        }
    }
}
