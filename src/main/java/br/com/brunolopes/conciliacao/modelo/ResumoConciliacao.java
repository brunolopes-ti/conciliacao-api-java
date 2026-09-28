package br.com.brunolopes.conciliacao.modelo;

import java.math.BigDecimal;

public record ResumoConciliacao(
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
