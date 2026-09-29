package br.com.brunolopes.conciliacao.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import br.com.brunolopes.conciliacao.testutil.BancoTestePostgresql;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(
        named = "CONCILIACAO_TESTE_POSTGRESQL",
        matches = "true"
)
@EnabledIfEnvironmentVariable(
        named = "CONCILIACAO_COBOL_EXECUTAVEL",
        matches = ".+"
)
class ConciliacaoHttpPostgresqlCobolTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcApp;

    private JdbcTemplate jdbcAdmin;

    @DynamicPropertySource
    static void configurarAplicacao(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.autoconfigure.exclude",
                () ->
                        "org.springframework.boot.flyway.autoconfigure."
                                + "FlywayAutoConfiguration"
        );

        registry.add(
                "spring.flyway.enabled",
                () -> "false"
        );

        registry.add(
                "conciliacao.persistencia.habilitada",
                () -> "true"
        );

        registry.add(
                "conciliacao.api.execucao-habilitada",
                () -> "true"
        );

        registry.add(
                "conciliacao.cobol.executavel",
                () ->
                        variavelObrigatoriaEstatica(
                                "CONCILIACAO_COBOL_EXECUTAVEL"
                        )
        );

        registry.add(
                "conciliacao.cobol.timeout-ms",
                () -> "10000"
        );

        registry.add(
                "spring.datasource.url",
                () ->
                        variavelObrigatoriaEstatica(
                                "CONCILIACAO_DB_URL"
                        )
        );

        registry.add(
                "spring.datasource.username",
                () ->
                        variavelObrigatoriaEstatica(
                                "CONCILIACAO_DB_USER"
                        )
        );

        registry.add(
                "spring.datasource.password",
                () ->
                        variavelObrigatoriaEstatica(
                                "CONCILIACAO_DB_PASSWORD"
                        )
        );
    }

    @BeforeEach
    void preparar() {
        String urlAdmin =
                variavelObrigatoria(
                        "CONCILIACAO_FLYWAY_URL"
                );

        String usuarioAdmin =
                variavelObrigatoria(
                        "CONCILIACAO_FLYWAY_USER"
                );

        String senhaAdmin =
                variavelObrigatoria(
                        "CONCILIACAO_FLYWAY_PASSWORD"
                );

        String bancoEsperado =
                variavelObrigatoria(
                        "CONCILIACAO_DB_TEST_NAME"
                );

        if ("conciliacao_pagamentos"
                .equals(bancoEsperado)) {

            throw new IllegalStateException(
                    "Teste HTTP nao pode usar "
                            + "o banco principal."
            );
        }

        DataSource dataSourceAdmin =
                new DriverManagerDataSource(
                        urlAdmin,
                        usuarioAdmin,
                        senhaAdmin
                );

        jdbcAdmin =
                new JdbcTemplate(
                        dataSourceAdmin
                );

        String bancoApp =
                jdbcApp.queryForObject(
                        "SELECT current_database()",
                        String.class
                );

        String bancoAdmin =
                jdbcAdmin.queryForObject(
                        "SELECT current_database()",
                        String.class
                );

        assertEquals(
                bancoEsperado,
                bancoApp
        );

        assertEquals(
                bancoEsperado,
                bancoAdmin
        );

        if ("conciliacao_pagamentos".equals(
                bancoApp
        )
                || "conciliacao_pagamentos".equals(
                        bancoAdmin
                )) {

            throw new IllegalStateException(
                    "Protecao acionada: conexao "
                            + "apontou para o banco principal."
            );
        }

        assertEquals(
                "conciliacao_app",
                jdbcApp.queryForObject(
                        "SELECT current_user",
                        String.class
                )
        );

        limparBancoTeste();
        prepararDadosOrigem();
    }

    @AfterEach
    void limpar() {
        if (jdbcAdmin != null) {
            limparBancoTeste();
        }
    }

    @Test
    void deveExecutarConciliacaoCompletaPelaApiHttp()
            throws Exception {

        mockMvc.perform(
                        post("/api/conciliacoes")
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        jsonPath("$.id")
                                .isNumber()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CONCLUIDA")
                )
                .andExpect(
                        jsonPath("$.resumo.conferidos")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.resumo.acima")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.resumo.abaixo")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.resumo.duplicados")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.resumo.semRecebimento")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.resumo.semPrevisao")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.resumo.totalEsperado")
                                .value(465.75)
                )
                .andExpect(
                        jsonPath("$.resumo.totalRecebido")
                                .value(506.25)
                )
                .andExpect(
                        jsonPath("$.resumo.saldoGlobal")
                                .value(40.50)
                )
                .andExpect(
                        jsonPath("$.detalhes.length()")
                                .value(5)
                )
                .andExpect(
                        jsonPath(
                                "$.detalhes[0].identificador"
                        ).value("COB001")
                )
                .andExpect(
                        jsonPath(
                                "$.detalhes[0].status"
                        ).value("DUPLICADO")
                )
                .andExpect(
                        jsonPath(
                                "$.detalhes[1].identificador"
                        ).value("COB002")
                )
                .andExpect(
                        jsonPath(
                                "$.detalhes[1].status"
                        ).value("ABAIXO_DO_ESPERADO")
                )
                .andExpect(
                        jsonPath(
                                "$.detalhes[2].identificador"
                        ).value("COB003")
                )
                .andExpect(
                        jsonPath(
                                "$.detalhes[2].status"
                        ).value("CONFERIDO")
                )
                .andExpect(
                        jsonPath(
                                "$.detalhes[3].identificador"
                        ).value("COB004")
                )
                .andExpect(
                        jsonPath(
                                "$.detalhes[3].status"
                        ).value("SEM_RECEBIMENTO")
                )
                .andExpect(
                        jsonPath(
                                "$.detalhes[4].identificador"
                        ).value("COB999")
                )
                .andExpect(
                        jsonPath(
                                "$.detalhes[4].status"
                        ).value("SEM_PREVISAO")
                )
                .andExpect(
                        jsonPath("$.executavel")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.relatorio")
                                .doesNotExist()
                );

        validarPersistenciaDefinitiva();
    }

    private void validarPersistenciaDefinitiva() {
        Long quantidadeExecucoes =
                jdbcApp.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacoes
                        """,
                        Long.class
                );

        assertEquals(
                1L,
                quantidadeExecucoes
        );

        Long conciliacaoId =
                jdbcApp.queryForObject(
                        """
                        SELECT id
                        FROM public.conciliacoes
                        """,
                        Long.class
                );

        assertNotNull(
                conciliacaoId
        );

        String status =
                jdbcApp.queryForObject(
                        """
                        SELECT status
                        FROM public.conciliacoes
                        WHERE id = ?
                        """,
                        String.class,
                        conciliacaoId
                );

        Integer versao =
                jdbcApp.queryForObject(
                        """
                        SELECT resultado_versao
                        FROM public.conciliacoes
                        WHERE id = ?
                        """,
                        Integer.class,
                        conciliacaoId
                );

        Long cobrancasSnapshot =
                jdbcApp.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_snapshot_cobrancas
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
                );

        Long pagamentosSnapshot =
                jdbcApp.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_snapshot_pagamentos
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
                );

        Long resumos =
                jdbcApp.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_resumos
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
                );

        Long detalhes =
                jdbcApp.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM public.conciliacao_detalhes
                        WHERE conciliacao_id = ?
                        """,
                        Long.class,
                        conciliacaoId
                );

        assertEquals(
                "CONCLUIDA",
                status
        );

        assertEquals(
                1,
                versao
        );

        assertEquals(
                4L,
                cobrancasSnapshot
        );

        assertEquals(
                5L,
                pagamentosSnapshot
        );

        assertEquals(
                1L,
                resumos
        );

        assertEquals(
                5L,
                detalhes
        );

        BigDecimal totalEsperado =
                jdbcApp.queryForObject(
                        """
                        SELECT total_esperado
                        FROM public.conciliacao_resumos
                        WHERE conciliacao_id = ?
                        """,
                        BigDecimal.class,
                        conciliacaoId
                );

        BigDecimal totalRecebido =
                jdbcApp.queryForObject(
                        """
                        SELECT total_recebido
                        FROM public.conciliacao_resumos
                        WHERE conciliacao_id = ?
                        """,
                        BigDecimal.class,
                        conciliacaoId
                );

        BigDecimal saldoGlobal =
                jdbcApp.queryForObject(
                        """
                        SELECT saldo_global
                        FROM public.conciliacao_resumos
                        WHERE conciliacao_id = ?
                        """,
                        BigDecimal.class,
                        conciliacaoId
                );

        assertBigDecimal(
                "465.75",
                totalEsperado
        );

        assertBigDecimal(
                "506.25",
                totalRecebido
        );

        assertBigDecimal(
                "40.50",
                saldoGlobal
        );
    }

    private void prepararDadosOrigem() {
        inserirCobranca(
                "COB001",
                "100.50"
        );

        inserirCobranca(
                "COB002",
                "200.00"
        );

        inserirCobranca(
                "COB003",
                "75.25"
        );

        inserirCobranca(
                "COB004",
                "90.00"
        );

        inserirPagamento(
                "COB001",
                "100.50"
        );

        inserirPagamento(
                "COB002",
                "180.00"
        );

        inserirPagamento(
                "COB999",
                "50.00"
        );

        inserirPagamento(
                "COB001",
                "100.50"
        );

        inserirPagamento(
                "COB003",
                "75.25"
        );
    }

    private void inserirCobranca(
            String identificador,
            String valor
    ) {
        int linhas =
                jdbcApp.update(
                        """
                        INSERT INTO public.cobrancas (
                            identificador,
                            valor_esperado
                        )
                        VALUES (?, ?)
                        """,
                        identificador,
                        new BigDecimal(valor)
                );

        assertEquals(
                1,
                linhas
        );
    }

    private void inserirPagamento(
            String identificador,
            String valor
    ) {
        int linhas =
                jdbcApp.update(
                        """
                        INSERT INTO public.pagamentos (
                            identificador_cobranca,
                            valor_pago
                        )
                        VALUES (?, ?)
                        """,
                        identificador,
                        new BigDecimal(valor)
                );

        assertEquals(
                1,
                linhas
        );
    }

    private void limparBancoTeste() {
        BancoTestePostgresql.limpar(
                jdbcAdmin
        );
    }

    private void assertBigDecimal(
            String esperado,
            BigDecimal atual
    ) {
        assertNotNull(
                atual
        );

        assertEquals(
                0,
                new BigDecimal(
                        esperado
                ).compareTo(
                        atual
                )
        );
    }

    private String variavelObrigatoria(
            String nome
    ) {
        return variavelObrigatoriaEstatica(
                nome
        );
    }

    private static String variavelObrigatoriaEstatica(
            String nome
    ) {
        String valor =
                System.getenv(
                        nome
                );

        if (valor == null
                || valor.isBlank()) {

            throw new IllegalStateException(
                    "Variavel obrigatoria ausente: "
                            + nome
            );
        }

        return valor;
    }
}
