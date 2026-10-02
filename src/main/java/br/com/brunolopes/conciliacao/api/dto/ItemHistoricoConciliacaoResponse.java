package br.com.brunolopes.conciliacao.api.dto;

import java.time.OffsetDateTime;

public record ItemHistoricoConciliacaoResponse(
        long id,
        String status,
        OffsetDateTime criadaEm,
        OffsetDateTime iniciadaEm,
        OffsetDateTime finalizadaEm,
        Integer resultadoVersao,
        String erroCodigo,
        ResumoConciliacaoResponse resumo
) {
}
