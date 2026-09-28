package br.com.brunolopes.conciliacao.aplicacao;

import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;

public record ResultadoServicoConciliacao(
        Long id,
        StatusExecucaoConciliacao status,
        ResultadoConciliacao resultado
) {

    public ResultadoServicoConciliacao {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Identificador da conciliacao e obrigatorio.");
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "Status da conciliacao e obrigatorio.");
        }

        if (resultado == null) {
            throw new IllegalArgumentException(
                    "Resultado da conciliacao e obrigatorio.");
        }
    }
}
