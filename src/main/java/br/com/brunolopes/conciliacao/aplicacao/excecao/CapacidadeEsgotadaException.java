package br.com.brunolopes.conciliacao.aplicacao.excecao;

public final class CapacidadeEsgotadaException extends RuntimeException {
    public CapacidadeEsgotadaException() {
        super("Nao ha vaga disponivel para processamento COBOL.");
    }
}
