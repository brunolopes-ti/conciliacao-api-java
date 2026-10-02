package br.com.brunolopes.conciliacao.aplicacao.excecao;

public class RelatorioIndisponivelException
        extends RuntimeException {

    public RelatorioIndisponivelException() {
        super(
                "Relatorio disponivel somente para conciliacoes concluidas."
        );
    }
}
