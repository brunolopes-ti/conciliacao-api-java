package br.com.brunolopes.conciliacao.persistencia;

import br.com.brunolopes.conciliacao.integracao.SnapshotExecucaoCobol;

public interface RepositorioSnapshotConciliacao {

    SnapshotExecucaoCobol capturarEPersistir(long conciliacaoId);
}
