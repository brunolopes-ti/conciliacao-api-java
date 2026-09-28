package br.com.brunolopes.conciliacao.api.dto;

public record ErroResponse(
        String codigo,
        String mensagem
) {

    public ErroResponse {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(
                    "Codigo do erro e obrigatorio.");
        }

        if (mensagem == null || mensagem.isBlank()) {
            throw new IllegalArgumentException(
                    "Mensagem do erro e obrigatoria.");
        }
    }
}
