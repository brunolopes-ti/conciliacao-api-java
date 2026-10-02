package br.com.brunolopes.conciliacao.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ConciliacaoConsultadaResponse(
        long id,
        String status,
        OffsetDateTime criadaEm,
        OffsetDateTime iniciadaEm,
        OffsetDateTime finalizadaEm,
        Integer resultadoVersao,
        String erroCodigo,
        ResumoConciliacaoResponse resumo,
        List<DetalheConciliacaoConsultadoResponse> detalhes
) {

    public ConciliacaoConsultadaResponse {
        detalhes = List.copyOf(detalhes);
    }
}
