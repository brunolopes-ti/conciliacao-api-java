package br.com.brunolopes.conciliacao.modelo;

import java.math.BigDecimal;

public record DetalheConciliacao(
        String identificador,
        BigDecimal valorEsperado,
        BigDecimal valorRecebido,
        BigDecimal diferenca,
        StatusConciliacao status,
        int quantidadeRecebimentos
) {
}
