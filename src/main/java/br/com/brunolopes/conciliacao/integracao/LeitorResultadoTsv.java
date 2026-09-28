package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;

public final class LeitorResultadoTsv {

    private static final long TAMANHO_MAXIMO_ARQUIVO =
            2L * 1024 * 1024;

    private static final int TAMANHO_MAXIMO_LINHA = 1024;

    private LeitorResultadoTsv() {
    }

    public static ResultadoConciliacao ler(
            Path diretorioExecucao,
            Path arquivoResultado
    ) {
        if (diretorioExecucao == null
                || arquivoResultado == null) {
            throw new IllegalArgumentException(
                    "Diretorio e arquivo do resultado sao obrigatorios.");
        }

        try {
            validarCaminho(
                    diretorioExecucao,
                    arquivoResultado);

            byte[] conteudo;
            try (var entrada = Files.newInputStream(
                    arquivoResultado, LinkOption.NOFOLLOW_LINKS)) {
                conteudo = entrada.readNBytes(
                        (int) TAMANHO_MAXIMO_ARQUIVO + 1);
            }

            validarTamanho(conteudo);
            validarBom(conteudo);
            validarQuebrasDeLinha(conteudo);

            String texto = decodificarUtf8(conteudo);

            validarTamanhoDasLinhas(texto);

            List<String> linhas =
                    separarLinhas(texto);

            return InterpretadorResultadoTsv.interpretar(
                    linhas);

        } catch (IOException erro) {
            throw new IllegalArgumentException(
                    "Nao foi possivel ler o resultado TSV.",
                    erro);
        }
    }

    private static void validarCaminho(
            Path diretorioExecucao,
            Path arquivoResultado
    ) throws IOException {

        Path diretorioReal =
                diretorioExecucao.toRealPath();

        if (!Files.isDirectory(
                diretorioReal,
                LinkOption.NOFOLLOW_LINKS)) {

            throw new IllegalArgumentException(
                    "Diretorio da execucao invalido.");
        }

        if (!Files.exists(
                arquivoResultado,
                LinkOption.NOFOLLOW_LINKS)) {

            throw new IllegalArgumentException(
                    "Arquivo resultado.tsv inexistente.");
        }

        if (Files.isSymbolicLink(arquivoResultado)) {
            throw new IllegalArgumentException(
                    "resultado.tsv nao pode ser link simbolico.");
        }

        if (!Files.isRegularFile(
                arquivoResultado,
                LinkOption.NOFOLLOW_LINKS)) {

            throw new IllegalArgumentException(
                    "resultado.tsv deve ser arquivo regular.");
        }

        Path arquivoReal =
                arquivoResultado.toRealPath();

        if (!arquivoReal.startsWith(diretorioReal)) {
            throw new IllegalArgumentException(
                    "resultado.tsv esta fora do diretorio da execucao.");
        }

        long tamanho = Files.size(arquivoResultado);

        if (tamanho > TAMANHO_MAXIMO_ARQUIVO) {
            throw new IllegalArgumentException(
                    "resultado.tsv excede o tamanho maximo permitido.");
        }
    }

    private static void validarTamanho(
            byte[] conteudo
    ) {
        if (conteudo.length == 0) {
            throw new IllegalArgumentException(
                    "resultado.tsv esta vazio.");
        }

        if (conteudo.length > TAMANHO_MAXIMO_ARQUIVO) {
            throw new IllegalArgumentException(
                    "resultado.tsv excede o tamanho maximo permitido.");
        }
    }

    private static void validarBom(
            byte[] conteudo
    ) {
        if (conteudo.length >= 3
                && (conteudo[0] & 0xFF) == 0xEF
                && (conteudo[1] & 0xFF) == 0xBB
                && (conteudo[2] & 0xFF) == 0xBF) {

            throw new IllegalArgumentException(
                    "resultado.tsv nao pode possuir BOM.");
        }
    }

    private static void validarQuebrasDeLinha(
            byte[] conteudo
    ) {
        for (byte caractere : conteudo) {
            if (caractere == '\r') {
                throw new IllegalArgumentException(
                        "resultado.tsv deve utilizar apenas LF.");
            }
        }

        if (conteudo[conteudo.length - 1] != '\n') {
            throw new IllegalArgumentException(
                    "resultado.tsv deve terminar com LF.");
        }
    }

    private static String decodificarUtf8(
            byte[] conteudo
    ) {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(
                            CodingErrorAction.REPORT)
                    .onUnmappableCharacter(
                            CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(conteudo))
                    .toString();

        } catch (CharacterCodingException erro) {
            throw new IllegalArgumentException(
                    "resultado.tsv nao possui UTF-8 valido.",
                    erro);
        }
    }

    private static void validarTamanhoDasLinhas(
            String texto
    ) {
        String[] linhas = texto.split("\n", -1);

        for (int indice = 0;
                indice < linhas.length - 1;
                indice++) {

            if (linhas[indice].length()
                    > TAMANHO_MAXIMO_LINHA) {

                throw new IllegalArgumentException(
                        "Linha do resultado.tsv excede "
                        + "o tamanho maximo permitido.");
            }
        }
    }

    private static List<String> separarLinhas(
            String texto
    ) {
        String semLfFinal =
                texto.substring(
                        0,
                        texto.length() - 1);

        return Arrays.asList(
                semLfFinal.split("\n", -1));
    }
}
