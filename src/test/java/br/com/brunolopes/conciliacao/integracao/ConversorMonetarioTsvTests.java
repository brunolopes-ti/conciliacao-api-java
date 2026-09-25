package br.com.brunolopes.conciliacao.integracao;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversorMonetarioTsvTests {

    @Test
    void deveConverterValoresESeusLimites() {
        assertEquals(
                new BigDecimal("100.50"),
                ConversorMonetarioTsv.lerValor("100.50"));

        assertEquals(
                new BigDecimal("0.00"),
                ConversorMonetarioTsv.lerValor("0.00"));

        assertEquals(
                new BigDecimal("99999.99"),
                ConversorMonetarioTsv.lerValor("99999.99"));

        assertEquals(
                new BigDecimal("99999990.00"),
                ConversorMonetarioTsv.lerTotal("99999990.00"));
    }

    @Test
    void deveAceitarDiferencaESaldoNegativos() {
        assertEquals(
                new BigDecimal("-10.50"),
                ConversorMonetarioTsv.lerDiferenca("-10.50"));

        assertEquals(
                new BigDecimal("-99999990.00"),
                ConversorMonetarioTsv.lerSaldo("-99999990.00"));
    }

    @Test
    void deveRejeitarFormatosInvalidos() {
        String[] entradas = {
                "", "100", "100.5", "100.509",
                "100,50", "+100.50", " 100.50", "100.50 ",
                "abc", "1e2", ".50", "100.", "NULL"
        };

        for (String entrada : entradas) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ConversorMonetarioTsv.lerValor(entrada),
                    "Deveria rejeitar: " + entrada);
        }
    }

    @Test
    void deveRejeitarNegativosEmValoresETotais() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ConversorMonetarioTsv.lerValor("-1.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> ConversorMonetarioTsv.lerTotal("-1.00"));
    }

    @Test
    void deveRejeitarValoresAcimaDosLimites() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ConversorMonetarioTsv.lerValor("100000.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> ConversorMonetarioTsv.lerDiferenca("-100000.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> ConversorMonetarioTsv.lerTotal("99999990.01"));

        assertThrows(
                IllegalArgumentException.class,
                () -> ConversorMonetarioTsv.lerSaldo("-99999990.01"));
    }

    @Test
    void deveRejeitarAusenciaEZeroNegativo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ConversorMonetarioTsv.lerValor(null));

        assertThrows(
                IllegalArgumentException.class,
                () -> ConversorMonetarioTsv.lerDiferenca("-0.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> ConversorMonetarioTsv.lerSaldo("-0.00"));
    }
}
