package br.com.brunolopes.conciliacao.persistencia;

public class EmailUsuarioDuplicadoException
        extends RuntimeException {

    public EmailUsuarioDuplicadoException(
            String mensagem,
            Throwable causa
    ) {
        super(
                mensagem,
                causa
        );
    }
}
