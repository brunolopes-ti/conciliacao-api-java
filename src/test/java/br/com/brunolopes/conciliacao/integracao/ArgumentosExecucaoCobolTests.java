package br.com.brunolopes.conciliacao.integracao;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArgumentosExecucaoCobolTests {

    @Test
    void deveCriarArgumentosAPartirDoDiretorioExecucao() {
        DiretorioExecucaoCobol execucao =
                DiretorioExecucaoCobol.criar();

        ArgumentosExecucaoCobol argumentos =
                ArgumentosExecucaoCobol.de(execucao);

        assertEquals(
                execucao.esperados(),
                argumentos.esperados());

        assertEquals(
                execucao.recebidos(),
                argumentos.recebidos());

        assertEquals(
                execucao.relatorio(),
                argumentos.relatorio());

        assertEquals(
                execucao.resultado(),
                argumentos.resultado());
    }

    @Test
    void deveGerarListaNaOrdemExigidaPeloCobol() {
        ArgumentosExecucaoCobol argumentos =
                new ArgumentosExecucaoCobol(
                        Path.of("/tmp/esperados.csv"),
                        Path.of("/tmp/recebidos.csv"),
                        Path.of("/tmp/relatorio.txt"),
                        Path.of("/tmp/resultado.tsv"));

        List<String> lista =
                argumentos.comoLista();

        assertEquals(4, lista.size());
        assertEquals("/tmp/esperados.csv", lista.get(0));
        assertEquals("/tmp/recebidos.csv", lista.get(1));
        assertEquals("/tmp/relatorio.txt", lista.get(2));
        assertEquals("/tmp/resultado.tsv", lista.get(3));
    }

    @Test
    void devePreservarEspacosNosCaminhos() {
        ArgumentosExecucaoCobol argumentos =
                new ArgumentosExecucaoCobol(
                        Path.of("/tmp/meu teste/esperados.csv"),
                        Path.of("/tmp/meu teste/recebidos.csv"),
                        Path.of("/tmp/meu teste/relatorio.txt"),
                        Path.of("/tmp/meu teste/resultado.tsv"));

        List<String> lista =
                argumentos.comoLista();

        assertEquals(
                "/tmp/meu teste/esperados.csv",
                lista.get(0));

        assertEquals(
                "/tmp/meu teste/resultado.tsv",
                lista.get(3));
    }

    @Test
    void deveRejeitarCaminhosNulos() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ArgumentosExecucaoCobol(
                        null,
                        Path.of("recebidos.csv"),
                        Path.of("relatorio.txt"),
                        Path.of("resultado.tsv")));
    }

    @Test
    void deveRejeitarExecucaoNula() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ArgumentosExecucaoCobol.de(null));
    }
}
