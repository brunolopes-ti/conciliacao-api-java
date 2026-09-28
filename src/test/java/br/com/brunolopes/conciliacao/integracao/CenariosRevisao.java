package br.com.brunolopes.conciliacao.integracao;

import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import br.com.brunolopes.conciliacao.modelo.*;

/** Cenarios compartilhados entre JUnit e verificacao local sem Spring/Maven. */
final class CenariosRevisao {
    interface Acao { void executar() throws Exception; }
    static void exigir(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
    static void rejeitar(Acao acao) throws Exception {
        try { acao.executar(); }
        catch (IllegalArgumentException | IllegalStateException esperado) { return; }
        throw new AssertionError("Entrada deveria ser rejeitada.");
    }
    static Path script(Path pasta, String codigo) throws Exception {
        Path arquivo = pasta.resolve("programa.sh");
        Files.writeString(arquivo, "#!/bin/sh\n" + codigo);
        Files.setPosixFilePermissions(arquivo,
                java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        return arquivo;
    }
    static SnapshotExecucaoCobol.Item item(String id, String valor) {
        return new SnapshotExecucaoCobol.Item(id, new BigDecimal(valor));
    }
    static SnapshotExecucaoCobol snapshot() {
        return new SnapshotExecucaoCobol(List.of(item("P1", "100.00")),
                List.of(item("P1", "90.00"), item("P1", "20.00"),
                        item("X", "5.00"), item("X", "7.00")));
    }
    static ResultadoConciliacao resultado(String total, String saldo) {
        return InterpretadorResultadoTsv.interpretar(List.of(
                "VERSAO\t1",
                "DETALHE\tP1\t100.00\t90.00\t-10.00\tDUPLICADO\t2",
                "DETALHE\tX\t\t5.00\t\tSEM_PREVISAO\t1",
                "DETALHE\tX\t\t7.00\t\tSEM_PREVISAO\t1",
                "RESUMO\t0\t0\t0\t1\t0\t2\t100.00\t" + total + "\t" + saldo));
    }
    static void snapshotValido() {
        ValidadorSnapshotCobol.validar(snapshot(), resultado("122.00", "22.00"));
    }
    static void snapshotAdulterado() throws Exception {
        // Os totais adulterados sao internamente coerentes, mas omitem o segundo pagamento.
        rejeitar(() -> ValidadorSnapshotCobol.validar(snapshot(), resultado("102.00", "2.00")));
        var correto = resultado("122.00", "22.00");
        var invertidos = new ResultadoConciliacao(1,
                List.of(correto.detalhes().get(0), correto.detalhes().get(2), correto.detalhes().get(1)),
                correto.resumo());
        rejeitar(() -> ValidadorSnapshotCobol.validar(snapshot(), invertidos));
        var primeiroAlterado = new SnapshotExecucaoCobol(List.of(item("P1", "100.00")),
                List.of(item("P1", "20.00"), item("P1", "90.00"), item("X", "5.00"), item("X", "7.00")));
        rejeitar(() -> ValidadorSnapshotCobol.validar(primeiroAlterado, correto));
    }
    static void limiteRelatorio() throws Exception {
        try (var e = DiretorioExecucaoCobol.criar()) {
            Files.writeString(e.resultado(), "TSV");
            try (var arquivo = new RandomAccessFile(e.relatorio().toFile(), "rw")) {
                arquivo.setLength(ValidadorArquivosSaidaCobol.LIMITE_RELATORIO_BYTES);
                ValidadorArquivosSaidaCobol.validar(e);
                arquivo.setLength(ValidadorArquivosSaidaCobol.LIMITE_RELATORIO_BYTES + 1);
                rejeitar(() -> ValidadorArquivosSaidaCobol.validar(e));
            }
        }
    }
    static void limiteTsv() throws Exception {
        try (var e = DiretorioExecucaoCobol.criar()) {
            try (var arquivo = new RandomAccessFile(e.resultado().toFile(), "rw")) {
                arquivo.setLength(2L * 1024 * 1024 + 1);
            }
            rejeitar(() -> LeitorResultadoTsv.ler(e.diretorio(), e.resultado()));
        }
    }
    static void limpeza() throws Exception {
        Path externo = Files.createTempFile("preservar-", ".txt");
        Path pasta;
        try {
            Files.writeString(externo, "preservado");
            try (var e = DiretorioExecucaoCobol.criar()) {
                pasta = e.diretorio();
                Files.createSymbolicLink(e.diretorio().resolve("link"), externo);
                Files.writeString(e.relatorio(), "relatorio");
            }
            exigir(!Files.exists(pasta), "Pasta temporaria permaneceu.");
            exigir(Files.readString(externo).equals("preservado"), "Destino do link foi alterado.");
        } finally { Files.deleteIfExists(externo); }
    }
    static void stdin() throws Exception {
        try (var e = DiretorioExecucaoCobol.criar()) {
            var p = script(e.diretorio(), "cat >/dev/null\nprintf pronto\n");
            var r = new ExecutorCobol(p, Duration.ofSeconds(2)).executar(List.of());
            exigir(r.sucesso() && r.saidaProcesso().equals("pronto"), "EOF nao recebido.");
        }
    }
    static void saidaExcessiva() throws Exception {
        try (var e = DiretorioExecucaoCobol.criar()) {
            var p = script(e.diretorio(), "head -c 200000 /dev/zero | tr '\\000' x\n");
            var r = new ExecutorCobol(p, Duration.ofSeconds(5)).executar(List.of());
            exigir(r.sucesso() && r.saidaTruncada() && r.saidaProcesso().length() == 65536,
                    "Captura excedeu limite ou processo bloqueou.");
        }
    }
    static void orfao() throws Exception {
        try (var e = DiretorioExecucaoCobol.criar()) {
            var pid = e.diretorio().resolve("filho.pid");
            var p = script(e.diretorio(), "sleep 10 &\nprintf '%s' $! > \"$1\"\nexit 0\n");
            long inicio = System.nanoTime();
            try {
                var r = new ExecutorCobol(p).executar(List.of(pid.toString()));
                exigir(r.sucesso(), "Processo principal falhou.");
                exigir(Duration.ofNanos(System.nanoTime() - inicio).toMillis() < 3500,
                        "Coleta ficou bloqueada.");
            } finally {
                if (Files.exists(pid)) {
                    ProcessHandle.of(Long.parseLong(Files.readString(pid)))
                            .ifPresent(ProcessHandle::destroyForcibly);
                }
            }
        }
    }
    static void interrupcao() throws Exception {
        try (var e = DiretorioExecucaoCobol.criar()) {
            Path pid = e.diretorio().resolve("filho.pid");
            Path p = script(e.diretorio(), "sleep 30 &\nprintf '%s' $! > \"$1\"\nwait\n");
            var interrompida = new AtomicBoolean();
            var falha = new AtomicReference<Throwable>();
            Thread t = new Thread(() -> {
                try { new ExecutorCobol(p).executar(List.of(pid.toString())); }
                catch (Throwable erro) { falha.set(erro); interrompida.set(Thread.currentThread().isInterrupted()); }
            });
            t.start();
            try {
                for (int i = 0; i < 500 && (!Files.exists(pid) || Files.size(pid) == 0); i++) Thread.sleep(10);
                exigir(Files.exists(pid), "Filho nao iniciou.");
                t.interrupt();
                t.join(10000);
                exigir(!t.isAlive() && falha.get() instanceof IllegalStateException && interrompida.get(),
                        "Interrupcao nao foi preservada.");
                long numero = Long.parseLong(Files.readString(pid));
                exigir(!ProcessHandle.of(numero).map(ProcessHandle::isAlive).orElse(false), "Filho permaneceu vivo.");
            } finally {
                t.interrupt();
                t.join(10000);
                if (Files.exists(pid) && Files.size(pid) > 0) {
                    ProcessHandle.of(Long.parseLong(Files.readString(pid))).ifPresent(ProcessHandle::destroyForcibly);
                }
            }
        }
    }
    static void cobolReal() throws Exception {
        String configurado = System.getenv("COBOL_EXECUTAVEL_TESTE");
        if (configurado == null || configurado.isBlank()) throw new AssertionError("Defina COBOL_EXECUTAVEL_TESTE.");
        try (var e = DiretorioExecucaoCobol.criar()) {
            Files.writeString(e.esperados(), "P1;100.00\nP2;10.00\nP3;10.00\nP4;10.00\nP5;10.00\n");
            Files.writeString(e.recebidos(), "P1;90.00\nP1;20.00\nX;5.00\nX;7.00\nP2;10.00\nP3;15.00\nP4;5.00\n");
            var snapshot = new SnapshotExecucaoCobol(
                    List.of(item("P1", "100.00"), item("P2", "10.00"), item("P3", "10.00"), item("P4", "10.00"), item("P5", "10.00")),
                    List.of(item("P1", "90.00"), item("P1", "20.00"), item("X", "5.00"), item("X", "7.00"), item("P2", "10.00"), item("P3", "15.00"), item("P4", "5.00")));
            var r = new IntegradorExecucaoCobol(new ExecutorCobol(Path.of(configurado))).executar(e, snapshot);
            exigir(r.resultado().detalhes().size() == 7, "Quantidade de detalhes incorreta.");
            exigir(r.resultado().resumo().totalRecebido().compareTo(new BigDecimal("152.00")) == 0, "Total bruto incorreto.");
            exigir(r.resultado().detalhes().stream().map(DetalheConciliacao::status).distinct().count() == 6,
                    "Nao foram exercitados todos os status.");
            exigir(!Files.readString(r.relatorio()).isBlank(), "Relatorio vazio.");
        }
    }
    public static void main(String[] args) throws Exception {
        Acao[] acoes = { CenariosRevisao::snapshotValido, CenariosRevisao::snapshotAdulterado,
                CenariosRevisao::limiteRelatorio, CenariosRevisao::limiteTsv, CenariosRevisao::limpeza,
                CenariosRevisao::stdin, CenariosRevisao::saidaExcessiva, CenariosRevisao::orfao,
                CenariosRevisao::interrupcao, CenariosRevisao::cobolReal };
        String[] nomes = { "snapshot valido", "snapshot adulterado", "limite relatorio", "limite TSV",
                "limpeza preserva destino de link", "stdin fechado", "saida limitada", "coleta sem bloqueio",
                "interrupcao encerra filho", "integracao GnuCOBOL real com seis status" };
        for (int i = 0; i < acoes.length; i++) { acoes[i].executar(); System.out.println("PASSOU: " + nomes[i]); }
    }
}
