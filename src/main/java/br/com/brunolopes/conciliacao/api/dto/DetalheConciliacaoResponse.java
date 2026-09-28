package br.com.brunolopes.conciliacao.api.dto;

import java.math.BigDecimal;

public record DetalheConciliacaoResponse(
        String identificador,
        BigDecimal valorEsperado,
        BigDecimal valorRecebido,
        BigDecimal diferenca,
        String status,
        int quantidadeRecebimentos
) {
}
