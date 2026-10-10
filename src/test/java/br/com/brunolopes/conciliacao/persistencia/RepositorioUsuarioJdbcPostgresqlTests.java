package br.com.brunolopes.conciliacao.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.brunolopes.conciliacao.modelo.usuario.Usuario;

@EnabledIfEnvironmentVariable(
        named = "CONCILIACAO_TESTE_POSTGRESQL",
        matches = "true"
)
class RepositorioUsuarioJdbcPostgresqlTests {

    private JdbcTemplate jdbcTemplate;
    private RepositorioUsuarioJdbc repositorio;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void preparar() {
        String url =
                variavelObrigatoria(
                        "CONCILIACAO_DB_URL"
                );

        String usuario =
                variavelObrigatoria(
                        "CONCILIACAO_DB_USER"
                );

        String senha =
                variavelObrigatoria(
                        "CONCILIACAO_DB_PASSWORD"
                );

        String bancoEsperado =
                variavelObrigatoria(
                        "CONCILIACAO_DB_TEST_NAME"
                );

        if ("conciliacao_pagamentos".equals(
                bancoEsperado
        )) {
            throw new IllegalStateException(
                    "Teste PostgreSQL nao pode usar o banco principal."
            );
        }

        DataSource dataSource =
                new DriverManagerDataSource(
                        url,
                        usuario,
                        senha
                );

        jdbcTemplate =
                new JdbcTemplate(
                        dataSource
                );

        String bancoAtual =
                jdbcTemplate.queryForObject(
                        "SELECT current_database()",
                        String.class
                );

        String usuarioAtual =
                jdbcTemplate.queryForObject(
                        "SELECT current_user",
                        String.class
                );

        assertEquals(
                bancoEsperado,
                bancoAtual,
                "Conexao apontou para banco diferente do esperado."
        );

        assertEquals(
                "conciliacao_app",
                usuarioAtual,
                "Teste deve executar com o usuario da aplicacao."
        );

        repositorio =
                new RepositorioUsuarioJdbc(
                        jdbcTemplate
                );

        transactionTemplate =
                new TransactionTemplate(
                        new DataSourceTransactionManager(
                                dataSource
                        )
                );
    }

    @Test
    void deveCriarEBuscarUsuarioPorEmail() {
        transactionTemplate.executeWithoutResult(
                statusTransacao -> {

                    String email =
                            novoEmail();

                    String senhaHash =
                            hashTeste(
                                    'a'
                            );

                    Usuario criado =
                            repositorio.criar(
                                    "Usuario JDBC",
                                    email,
                                    senhaHash
                            );

                    assertTrue(
                            criado.id() > 0
                    );

                    assertEquals(
                            "Usuario JDBC",
                            criado.nome()
                    );

                    assertEquals(
                            email,
                            criado.email()
                    );

                    assertEquals(
                            senhaHash,
                            criado.senhaHash()
                    );

                    assertTrue(
                            criado.ativo()
                    );

                    assertNotNull(
                            criado.criadoEm()
                    );

                    Optional<Usuario> encontrado =
                            repositorio.buscarPorEmail(
                                    email
                            );

                    assertTrue(
                            encontrado.isPresent()
                    );

                    Usuario usuarioEncontrado =
                            encontrado.orElseThrow();

                    assertEquals(
                            criado.id(),
                            usuarioEncontrado.id()
                    );

                    assertEquals(
                            criado.nome(),
                            usuarioEncontrado.nome()
                    );

                    assertEquals(
                            criado.email(),
                            usuarioEncontrado.email()
                    );

                    assertEquals(
                            criado.senhaHash(),
                            usuarioEncontrado.senhaHash()
                    );

                    assertEquals(
                            criado.ativo(),
                            usuarioEncontrado.ativo()
                    );

                    statusTransacao.setRollbackOnly();
                }
        );
    }

    @Test
    void deveRetornarVazioQuandoEmailNaoExiste() {
        String email =
                novoEmail();

        Optional<Usuario> resultado =
                repositorio.buscarPorEmail(
                        email
                );

        assertFalse(
                resultado.isPresent()
        );
    }

    @Test
    void deveInformarEmailDuplicado() {
        transactionTemplate.executeWithoutResult(
                statusTransacao -> {

                    String email =
                            novoEmail();

                    repositorio.criar(
                            "Primeiro Usuario",
                            email,
                            hashTeste(
                                    'b'
                            )
                    );

                    assertThrows(
                            EmailUsuarioDuplicadoException.class,
                            () ->
                                    repositorio.criar(
                                            "Segundo Usuario",
                                            email,
                                            hashTeste(
                                                    'c'
                                            )
                                    )
                    );

                    statusTransacao.setRollbackOnly();
                }
        );
    }

    private String novoEmail() {
        return "jdbc-"
                + UUID.randomUUID()
                + "@teste.com";
    }

    private String hashTeste(
            char caractere
    ) {
        return "$2a$10$"
                + String.valueOf(
                        caractere
                ).repeat(
                        53
                );
    }

    private String variavelObrigatoria(
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
