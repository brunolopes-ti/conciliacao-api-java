package br.com.brunolopes.conciliacao.aplicacao;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.brunolopes.conciliacao.aplicacao.excecao.EmailJaCadastradoException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.RequisicaoInvalidaException;
import br.com.brunolopes.conciliacao.modelo.usuario.Usuario;
import br.com.brunolopes.conciliacao.modelo.usuario.UsuarioCadastrado;
import br.com.brunolopes.conciliacao.persistencia.EmailUsuarioDuplicadoException;
import br.com.brunolopes.conciliacao.persistencia.RepositorioUsuario;

public final class ServicoCadastroUsuarioReal
        implements ServicoCadastroUsuario {

    private static final int TAMANHO_MINIMO_NOME = 2;
    private static final int TAMANHO_MAXIMO_NOME = 100;

    private static final int TAMANHO_MINIMO_EMAIL = 3;
    private static final int TAMANHO_MAXIMO_EMAIL = 254;

    private static final int TAMANHO_MINIMO_SENHA = 8;
    private static final int LIMITE_BYTES_BCRYPT = 72;

    private final RepositorioUsuario repositorio;
    private final PasswordEncoder passwordEncoder;

    public ServicoCadastroUsuarioReal(
            RepositorioUsuario repositorio,
            PasswordEncoder passwordEncoder
    ) {
        if (repositorio == null) {
            throw new IllegalArgumentException(
                    "Repositorio de usuario e obrigatorio."
            );
        }

        if (passwordEncoder == null) {
            throw new IllegalArgumentException(
                    "PasswordEncoder e obrigatorio."
            );
        }

        this.repositorio =
                repositorio;

        this.passwordEncoder =
                passwordEncoder;
    }

    @Override
    public UsuarioCadastrado cadastrar(
            String nome,
            String email,
            String senha
    ) {
        String nomeNormalizado =
                normalizarNome(
                        nome
                );

        String emailNormalizado =
                normalizarEmail(
                        email
                );

        validarSenha(
                senha
        );

        String senhaHash =
                passwordEncoder.encode(
                        senha
                );

        Usuario usuario;

        try {
            usuario =
                    repositorio.criar(
                            nomeNormalizado,
                            emailNormalizado,
                            senhaHash
                    );

        } catch (EmailUsuarioDuplicadoException erro) {
            throw new EmailJaCadastradoException(
                    "Email ja cadastrado."
            );
        }

        return new UsuarioCadastrado(
                usuario.id(),
                usuario.nome(),
                usuario.email(),
                usuario.ativo(),
                usuario.criadoEm()
        );
    }

    private String normalizarNome(
            String nome
    ) {
        if (nome == null) {
            throw new RequisicaoInvalidaException(
                    "Nome e obrigatorio."
            );
        }

        String normalizado =
                nome.strip();

        if (normalizado.length()
                < TAMANHO_MINIMO_NOME
                || normalizado.length()
                > TAMANHO_MAXIMO_NOME) {

            throw new RequisicaoInvalidaException(
                    "Nome deve possuir entre 2 e 100 caracteres."
            );
        }

        return normalizado;
    }

    private String normalizarEmail(
            String email
    ) {
        if (email == null) {
            throw new RequisicaoInvalidaException(
                    "Email e obrigatorio."
            );
        }

        String normalizado =
                email.strip()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (normalizado.length()
                < TAMANHO_MINIMO_EMAIL
                || normalizado.length()
                > TAMANHO_MAXIMO_EMAIL) {

            throw new RequisicaoInvalidaException(
                    "Email invalido."
            );
        }

        int primeiroArroba =
                normalizado.indexOf(
                        '@'
                );

        int ultimoArroba =
                normalizado.lastIndexOf(
                        '@'
                );

        if (primeiroArroba <= 0
                || primeiroArroba
                        != ultimoArroba
                || primeiroArroba
                        == normalizado.length() - 1
                || possuiEspaco(
                        normalizado
                )) {

            throw new RequisicaoInvalidaException(
                    "Email invalido."
            );
        }

        return normalizado;
    }

    private void validarSenha(
            String senha
    ) {
        if (senha == null
                || senha.isBlank()) {

            throw new RequisicaoInvalidaException(
                    "Senha e obrigatoria."
            );
        }

        int quantidadeCaracteres =
                senha.codePointCount(
                        0,
                        senha.length()
                );

        if (quantidadeCaracteres
                < TAMANHO_MINIMO_SENHA) {

            throw new RequisicaoInvalidaException(
                    "Senha deve possuir pelo menos 8 caracteres."
            );
        }

        int quantidadeBytes =
                senha.getBytes(
                        StandardCharsets.UTF_8
                ).length;

        if (quantidadeBytes
                > LIMITE_BYTES_BCRYPT) {

            throw new RequisicaoInvalidaException(
                    "Senha excede o limite suportado."
            );
        }
    }

    private boolean possuiEspaco(
            String valor
    ) {
        return valor.codePoints()
                .anyMatch(
                        codigo ->
                                Character.isWhitespace(
                                        codigo
                                )
                                        || Character.isSpaceChar(
                                                codigo
                                        )
                );
    }
}
