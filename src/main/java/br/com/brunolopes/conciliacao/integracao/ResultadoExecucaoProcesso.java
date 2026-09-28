package br.com.brunolopes.conciliacao.integracao;

public record ResultadoExecucaoProcesso(
        int codigoSaida,
        String saidaProcesso,
        boolean saidaTruncada
) {

    public ResultadoExecucaoProcesso(
            int codigoSaida,
            String saidaProcesso
    ) {
        this(
                codigoSaida,
                saidaProcesso,
                false);
    }

    public ResultadoExecucaoProcesso {
        if (codigoSaida < 0) {
            throw new IllegalArgumentException(
                    "Codigo de saida nao pode ser negativo.");
        }

        if (saidaProcesso == null) {
            throw new IllegalArgumentException(
                    "Saida do processo nao pode ser nula.");
        }
    }

    public boolean sucesso() {
        return codigoSaida == 0;
    }
}
