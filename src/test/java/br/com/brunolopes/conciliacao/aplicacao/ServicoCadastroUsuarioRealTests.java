package br.com.brunolopes.conciliacao.aplicacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.brunolopes.conciliacao.aplicacao.excecao.EmailJaCadastradoException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.RequisicaoInvalidaException;
import br.com.brunolopes.conciliacao.modelo.usuario.Usuario;
import br.com.brunolopes.conciliacao.modelo.usuario.UsuarioCadastrado;
import br.com.brunolopes.conciliacao.persistencia.EmailUsuarioDuplicadoException;
import br.com.brunolopes.conciliacao.persistencia.RepositorioUsuario;

class ServicoCadastroUsuarioRealTests {

    @Test
    void deveCadastrarUsuarioComEmailNormalizadoESenhaHash() {
        RepositorioFake repositorio =
                new RepositorioFake();

        PasswordEncoder encoder =
                new BCryptPasswordEncoder(
                        4
                );

        ServicoCadastroUsuarioReal servico =
                new ServicoCadastroUsuarioReal(
                        repositorio,
                        encoder
                );

        UsuarioCadastrado resultado =
                servico.cadastrar(
                        "  Bruno Teste  ",
                        "  BRUNO@TESTE.COM  ",
                        "Senha123"
                );

        assertEquals(
                "Bruno Teste",
                repositorio.nome
        );

        assertEquals(
                "bruno@teste.com",
                repositorio.email
        );

        assertFalse(
                "Senha123".equals(
                        repositorio.senhaHash
                )
        );

        assertTrue(
                encoder.matches(
                        "Senha123",
                        repositorio.senhaHash
                )
        );

        assertEquals(
                10,
                resultado.id()
        );

        assertEquals(
                "Bruno Teste",
                resultado.nome()
        );

        assertEquals(
                "bruno@teste.com",
                resultado.email()
        );

        assertTrue(
                resultado.ativo()
        );
    }

    @Test
    void deveRejeitarDadosInvalidos() {
        ServicoCadastroUsuarioReal servico =
                novoServico();

        assertThrows(
                RequisicaoInvalidaException.class,
                () ->
                        servico.cadastrar(
                                null,
                                "usuario@teste.com",
                                "Senha123"
                        )
        );

        assertThrows(
                RequisicaoInvalidaException.class,
                () ->
                        servico.cadastrar(
                                "A",
                                "usuario@teste.com",
                                "Senha123"
                        )
        );

        assertThrows(
                RequisicaoInvalidaException.class,
                () ->
                        servico.cadastrar(
                                "Usuario",
                                "email-invalido",
                                "Senha123"
                        )
        );

        assertThrows(
                RequisicaoInvalidaException.class,
                () ->
                        servico.cadastrar(
                                "Usuario",
                                "usuario@@teste.com",
                                "Senha123"
                        )
        );

        assertThrows(
                RequisicaoInvalidaException.class,
                () ->
                        servico.cadastrar(
                                "Usuario",
                                "usuario@teste.com",
                                "1234567"
                        )
        );

        assertThrows(
                RequisicaoInvalidaException.class,
                () ->
                        servico.cadastrar(
                                "Usuario",
                                "usuario@teste.com",
                                "a".repeat(
                                        73
                                )
                        )
        );
    }

    @Test
    void deveInformarEmailJaCadastrado() {
        RepositorioFake repositorio =
                new RepositorioFake();

        repositorio.duplicado =
                true;

        ServicoCadastroUsuarioReal servico =
                new ServicoCadastroUsuarioReal(
                        repositorio,
                        new BCryptPasswordEncoder(
                                4
                        )
                );

        assertThrows(
                EmailJaCadastradoException.class,
                () ->
                        servico.cadastrar(
                                "Usuario",
                                "usuario@teste.com",
                                "Senha123"
                        )
        );
    }

    @Test
    void deveRejeitarDependenciasNulas() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ServicoCadastroUsuarioReal(
                                null,
                                new BCryptPasswordEncoder(
                                        4
                                )
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ServicoCadastroUsuarioReal(
                                new RepositorioFake(),
                                null
                        )
        );
    }

    private ServicoCadastroUsuarioReal novoServico() {
        return new ServicoCadastroUsuarioReal(
                new RepositorioFake(),
                new BCryptPasswordEncoder(
                        4
                )
        );
    }

    private static final class RepositorioFake
            implements RepositorioUsuario {

        private String nome;
        private String email;
        private String senhaHash;
        private boolean duplicado;

        @Override
        public Usuario criar(
                String nome,
                String email,
                String senhaHash
        ) {
            if (duplicado) {
                throw new EmailUsuarioDuplicadoException(
                        "Email duplicado.",
                        new RuntimeException(
                                "Teste"
                        )
                );
            }

            this.nome =
                    nome;

            this.email =
                    email;

            this.senhaHash =
                    senhaHash;

            return new Usuario(
                    10,
                    nome,
                    email,
                    senhaHash,
                    true,
                    OffsetDateTime.parse(
                            "2026-10-08T12:00:00Z"
                    )
            );
        }

        @Override
        public Optional<Usuario> buscarPorEmail(
                String email
        ) {
            return Optional.empty();
        }
    }
}
