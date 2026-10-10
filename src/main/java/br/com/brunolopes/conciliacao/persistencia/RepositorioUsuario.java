package br.com.brunolopes.conciliacao.persistencia;

import java.util.Optional;

import br.com.brunolopes.conciliacao.modelo.usuario.Usuario;

public interface RepositorioUsuario {

    Usuario criar(
            String nome,
            String email,
            String senhaHash
    );

    Optional<Usuario> buscarPorEmail(
            String email
    );
}
