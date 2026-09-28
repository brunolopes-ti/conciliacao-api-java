package br.com.brunolopes.conciliacao.aplicacao.excecao;

public class ConflitoDadosException extends RuntimeException {

    public ConflitoDadosException(String mensagem) {
        super(mensagem);
    }
}
