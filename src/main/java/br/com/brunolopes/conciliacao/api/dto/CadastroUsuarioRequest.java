package br.com.brunolopes.conciliacao.api.dto;

public record CadastroUsuarioRequest(
        String nome,
        String email,
        String senha
) {
}

