package br.com.brunolopes.conciliacao.integracao;

import org.junit.jupiter.api.Test;

class RevisaoIntegracaoTests {
    @Test void deveConferirSnapshotValido() { CenariosRevisao.snapshotValido(); }
    @Test void deveRejeitarResultadoDivergenteDoSnapshot() throws Exception { CenariosRevisao.snapshotAdulterado(); }
    @Test void deveLimitarRelatorio() throws Exception { CenariosRevisao.limiteRelatorio(); }
    @Test void deveLimitarLeituraTsv() throws Exception { CenariosRevisao.limiteTsv(); }
    @Test void deveLimparSemSeguirLinks() throws Exception { CenariosRevisao.limpeza(); }
    @Test void deveFecharEntradaPadrao() throws Exception { CenariosRevisao.stdin(); }
    @Test void deveLimitarSaidaSemBloquear() throws Exception { CenariosRevisao.saidaExcessiva(); }
    @Test void deveEncerrarFilhoNaInterrupcao() throws Exception { CenariosRevisao.interrupcao(); }
}
