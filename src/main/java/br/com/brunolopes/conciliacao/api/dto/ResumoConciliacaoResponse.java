package br.com.brunolopes.conciliacao.api.dto;

import java.math.BigDecimal;

public record ResumoConciliacaoResponse(
        int conferidos,
        int acima,
        int abaixo,
        int duplicados,
        int semRecebimento,
        int semPrevisao,
        BigDecimal totalEsperado,
        BigDecimal totalRecebido,
        BigDecimal saldoGlobal
) {
}
