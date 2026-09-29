package br.com.brunolopes.conciliacao.configuracao;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.brunolopes.conciliacao.aplicacao.ServicoConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.ServicoConciliacaoReal;
import br.com.brunolopes.conciliacao.integracao.ExecutorCobol;
import br.com.brunolopes.conciliacao.integracao.IntegradorExecucaoCobol;
import br.com.brunolopes.conciliacao.integracao.ProcessadorConciliacaoCobol;
import br.com.brunolopes.conciliacao.integracao.ProcessadorConciliacaoCobolLimitado;
import br.com.brunolopes.conciliacao.integracao.ProcessadorConciliacaoCobolReal;
import br.com.brunolopes.conciliacao.persistencia.RepositorioExecucaoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioResultadoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioSnapshotConciliacao;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "conciliacao.api.execucao-habilitada",
        havingValue = "true"
)
public class ConfiguracaoConciliacaoReal {

    @Bean
    public ExecutorCobol executorCobol(
            @Value("${conciliacao.cobol.executavel:}")
            String executavel,

            @Value("${conciliacao.cobol.timeout-ms:30000}")
            long timeoutMs
    ) {
        if (executavel == null
                || executavel.isBlank()) {

            throw new IllegalStateException(
                    "Caminho do executavel COBOL "
                            + "nao foi configurado."
            );
        }

        Path caminho =
                Path.of(
                        executavel
                );

        if (!caminho.isAbsolute()) {
            throw new IllegalStateException(
                    "Caminho do executavel COBOL "
                            + "deve ser absoluto."
            );
        }

        if (timeoutMs <= 0) {
            throw new IllegalStateException(
                    "Timeout do COBOL deve ser "
                            + "maior que zero."
            );
        }

        ExecutorCobol executor =
                new ExecutorCobol(
                        caminho,
                        Duration.ofMillis(
                                timeoutMs
                        )
                );

        executor.validarExecutavel();

        return executor;
    }

    @Bean
    public IntegradorExecucaoCobol integradorExecucaoCobol(
            ExecutorCobol executorCobol
    ) {
        return new IntegradorExecucaoCobol(
                executorCobol
        );
    }

    @Bean
    public ProcessadorConciliacaoCobol
            processadorConciliacaoCobol(
                    IntegradorExecucaoCobol integrador,

                    @Value(
                            "${conciliacao.cobol.max-concorrencia:1}"
                    )
                    int maxConcorrencia
            ) {

        if (maxConcorrencia <= 0) {
            throw new IllegalStateException(
                    "Limite de concorrencia do COBOL "
                            + "deve ser maior que zero."
            );
        }

        ProcessadorConciliacaoCobol real =
                new ProcessadorConciliacaoCobolReal(
                        integrador
                );

        return new ProcessadorConciliacaoCobolLimitado(
                real,
                maxConcorrencia
        );
    }

    @Bean
    public ServicoConciliacao servicoConciliacao(
            RepositorioExecucaoConciliacao
                    repositorioExecucao,

            RepositorioSnapshotConciliacao
                    repositorioSnapshot,

            RepositorioResultadoConciliacao
                    repositorioResultado,

            ProcessadorConciliacaoCobol
                    processadorCobol
    ) {
        return new ServicoConciliacaoReal(
                repositorioExecucao,
                repositorioSnapshot,
                repositorioResultado,
                processadorCobol
        );
    }
}
