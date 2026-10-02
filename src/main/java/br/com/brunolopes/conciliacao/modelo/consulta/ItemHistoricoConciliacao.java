package br.com.brunolopes.conciliacao.modelo.consulta;

import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;
import java.time.OffsetDateTime;

import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;

public record ItemHistoricoConciliacao(
        long id,
        StatusExecucaoConciliacao status,
        OffsetDateTime criadaEm,
        OffsetDateTime iniciadaEm,
        OffsetDateTime finalizadaEm,
        Integer resultadoVersao,
        String erroCodigo,
        ResumoConciliacao resumo
) {
}
