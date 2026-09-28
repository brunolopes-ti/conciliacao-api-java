package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorArquivosSaidaCobolTests {

    @Test
    void deveAceitarArquivosDeSaidaValidos()
            throws IOException {

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        criarSaidasValidas(execucao);

        ValidadorArquivosSaidaCobol.validar(
                execucao);
    }

    @Test
    void deveRejeitarRelatorioAusente()
            throws IOException {

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        Files.writeString(
                execucao.resultado(),
                "resultado",
                StandardCharsets.UTF_8);

        assertThrows(
                IllegalStateException.class,
                () -> ValidadorArquivosSaidaCobol.validar(
                        execucao));
    }

    @Test
    void deveRejeitarResultadoAusente()
            throws IOException {

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        Files.writeString(
                execucao.relatorio(),
                "relatorio",
                StandardCharsets.UTF_8);

        assertThrows(
                IllegalStateException.class,
                () -> ValidadorArquivosSaidaCobol.validar(
                        execucao));
    }

    @Test
    void deveRejeitarRelatorioVazio()
            throws IOException {

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        Files.createFile(
                execucao.relatorio());

        Files.writeString(
                execucao.resultado(),
                "resultado",
                StandardCharsets.UTF_8);

        assertThrows(
                IllegalStateException.class,
                () -> ValidadorArquivosSaidaCobol.validar(
                        execucao));
    }

    @Test
    void deveRejeitarRelatorioComoDiretorio()
            throws IOException {

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        Files.createDirectory(
                execucao.relatorio());

        Files.writeString(
                execucao.resultado(),
                "resultado",
                StandardCharsets.UTF_8);

        assertThrows(
                IllegalStateException.class,
                () -> ValidadorArquivosSaidaCobol.validar(
                        execucao));
    }

    @Test
    void deveRejeitarResultadoComoDiretorio()
            throws IOException {

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        Files.writeString(
                execucao.relatorio(),
                "relatorio",
                StandardCharsets.UTF_8);

        Files.createDirectory(
                execucao.resultado());

        assertThrows(
                IllegalStateException.class,
                () -> ValidadorArquivosSaidaCobol.validar(
                        execucao));
    }

    @Test
    void deveRejeitarRelatorioComoLinkSimbolico()
            throws IOException {

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        Path arquivoReal =
                execucao.diretorio()
                        .resolve("relatorio-real.txt");

        Files.writeString(
                arquivoReal,
                "relatorio",
                StandardCharsets.UTF_8);

        Files.createSymbolicLink(
                execucao.relatorio(),
                arquivoReal);

        Files.writeString(
                execucao.resultado(),
                "resultado",
                StandardCharsets.UTF_8);

        assertThrows(
                IllegalStateException.class,
                () -> ValidadorArquivosSaidaCobol.validar(
                        execucao));
    }

    @Test
    void deveRejeitarResultadoComoLinkSimbolico()
            throws IOException {

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        Files.writeString(
                execucao.relatorio(),
                "relatorio",
                StandardCharsets.UTF_8);

        Path arquivoReal =
                execucao.diretorio()
                        .resolve("resultado-real.tsv");

        Files.writeString(
                arquivoReal,
                "resultado",
                StandardCharsets.UTF_8);

        Files.createSymbolicLink(
                execucao.resultado(),
                arquivoReal);

        assertThrows(
                IllegalStateException.class,
                () -> ValidadorArquivosSaidaCobol.validar(
                        execucao));
    }

    @Test
    void deveRejeitarExecucaoNula() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ValidadorArquivosSaidaCobol.validar(
                        null));
    }

    private void criarSaidasValidas(
            DiretorioExecucaoCobol execucao
    ) throws IOException {

        Files.writeString(
                execucao.relatorio(),
                "Relatorio da conciliacao\n",
                StandardCharsets.UTF_8);

        Files.writeString(
                execucao.resultado(),
                "resultado\n",
                StandardCharsets.UTF_8);
    }
}
