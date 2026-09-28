package br.com.brunolopes.conciliacao.integracao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodigoSaidaCobolTests {

    @Test
    void deveInterpretarCodigoZeroComoProcessamentoConcluido() {
        assertEquals(
                CodigoSaidaCobol.PROCESSAMENTO_CONCLUIDO,
                CodigoSaidaCobol.de(0));
    }

    @Test
    void deveInterpretarCodigoUmComoFalhaDeProcessamento() {
        assertEquals(
                CodigoSaidaCobol.FALHA_ENTRADA_VALIDACAO_OPERACAO,
                CodigoSaidaCobol.de(1));
    }

    @Test
    void deveInterpretarCodigoDoisComoErroDeArgumentos() {
        assertEquals(
                CodigoSaidaCobol.ERRO_USO_CONFIGURACAO_ARGUMENTOS,
                CodigoSaidaCobol.de(2));
    }

    @Test
    void somenteCodigoZeroDeveIndicarProcessamentoConcluido() {
        assertTrue(
                CodigoSaidaCobol
                        .PROCESSAMENTO_CONCLUIDO
                        .processamentoConcluido());

        assertFalse(
                CodigoSaidaCobol
                        .FALHA_ENTRADA_VALIDACAO_OPERACAO
                        .processamentoConcluido());

        assertFalse(
                CodigoSaidaCobol
                        .ERRO_USO_CONFIGURACAO_ARGUMENTOS
                        .processamentoConcluido());
    }

    @Test
    void deveRejeitarCodigoDesconhecido() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CodigoSaidaCobol.de(99));
    }
}
