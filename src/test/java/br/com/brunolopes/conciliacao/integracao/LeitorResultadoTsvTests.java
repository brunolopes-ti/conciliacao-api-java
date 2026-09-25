package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeitorResultadoTsvTests {

    @TempDir
    Path diretorioTemporario;

    @Test
    void deveLerArquivoTsvValido() throws IOException {
        Path execucao =
                Files.createDirectory(
                        diretorioTemporario.resolve("execucao"));

        Path resultado =
                execucao.resolve("resultado.tsv");

        String conteudo =
                "VERSAO\t1\n"
                + "DETALHE\tP001\t100.00\t100.00\t0.00"
                + "\tCONFERIDO\t1\n"
                + "RESUMO\t1\t0\t0\t0\t0\t0"
                + "\t100.00\t100.00\t0.00\n";

        Files.writeString(
                resultado,
                conteudo,
                StandardCharsets.UTF_8);

        var conciliacao =
                LeitorResultadoTsv.ler(
                        execucao,
                        resultado);

        assertEquals(1, conciliacao.versao());
        assertEquals(1, conciliacao.detalhes().size());
    }

    @Test
    void deveRejeitarArquivoInexistente()
            throws IOException {

        Path execucao =
                Files.createDirectory(
                        diretorioTemporario.resolve("execucao"));

        Path resultado =
                execucao.resolve("resultado.tsv");

        assertThrows(
                IllegalArgumentException.class,
                () -> LeitorResultadoTsv.ler(
                        execucao,
                        resultado));
    }

    @Test
    void deveRejeitarDiretorioComoResultado()
            throws IOException {

        Path execucao =
                Files.createDirectory(
                        diretorioTemporario.resolve("execucao"));

        Path resultado =
                Files.createDirectory(
                        execucao.resolve("resultado.tsv"));

        assertThrows(
                IllegalArgumentException.class,
                () -> LeitorResultadoTsv.ler(
                        execucao,
                        resultado));
    }

    @Test
    void deveRejeitarArquivoForaDoDiretorio()
            throws IOException {

        Path execucao =
                Files.createDirectory(
                        diretorioTemporario.resolve("execucao"));

        Path resultado =
                diretorioTemporario.resolve("resultado.tsv");

        Files.writeString(
                resultado,
                criarConteudoValido(),
                StandardCharsets.UTF_8);

        assertThrows(
                IllegalArgumentException.class,
                () -> LeitorResultadoTsv.ler(
                        execucao,
                        resultado));
    }

    @Test
    void deveRejeitarLinkSimbolico()
            throws IOException {

        Path execucao =
                Files.createDirectory(
                        diretorioTemporario.resolve("execucao"));

        Path original =
                execucao.resolve("original.tsv");

        Files.writeString(
                original,
                criarConteudoValido(),
                StandardCharsets.UTF_8);

        Path link =
                execucao.resolve("resultado.tsv");

        Files.createSymbolicLink(
                link,
                original.getFileName());

        assertThrows(
                IllegalArgumentException.class,
                () -> LeitorResultadoTsv.ler(
                        execucao,
                        link));
    }

    @Test
    void deveRejeitarBom()
            throws IOException {

        Path execucao =
                Files.createDirectory(
                        diretorioTemporario.resolve("execucao"));

        Path resultado =
                execucao.resolve("resultado.tsv");

        byte[] texto =
                criarConteudoValido()
                        .getBytes(StandardCharsets.UTF_8);

        byte[] comBom =
                new byte[texto.length + 3];

        comBom[0] = (byte) 0xEF;
        comBom[1] = (byte) 0xBB;
        comBom[2] = (byte) 0xBF;

        System.arraycopy(
                texto,
                0,
                comBom,
                3,
                texto.length);

        Files.write(resultado, comBom);

        assertThrows(
                IllegalArgumentException.class,
                () -> LeitorResultadoTsv.ler(
                        execucao,
                        resultado));
    }

    @Test
    void deveRejeitarCrLf()
            throws IOException {

        Path execucao =
                Files.createDirectory(
                        diretorioTemporario.resolve("execucao"));

        Path resultado =
                execucao.resolve("resultado.tsv");

        String conteudo =
                criarConteudoValido()
                        .replace("\n", "\r\n");

        Files.writeString(
                resultado,
                conteudo,
                StandardCharsets.UTF_8);

        assertThrows(
                IllegalArgumentException.class,
                () -> LeitorResultadoTsv.ler(
                        execucao,
                        resultado));
    }

    @Test
    void deveRejeitarAusenciaDeLfFinal()
            throws IOException {

        Path execucao =
                Files.createDirectory(
                        diretorioTemporario.resolve("execucao"));

        Path resultado =
                execucao.resolve("resultado.tsv");

        String conteudo =
                criarConteudoValido();

        conteudo =
                conteudo.substring(
                        0,
                        conteudo.length() - 1);

        Files.writeString(
                resultado,
                conteudo,
                StandardCharsets.UTF_8);

        assertThrows(
                IllegalArgumentException.class,
                () -> LeitorResultadoTsv.ler(
                        execucao,
                        resultado));
    }

    @Test
    void deveRejeitarUtf8Invalido()
            throws IOException {

        Path execucao =
                Files.createDirectory(
                        diretorioTemporario.resolve("execucao"));

        Path resultado =
                execucao.resolve("resultado.tsv");

        byte[] invalido = {
                (byte) 0xC3,
                (byte) 0x28,
                (byte) '\n'
        };

        Files.write(resultado, invalido);

        assertThrows(
                IllegalArgumentException.class,
                () -> LeitorResultadoTsv.ler(
                        execucao,
                        resultado));
    }

    private static String criarConteudoValido() {
        return "VERSAO\t1\n"
                + "DETALHE\tP001\t100.00\t100.00\t0.00"
                + "\tCONFERIDO\t1\n"
                + "RESUMO\t1\t0\t0\t0\t0\t0"
                + "\t100.00\t100.00\t0.00\n";
    }
}
