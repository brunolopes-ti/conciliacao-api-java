package br.com.brunolopes.conciliacao.testutil;

import org.springframework.jdbc.core.JdbcTemplate;

public final class BancoTestePostgresql {

    private static final String BANCO_PRINCIPAL =
            "conciliacao_pagamentos";

    private BancoTestePostgresql() {
    }

    public static void limpar(
            JdbcTemplate jdbcAdmin
    ) {
        validarBancoSeguro(
                jdbcAdmin
        );

        jdbcAdmin.execute(
                """
                DROP TRIGGER IF EXISTS
                    teste_falha_resultado_detalhe
                ON public.conciliacao_detalhes
                """
        );

        jdbcAdmin.execute(
                """
                DROP FUNCTION IF EXISTS
                    public.teste_falha_resultado_detalhe()
                """
        );

        jdbcAdmin.execute(
                """
                DROP TRIGGER IF EXISTS
                    teste_falha_snapshot_pagamento
                ON public.conciliacao_snapshot_pagamentos
                """
        );

        jdbcAdmin.execute(
                """
                DROP FUNCTION IF EXISTS
                    public.teste_falha_snapshot_pagamento()
                """
        );

        /*
         * Validamos novamente imediatamente antes
         * da operacao destrutiva mais importante.
         */
        validarBancoSeguro(
                jdbcAdmin
        );

        jdbcAdmin.execute(
                """
                TRUNCATE TABLE
                    public.conciliacao_detalhes,
                    public.conciliacao_resumos,
                    public.conciliacao_snapshot_pagamentos,
                    public.conciliacao_snapshot_cobrancas,
                    public.conciliacoes,
                    public.pagamentos,
                    public.cobrancas
                RESTART IDENTITY
                """
        );
    }

    private static void validarBancoSeguro(
            JdbcTemplate jdbcAdmin
    ) {
        if (jdbcAdmin == null) {
            throw new IllegalStateException(
                    "Conexao administrativa "
                            + "do banco de teste ausente."
            );
        }

        String bancoEsperado =
                variavelObrigatoria(
                        "CONCILIACAO_DB_TEST_NAME"
                );

        String bancoAtual =
                jdbcAdmin.queryForObject(
                        "SELECT current_database()",
                        String.class
                );

        if (BANCO_PRINCIPAL.equals(
                bancoEsperado
        )) {
            throw new IllegalStateException(
                    "Protecao acionada: "
                            + "CONCILIACAO_DB_TEST_NAME "
                            + "aponta para o banco principal."
            );
        }

        if (bancoAtual == null
                || bancoAtual.isBlank()) {

            throw new IllegalStateException(
                    "Nao foi possivel identificar "
                            + "o banco conectado."
            );
        }

        if (BANCO_PRINCIPAL.equals(
                bancoAtual
        )) {
            throw new IllegalStateException(
                    "Protecao acionada: tentativa "
                            + "de limpar o banco principal."
            );
        }

        if (!bancoEsperado.equals(
                bancoAtual
        )) {
            throw new IllegalStateException(
                    "Protecao acionada: banco atual "
                            + "difere do banco de teste esperado. "
                            + "Esperado: "
                            + bancoEsperado
                            + ". Atual: "
                            + bancoAtual
                            + "."
            );
        }
    }

    private static String variavelObrigatoria(
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
