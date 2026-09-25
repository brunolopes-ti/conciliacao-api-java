package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContratoResultadoTsvTests {

    @TempDir
    Path diretorioTemporario;

    @Test
    void deveAceitarCenarioCompletoComTodosOsStatus()
            throws IOException {

        Path execucao = criarDiretorioExecucao();

        String conteudo =
                "VERSAO\t1\n"
                + "DETALHE\tP001\t100.00\t100.00\t0.00"
                + "\tCONFERIDO\t1\n"
                + "DETALHE\tP002\t200.00\t220.00\t20.00"
                + "\tACIMA_DO_ESPERADO\t1\n"
                + "DETALHE\tP003\t150.00\t125.00\t-25.00"
                + "\tABAIXO_DO_ESPERADO\t1\n"
                + "DETALHE\tP004\t80.00\t70.00\t-10.00"
                + "\tDUPLICADO\t2\n"
                + "DETALHE\tP005\t50.00\t\t"
                + "\tSEM_RECEBIMENTO\t0\n"
                + "DETALHE\tP999\t\t30.00\t"
                + "\tSEM_PREVISAO\t1\n"
                + "RESUMO\t1\t1\t1\t1\t1\t1"
                + "\t580.00\t555.00\t-25.00\n";

        Path resultado =
                gravarResultado(execucao, conteudo);

        ResultadoConciliacao conciliacao =
                LeitorResultadoTsv.ler(
                        execucao,
                        resultado);

        assertEquals(1, conciliacao.versao());
        assertEquals(6, conciliacao.detalhes().size());

        assertEquals(
                StatusConciliacao.CONFERIDO,
                conciliacao.detalhes().get(0).status());

        assertEquals(
                StatusConciliacao.ACIMA_DO_ESPERADO,
                conciliacao.detalhes().get(1).status());

        assertEquals(
                StatusConciliacao.ABAIXO_DO_ESPERADO,
                conciliacao.detalhes().get(2).status());

        assertEquals(
                StatusConciliacao.DUPLICADO,
                conciliacao.detalhes().get(3).status());

        assertEquals(
                StatusConciliacao.SEM_RECEBIMENTO,
                conciliacao.detalhes().get(4).status());

        assertEquals(
                StatusConciliacao.SEM_PREVISAO,
                conciliacao.detalhes().get(5).status());

        assertEquals(1, conciliacao.resumo().conferidos());
        assertEquals(1, conciliacao.resumo().acima());
        assertEquals(1, conciliacao.resumo().abaixo());
        assertEquals(1, conciliacao.resumo().duplicados());
        assertEquals(1, conciliacao.resumo().semRecebimento());
        assertEquals(1, conciliacao.resumo().semPrevisao());
    }

    @Test
    void deveAceitarSemPrevisaoRepetidoComMesmoIdentificador()
            throws IOException {

        Path execucao = criarDiretorioExecucao();

        String conteudo =
                "VERSAO\t1\n"
                + "DETALHE\tP001\t100.00\t\t"
                + "\tSEM_RECEBIMENTO\t0\n"
                + "DETALHE\tP999\t\t10.00\t"
                + "\tSEM_PREVISAO\t1\n"
                + "DETALHE\tP999\t\t20.00\t"
                + "\tSEM_PREVISAO\t1\n"
                + "RESUMO\t0\t0\t0\t0\t1\t2"
                + "\t100.00\t30.00\t-70.00\n";

        Path resultado =
                gravarResultado(execucao, conteudo);

        ResultadoConciliacao conciliacao =
                LeitorResultadoTsv.ler(
                        execucao,
                        resultado);

        assertEquals(3, conciliacao.detalhes().size());
        assertEquals(2, conciliacao.resumo().semPrevisao());

        assertEquals(
                "P999",
                conciliacao.detalhes().get(1).identificador());

        assertEquals(
                "P999",
                conciliacao.detalhes().get(2).identificador());
    }

    @Test
    void deveRejeitarResumoComContadorDivergente()
            throws IOException {

        Path execucao = criarDiretorioExecucao();

        String conteudo =
                "VERSAO\t1\n"
                + "DETALHE\tP001\t100.00\t100.00\t0.00"
                + "\tCONFERIDO\t1\n"
                + "RESUMO\t0\t0\t0\t0\t0\t0"
                + "\t100.00\t100.00\t0.00\n";

        Path resultado =
                gravarResultado(execucao, conteudo);

        assertThrows(
                IllegalArgumentException.class,
                () -> LeitorResultadoTsv.ler(
                        execucao,
                        resultado));
    }

    @Test
    void deveRejeitarValorMonetarioForaDoFormato()
            throws IOException {

        Path execucao = criarDiretorioExecucao();

        String conteudo =
                "VERSAO\t1\n"
                + "DETALHE\tP001\t100.5\t100.00\t0.00"
                + "\tCONFERIDO\t1\n"
                + "RESUMO\t1\t0\t0\t0\t0\t0"
                + "\t100.00\t100.00\t0.00\n";

        Path resultado =
                gravarResultado(execucao, conteudo);

        assertThrows(
                IllegalArgumentException.class,
                () -> LeitorResultadoTsv.ler(
                        execucao,
                        resultado));
    }

    @Test
    void deveRejeitarLinhaVaziaNoMeioDoArquivo()
            throws IOException {

        Path execucao = criarDiretorioExecucao();

        String conteudo =
                "VERSAO\t1\n"
                + "\n"
                + "RESUMO\t0\t0\t0\t0\t0\t0"
                + "\t0.00\t0.00\t0.00\n";

        Path resultado =
                gravarResultado(execucao, conteudo);

        assertThrows(
                IllegalArgumentException.class,
                () -> LeitorResultadoTsv.ler(
                        execucao,
                        resultado));
    }

    @Test
    void deveRejeitarDuplicadoComQuantidadeUm()
            throws IOException {

        Path execucao = criarDiretorioExecucao();

        String conteudo =
                "VERSAO\t1\n"
                + "DETALHE\tP001\t100.00\t90.00\t-10.00"
                + "\tDUPLICADO\t1\n"
                + "RESUMO\t0\t0\t0\t1\t0\t0"
                + "\t100.00\t90.00\t-10.00\n";

        Path resultado =
                gravarResultado(execucao, conteudo);

        assertThrows(
                IllegalArgumentException.class,
                () -> LeitorResultadoTsv.ler(
                        execucao,
                        resultado));
    }

    private Path criarDiretorioExecucao()
            throws IOException {

        return Files.createDirectory(
                diretorioTemporario.resolve(
                        "execucao-" + System.nanoTime()));
    }

    private Path gravarResultado(
            Path execucao,
            String conteudo
    ) throws IOException {

        Path resultado =
                execucao.resolve("resultado.tsv");

        Files.writeString(
                resultado,
                conteudo,
                StandardCharsets.UTF_8);

        return resultado;
    }
}
