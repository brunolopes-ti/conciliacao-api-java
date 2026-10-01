package br.com.brunolopes.conciliacao.aplicacao;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import br.com.brunolopes.conciliacao.aplicacao.excecao.*;
import br.com.brunolopes.conciliacao.integracao.*;
import br.com.brunolopes.conciliacao.persistencia.*;
import java.math.BigDecimal;
import java.util.List;

class RevisaoBloco5ServicoTests {
    static class Registro implements RepositorioExecucaoConciliacao {
        long id; String codigo; String detalhe;
        public long criar() { return 81; }
        public void iniciar(long id) { }
        public void falhar(long id, String codigo, String detalhe) {
            this.id = id; this.codigo = codigo; this.detalhe = detalhe;
        }
    }
    private ServicoConciliacaoReal servico(Registro registro, RuntimeException falha) {
        var itens = List.of(new SnapshotExecucaoCobol.Item("P1", BigDecimal.ONE));
        return new ServicoConciliacaoReal(registro,
                id -> new SnapshotExecucaoCobol(itens, itens),
                (id, resultado) -> fail("Nao deve concluir apos falha"),
                snapshot -> { throw falha; });
    }
    @Test void persisteDiagnosticoLimitadoComId() {
        var registro = new Registro();
        var causa = new FalhaExecucaoCobolException("exit 1", null, "marcador-" + "x".repeat(5000));
        assertThrows(FalhaCobolException.class,
                () -> servico(registro, new FalhaCobolException("falha", causa)).executar());
        assertEquals(81, registro.id);
        assertEquals("FALHA_COBOL", registro.codigo);
        assertTrue(registro.detalhe.contains("marcador-"));
        assertTrue(registro.detalhe.length() <= 4000);
    }
    @Test void registraCapacidadeEsgotada() {
        var registro = new Registro();
        assertThrows(CapacidadeEsgotadaException.class,
                () -> servico(registro, new CapacidadeEsgotadaException()).executar());
        assertEquals("CAPACIDADE_ESGOTADA", registro.codigo);
    }
}
