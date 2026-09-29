package br.com.brunolopes.conciliacao.integracao;

public class TimeoutExecucaoCobolException
        extends IllegalStateException {

    public TimeoutExecucaoCobolException(
            String mensagem
    ) {
        super(mensagem);
    }

    public TimeoutExecucaoCobolException(
            String mensagem,
            Throwable causa
    ) {
        super(mensagem, causa);
    }
}
