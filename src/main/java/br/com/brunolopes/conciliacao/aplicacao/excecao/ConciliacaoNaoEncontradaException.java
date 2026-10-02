package br.com.brunolopes.conciliacao.aplicacao.excecao;

public class ConciliacaoNaoEncontradaException
        extends RuntimeException {

    public ConciliacaoNaoEncontradaException() {
        super(
                "Conciliacao nao encontrada."
        );
    }
}
