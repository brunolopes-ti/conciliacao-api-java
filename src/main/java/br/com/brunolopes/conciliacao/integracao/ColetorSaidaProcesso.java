package br.com.brunolopes.conciliacao.integracao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ColetorSaidaProcesso {

    private ColetorSaidaProcesso() {
    }

    public static SaidaProcesso coletar(
            InputStream entrada,
            int limiteBytes
    ) throws IOException {

        if (entrada == null) {
            throw new IllegalArgumentException(
                    "Fluxo de entrada e obrigatorio.");
        }

        if (limiteBytes <= 0) {
            throw new IllegalArgumentException(
                    "Limite de bytes deve ser maior que zero.");
        }

        ByteArrayOutputStream armazenado =
                new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int quantidadeLida;
        boolean truncada = false;

        while ((quantidadeLida = entrada.read(buffer)) != -1) {

            int espacoDisponivel =
                    limiteBytes - armazenado.size();

            if (espacoDisponivel > 0) {
                int quantidadeParaGuardar =
                        Math.min(
                                quantidadeLida,
                                espacoDisponivel);

                armazenado.write(
                        buffer,
                        0,
                        quantidadeParaGuardar);
            }

            if (armazenado.size() >= limiteBytes
                    && quantidadeLida > espacoDisponivel) {

                truncada = true;
            }
        }

        String conteudo =
                armazenado.toString(
                        StandardCharsets.UTF_8);

        return new SaidaProcesso(
                conteudo,
                truncada);
    }
}
