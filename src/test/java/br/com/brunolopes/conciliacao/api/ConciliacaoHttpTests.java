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
    void deveRejeitarCorpoInesperado() throws Exception {
        mockMvc.perform(
                        post("/api/conciliacoes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"arquivo\":\"teste.csv\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.codigo")
                                .value("REQUISICAO_INVALIDA"))
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "A requisicao nao aceita corpo."));
    }

    @Test
    void deveRejeitarParametroInesperado() throws Exception {
        mockMvc.perform(
                        post("/api/conciliacoes")
                                .queryParam(
                                        "arquivo",
                                        "teste.csv"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.codigo")
                                .value("REQUISICAO_INVALIDA"))
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "A requisicao nao aceita parametros."));
    }

    @Test
    void deveRetornar409ParaConflitoDeDados() throws Exception {
        String detalheInterno =
                "/tmp/dados-internos";

        servico.definirFalha(
                new ConflitoDadosException(
                        detalheInterno));

        mockMvc.perform(
                        post("/api/conciliacoes"))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.codigo")
                                .value("CONFLITO_DADOS"))
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "O estado atual dos dados impede iniciar a conciliacao."))
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                detalheInterno))));
    }

    @Test
    void deveRetornar422ParaResultadoInvalido() throws Exception {
        String detalheInterno =
                "resultado.tsv em /tmp/execucao";

        servico.definirFalha(
                new ResultadoInvalidoException(
                        detalheInterno));

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
                                        "O resultado da conciliacao nao passou pelas validacoes."))
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                detalheInterno))));
    }

    @Test
    void deveRetornar502ParaFalhaCobol() throws Exception {
        String detalheInterno =
                "/home/usuario/bin/conciliacao exit=1";

        servico.definirFalha(
                new FalhaCobolException(
                        detalheInterno));

        mockMvc.perform(
                        post("/api/conciliacoes"))
                .andExpect(status().isBadGateway())
                .andExpect(
                        jsonPath("$.codigo")
                                .value("FALHA_COBOL"))
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "O motor COBOL nao concluiu o processamento."))
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                detalheInterno))));
    }

    @Test
    void deveRetornar504ParaTimeoutCobol() throws Exception {
        String detalheInterno =
                "timeout executando /home/usuario/conciliacao";

        servico.definirFalha(
                new TimeoutCobolException(
                        detalheInterno));

        mockMvc.perform(
                        post("/api/conciliacoes"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(
                        jsonPath("$.codigo")
                                .value("TIMEOUT_COBOL"))
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "O processamento da conciliacao excedeu o tempo permitido."))
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                detalheInterno))));
    }

    @Test
    void deveOcultarDetalhesDeErroInterno() throws Exception {
        String detalheInterno =
                "/tmp/conciliacao-segreda/resultado.tsv";

        servico.definirFalha(
                new RuntimeException(
                        detalheInterno));

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
                                                detalheInterno))));
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
