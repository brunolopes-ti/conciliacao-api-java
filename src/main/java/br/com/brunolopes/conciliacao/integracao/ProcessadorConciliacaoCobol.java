package br.com.brunolopes.conciliacao.integracao;

import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;

public interface ProcessadorConciliacaoCobol {

    ResultadoConciliacao processar(
            SnapshotExecucaoCobol snapshot
    );
}
