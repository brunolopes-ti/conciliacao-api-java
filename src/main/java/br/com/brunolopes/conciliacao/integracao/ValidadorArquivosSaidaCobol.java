package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public final class ValidadorArquivosSaidaCobol {

    public static final long LIMITE_RELATORIO_BYTES = 2L * 1024 * 1024;

    private ValidadorArquivosSaidaCobol() {
    }

    public static void validar(
            DiretorioExecucaoCobol execucao
    ) {
        if (execucao == null) {
            throw new IllegalArgumentException(
                    "Diretorio da execucao COBOL e obrigatorio.");
        }

        Path diretorioReal =
                obterDiretorioReal(
                        execucao.diretorio());

        validarArquivo(
                diretorioReal,
                execucao.relatorio(),
                "Relatorio COBOL");

        validarArquivo(
                diretorioReal,
                execucao.resultado(),
                "Resultado COBOL");

        validarRelatorioNaoVazio(
                execucao.relatorio());
    }

    private static Path obterDiretorioReal(
            Path diretorio
    ) {
        try {
            if (diretorio == null) {
                throw new IllegalArgumentException(
                        "Diretorio da execucao e obrigatorio.");
            }

            if (!Files.isDirectory(
                    diretorio,
                    LinkOption.NOFOLLOW_LINKS)) {

                throw new IllegalStateException(
                        "Diretorio da execucao COBOL e invalido.");
            }

            return diretorio.toRealPath();

        } catch (IOException erro) {
            throw new IllegalStateException(
                    "Nao foi possivel validar "
                    + "o diretorio da execucao COBOL.",
                    erro);
        }
    }

    private static void validarArquivo(
            Path diretorioReal,
            Path arquivo,
            String nome
    ) {
        try {
            if (arquivo == null) {
                throw new IllegalStateException(
                        nome + " nao foi informado.");
            }

            if (!Files.exists(
                    arquivo,
                    LinkOption.NOFOLLOW_LINKS)) {

                throw new IllegalStateException(
                        nome + " nao existe.");
            }

            if (Files.isSymbolicLink(arquivo)) {
                throw new IllegalStateException(
                        nome + " nao pode ser link simbolico.");
            }

            if (!Files.isRegularFile(
                    arquivo,
                    LinkOption.NOFOLLOW_LINKS)) {

                throw new IllegalStateException(
                        nome + " deve ser arquivo regular.");
            }

            if (!Files.isReadable(arquivo)) {
                throw new IllegalStateException(
                        nome + " nao pode ser lido.");
            }

            Path arquivoReal =
                    arquivo.toRealPath();

            if (!arquivoReal.startsWith(
                    diretorioReal)) {

                throw new IllegalStateException(
                        nome
                        + " esta fora do diretorio da execucao.");
            }

        } catch (IOException erro) {
            throw new IllegalStateException(
                    "Falha ao validar "
                    + nome
                    + ".",
                    erro);
        }
    }

    private static void validarRelatorioNaoVazio(
            Path relatorio
    ) {
        try {
            long tamanho = Files.size(relatorio);
            if (tamanho > LIMITE_RELATORIO_BYTES) {
                throw new IllegalStateException(
                        "Relatorio COBOL excede o tamanho maximo permitido.");
            }
            if (tamanho == 0) {
                throw new IllegalStateException(
                        "Relatorio COBOL esta vazio.");
            }

        } catch (IOException erro) {
            throw new IllegalStateException(
                    "Nao foi possivel verificar "
                    + "o tamanho do relatorio COBOL.",
                    erro);
        }
    }
}
