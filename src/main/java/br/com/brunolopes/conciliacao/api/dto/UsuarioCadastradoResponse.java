package br.com.brunolopes.conciliacao.api.dto;

import java.time.OffsetDateTime;

public record UsuarioCadastradoResponse(
        long id,
        String nome,
        String email,
        boolean ativo,
        OffsetDateTime criadoEm
) {
}
