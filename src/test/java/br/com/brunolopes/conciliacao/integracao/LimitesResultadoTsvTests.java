package br.com.brunolopes.conciliacao.integracao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LimitesResultadoTsvTests {

    @Test
    void deveRejeitarMaisDeDoisMilDetalhes() {
        List<DetalheConciliacao> detalhes =
                new ArrayList<>();

        for (int indice = 0; indice < 2001; indice++) {
            detalhes.add(
                    new DetalheConciliacao(
                            "P" + indice,
                            null,
                            BigDecimal.ZERO,
                            null,
                            StatusConciliacao.SEM_PREVISAO,
                            1));
        }

        ResultadoConciliacao resultado =
                new ResultadoConciliacao(
                        1,
                        detalhes,
                        new ResumoConciliacao(
                                0,
                                0,
                                0,
                                0,
                                0,
                                2001,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO));

        assertThrows(
                IllegalArgumentException.class,
                () -> ValidadorResultadoTsv.validar(resultado));
    }
}
