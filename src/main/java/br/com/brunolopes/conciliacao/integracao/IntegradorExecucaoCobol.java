package br.com.brunolopes.conciliacao.integracao;

import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;

public final class IntegradorExecucaoCobol {

    private final ExecutorCobol executor;

    public IntegradorExecucaoCobol(
            ExecutorCobol executor
    ) {
        if (executor == null) {
            throw new IllegalArgumentException(
                    "Executor COBOL e obrigatorio."
            );
        }

        this.executor = executor;
    }

    public ResultadoExecucaoCobol executar(
            DiretorioExecucaoCobol execucao
    ) {
        if (execucao == null) {
            throw new IllegalArgumentException(
                    "Diretorio da execucao COBOL e obrigatorio."
            );
        }

        ArgumentosExecucaoCobol argumentos =
                ArgumentosExecucaoCobol.de(
                        execucao
                );

        ResultadoExecucaoProcesso processo;

        try {
            processo =
                    executor.executar(
                            argumentos
                    );

        } catch (TimeoutExecucaoCobolException erro) {
            throw erro;

        } catch (RuntimeException erro) {
            throw new FalhaExecucaoCobolException(
                    "Falha ao executar o motor COBOL.",
                    erro
            );
        }

        CodigoSaidaCobol codigoSaida;
        try {
            codigoSaida = interpretarCodigoSaida(processo.codigoSaida());
        } catch (FalhaExecucaoCobolException erro) {
            throw new FalhaExecucaoCobolException(erro.getMessage(), erro,
                    processo.saidaProcesso());
        }

        if (!codigoSaida.processamentoConcluido()) {
            throw new FalhaExecucaoCobolException(
                    "Execucao COBOL terminou com falha. Codigo: "
                            + codigoSaida.codigo(),
                    null, processo.saidaProcesso()
            );
        }

        ResultadoConciliacao resultado;

        try {
            ValidadorArquivosSaidaCobol.validar(
                    execucao
            );

            resultado =
                    LeitorResultadoTsv.ler(
                            execucao.diretorio(),
                            execucao.resultado()
                    );

        } catch (RuntimeException erro) {
            throw new ResultadoCobolInvalidoException(
                    "Saidas produzidas pelo COBOL sao invalidas.",
                    erro
            );
        }

        return new ResultadoExecucaoCobol(
                processo,
                resultado,
                execucao.relatorio()
        );
    }

    public ResultadoExecucaoCobol executar(
            DiretorioExecucaoCobol execucao,
            SnapshotExecucaoCobol snapshot
    ) {
        if (snapshot == null) {
            throw new IllegalArgumentException(
                    "Snapshot e obrigatorio."
            );
        }

        ResultadoExecucaoCobol resultado =
                executar(
                        execucao
                );

        try {
            ValidadorSnapshotCobol.validar(
                    snapshot,
                    resultado.resultado()
            );

        } catch (RuntimeException erro) {
            throw new ResultadoCobolInvalidoException(
                    "Resultado COBOL diverge do snapshot de entrada.",
                    erro
            );
        }

        return resultado;
    }

    private CodigoSaidaCobol interpretarCodigoSaida(
            int codigo
    ) {
        try {
            return CodigoSaidaCobol.de(
                    codigo
            );

        } catch (IllegalArgumentException erro) {
            throw new FalhaExecucaoCobolException(
                    "Processo COBOL retornou "
                            + "codigo de saida desconhecido: "
                            + codigo,
                    erro
            );
        }
    }
}
