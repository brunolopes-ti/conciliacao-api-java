package br.com.brunolopes.conciliacao.api.mapeamento;

import java.util.List;

import br.com.brunolopes.conciliacao.api.dto.ConciliacaoResponse;
import br.com.brunolopes.conciliacao.api.dto.DetalheConciliacaoResponse;
import br.com.brunolopes.conciliacao.api.dto.ResumoConciliacaoResponse;
import br.com.brunolopes.conciliacao.aplicacao.ResultadoServicoConciliacao;
import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;

public final class ConciliacaoResponseMapper {

    private ConciliacaoResponseMapper() {
    }

    public static ConciliacaoResponse mapear(
            ResultadoServicoConciliacao resultadoServico
    ) {
        if (resultadoServico == null) {
            throw new IllegalArgumentException(
                    "Resultado do servico e obrigatorio.");
        }

        ResumoConciliacao resumo =
                resultadoServico.resultado().resumo();

        List<DetalheConciliacaoResponse> detalhes =
                resultadoServico.resultado()
                        .detalhes()
                        .stream()
                        .map(ConciliacaoResponseMapper::mapearDetalhe)
                        .toList();

        ResumoConciliacaoResponse resumoResponse =
                new ResumoConciliacaoResponse(
                        resumo.conferidos(),
                        resumo.acima(),
                        resumo.abaixo(),
                        resumo.duplicados(),
                        resumo.semRecebimento(),
                        resumo.semPrevisao(),
                        resumo.totalEsperado(),
                        resumo.totalRecebido(),
                        resumo.saldoGlobal());

        return new ConciliacaoResponse(
                resultadoServico.id(),
                resultadoServico.status().name(),
                resumoResponse,
                detalhes);
    }

    private static DetalheConciliacaoResponse mapearDetalhe(
            DetalheConciliacao detalhe
    ) {
        return new DetalheConciliacaoResponse(
                detalhe.identificador(),
                detalhe.valorEsperado(),
                detalhe.valorRecebido(),
                detalhe.diferenca(),
                detalhe.status().name(),
                detalhe.quantidadeRecebimentos());
    }
}
