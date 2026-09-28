package br.com.brunolopes.conciliacao.api;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.brunolopes.conciliacao.api.erro.TratadorGlobalExcecoes;
import br.com.brunolopes.conciliacao.aplicacao.ResultadoServicoConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.ServicoConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.StatusExecucaoConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.excecao.ConflitoDadosException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.FalhaCobolException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.ResultadoInvalidoException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.TimeoutCobolException;
import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConciliacaoHttpTests {

    private MockMvc mockMvc;

    private ServicoConciliacaoTeste servico;

    @BeforeEach
    void preparar() {
        servico =
                new ServicoConciliacaoTeste();

        ConciliacaoController controller =
                new ConciliacaoController(
                        servico);

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .setControllerAdvice(
                                new TratadorGlobalExcecoes())
                        .build();
    }

    @Test
    void deveCriarConciliacaoERetornar201() throws Exception {
        mockMvc.perform(
                        post("/api/conciliacoes"))
                .andExpect(status().isCreated())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(
                        jsonPath("$.status")
                                .value("CONCLUIDA"))
                .andExpect(
                        jsonPath("$.resumo.conferidos")
                                .value(1))
                .andExpect(
                        jsonPath("$.resumo.totalEsperado")
                                .value(100.00))
                .andExpect(
                        jsonPath("$.resumo.totalRecebido")
                                .value(100.00))
                .andExpect(
                        jsonPath("$.resumo.saldoGlobal")
                                .value(0.00))
                .andExpect(
                        jsonPath("$.detalhes.length()")
                                .value(1))
                .andExpect(
                        jsonPath(
                                "$.detalhes[0].identificador")
                                .value("P001"))
                .andExpect(
                        jsonPath(
                                "$.detalhes[0].status")
                                .value("CONFERIDO"))
                .andExpect(
                        jsonPath(
                                "$.detalhes[0].quantidadeRecebimentos")
                                .value(1))
                .andExpect(
                        jsonPath("$.executavel")
                                .doesNotExist())
                .andExpect(
                        jsonPath("$.relatorio")
                                .doesNotExist());
    }

    @Test
    void deveRetornar409ParaConflitoDeDados() throws Exception {
        servico.definirFalha(
                new ConflitoDadosException(
                        "Dados indisponiveis para conciliacao."));

        mockMvc.perform(
                        post("/api/conciliacoes"))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.codigo")
                                .value("CONFLITO_DADOS"))
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Dados indisponiveis para conciliacao."));
    }

    @Test
    void deveRetornar422ParaResultadoInvalido() throws Exception {
        servico.definirFalha(
                new ResultadoInvalidoException(
                        "Resultado da conciliacao invalido."));

        mockMvc.perform(
                        post("/api/conciliacoes"))
                .andExpect(
                        status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.codigo")
                                .value("RESULTADO_INVALIDO"))
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Resultado da conciliacao invalido."));
    }

    @Test
    void deveRetornar502ParaFalhaCobol() throws Exception {
        servico.definirFalha(
                new FalhaCobolException(
                        "Falha no processamento COBOL."));

        mockMvc.perform(
                        post("/api/conciliacoes"))
                .andExpect(status().isBadGateway())
                .andExpect(
                        jsonPath("$.codigo")
                                .value("FALHA_COBOL"))
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Falha no processamento COBOL."));
    }

    @Test
    void deveRetornar504ParaTimeoutCobol() throws Exception {
        servico.definirFalha(
                new TimeoutCobolException(
                        "Tempo limite do COBOL excedido."));

        mockMvc.perform(
                        post("/api/conciliacoes"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(
                        jsonPath("$.codigo")
                                .value("TIMEOUT_COBOL"))
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Tempo limite do COBOL excedido."));
    }

    @Test
    void deveOcultarDetalhesDeErroInterno() throws Exception {
        servico.definirFalha(
                new RuntimeException(
                        "/tmp/conciliacao-segreda/resultado.tsv"));

        mockMvc.perform(
                        post("/api/conciliacoes"))
                .andExpect(
                        status().isInternalServerError())
                .andExpect(
                        jsonPath("$.codigo")
                                .value("ERRO_INTERNO"))
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Ocorreu uma falha interna no processamento."))
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "/tmp/conciliacao-segreda"))));
    }

    static class ServicoConciliacaoTeste
            implements ServicoConciliacao {

        private RuntimeException falha;

        void definirFalha(
                RuntimeException falha
        ) {
            this.falha = falha;
        }

        @Override
        public ResultadoServicoConciliacao executar() {
            if (falha != null) {
                throw falha;
            }

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
}
