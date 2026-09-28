package br.com.brunolopes.conciliacao.integracao;

import org.junit.jupiter.api.Test;

class ExecutorCobolColetaSaidaTimeoutTests {
    @Test
    void naoDeveEsperarIndefinidamenteQuandoFilhoMantiverSaidaAberta() throws Exception {
        // A coleta termina com o pai; nao depende do EOF herdado por um orfao.
        CenariosRevisao.orfao();
    }
}
