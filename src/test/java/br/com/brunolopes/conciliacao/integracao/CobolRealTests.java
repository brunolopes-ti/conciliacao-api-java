package br.com.brunolopes.conciliacao.integracao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "COBOL_EXECUTAVEL_TESTE", matches = ".+")
class CobolRealTests {
    @Test void deveExecutarMotorRealEConferirSnapshot() throws Exception {
        CenariosRevisao.cobolReal();
    }
}
