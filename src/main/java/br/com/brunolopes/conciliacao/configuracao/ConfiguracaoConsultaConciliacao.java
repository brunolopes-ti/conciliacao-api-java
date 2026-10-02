package br.com.brunolopes.conciliacao.configuracao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.brunolopes.conciliacao.aplicacao.ServicoConsultaConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.ServicoConsultaConciliacaoReal;
import br.com.brunolopes.conciliacao.persistencia.RepositorioConsultaConciliacao;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "conciliacao.persistencia.habilitada",
        havingValue = "true",
        matchIfMissing = true
)
public class ConfiguracaoConsultaConciliacao {

    @Bean
    public ServicoConsultaConciliacao servicoConsultaConciliacao(
            RepositorioConsultaConciliacao repositorio
    ) {
        return new ServicoConsultaConciliacaoReal(
                repositorio
        );
    }
}
