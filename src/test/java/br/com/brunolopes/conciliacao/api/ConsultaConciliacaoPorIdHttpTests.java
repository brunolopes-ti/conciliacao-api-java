package br.com.brunolopes.conciliacao.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.brunolopes.conciliacao.api.erro.TratadorGlobalExcecoes;
import br.com.brunolopes.conciliacao.aplicacao.ServicoConsultaConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.ServicoConsultaConciliacaoReal;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;
import br.com.brunolopes.conciliacao.modelo.consulta.ConciliacaoConsultada;
import br.com.brunolopes.conciliacao.modelo.consulta.DetalheConciliacaoConsultado;
import br.com.brunolopes.conciliacao.modelo.consulta.ItemHistoricoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioConsultaConciliacao;

class ConsultaConciliacaoPorIdHttpTests {

    private MockMvc mockMvc;
    private RepositorioFake repositorio;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioFake();

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
                        .standaloneSetup(controller)
                        .setControllerAdvice(
                                new TratadorGlobalExcecoes()
                        )
                        .build();
    }

    @Test
    void deveBuscarConciliacaoConcluidaPorId() throws Exception {
        repositorio.resultado =
                Optional.of(
                        criarConcluida()
                );

        mockMvc.perform(
                        get("/api/conciliacoes/15")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(15)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CONCLUIDA")
                )
                .andExpect(
                        jsonPath("$.resultadoVersao")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.resumo.conferidos")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.detalhes.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.detalhes[0].ordem")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.detalhes[0].identificador")
                                .value("P001")
                )
                .andExpect(
                        jsonPath("$.detalhes[1].ordem")
                                .value(2)
                );
    }

    @Test
    void deveBuscarConciliacaoFalhaSemResultado() throws Exception {
        repositorio.resultado =
                Optional.of(
                        criarFalha()
                );

        mockMvc.perform(
                        get("/api/conciliacoes/20")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("FALHOU")
                )
                .andExpect(
                        jsonPath("$.erroCodigo")
                                .value("ERRO_TESTE")
                )
                .andExpect(
                        jsonPath("$.resumo")
                                .value((Object) null)
                )
                .andExpect(
                        jsonPath("$.detalhes.length()")
                                .value(0)
                );
    }

    @Test
    void deveRetornar404QuandoConciliacaoNaoExiste()
            throws Exception {

        repositorio.resultado =
                Optional.empty();

        mockMvc.perform(
                        get("/api/conciliacoes/999")
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value(
                                        "CONCILIACAO_NAO_ENCONTRADA"
                                )
                )
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Conciliacao nao encontrada."
                                )
                );
    }

    @Test
    void deveRejeitarIdZero() throws Exception {
        mockMvc.perform(
                        get("/api/conciliacoes/0")
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
    void deveRejeitarIdNaoNumerico() throws Exception {
        mockMvc.perform(
                        get("/api/conciliacoes/abc")
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
    void deveRejeitarParametroNaConsultaPorId()
            throws Exception {

        mockMvc.perform(
                        get("/api/conciliacoes/15")
                                .queryParam(
                                        "teste",
                                        "1"
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

    private ConciliacaoConsultada criarConcluida() {
        ResumoConciliacao resumo =
                new ResumoConciliacao(
                        1,
                        0,
                        0,
                        0,
                        1,
                        0,
                        new BigDecimal("300.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("-200.00")
                );

        List<DetalheConciliacaoConsultado> detalhes =
                List.of(
                        new DetalheConciliacaoConsultado(
                                1,
                                "P001",
                                new BigDecimal("100.00"),
                                new BigDecimal("100.00"),
                                new BigDecimal("0.00"),
                                StatusConciliacao.CONFERIDO,
                                1
                        ),
                        new DetalheConciliacaoConsultado(
                                2,
                                "P002",
                                new BigDecimal("200.00"),
                                null,
                                null,
                                StatusConciliacao.SEM_RECEBIMENTO,
                                0
                        )
                );

        return new ConciliacaoConsultada(
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
                resumo,
                detalhes
        );
    }

    private ConciliacaoConsultada criarFalha() {
        return new ConciliacaoConsultada(
                20,
                StatusExecucaoConciliacao.FALHOU,
                OffsetDateTime.parse(
                        "2026-10-02T12:00:00Z"
                ),
                OffsetDateTime.parse(
                        "2026-10-02T12:00:01Z"
                ),
                OffsetDateTime.parse(
                        "2026-10-02T12:00:02Z"
                ),
                null,
                "ERRO_TESTE",
                null,
                List.of()
        );
    }

    private static class RepositorioFake
            implements RepositorioConsultaConciliacao {

        private Optional<ConciliacaoConsultada> resultado =
                Optional.empty();

        @Override
        public List<ItemHistoricoConciliacao> listar(
                int limite,
                long deslocamento,
                StatusExecucaoConciliacao status
        ) {
            return List.of();
        }

        @Override
        public long contar(
                StatusExecucaoConciliacao status
        ) {
            return 0;
        }

        @Override
        public Optional<ConciliacaoConsultada> buscarPorId(
                long conciliacaoId
        ) {
            return resultado;
        }
    }
}
