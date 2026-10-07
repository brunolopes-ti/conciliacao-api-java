package br.com.brunolopes.conciliacao.aplicacao;

import br.com.brunolopes.conciliacao.persistencia.RepositorioExecucaoConciliacao;

/** Marca como falha execucoes antigas que permaneceram EM_PROCESSAMENTO. */
public final class RecuperadorExecucoesAbandonadas {

    private final RepositorioExecucaoConciliacao repositorio;
    private final long idadeMinimaMs;

    public RecuperadorExecucoesAbandonadas(
            RepositorioExecucaoConciliacao repositorio,
            long idadeMinimaMs
    ) {
        if (repositorio == null) {
            throw new IllegalArgumentException(
                    "Repositorio de execucoes e obrigatorio."
            );
        }

        if (idadeMinimaMs <= 0) {
            throw new IllegalArgumentException(
                    "Idade minima deve ser maior que zero."
            );
        }

        this.repositorio = repositorio;
        this.idadeMinimaMs = idadeMinimaMs;
    }

    public int recuperar() {
        return repositorio.falharAbandonadas(
                idadeMinimaMs
        );
    }
}
