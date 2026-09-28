package br.com.brunolopes.conciliacao.integracao;

public record SaidaProcesso(
        String conteudo,
        boolean truncada
) {

    public SaidaProcesso {
        if (conteudo == null) {
            throw new IllegalArgumentException(
                    "Conteudo da saida nao pode ser nulo.");
        }
    }
}
