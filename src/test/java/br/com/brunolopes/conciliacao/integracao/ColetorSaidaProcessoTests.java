package br.com.brunolopes.conciliacao.integracao;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColetorSaidaProcessoTests {

    @Test
    void deveColetarSaidaDentroDoLimite() throws IOException {
        SaidaProcesso saida = ColetorSaidaProcesso.coletar(
                entrada("processamento concluido"), 1024);

        assertEquals("processamento concluido", saida.conteudo());
        assertFalse(saida.truncada());
    }

    @Test
    void deveTruncarConteudoAcimaDoLimite() throws IOException {
        SaidaProcesso saida = ColetorSaidaProcesso.coletar(
                entrada("1234567890"), 5);

        assertEquals("12345", saida.conteudo());
        assertTrue(saida.truncada());
    }

    @Test
    void deveAceitarSaidaVazia() throws IOException {
        SaidaProcesso saida = ColetorSaidaProcesso.coletar(
                entrada(""), 100);

        assertEquals("", saida.conteudo());
        assertFalse(saida.truncada());
    }

    @Test
    void deveRejeitarLimiteZero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ColetorSaidaProcesso.coletar(
                        entrada("teste"), 0));
    }

    @Test
    void deveRejeitarEntradaNula() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ColetorSaidaProcesso.coletar(
                        (InputStream) null, 100));
    }

    private ByteArrayInputStream entrada(String conteudo) {
        return new ByteArrayInputStream(
                conteudo.getBytes(StandardCharsets.UTF_8));
    }
}
