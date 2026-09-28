package br.com.brunolopes.conciliacao.integracao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultadoExecucaoProcessoTests {

    @Test
    void deveIndicarSucessoQuandoCodigoForZero() {
        ResultadoExecucaoProcesso resultado =
                new ResultadoExecucaoProcesso(
                        0,
                        "Processamento concluido.");

        assertTrue(resultado.sucesso());
    }

    @Test
    void deveIndicarFalhaQuandoCodigoForDiferenteDeZero() {
        ResultadoExecucaoProcesso resultado =
                new ResultadoExecucaoProcesso(
                        1,
                        "Falha no processamento.");

        assertFalse(resultado.sucesso());
    }

    @Test
    void deveAceitarSaidaVazia() {
        ResultadoExecucaoProcesso resultado =
                new ResultadoExecucaoProcesso(
                        0,
                        "");

        assertTrue(resultado.sucesso());
    }

    @Test
    void deveRejeitarCodigoNegativo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResultadoExecucaoProcesso(
                        -1,
                        ""));
    }

    @Test
    void deveRejeitarSaidaNula() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResultadoExecucaoProcesso(
                        0,
                        null));
    }
}
