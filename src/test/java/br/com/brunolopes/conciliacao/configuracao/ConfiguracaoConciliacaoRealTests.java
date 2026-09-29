package br.com.brunolopes.conciliacao.configuracao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import br.com.brunolopes.conciliacao.aplicacao.ServicoConciliacao;
import br.com.brunolopes.conciliacao.integracao.ExecutorCobol;
import br.com.brunolopes.conciliacao.persistencia.RepositorioExecucaoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioResultadoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioSnapshotConciliacao;

class ConfiguracaoConciliacaoRealTests {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            ConfiguracaoConciliacaoReal.class
                    )
                    .withBean(
                            RepositorioExecucaoConciliacao.class,
                            RepositorioExecucaoTeste::new
                    )
                    .withBean(
                            RepositorioSnapshotConciliacao.class,
                            () -> conciliacaoId -> null
                    )
                    .withBean(
                            RepositorioResultadoConciliacao.class,
                            () -> (
                                    conciliacaoId,
                                    resultado
                            ) -> {
                            }
                    );

    @Test
    void naoDeveCriarIntegracaoRealQuandoApiEstaDesabilitada() {
        contextRunner
                .withPropertyValues(
                        "conciliacao.api.execucao-habilitada=false"
                )
                .run(contexto -> {
                    assertThat(
                            contexto.getBeansOfType(
                                    ServicoConciliacao.class
                            )
                    ).isEmpty();

                    assertThat(
                            contexto.getBeansOfType(
                                    ExecutorCobol.class
                            )
                    ).isEmpty();
                });
    }

    @Test
    void deveMontarIntegracaoRealQuandoConfiguracaoEValida() {
        contextRunner
                .withPropertyValues(
                        "conciliacao.api.execucao-habilitada=true",
                        "conciliacao.cobol.executavel=/bin/true",
                        "conciliacao.cobol.timeout-ms=1000"
                )
                .run(contexto -> {
                    assertThat(
                            contexto
                    ).hasNotFailed();

                    assertThat(
                            contexto.getBeansOfType(
                                    ExecutorCobol.class
                            )
                    ).hasSize(1);

                    assertThat(
                            contexto.getBeansOfType(
                                    ServicoConciliacao.class
                            )
                    ).hasSize(1);
                });
    }

    @Test
    void deveFalharAoHabilitarApiSemExecutavelCobol() {
        contextRunner
                .withPropertyValues(
                        "conciliacao.api.execucao-habilitada=true",
                        "conciliacao.cobol.executavel=",
                        "conciliacao.cobol.timeout-ms=1000"
                )
                .run(contexto ->
                        assertThat(
                                contexto
                        ).hasFailed()
                );
    }

    private static final class RepositorioExecucaoTeste
            implements RepositorioExecucaoConciliacao {

        @Override
        public long criar() {
            return 1L;
        }

        @Override
        public void iniciar(
                long conciliacaoId
        ) {
        }

        @Override
        public void falhar(
                long conciliacaoId,
                String codigoErro,
                String detalheErro
        ) {
        }
    }
}
