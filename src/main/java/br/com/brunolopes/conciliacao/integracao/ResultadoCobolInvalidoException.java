package br.com.brunolopes.conciliacao.integracao;

public class ResultadoCobolInvalidoException
        extends IllegalArgumentException {

    public ResultadoCobolInvalidoException(
            String mensagem
    ) {
        super(mensagem);
    }

    public ResultadoCobolInvalidoException(
            String mensagem,
            Throwable causa
    ) {
        super(mensagem, causa);
    }
}
