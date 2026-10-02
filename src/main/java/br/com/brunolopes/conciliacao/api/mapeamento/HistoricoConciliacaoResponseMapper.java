package br.com.brunolopes.conciliacao.api.mapeamento;

import java.util.List;

import br.com.brunolopes.conciliacao.api.dto.ItemHistoricoConciliacaoResponse;
import br.com.brunolopes.conciliacao.api.dto.PaginaHistoricoConciliacaoResponse;
import br.com.brunolopes.conciliacao.api.dto.ResumoConciliacaoResponse;
import br.com.brunolopes.conciliacao.aplicacao.PaginaHistoricoConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.consulta.ItemHistoricoConciliacao;

public final class HistoricoConciliacaoResponseMapper {

    private HistoricoConciliacaoResponseMapper() {
    }

    public static PaginaHistoricoConciliacaoResponse mapear(
            PaginaHistoricoConciliacao pagina
    ) {
        if (pagina == null) {
            throw new IllegalArgumentException(
                    "Pagina de historico e obrigatoria."
            );
        }

        List<ItemHistoricoConciliacaoResponse> itens =
                pagina.itens()
                        .stream()
                        .map(
                                HistoricoConciliacaoResponseMapper
                                        ::mapearItem
                        )
                        .toList();

        return new PaginaHistoricoConciliacaoResponse(
                pagina.pagina(),
                pagina.tamanho(),
                pagina.totalElementos(),
                pagina.totalPaginas(),
                itens
        );
    }

    private static ItemHistoricoConciliacaoResponse mapearItem(
            ItemHistoricoConciliacao item
    ) {
        return new ItemHistoricoConciliacaoResponse(
                item.id(),
                item.status().name(),
                item.criadaEm(),
                item.iniciadaEm(),
                item.finalizadaEm(),
                item.resultadoVersao(),
                item.erroCodigo(),
                mapearResumo(
                        item.resumo()
                )
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
