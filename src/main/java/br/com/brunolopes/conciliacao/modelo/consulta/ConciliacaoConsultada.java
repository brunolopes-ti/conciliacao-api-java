package br.com.brunolopes.conciliacao.modelo.consulta;

import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;
import java.time.OffsetDateTime;
import java.util.List;

import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;

public record ConciliacaoConsultada(
        long id,
        StatusExecucaoConciliacao status,
        OffsetDateTime criadaEm,
        OffsetDateTime iniciadaEm,
        OffsetDateTime finalizadaEm,
        Integer resultadoVersao,
        String erroCodigo,
        ResumoConciliacao resumo,
        List<DetalheConciliacaoConsultado> detalhes
) {

    public ConciliacaoConsultada {
        detalhes = List.copyOf(detalhes);
    }
}
