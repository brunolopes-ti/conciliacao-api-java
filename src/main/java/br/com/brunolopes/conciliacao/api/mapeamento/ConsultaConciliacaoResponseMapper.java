package br.com.brunolopes.conciliacao.api.mapeamento;

import java.util.List;

import br.com.brunolopes.conciliacao.api.dto.ConciliacaoConsultadaResponse;
import br.com.brunolopes.conciliacao.api.dto.DetalheConciliacaoConsultadoResponse;
import br.com.brunolopes.conciliacao.api.dto.ResumoConciliacaoResponse;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.consulta.ConciliacaoConsultada;
import br.com.brunolopes.conciliacao.modelo.consulta.DetalheConciliacaoConsultado;

public final class ConsultaConciliacaoResponseMapper {

    private ConsultaConciliacaoResponseMapper() {
    }

    public static ConciliacaoConsultadaResponse mapear(
            ConciliacaoConsultada conciliacao
    ) {
        if (conciliacao == null) {
            throw new IllegalArgumentException(
                    "Conciliacao consultada e obrigatoria."
            );
        }

        List<DetalheConciliacaoConsultadoResponse> detalhes =
                conciliacao.detalhes()
                        .stream()
                        .map(
                                ConsultaConciliacaoResponseMapper
                                        ::mapearDetalhe
                        )
                        .toList();

        return new ConciliacaoConsultadaResponse(
                conciliacao.id(),
                conciliacao.status().name(),
                conciliacao.criadaEm(),
                conciliacao.iniciadaEm(),
                conciliacao.finalizadaEm(),
                conciliacao.resultadoVersao(),
                conciliacao.erroCodigo(),
                mapearResumo(
                        conciliacao.resumo()
                ),
                detalhes
        );
    }

    private static DetalheConciliacaoConsultadoResponse mapearDetalhe(
            DetalheConciliacaoConsultado detalhe
    ) {
        return new DetalheConciliacaoConsultadoResponse(
                detalhe.ordem(),
                detalhe.identificador(),
                detalhe.valorEsperado(),
                detalhe.valorRecebido(),
                detalhe.diferenca(),
                detalhe.status().name(),
                detalhe.quantidadeRecebimentos()
        );
    }

    private static ResumoConciliacaoResponse mapearResumo(
            ResumoConciliacao resumo
    ) {
        if (resumo == null) {
            return null;
        }

        return new ResumoConciliacaoResponse(
                resumo.conferidos(),
                resumo.acima(),
                resumo.abaixo(),
                resumo.duplicados(),
                resumo.semRecebimento(),
                resumo.semPrevisao(),
                resumo.totalEsperado(),
                resumo.totalRecebido(),
                resumo.saldoGlobal()
        );
    }
}
