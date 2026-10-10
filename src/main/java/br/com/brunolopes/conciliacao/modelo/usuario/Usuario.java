package br.com.brunolopes.conciliacao.modelo.usuario;

import java.time.OffsetDateTime;

public record Usuario(
        long id,
        String nome,
        String email,
        String senhaHash,
        boolean ativo,
        OffsetDateTime criadoEm
) {
}
