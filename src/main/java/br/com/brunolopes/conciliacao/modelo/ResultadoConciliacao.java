package br.com.brunolopes.conciliacao.modelo;

import java.util.List;

public record ResultadoConciliacao(
        int versao,
        List<DetalheConciliacao> detalhes,
        ResumoConciliacao resumo
) {
    public ResultadoConciliacao {
        detalhes = List.copyOf(detalhes);
    }
}
