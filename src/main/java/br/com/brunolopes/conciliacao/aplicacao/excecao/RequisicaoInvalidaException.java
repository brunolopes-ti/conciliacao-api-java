package br.com.brunolopes.conciliacao.aplicacao.excecao;

public class RequisicaoInvalidaException extends RuntimeException {

    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
