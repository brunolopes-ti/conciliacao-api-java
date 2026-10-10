package br.com.brunolopes.conciliacao.aplicacao;

import br.com.brunolopes.conciliacao.modelo.usuario.UsuarioCadastrado;

public interface ServicoCadastroUsuario {

    UsuarioCadastrado cadastrar(
            String nome,
            String email,
            String senha
    );
}
