package br.com.brunolopes.conciliacao.integracao;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterpretadorResultadoTsvTests {

    @Test
    void deveInterpretarResultadoValido() {
        List<String> linhas = List.of(
                "VERSAO\t1",
                "DETALHE\tP001\t100.50\t90.00\t-10.50"
                        + "\tABAIXO_DO_ESPERADO\t1",
                "RESUMO\t0\t0\t1\t0\t0\t0"
                        + "\t100.50\t90.00\t-10.50"
        );

        ResultadoConciliacao resultado =
                InterpretadorResultadoTsv.interpretar(linhas);

        assertEquals(1, resultado.versao());
        assertEquals(1, resultado.detalhes().size());

        assertEquals(
                "P001",
                resultado.detalhes().get(0).identificador());

        assertEquals(
                new BigDecimal("100.50"),
                resultado.detalhes().get(0).valorEsperado());

        assertEquals(
                new BigDecimal("90.00"),
                resultado.detalhes().get(0).valorRecebido());

        assertEquals(
                new BigDecimal("-10.50"),
                resultado.detalhes().get(0).diferenca());

        assertEquals(
                StatusConciliacao.ABAIXO_DO_ESPERADO,
                resultado.detalhes().get(0).status());

        assertEquals(
                new BigDecimal("-10.50"),
                resultado.resumo().saldoGlobal());
    }

    @Test
    void devePreservarCamposVaziosDoDetalhe() {
        List<String> linhas = List.of(
                "VERSAO\t1",
                "DETALHE\tP002\t200.00\t\t"
                        + "\tSEM_RECEBIMENTO\t0",
                "RESUMO\t0\t0\t0\t0\t1\t0"
                        + "\t200.00\t0.00\t-200.00"
        );

        ResultadoConciliacao resultado =
                InterpretadorResultadoTsv.interpretar(linhas);

        assertEquals(
                new BigDecimal("200.00"),
                resultado.detalhes().get(0).valorEsperado());

        assertNull(
                resultado.detalhes().get(0).valorRecebido());

        assertNull(
                resultado.detalhes().get(0).diferenca());

        assertEquals(
                StatusConciliacao.SEM_RECEBIMENTO,
                resultado.detalhes().get(0).status());
    }

    @Test
    void deveRejeitarVersaoDesconhecida() {
        List<String> linhas = List.of(
                "VERSAO\t2",
                "RESUMO\t0\t0\t0\t0\t0\t0"
                        + "\t0.00\t0.00\t0.00"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> InterpretadorResultadoTsv.interpretar(linhas));
    }

    @Test
    void deveRejeitarQuantidadeIncorretaDeCampos() {
        List<String> linhas = List.of(
                "VERSAO\t1",
                "DETALHE\tP001\t100.00",
                "RESUMO\t0\t0\t0\t0\t0\t0"
                        + "\t100.00\t0.00\t-100.00"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> InterpretadorResultadoTsv.interpretar(linhas));
    }

    @Test
    void deveRejeitarStatusDesconhecido() {
        List<String> linhas = List.of(
                "VERSAO\t1",
                "DETALHE\tP001\t100.00\t100.00\t0.00"
                        + "\tSTATUS_INVENTADO\t1",
                "RESUMO\t1\t0\t0\t0\t0\t0"
                        + "\t100.00\t100.00\t0.00"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> InterpretadorResultadoTsv.interpretar(linhas));
    }

    @Test
    void deveRejeitarResumoForaDaUltimaLinha() {
        List<String> linhas = List.of(
                "VERSAO\t1",
                "RESUMO\t0\t0\t0\t0\t0\t0"
                        + "\t0.00\t0.00\t0.00",
                "DETALHE\tP001\t100.00\t100.00\t0.00"
                        + "\tCONFERIDO\t1"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> InterpretadorResultadoTsv.interpretar(linhas));
    }
}
