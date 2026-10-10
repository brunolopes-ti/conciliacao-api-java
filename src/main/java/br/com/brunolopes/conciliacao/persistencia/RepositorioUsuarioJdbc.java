package br.com.brunolopes.conciliacao.persistencia;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import br.com.brunolopes.conciliacao.modelo.usuario.Usuario;

@Repository
@ConditionalOnProperty(
        name = "conciliacao.persistencia.habilitada",
        havingValue = "true",
        matchIfMissing = true
)
public class RepositorioUsuarioJdbc
        implements RepositorioUsuario {

    private final JdbcTemplate jdbcTemplate;

    public RepositorioUsuarioJdbc(
            JdbcTemplate jdbcTemplate
    ) {
        if (jdbcTemplate == null) {
            throw new IllegalArgumentException(
                    "JdbcTemplate e obrigatorio."
            );
        }

        this.jdbcTemplate =
                jdbcTemplate;
    }

    @Override
    public Usuario criar(
            String nome,
            String email,
            String senhaHash
    ) {
        try {
            Usuario usuario =
                    jdbcTemplate.queryForObject(
                            """
                            INSERT INTO public.usuarios (
                                nome,
                                email,
                                senha_hash
                            )
                            VALUES (
                                ?,
                                ?,
                                ?
                            )
                            RETURNING
                                id,
                                nome,
                                email,
                                senha_hash,
                                ativo,
                                criado_em
                            """,
                            (resultado, numeroLinha) ->
                                    mapear(
                                            resultado
                                    ),
                            nome,
                            email,
                            senhaHash
                    );

            if (usuario == null) {
                throw new IllegalStateException(
                        "Banco nao retornou o usuario criado."
                );
            }

            return usuario;

        } catch (DuplicateKeyException erro) {
            throw new EmailUsuarioDuplicadoException(
                    "Email de usuario ja cadastrado.",
                    erro
            );
        }
    }

    @Override
    public Optional<Usuario> buscarPorEmail(
            String email
    ) {
        List<Usuario> usuarios =
                jdbcTemplate.query(
                        """
                        SELECT
                            id,
                            nome,
                            email,
                            senha_hash,
                            ativo,
                            criado_em
                        FROM public.usuarios
                        WHERE email = ?
                        LIMIT 1
                        """,
                        (resultado, numeroLinha) ->
                                mapear(
                                        resultado
                                ),
                        email
                );

        if (usuarios.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                usuarios.getFirst()
        );
    }

    private Usuario mapear(
            java.sql.ResultSet resultado
    ) throws java.sql.SQLException {

        return new Usuario(
                resultado.getLong(
                        "id"
                ),
                resultado.getString(
                        "nome"
                ),
                resultado.getString(
                        "email"
                ),
                resultado.getString(
                        "senha_hash"
                ),
                resultado.getBoolean(
                        "ativo"
                ),
                resultado.getObject(
                        "criado_em",
                        OffsetDateTime.class
                )
        );
    }
}
