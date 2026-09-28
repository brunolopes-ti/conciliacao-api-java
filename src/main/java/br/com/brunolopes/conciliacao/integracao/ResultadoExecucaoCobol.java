package br.com.brunolopes.conciliacao.integracao;

import java.nio.file.Path;

import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;

public record ResultadoExecucaoCobol(
        ResultadoExecucaoProcesso processo,
        ResultadoConciliacao resultado,
        Path relatorio
) {

    public ResultadoExecucaoCobol {
        if (processo == null) {
            throw new IllegalArgumentException(
                    "Resultado do processo e obrigatorio.");
        }

        if (resultado == null) {
            throw new IllegalArgumentException(
                    "Resultado da conciliacao e obrigatorio.");
        }

        if (relatorio == null) {
            throw new IllegalArgumentException(
                    "Caminho do relatorio e obrigatorio.");
        }
    }
}
