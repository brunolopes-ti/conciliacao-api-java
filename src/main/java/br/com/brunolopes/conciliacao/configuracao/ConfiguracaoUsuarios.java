package br.com.brunolopes.conciliacao.configuracao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.brunolopes.conciliacao.aplicacao.ServicoCadastroUsuario;
import br.com.brunolopes.conciliacao.aplicacao.ServicoCadastroUsuarioReal;
import br.com.brunolopes.conciliacao.persistencia.RepositorioUsuario;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "conciliacao.persistencia.habilitada",
        havingValue = "true",
        matchIfMissing = true
)
public class ConfiguracaoUsuarios {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ServicoCadastroUsuario servicoCadastroUsuario(
            RepositorioUsuario repositorioUsuario,
            PasswordEncoder passwordEncoder
    ) {
        return new ServicoCadastroUsuarioReal(
                repositorioUsuario,
                passwordEncoder
        );
    }
}
