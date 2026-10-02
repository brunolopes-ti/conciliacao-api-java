package br.com.brunolopes.conciliacao.api.mapeamento;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.brunolopes.conciliacao.api.dto.ConciliacaoResponse;
import br.com.brunolopes.conciliacao.aplicacao.ResultadoServicoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;
import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConciliacaoResponseMapperTests {

    @Test
    void deveMapearResultadoDoServicoParaRespostaHttp() {
        DetalheConciliacao detalhe =
                new DetalheConciliacao(
                        "P001",
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("0.00"),
                        StatusConciliacao.CONFERIDO,
                        1);

        ResumoConciliacao resumo =
                new ResumoConciliacao(
                        1,
                        0,
                        0,
                        0,
                        0,
                        0,
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("0.00"));

        ResultadoConciliacao resultado =
                new ResultadoConciliacao(
                        1,
                        List.of(detalhe),
                        resumo);

        ResultadoServicoConciliacao resultadoServico =
                new ResultadoServicoConciliacao(
                        123L,
                        StatusExecucaoConciliacao.CONCLUIDA,
                        resultado);

        ConciliacaoResponse response =
                ConciliacaoResponseMapper.mapear(
                        resultadoServico);

        assertEquals(123L, response.id());
        assertEquals("CONCLUIDA", response.status());

        assertEquals(
                new BigDecimal("100.00"),
                response.resumo().totalEsperado());

        assertEquals(1, response.detalhes().size());
        assertEquals(
                "P001",
                response.detalhes().get(0).identificador());

        assertEquals(
                "CONFERIDO",
                response.detalhes().get(0).status());

        assertEquals(
                1,
                response.detalhes()
                        .get(0)
                        .quantidadeRecebimentos());
    }

    @Test
    void deveRejeitarResultadoNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ConciliacaoResponseMapper.mapear(null));
    }
}
