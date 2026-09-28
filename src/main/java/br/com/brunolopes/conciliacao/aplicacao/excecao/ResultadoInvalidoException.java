package br.com.brunolopes.conciliacao.aplicacao.excecao;

public class ResultadoInvalidoException extends RuntimeException {

    public ResultadoInvalidoException(String mensagem) {
        super(mensagem);
    }

    public ResultadoInvalidoException(
            String mensagem,
            Throwable causa
    ) {
        super(mensagem, causa);
    }
}
