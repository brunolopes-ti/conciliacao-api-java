package br.com.brunolopes.conciliacao.aplicacao.excecao;

public class EmailJaCadastradoException
        extends RuntimeException {

    public EmailJaCadastradoException(
            String mensagem
    ) {
        super(
                mensagem
        );
    }
}
