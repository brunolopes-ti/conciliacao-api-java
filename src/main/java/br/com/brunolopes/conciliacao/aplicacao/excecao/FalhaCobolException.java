package br.com.brunolopes.conciliacao.aplicacao.excecao;

public class FalhaCobolException extends RuntimeException {

    public FalhaCobolException(String mensagem) {
        super(mensagem);
    }

    public FalhaCobolException(
            String mensagem,
            Throwable causa
    ) {
        super(mensagem, causa);
    }
}
