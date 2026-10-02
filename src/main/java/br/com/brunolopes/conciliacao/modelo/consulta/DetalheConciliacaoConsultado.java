package br.com.brunolopes.conciliacao.modelo.consulta;

import java.math.BigDecimal;

import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

public record DetalheConciliacaoConsultado(
        int ordem,
        String identificador,
        BigDecimal valorEsperado,
        BigDecimal valorRecebido,
        BigDecimal diferenca,
        StatusConciliacao status,
        int quantidadeRecebimentos
) {
}
