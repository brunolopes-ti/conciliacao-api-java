package br.com.brunolopes.conciliacao.aplicacao;

import java.util.List;

import br.com.brunolopes.conciliacao.modelo.consulta.ItemHistoricoConciliacao;

public record PaginaHistoricoConciliacao(
        int pagina,
        int tamanho,
        long totalElementos,
        long totalPaginas,
        List<ItemHistoricoConciliacao> itens
) {

    public PaginaHistoricoConciliacao {
        itens = List.copyOf(itens);
    }
}
