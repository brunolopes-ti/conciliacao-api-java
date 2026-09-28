package br.com.brunolopes.conciliacao.integracao;

public enum CodigoSaidaCobol {

    PROCESSAMENTO_CONCLUIDO(0),

    FALHA_ENTRADA_VALIDACAO_OPERACAO(1),

    ERRO_USO_CONFIGURACAO_ARGUMENTOS(2);

    private final int codigo;

    CodigoSaidaCobol(int codigo) {
        this.codigo = codigo;
    }

    public int codigo() {
        return codigo;
    }

    public boolean processamentoConcluido() {
        return this == PROCESSAMENTO_CONCLUIDO;
    }

    public static CodigoSaidaCobol de(
            int codigo
    ) {
        return switch (codigo) {
            case 0 ->
                    PROCESSAMENTO_CONCLUIDO;

            case 1 ->
                    FALHA_ENTRADA_VALIDACAO_OPERACAO;

            case 2 ->
                    ERRO_USO_CONFIGURACAO_ARGUMENTOS;

            default ->
                    throw new IllegalArgumentException(
                            "Codigo de saida COBOL desconhecido: "
                            + codigo);
        };
    }
}
