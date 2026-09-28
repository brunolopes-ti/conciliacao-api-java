package br.com.brunolopes.conciliacao.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultadoConciliacaoTests {

    @Test
    void devePreservarDetalhesQuandoListaOriginalForAlterada() {
        var detalhe = criarDetalhe();
        var listaOriginal = new ArrayList<DetalheConciliacao>();
        listaOriginal.add(detalhe);

        var resultado = new ResultadoConciliacao(
                1, listaOriginal, criarResumo());

        listaOriginal.clear();

        assertEquals(1, resultado.detalhes().size());
        assertEquals(detalhe, resultado.detalhes().get(0));
    }

    @Test
    void deveImpedirAlteracaoDaListaPeloResultado() {
        var listaOriginal = new ArrayList<DetalheConciliacao>();
        listaOriginal.add(criarDetalhe());

        var resultado = new ResultadoConciliacao(
                1, listaOriginal, criarResumo());

        assertThrows(
                UnsupportedOperationException.class,
                () -> resultado.detalhes().clear());
    }

    private DetalheConciliacao criarDetalhe() {
        return new DetalheConciliacao(
                "P001",
                new BigDecimal("100.50"),
                new BigDecimal("90.00"),
                new BigDecimal("-10.50"),
                StatusConciliacao.ABAIXO_DO_ESPERADO,
                1);
    }

    private ResumoConciliacao criarResumo() {
        return new ResumoConciliacao(
                0, 0, 1, 0, 0, 0,
                new BigDecimal("100.50"),
                new BigDecimal("90.00"),
                new BigDecimal("-10.50"));
    }
}
