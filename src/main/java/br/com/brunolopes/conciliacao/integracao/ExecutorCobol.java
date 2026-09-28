package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ExecutorCobol {

    static final int LIMITE_SAIDA_BYTES =
            64 * 1024;

    static final Duration TIMEOUT_PADRAO =
            Duration.ofSeconds(30);

    static final Duration TEMPO_ENCERRAMENTO =
            Duration.ofSeconds(2);

    static final Duration TEMPO_COLETA_SAIDA =
            Duration.ofSeconds(2);

    private final Path executavel;
    private final Duration timeout;

    public ExecutorCobol(Path executavel) {
        this(
                executavel,
                TIMEOUT_PADRAO);
    }

    public ExecutorCobol(
            Path executavel,
            Duration timeout
    ) {
        if (executavel == null) {
            throw new IllegalArgumentException(
                    "Caminho do executavel COBOL e obrigatorio.");
        }

        if (timeout == null
                || timeout.isZero()
                || timeout.isNegative()) {

            throw new IllegalArgumentException(
                    "Timeout deve ser maior que zero.");
        }

        this.executavel =
                executavel.toAbsolutePath().normalize();

        this.timeout =
                timeout;
    }

    public Path executavel() {
        return executavel;
    }

    public void validarExecutavel() {
        if (!Files.exists(executavel)) {
            throw new IllegalArgumentException(
                    "Executavel COBOL nao existe.");
        }

        if (!Files.isRegularFile(executavel)) {
            throw new IllegalArgumentException(
                    "Executavel COBOL deve ser um arquivo regular.");
        }

        if (!Files.isExecutable(executavel)) {
            throw new IllegalArgumentException(
                    "Arquivo COBOL nao possui permissao de execucao.");
        }
    }

    public ResultadoExecucaoProcesso executar(
            ArgumentosExecucaoCobol argumentos
    ) {
        if (argumentos == null) {
            throw new IllegalArgumentException(
                    "Argumentos da execucao COBOL sao obrigatorios.");
        }

        return executar(
                argumentos.comoLista());
    }

    public ResultadoExecucaoProcesso executar(
            List<String> argumentos
    ) {
        validarExecutavel();

        if (argumentos == null) {
            throw new IllegalArgumentException(
                    "Lista de argumentos nao pode ser nula.");
        }

        List<String> comando =
                new ArrayList<>();

        comando.add(
                executavel.toString());

        comando.addAll(
                argumentos);

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        comando);

        processBuilder.redirectErrorStream(
                true);

        Process processo = null;

        Future<SaidaProcesso> futuraSaida =
                null;

        ExecutorService executorSaida =
                Executors.newSingleThreadExecutor();

        try {
            processo =
                    processBuilder.start();

            // O contrato nao utiliza stdin: EOF imediato evita espera por entrada.
            processo.getOutputStream().close();

            Process processoEmExecucao =
                    processo;

            futuraSaida =
                    executorSaida.submit(
                            () ->
                                    ColetorSaidaProcesso.coletar(
                                            processoEmExecucao,
                                            LIMITE_SAIDA_BYTES));

            boolean terminou =
                    processo.waitFor(
                            timeout.toMillis(),
                            TimeUnit.MILLISECONDS);

            if (!terminou) {
                encerrarArvoreProcessos(
                        processo);

                throw new IllegalStateException(
                        "Tempo limite da execucao COBOL excedido.");
            }

            int codigoSaida =
                    processo.exitValue();

            SaidaProcesso saida =
                    obterSaida(
                            processo,
                            futuraSaida);

            return new ResultadoExecucaoProcesso(
                    codigoSaida,
                    saida.conteudo(),
                    saida.truncada());

        } catch (IOException erro) {
            throw new IllegalStateException(
                    "Falha ao iniciar ou ler "
                    + "a saida do processo externo.",
                    erro);

        } catch (InterruptedException erro) {
            // InterruptedException limpa o sinal; limpar a arvore antes de restaura-lo.
            IllegalStateException falha = new IllegalStateException(
                    "Execucao do processo foi interrompida.", erro);
            try {
                if (processo != null && processo.isAlive()) {
                    encerrarArvoreProcessos(processo);
                }
            } catch (InterruptedException | RuntimeException encerramento) {
                falha.addSuppressed(encerramento);
            } finally {
                Thread.currentThread().interrupt();
            }
            throw falha;

        } catch (ExecutionException erro) {
            throw new IllegalStateException(
                    "Falha ao coletar "
                    + "a saida do processo externo.",
                    erro.getCause());

        } finally {
            limparRecursos(
                    processo,
                    futuraSaida,
                    executorSaida);
        }
    }

    private SaidaProcesso obterSaida(
            Process processo,
            Future<SaidaProcesso> futuraSaida
    ) throws InterruptedException, ExecutionException {

        try {
            return futuraSaida.get(
                    TEMPO_COLETA_SAIDA.toMillis(),
                    TimeUnit.MILLISECONDS);

        } catch (TimeoutException erro) {

            if (processo.isAlive()) {
                encerrarArvoreProcessos(
                        processo);
            }

            futuraSaida.cancel(
                    true);

            try {
                processo.getInputStream()
                        .close();

            } catch (IOException ignored) {
            }

            throw new IllegalStateException(
                    "Tempo limite para coletar "
                    + "a saida do processo COBOL excedido.",
                    erro);
        }
    }

    private void encerrarArvoreProcessos(
            Process processo
    ) throws InterruptedException {

        List<ProcessHandle> descendentes =
                processo.descendants()
                        .toList();

        ProcessHandle principal =
                processo.toHandle();

        for (ProcessHandle descendente
                : descendentes) {

            if (descendente.isAlive()) {
                descendente.destroy();
            }
        }

        // Encerrar filhos antes do pai permite que ele recolha seus processos.
        if (!descendentes.isEmpty()) {
            long fim = System.nanoTime() + TEMPO_ENCERRAMENTO.toNanos();
            while (descendentes.stream().anyMatch(ProcessHandle::isAlive)
                    && System.nanoTime() < fim) {
                Thread.sleep(10);
            }
            for (ProcessHandle filho : descendentes) {
                if (filho.isAlive()) {
                    filho.destroyForcibly();
                }
            }
            processo.waitFor(TEMPO_ENCERRAMENTO.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (principal.isAlive()) {
            principal.destroy();
        }

        boolean terminou =
                aguardarEncerramento(
                        principal,
                        descendentes,
                        TEMPO_ENCERRAMENTO);

        if (terminou) {
            return;
        }

        for (ProcessHandle descendente
                : descendentes) {

            if (descendente.isAlive()) {
                descendente.destroyForcibly();
            }
        }

        if (principal.isAlive()) {
            principal.destroyForcibly();
        }

        terminou =
                aguardarEncerramento(
                        principal,
                        descendentes,
                        TEMPO_ENCERRAMENTO);

        if (!terminou) {
            throw new IllegalStateException(
                    "Nao foi possivel encerrar "
                    + "completamente o processo COBOL.");
        }
    }

    private boolean aguardarEncerramento(
            ProcessHandle principal,
            List<ProcessHandle> descendentes,
            Duration limite
    ) throws InterruptedException {

        long fim =
                System.nanoTime()
                + limite.toNanos();

        while (System.nanoTime() < fim) {

            boolean principalVivo =
                    principal.isAlive();

            boolean descendenteVivo =
                    descendentes.stream()
                            .anyMatch(
                                    ProcessHandle::isAlive);

            if (!principalVivo
                    && !descendenteVivo) {

                return true;
            }

            Thread.sleep(10);
        }

        boolean principalVivo =
                principal.isAlive();

        boolean descendenteVivo =
                descendentes.stream()
                        .anyMatch(
                                ProcessHandle::isAlive);

        return !principalVivo
                && !descendenteVivo;
    }

    private void limparRecursos(
            Process processo,
            Future<SaidaProcesso> futuraSaida,
            ExecutorService executorSaida
    ) {
        if (futuraSaida != null
                && !futuraSaida.isDone()) {

            futuraSaida.cancel(
                    true);
        }

        if (processo != null) {

            if (processo.isAlive()) {
                processo.destroyForcibly();
            }

            fecharFluxos(
                    processo);
        }

        executorSaida.shutdownNow();

        try {
            executorSaida.awaitTermination(
                    1,
                    TimeUnit.SECONDS);

        } catch (InterruptedException erro) {
            Thread.currentThread()
                    .interrupt();
        }
    }

    private void fecharFluxos(
            Process processo
    ) {
        try {
            processo.getInputStream()
                    .close();

        } catch (IOException ignored) {
        }

        try {
            processo.getErrorStream()
                    .close();

        } catch (IOException ignored) {
        }

        try {
            processo.getOutputStream()
                    .close();

        } catch (IOException ignored) {
        }
    }
}
