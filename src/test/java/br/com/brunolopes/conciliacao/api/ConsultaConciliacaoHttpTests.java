package br.com.brunolopes.conciliacao.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.brunolopes.conciliacao.api.erro.TratadorGlobalExcecoes;
import br.com.brunolopes.conciliacao.aplicacao.ServicoConsultaConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.ServicoConsultaConciliacaoReal;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;
import br.com.brunolopes.conciliacao.modelo.consulta.ConciliacaoConsultada;
import br.com.brunolopes.conciliacao.modelo.consulta.ItemHistoricoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioConsultaConciliacao;

class ConsultaConciliacaoHttpTests {

    private MockMvc mockMvc;
    private RepositorioFake repositorio;

    @BeforeEach
    void preparar() {
        repositorio =
                new RepositorioFake();

        repositorio.total = 1;

        repositorio.itens =
                List.of(
                        criarItemConcluido()
                );

        ServicoConsultaConciliacao servico =
                new ServicoConsultaConciliacaoReal(
                        repositorio
                );

        ConsultaConciliacaoController controller =
                new ConsultaConciliacaoController(
                        servico
                );

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(
                                controller
                        )
                        .setControllerAdvice(
                                new TratadorGlobalExcecoes()
                        )
                        .build();
    }

    @Test
    void deveListarComValoresPadrao() throws Exception {
        mockMvc.perform(
                        get("/api/conciliacoes")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        jsonPath("$.pagina")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.tamanho")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.totalElementos")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.totalPaginas")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.itens.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.itens[0].id")
                                .value(15)
                )
                .andExpect(
                        jsonPath("$.itens[0].status")
                                .value("CONCLUIDA")
                )
                .andExpect(
                        jsonPath("$.itens[0].resumo.conferidos")
                                .value(1)
                );

        org.junit.jupiter.api.Assertions.assertEquals(
                20,
                repositorio.ultimoLimite
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                0,
                repositorio.ultimoDeslocamento
        );
    }

    @Test
    void deveAceitarPaginacaoEStatus() throws Exception {
        repositorio.total = 45;

        mockMvc.perform(
                        get("/api/conciliacoes")
                                .queryParam(
                                        "pagina",
                                        "2"
                                )
                                .queryParam(
                                        "tamanho",
                                        "10"
                                )
                                .queryParam(
                                        "status",
                                        "CONCLUIDA"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.pagina")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.tamanho")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.totalElementos")
                                .value(45)
                )
                .andExpect(
                        jsonPath("$.totalPaginas")
                                .value(5)
                );

        org.junit.jupiter.api.Assertions.assertEquals(
                10,
                repositorio.ultimoLimite
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                20,
                repositorio.ultimoDeslocamento
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                StatusExecucaoConciliacao.CONCLUIDA,
                repositorio.ultimoStatus
        );
    }

    @Test
    void deveRejeitarPaginaNaoNumerica() throws Exception {
        mockMvc.perform(
                        get("/api/conciliacoes")
                                .queryParam(
                                        "pagina",
                                        "abc"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value("REQUISICAO_INVALIDA")
                )
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Parametro pagina deve ser um numero inteiro."
                                )
                );
    }

    @Test
    void deveRejeitarPaginaNegativa() throws Exception {
        mockMvc.perform(
                        get("/api/conciliacoes")
                                .queryParam(
                                        "pagina",
                                        "-1"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value("REQUISICAO_INVALIDA")
                )
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Pagina nao pode ser negativa."
                                )
                );
    }

    @Test
    void deveRejeitarTamanhoZero() throws Exception {
        mockMvc.perform(
                        get("/api/conciliacoes")
                                .queryParam(
                                        "tamanho",
                                        "0"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value("REQUISICAO_INVALIDA")
                );
    }

    @Test
    void deveRejeitarTamanhoAcimaDoLimite() throws Exception {
        mockMvc.perform(
                        get("/api/conciliacoes")
                                .queryParam(
                                        "tamanho",
                                        "101"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value("REQUISICAO_INVALIDA")
                );
    }

    @Test
    void deveRejeitarStatusInvalido() throws Exception {
        mockMvc.perform(
                        get("/api/conciliacoes")
                                .queryParam(
                                        "status",
                                        "ABACAXI"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value("REQUISICAO_INVALIDA")
                )
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Status de conciliacao invalido."
                                )
                );
    }

    @Test
    void deveRejeitarStatusVazio() throws Exception {
        mockMvc.perform(
                        get("/api/conciliacoes")
                                .queryParam(
                                        "status",
                                        ""
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value("REQUISICAO_INVALIDA")
                );
    }

    @Test
    void deveRejeitarParametroDesconhecido() throws Exception {
        mockMvc.perform(
                        get("/api/conciliacoes")
                                .queryParam(
                                        "arquivo",
                                        "teste.csv"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value("REQUISICAO_INVALIDA")
                )
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Parametro nao permitido: arquivo."
                                )
                );
    }

    @Test
    void deveRejeitarParametroRepetido() throws Exception {
        mockMvc.perform(
                        get("/api/conciliacoes")
                                .queryParam(
                                        "pagina",
                                        "1",
                                        "2"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value("REQUISICAO_INVALIDA")
                )
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Parametro deve ser informado uma unica vez: pagina."
                                )
                );
    }

    private ItemHistoricoConciliacao criarItemConcluido() {
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
                        new BigDecimal("0.00")
                );

        return new ItemHistoricoConciliacao(
                15,
                StatusExecucaoConciliacao.CONCLUIDA,
                OffsetDateTime.parse(
                        "2026-10-02T12:00:00Z"
                ),
                OffsetDateTime.parse(
                        "2026-10-02T12:00:01Z"
                ),
                OffsetDateTime.parse(
                        "2026-10-02T12:00:02Z"
                ),
                1,
                null,
                resumo
        );
    }

    private static class RepositorioFake
            implements RepositorioConsultaConciliacao {

        private long total;

        private List<ItemHistoricoConciliacao> itens =
                List.of();

        private int ultimoLimite;
        private long ultimoDeslocamento;
        private StatusExecucaoConciliacao ultimoStatus;

        @Override
        public List<ItemHistoricoConciliacao> listar(
                int limite,
                long deslocamento,
                StatusExecucaoConciliacao status
        ) {
            ultimoLimite = limite;
            ultimoDeslocamento = deslocamento;
            ultimoStatus = status;

            return itens;
        }

        @Override
        public long contar(
                StatusExecucaoConciliacao status
        ) {
            ultimoStatus = status;

            return total;
        }

        @Override
        public Optional<ConciliacaoConsultada> buscarPorId(
                long conciliacaoId
        ) {
            return Optional.empty();
        }
    }
}
