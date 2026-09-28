package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiretorioExecucaoCobolTests {

    @Test
    void deveCriarDiretorioTemporario() {
        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        assertTrue(
                Files.exists(execucao.diretorio()));

        assertTrue(
                Files.isDirectory(execucao.diretorio()));
    }

    @Test
    void devePrepararOsCaminhosDaExecucao() {
        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        assertEquals(
                execucao.diretorio()
                        .resolve("esperados.csv"),
                execucao.esperados());

        assertEquals(
                execucao.diretorio()
                        .resolve("recebidos.csv"),
                execucao.recebidos());

        assertEquals(
                execucao.diretorio()
                        .resolve("relatorio.txt"),
                execucao.relatorio());

        assertEquals(
                execucao.diretorio()
                        .resolve("resultado.tsv"),
                execucao.resultado());

        assertEquals(
                execucao.diretorio()
                        .resolve("processo.log"),
                execucao.log());
    }

    @Test
    void deveCriarDiretoriosDiferentesParaCadaExecucao() {
        DiretorioExecucaoCobol primeira =
                DiretorioExecucaoCobol.criar();

        DiretorioExecucaoCobol segunda =
                DiretorioExecucaoCobol.criar();

        assertNotEquals(
                primeira.diretorio(),
                segunda.diretorio());
    }

    @Test
    void deveCriarDiretorioComCaminhoAbsoluto() {
        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        assertTrue(
                execucao.diretorio().isAbsolute());
    }

    @Test
    void deveRestringirPermissoesAoUsuario()
            throws IOException {

        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        String permissoes =
                PosixFilePermissions.toString(
                        Files.getPosixFilePermissions(
                                execucao.diretorio()));

        assertEquals(
                "rwx------",
                permissoes);
    }

    @Test
    void arquivosAindaNaoDevemExistirAposCriarDiretorio() {
        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        assertFalse(Files.exists(execucao.esperados()));
        assertFalse(Files.exists(execucao.recebidos()));
        assertFalse(Files.exists(execucao.relatorio()));
        assertFalse(Files.exists(execucao.resultado()));
        assertFalse(Files.exists(execucao.log()));
    }
}
