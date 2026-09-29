package br.com.brunolopes.conciliacao.integracao;

public class FalhaExecucaoCobolException
        extends IllegalStateException {

    public FalhaExecucaoCobolException(
            String mensagem
    ) {
        super(mensagem);
    }

    public FalhaExecucaoCobolException(
            String mensagem,
            Throwable causa
    ) {
        super(mensagem, causa);
    }
}
