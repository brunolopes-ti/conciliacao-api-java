package br.com.brunolopes.conciliacao.integracao;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorResultadoTsvTests {

    @Test
    void deveAceitarResultadoConsistente() {
        List<String> linhas = List.of(
                "VERSAO\t1",
                "DETALHE\tP001\t100.00\t100.00\t0.00"
                        + "\tCONFERIDO\t1",
                "DETALHE\tP002\t200.00\t\t"
                        + "\tSEM_RECEBIMENTO\t0",
                "DETALHE\tP999\t\t50.00\t"
                        + "\tSEM_PREVISAO\t1",
                "RESUMO\t1\t0\t0\t0\t1\t1"
                        + "\t300.00\t150.00\t-150.00"
        );

        assertDoesNotThrow(
                () -> InterpretadorResultadoTsv.interpretar(linhas));
    }

    @Test
    void deveRejeitarContadorConferidosIncorreto() {
        List<String> linhas = List.of(
                "VERSAO\t1",
                "DETALHE\tP001\t100.00\t100.00\t0.00"
                        + "\tCONFERIDO\t1",
                "RESUMO\t0\t0\t0\t0\t0\t0"
                        + "\t100.00\t100.00\t0.00"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> InterpretadorResultadoTsv.interpretar(linhas));
    }

    @Test
    void deveRejeitarContadorSemPrevisaoIncorreto() {
        List<String> linhas = List.of(
                "VERSAO\t1",
                "DETALHE\tP999\t\t50.00\t"
                        + "\tSEM_PREVISAO\t1",
                "RESUMO\t0\t0\t0\t0\t0\t0"
                        + "\t0.00\t50.00\t50.00"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> InterpretadorResultadoTsv.interpretar(linhas));
    }

    @Test
    void deveRejeitarTotalEsperadoIncorreto() {
        List<String> linhas = List.of(
                "VERSAO\t1",
                "DETALHE\tP001\t100.00\t100.00\t0.00"
                        + "\tCONFERIDO\t1",
                "RESUMO\t1\t0\t0\t0\t0\t0"
                        + "\t200.00\t100.00\t-100.00"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> InterpretadorResultadoTsv.interpretar(linhas));
    }

    @Test
    void deveRejeitarSaldoGlobalIncorreto() {
        List<String> linhas = List.of(
                "VERSAO\t1",
                "DETALHE\tP001\t100.00\t90.00\t-10.00"
                        + "\tABAIXO_DO_ESPERADO\t1",
                "RESUMO\t0\t0\t1\t0\t0\t0"
                        + "\t100.00\t90.00\t10.00"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> InterpretadorResultadoTsv.interpretar(linhas));
    }

    @Test
    void deveRejeitarDiferencaQueNaoCorrespondeAosValores() {
        List<String> linhas = List.of(
                "VERSAO\t1",
                "DETALHE\tP001\t100.00\t80.00\t-10.00"
                        + "\tABAIXO_DO_ESPERADO\t1",
                "RESUMO\t0\t0\t1\t0\t0\t0"
                        + "\t100.00\t80.00\t-20.00"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> InterpretadorResultadoTsv.interpretar(linhas));
    }

    @Test
    void deveAceitarDuplicadoSemTentarSomarPagamentosExtras() {
        List<String> linhas = List.of(
                "VERSAO\t1",
                "DETALHE\tP001\t100.00\t90.00\t-10.00"
                        + "\tDUPLICADO\t2",
                "RESUMO\t0\t0\t0\t1\t0\t0"
                        + "\t100.00\t150.00\t50.00"
        );

        assertDoesNotThrow(
                () -> InterpretadorResultadoTsv.interpretar(linhas));
    }
}
