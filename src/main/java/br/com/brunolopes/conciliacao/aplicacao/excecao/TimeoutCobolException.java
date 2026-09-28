package br.com.brunolopes.conciliacao.aplicacao.excecao;

public class TimeoutCobolException extends RuntimeException {

    public TimeoutCobolException(String mensagem) {
        super(mensagem);
    }

    public TimeoutCobolException(
            String mensagem,
            Throwable causa
    ) {
        super(mensagem, causa);
    }
}
