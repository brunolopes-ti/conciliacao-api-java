package br.com.brunolopes.conciliacao.configuracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import br.com.brunolopes.conciliacao.aplicacao.RecuperadorExecucoesAbandonadas;
import br.com.brunolopes.conciliacao.integracao.LimpadorDiretoriosTemporariosCobol;
import br.com.brunolopes.conciliacao.persistencia.RepositorioExecucaoConciliacao;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        name = "conciliacao.persistencia.habilitada",
        havingValue = "true",
        matchIfMissing = true
)
public class ConfiguracaoRecuperacaoConciliacao {

    @Bean
    @ConditionalOnProperty(
            name = "conciliacao.recuperacao.habilitada",
            havingValue = "true",
            matchIfMissing = true
    )
    public RecuperadorExecucoesAbandonadas recuperadorExecucoesAbandonadas(
            RepositorioExecucaoConciliacao repositorio,
            @Value("${conciliacao.recuperacao.idade-minima-ms:300000}")
            long idadeMinimaMs
    ) {
        return new RecuperadorExecucoesAbandonadas(
                repositorio,
                idadeMinimaMs
        );
    }

    @Bean
    @ConditionalOnProperty(
            name = "conciliacao.recuperacao.habilitada",
            havingValue = "true",
            matchIfMissing = true
    )
    public LimpadorDiretoriosTemporariosCobol limpadorTemporariosCobol(
            @Value("${conciliacao.recuperacao.temporarios-idade-minima-ms:1800000}")
            long idadeMinimaMs
    ) {
        return new LimpadorDiretoriosTemporariosCobol(
                idadeMinimaMs
        );
    }

    @Bean
    @ConditionalOnProperty(
            name = "conciliacao.recuperacao.habilitada",
            havingValue = "true",
            matchIfMissing = true
    )
    public RecuperacaoAgendada recuperacaoAgendada(
            RecuperadorExecucoesAbandonadas recuperador,
            LimpadorDiretoriosTemporariosCobol limpador
    ) {
        return new RecuperacaoAgendada(
                recuperador,
                limpador
        );
    }

    public static final class RecuperacaoAgendada {

        private static final Logger LOG =
                LoggerFactory.getLogger(
                        RecuperacaoAgendada.class
                );

        private final RecuperadorExecucoesAbandonadas recuperador;
        private final LimpadorDiretoriosTemporariosCobol limpador;

        private RecuperacaoAgendada(
                RecuperadorExecucoesAbandonadas recuperador,
                LimpadorDiretoriosTemporariosCobol limpador
        ) {
            this.recuperador = recuperador;
            this.limpador = limpador;
        }

        @Scheduled(
                initialDelayString =
                        "${conciliacao.recuperacao.atraso-inicial-ms:10000}",
                fixedDelayString =
                        "${conciliacao.recuperacao.intervalo-ms:60000}"
        )
        public void executar() {
            try {
                int recuperadas = recuperador.recuperar();
                int diretoriosRemovidos = limpador.limpar();

                if (recuperadas > 0 || diretoriosRemovidos > 0) {
                    LOG.warn(
                            "Recuperacao operacional: execucoes={}, temporarios={}",
                            recuperadas,
                            diretoriosRemovidos
                    );
                }

            } catch (RuntimeException erro) {
                LOG.error(
                        "Falha na rotina de recuperacao operacional.",
                        erro
                );
            }
        }
    }
}
