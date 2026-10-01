package br.com.brunolopes.conciliacao.integracao;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import br.com.brunolopes.conciliacao.aplicacao.excecao.CapacidadeEsgotadaException;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;

class RevisaoBloco5IntegracaoTests {
    @TempDir Path pasta;
    private SnapshotExecucaoCobol snapshot() {
        var itens = List.of(new SnapshotExecucaoCobol.Item("P1", BigDecimal.ONE));
        return new SnapshotExecucaoCobol(itens, itens);
    }
    @Test void rejeitaSaturacaoSemInvocarMotorELiberaVagaDepois() throws Exception {
        var entrou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        var chamadas = new java.util.concurrent.atomic.AtomicInteger();
        var limitado = new ProcessadorConciliacaoCobolLimitado(s -> {
            chamadas.incrementAndGet(); entrou.countDown();
            try { if (!liberar.await(5, TimeUnit.SECONDS)) throw new AssertionError("Teste bloqueado"); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            return null;
        }, 1, 20);
        var pool = Executors.newSingleThreadExecutor();
        try {
            var primeira = pool.submit(() -> limitado.processar(snapshot()));
            assertTrue(entrou.await(2, TimeUnit.SECONDS));
            assertThrows(CapacidadeEsgotadaException.class, () -> limitado.processar(snapshot()));
            assertEquals(1, chamadas.get());
            liberar.countDown(); primeira.get(2, TimeUnit.SECONDS);
            limitado.processar(snapshot()); assertEquals(2, chamadas.get());
        } finally { liberar.countDown(); pool.shutdownNow(); }
    }
    @Test void falhaDoDelegadoNaoConsomeVaga() {
        var limitado = new ProcessadorConciliacaoCobolLimitado(s -> {
            throw new IllegalArgumentException("falha simulada");
        }, 1, 0);
        assertThrows(IllegalArgumentException.class, () -> limitado.processar(snapshot()));
        assertThrows(IllegalArgumentException.class, () -> limitado.processar(snapshot()));
    }
    @Test void rejeitaEsperaNegativa() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProcessadorConciliacaoCobolLimitado(s -> null, 1, -1));
    }
    @Test void preservaDiagnosticoParaSaidaConhecidaEDesconhecida() throws Exception {
        for (int codigo : new int[]{1, 99}) {
            Path script = pasta.resolve("motor" + codigo);
            Files.writeString(script, "#!/bin/sh\nprintf 'diagnostico de teste\\n'\nexit " + codigo + "\n");
            Files.setPosixFilePermissions(script,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
            try (var diretorio = DiretorioExecucaoCobol.criar()) {
                var erro = assertThrows(FalhaExecucaoCobolException.class,
                        () -> new IntegradorExecucaoCobol(new ExecutorCobol(script)).executar(diretorio));
                assertTrue(erro.diagnostico().contains("diagnostico de teste"));
            }
        }
    }
    @Test void limitaDiagnosticoERemoveControles() {
        var erro = new FalhaExecucaoCobolException("falha", null, "x\n\u001b".repeat(10000));
        assertEquals(2000, erro.diagnostico().codePointCount(0, erro.diagnostico().length()));
        assertFalse(erro.diagnostico().codePoints().anyMatch(Character::isISOControl));
    }
}
