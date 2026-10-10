package br.com.brunolopes.conciliacao.modelo.usuario;

import java.time.OffsetDateTime;

public record UsuarioCadastrado(
        long id,
        String nome,
        String email,
        boolean ativo,
        OffsetDateTime criadoEm
) {
}
