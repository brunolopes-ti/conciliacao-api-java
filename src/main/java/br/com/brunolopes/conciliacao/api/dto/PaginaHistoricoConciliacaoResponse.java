package br.com.brunolopes.conciliacao.api.dto;

import java.util.List;

public record PaginaHistoricoConciliacaoResponse(
        int pagina,
        int tamanho,
        long totalElementos,
        long totalPaginas,
        List<ItemHistoricoConciliacaoResponse> itens
) {

    public PaginaHistoricoConciliacaoResponse {
        itens = List.copyOf(itens);
    }
}
