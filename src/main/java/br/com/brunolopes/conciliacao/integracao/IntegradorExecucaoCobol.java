package br.com.brunolopes.conciliacao.integracao;

import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;

public final class IntegradorExecucaoCobol {

    private final ExecutorCobol executor;

    public IntegradorExecucaoCobol(
            ExecutorCobol executor
    ) {
        if (executor == null) {
            throw new IllegalArgumentException(
                    "Executor COBOL e obrigatorio.");
        }

        this.executor = executor;
    }

    public ResultadoExecucaoCobol executar(
            DiretorioExecucaoCobol execucao
    ) {
        if (execucao == null) {
            throw new IllegalArgumentException(
                    "Diretorio da execucao COBOL e obrigatorio.");
        }

        ArgumentosExecucaoCobol argumentos =
                ArgumentosExecucaoCobol.de(
                        execucao);

        ResultadoExecucaoProcesso processo =
                executor.executar(
                        argumentos);

        CodigoSaidaCobol codigoSaida =
                interpretarCodigoSaida(
                        processo.codigoSaida());

        if (!codigoSaida.processamentoConcluido()) {
            throw new IllegalStateException(
                    "Execucao COBOL terminou com falha. Codigo: "
                    + codigoSaida.codigo());
        }

        ValidadorArquivosSaidaCobol.validar(
                execucao);

        ResultadoConciliacao resultado =
                LeitorResultadoTsv.ler(
                        execucao.diretorio(),
                        execucao.resultado());

        return new ResultadoExecucaoCobol(
                processo,
                resultado,
                execucao.relatorio());
    }

    private CodigoSaidaCobol interpretarCodigoSaida(
            int codigo
    ) {
        try {
            return CodigoSaidaCobol.de(
                    codigo);

        } catch (IllegalArgumentException erro) {
            throw new IllegalStateException(
                    "Processo COBOL retornou "
                    + "codigo de saida desconhecido: "
                    + codigo,
                    erro);
        }
    }
}
