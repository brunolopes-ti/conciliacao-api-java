package br.com.brunolopes.conciliacao.integracao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ColetorSaidaProcesso {

    private ColetorSaidaProcesso() {
    }

    /** Coleta sem read bloqueante: um filho orfao nao prende a thread coletora. */
    public static SaidaProcesso coletar(Process processo, int limiteBytes)
            throws IOException, InterruptedException {
        if (processo == null || limiteBytes <= 0) {
            throw new IllegalArgumentException("Processo e limite valido obrigatorios.");
        }
        InputStream entrada = processo.getInputStream();
        ByteArrayOutputStream armazenado = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        boolean truncada = false;
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Coleta interrompida.");
            }
            int disponivel = entrada.available();
            if (disponivel == 0) {
                if (!processo.isAlive()) {
                    // Verificar novamente apos observar o encerramento.
                    if (entrada.available() == 0) {
                        break;
                    }
                    continue;
                }
                Thread.sleep(5);
                continue;
            }
            int lidos = entrada.read(buffer, 0, Math.min(disponivel, buffer.length));
            if (lidos < 0) {
                break;
            }
            int guardar = Math.min(lidos, limiteBytes - armazenado.size());
            armazenado.write(buffer, 0, guardar);
            truncada |= guardar < lidos;
        }
        return new SaidaProcesso(armazenado.toString(StandardCharsets.UTF_8), truncada);
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
