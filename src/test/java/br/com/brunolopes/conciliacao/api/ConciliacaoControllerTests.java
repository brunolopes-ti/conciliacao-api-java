package br.com.brunolopes.conciliacao.api;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import br.com.brunolopes.conciliacao.api.dto.ConciliacaoResponse;
import br.com.brunolopes.conciliacao.aplicacao.ResultadoServicoConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.ServicoConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.StatusExecucaoConciliacao;
import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConciliacaoControllerTests {

    @Test
    void deveRetornar201ComResultadoDaConciliacao() {
        ServicoConciliacao servico =
                () -> criarResultadoServico();

        ConciliacaoController controller =
                new ConciliacaoController(servico);

        ResponseEntity<ConciliacaoResponse> resposta =
                controller.criar();

        assertEquals(
                HttpStatus.CREATED,
                resposta.getStatusCode());

        ConciliacaoResponse corpo =
                resposta.getBody();

        assertNotNull(corpo);
        assertEquals(123L, corpo.id());
        assertEquals("CONCLUIDA", corpo.status());
        assertEquals(1, corpo.detalhes().size());
        assertEquals(
                "CONFERIDO",
                corpo.detalhes().get(0).status());
    }

    private ResultadoServicoConciliacao criarResultadoServico() {
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

        return new ResultadoServicoConciliacao(
                123L,
                StatusExecucaoConciliacao.CONCLUIDA,
                resultado);
    }
}
