package br.com.brunolopes.conciliacao.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.brunolopes.conciliacao.api.erro.TratadorGlobalExcecoes;
import br.com.brunolopes.conciliacao.aplicacao.ServicoCadastroUsuario;
import br.com.brunolopes.conciliacao.aplicacao.excecao.EmailJaCadastradoException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.RequisicaoInvalidaException;
import br.com.brunolopes.conciliacao.modelo.usuario.UsuarioCadastrado;

class AuthControllerHttpTests {

    private MockMvc mockMvc;
    private ServicoCadastroUsuarioFake servico;

    @BeforeEach
    void preparar() {
        servico =
                new ServicoCadastroUsuarioFake();

        AuthController controller =
                new AuthController(
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
    void deveCadastrarUsuarioERetornar201()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/cadastro")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "nome": "Bruno Lopes",
                                          "email": "BRUNO@EMAIL.COM",
                                          "senha": "Senha123"
                                        }
                                        """
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
                                .value(15)
                )
                .andExpect(
                        jsonPath("$.nome")
                                .value("Bruno Lopes")
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("bruno@email.com")
                )
                .andExpect(
                        jsonPath("$.ativo")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.criadoEm")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.senha")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.senhaHash")
                                .doesNotExist()
                )
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "Senha123"
                                        )
                                )
                        )
                );
    }

    @Test
    void deveRetornar400ParaCadastroInvalido()
            throws Exception {

        servico.falha =
                new RequisicaoInvalidaException(
                        "Email invalido."
                );

        mockMvc.perform(
                        post("/api/auth/cadastro")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "nome": "Bruno Lopes",
                                          "email": "invalido",
                                          "senha": "Senha123"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value(
                                        "REQUISICAO_INVALIDA"
                                )
                )
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Email invalido."
                                )
                );
    }

    @Test
    void deveRetornar409ParaEmailJaCadastrado()
            throws Exception {

        servico.falha =
                new EmailJaCadastradoException(
                        "Email ja cadastrado."
                );

        mockMvc.perform(
                        post("/api/auth/cadastro")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "nome": "Bruno Lopes",
                                          "email": "bruno@email.com",
                                          "senha": "Senha123"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value(
                                        "EMAIL_JA_CADASTRADO"
                                )
                )
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Email ja cadastrado."
                                )
                );
    }

    @Test
    void deveRetornar400ParaJsonInvalido()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/cadastro")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "nome": "Bruno",
                                          "email":
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.codigo")
                                .value(
                                        "REQUISICAO_INVALIDA"
                                )
                )
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Corpo da requisicao invalido."
                                )
                );
    }

    @Test
    void deveRejeitarServicoNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new AuthController(
                                null
                        )
        );
    }

    private static final class ServicoCadastroUsuarioFake
            implements ServicoCadastroUsuario {

        private RuntimeException falha;

        @Override
        public UsuarioCadastrado cadastrar(
                String nome,
                String email,
                String senha
        ) {
            if (falha != null) {
                throw falha;
            }

            return new UsuarioCadastrado(
                    15,
                    "Bruno Lopes",
                    "bruno@email.com",
                    true,
                    OffsetDateTime.parse(
                            "2026-10-08T12:00:00Z"
                    )
            );
        }
    }
}
