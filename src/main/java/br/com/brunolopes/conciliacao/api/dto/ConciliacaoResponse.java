package br.com.brunolopes.conciliacao.api.dto;

import java.util.List;

public record ConciliacaoResponse(
        Long id,
        String status,
        ResumoConciliacaoResponse resumo,
        List<DetalheConciliacaoResponse> detalhes
) {

    public ConciliacaoResponse {
        detalhes = List.copyOf(detalhes);
    }
}
