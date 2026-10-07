package br.com.brunolopes.conciliacao.integracao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class ContratoIdentificadorRevisaoTests {

    private static final String ESPACO_FIGURA = "\u2007";

    @Test
    void deveRejeitarIdentificadorCompostoSomentePorU2007() {
        assertThrows(
                IllegalArgumentException.class,
                () -> item(ESPACO_FIGURA)
        );
    }

    @Test
    void deveRejeitarU2007NasExtremidades() {
        assertThrows(
                IllegalArgumentException.class,
                () -> item(ESPACO_FIGURA + "ABC")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> item("ABC" + ESPACO_FIGURA)
        );
    }

    @Test
    void devePermitirEspacoUnicodeNoInteriorSemNormalizarIdentidade() {
        assertDoesNotThrow(
                () -> item("A" + ESPACO_FIGURA + "B")
        );
    }

    private SnapshotExecucaoCobol.Item item(
            String identificador
    ) {
        return new SnapshotExecucaoCobol(
                List.of(
                        new SnapshotExecucaoCobol.Item(
                                identificador,
                                new BigDecimal("1.00")
                        )
                ),
                List.of(
                        new SnapshotExecucaoCobol.Item(
                                "PAGAMENTO",
                                new BigDecimal("1.00")
                        )
                )
        ).cobrancas().get(0);
    }
}
