package br.com.brunolopes.conciliacao.integracao;

import java.math.BigDecimal;

public final class ConversorMonetarioTsv {

    private static final BigDecimal LIMITE_INDIVIDUAL =
            new BigDecimal("99999.99");

    private static final BigDecimal LIMITE_TOTAL =
            new BigDecimal("99999990.00");

    private ConversorMonetarioTsv() {
    }

    public static BigDecimal lerValor(String texto) {
        return converter(texto, false, LIMITE_INDIVIDUAL);
    }

    public static BigDecimal lerDiferenca(String texto) {
        return converter(texto, true, LIMITE_INDIVIDUAL);
    }

    public static BigDecimal lerTotal(String texto) {
        return converter(texto, false, LIMITE_TOTAL);
    }

    public static BigDecimal lerSaldo(String texto) {
        return converter(texto, true, LIMITE_TOTAL);
    }

    private static BigDecimal converter(
            String texto,
            boolean permiteNegativo,
            BigDecimal limite
    ) {
        String formato = permiteNegativo
                ? "-?[0-9]+\\.[0-9]{2}"
                : "[0-9]+\\.[0-9]{2}";

        if (texto == null || !texto.matches(formato)) {
            throw new IllegalArgumentException(
                    "Valor monetario invalido no TSV: "
                    + "use ponto e exatamente duas casas decimais.");
        }

        BigDecimal valor = new BigDecimal(texto);

        if (texto.startsWith("-") && valor.signum() == 0) {
            throw new IllegalArgumentException(
                    "Zero nao deve possuir sinal negativo no TSV.");
        }

        if (valor.abs().compareTo(limite) > 0) {
            throw new IllegalArgumentException(
                    "Valor monetario fora do limite do TSV.");
        }

        return valor;
    }
}
