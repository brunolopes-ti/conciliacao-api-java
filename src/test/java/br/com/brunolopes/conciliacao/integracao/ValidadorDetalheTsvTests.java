package br.com.brunolopes.conciliacao.integracao;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorDetalheTsvTests {

    @Test
    void deveAceitarConferidoValido() {
        DetalheConciliacao detalhe =
                new DetalheConciliacao(
                        "P001",
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("0.00"),
                        StatusConciliacao.CONFERIDO,
                        1);

        assertDoesNotThrow(
                () -> ValidadorDetalheTsv.validar(detalhe));
    }

    @Test
    void deveRejeitarConferidoComDiferenca() {
        DetalheConciliacao detalhe =
                new DetalheConciliacao(
                        "P001",
                        new BigDecimal("100.00"),
                        new BigDecimal("90.00"),
                        new BigDecimal("-10.00"),
                        StatusConciliacao.CONFERIDO,
                        1);

        assertThrows(
                IllegalArgumentException.class,
                () -> ValidadorDetalheTsv.validar(detalhe));
    }

    @Test
    void deveRejeitarAcimaComDiferencaNegativa() {
        DetalheConciliacao detalhe =
                new DetalheConciliacao(
                        "P001",
                        new BigDecimal("100.00"),
                        new BigDecimal("110.00"),
                        new BigDecimal("-10.00"),
                        StatusConciliacao.ACIMA_DO_ESPERADO,
                        1);

        assertThrows(
                IllegalArgumentException.class,
                () -> ValidadorDetalheTsv.validar(detalhe));
    }

    @Test
    void deveRejeitarAbaixoComDiferencaPositiva() {
        DetalheConciliacao detalhe =
                new DetalheConciliacao(
                        "P001",
                        new BigDecimal("100.00"),
                        new BigDecimal("90.00"),
                        new BigDecimal("10.00"),
                        StatusConciliacao.ABAIXO_DO_ESPERADO,
                        1);

        assertThrows(
                IllegalArgumentException.class,
                () -> ValidadorDetalheTsv.validar(detalhe));
    }

    @Test
    void deveRejeitarDuplicadoComApenasUmRecebimento() {
        DetalheConciliacao detalhe =
                new DetalheConciliacao(
                        "P001",
                        new BigDecimal("100.00"),
                        new BigDecimal("90.00"),
                        new BigDecimal("-10.00"),
                        StatusConciliacao.DUPLICADO,
                        1);

        assertThrows(
                IllegalArgumentException.class,
                () -> ValidadorDetalheTsv.validar(detalhe));
    }

    @Test
    void deveAceitarSemRecebimentoValido() {
        DetalheConciliacao detalhe =
                new DetalheConciliacao(
                        "P002",
                        new BigDecimal("200.00"),
                        null,
                        null,
                        StatusConciliacao.SEM_RECEBIMENTO,
                        0);

        assertDoesNotThrow(
                () -> ValidadorDetalheTsv.validar(detalhe));
    }

    @Test
    void deveRejeitarSemRecebimentoComValorRecebido() {
        DetalheConciliacao detalhe =
                new DetalheConciliacao(
                        "P002",
                        new BigDecimal("200.00"),
                        new BigDecimal("10.00"),
                        null,
                        StatusConciliacao.SEM_RECEBIMENTO,
                        0);

        assertThrows(
                IllegalArgumentException.class,
                () -> ValidadorDetalheTsv.validar(detalhe));
    }

    @Test
    void deveAceitarSemPrevisaoValido() {
        DetalheConciliacao detalhe =
                new DetalheConciliacao(
                        "P999",
                        null,
                        new BigDecimal("50.00"),
                        null,
                        StatusConciliacao.SEM_PREVISAO,
                        1);

        assertDoesNotThrow(
                () -> ValidadorDetalheTsv.validar(detalhe));
    }

    @Test
    void deveRejeitarSemPrevisaoComValorEsperado() {
        DetalheConciliacao detalhe =
                new DetalheConciliacao(
                        "P999",
                        new BigDecimal("50.00"),
                        new BigDecimal("50.00"),
                        null,
                        StatusConciliacao.SEM_PREVISAO,
                        1);

        assertThrows(
                IllegalArgumentException.class,
                () -> ValidadorDetalheTsv.validar(detalhe));
    }

    @Test
    void interpretadorDeveAplicarValidacaoDoDetalhe() {
        List<String> linhas = List.of(
                "VERSAO\t1",
                "DETALHE\tP001\t100.00\t90.00\t-10.00"
                        + "\tCONFERIDO\t1",
                "RESUMO\t1\t0\t0\t0\t0\t0"
                        + "\t100.00\t90.00\t-10.00"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> InterpretadorResultadoTsv.interpretar(linhas));
    }
}
