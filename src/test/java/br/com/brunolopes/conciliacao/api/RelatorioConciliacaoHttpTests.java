package br.com.brunolopes.conciliacao.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import br.com.brunolopes.conciliacao.aplicacao.PaginaHistoricoConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.ServicoConsultaConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;
import br.com.brunolopes.conciliacao.modelo.consulta.ConciliacaoConsultada;
import br.com.brunolopes.conciliacao.modelo.consulta.DetalheConciliacaoConsultado;

class RelatorioConciliacaoHttpTests {

    private MockMvc mockMvc;
    private ServicoTeste servico;

    @BeforeEach
    void preparar() {
        servico =
                new ServicoTeste();

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
    void deveGerarRelatorioTxtDeterministico()
            throws Exception {

        servico.resultado =
                Optional.of(
                        criarConcluida()
                );

        String esperado =
                "RELATORIO DE CONCILIACAO\n"
                        + "ID: 15\n"
                        + "STATUS: CONCLUIDA\n"
                        + "VERSAO_RESULTADO: 1\n"
                        + "\n"
                        + "RESUMO\n"
                        + "CONFERIDOS: 1\n"
                        + "ACIMA: 0\n"
                        + "ABAIXO: 0\n"
                        + "DUPLICADOS: 0\n"
                        + "SEM_RECEBIMENTO: 1\n"
                        + "SEM_PREVISAO: 0\n"
                        + "TOTAL_ESPERADO: 300.00\n"
                        + "TOTAL_RECEBIDO: 100.00\n"
                        + "SALDO_GLOBAL: -200.00\n"
                        + "\n"
                        + "DETALHES\n"
                        + "ORDEM | IDENTIFICADOR | VALOR_ESPERADO | "
                        + "VALOR_RECEBIDO | DIFERENCA | STATUS | "
                        + "QUANTIDADE_RECEBIMENTOS\n"
                        + "1 | P001 | 100.00 | 100.00 | 0.00 | "
                        + "CONFERIDO | 1\n"
                        + "2 | P002 | 200.00 | - | - | "
                        + "SEM_RECEBIMENTO | 0\n";

        mockMvc.perform(
                        get(
                                "/api/conciliacoes/15/relatorio"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().contentType(
                                "text/plain;charset=UTF-8"
                        )
                )
                .andExpect(
                        header().string(
                                "Content-Disposition",
                                "attachment; filename=\"conciliacao-15.txt\""
                        )
                )
                .andExpect(
                        content().string(
                                esperado
                        )
                );
    }

    @Test
    void deveRetornar409QuandoRelatorioNaoDisponivel()
            throws Exception {

        servico.resultado =
                Optional.of(
                        criarFalha()
                );

        mockMvc.perform(
                        get(
                                "/api/conciliacoes/20/relatorio"
                        )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value(
                                        "RELATORIO_INDISPONIVEL"
                                )
                );
    }

    @Test
    void deveRetornar404QuandoConciliacaoNaoExiste()
            throws Exception {

        servico.resultado =
                Optional.empty();

        mockMvc.perform(
                        get(
                                "/api/conciliacoes/999/relatorio"
                        )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value(
                                        "CONCILIACAO_NAO_ENCONTRADA"
                                )
                );
    }

    @Test
    void deveRejeitarIdInvalido()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/conciliacoes/abc/relatorio"
                        )
                )
                .andExpect(
                        status().isBadRequest()
                );
    }

    @Test
    void deveRejeitarParametrosNoRelatorio()
            throws Exception {

        servico.resultado =
                Optional.of(
                        criarConcluida()
                );

        mockMvc.perform(
                        get(
                                "/api/conciliacoes/15/relatorio"
                        )
                                .queryParam(
                                        "teste",
                                        "1"
                                )
                )
                .andExpect(
                        status().isBadRequest()
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
                List.of(
                        new DetalheConciliacaoConsultado(
                                2,
                                "P002",
                                new BigDecimal("200.00"),
                                null,
                                null,
                                StatusConciliacao.SEM_RECEBIMENTO,
                                0
                        ),
                        new DetalheConciliacaoConsultado(
                                1,
                                "P001",
                                new BigDecimal("100.00"),
                                new BigDecimal("100.00"),
                                new BigDecimal("0.00"),
                                StatusConciliacao.CONFERIDO,
                                1
                        )
                )
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

    private static class ServicoTeste
            implements ServicoConsultaConciliacao {

        private Optional<ConciliacaoConsultada> resultado =
                Optional.empty();

        @Override
        public PaginaHistoricoConciliacao listar(
                int pagina,
                int tamanho,
                StatusExecucaoConciliacao status
        ) {
            return new PaginaHistoricoConciliacao(
                    pagina,
                    tamanho,
                    0,
                    0,
                    List.of()
            );
        }

        @Override
        public ConciliacaoConsultada buscarPorId(
                long conciliacaoId
        ) {
            return resultado.orElseThrow(
                    br.com.brunolopes.conciliacao.aplicacao.excecao
                            .ConciliacaoNaoEncontradaException::new
            );
        }
    }
}
