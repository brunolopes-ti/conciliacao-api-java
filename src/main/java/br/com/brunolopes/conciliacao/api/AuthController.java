package br.com.brunolopes.conciliacao.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.brunolopes.conciliacao.api.dto.CadastroUsuarioRequest;
import br.com.brunolopes.conciliacao.api.dto.UsuarioCadastradoResponse;
import br.com.brunolopes.conciliacao.aplicacao.ServicoCadastroUsuario;
import br.com.brunolopes.conciliacao.modelo.usuario.UsuarioCadastrado;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(
        name = "conciliacao.persistencia.habilitada",
        havingValue = "true",
        matchIfMissing = true
)
public class AuthController {

    private final ServicoCadastroUsuario servicoCadastroUsuario;

    public AuthController(
            ServicoCadastroUsuario servicoCadastroUsuario
    ) {
        if (servicoCadastroUsuario == null) {
            throw new IllegalArgumentException(
                    "Servico de cadastro de usuario e obrigatorio."
            );
        }

        this.servicoCadastroUsuario =
                servicoCadastroUsuario;
    }

    @PostMapping(
            value = "/cadastro",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UsuarioCadastradoResponse> cadastrar(
            @RequestBody CadastroUsuarioRequest requisicao
    ) {
        UsuarioCadastrado usuario =
                servicoCadastroUsuario.cadastrar(
                        requisicao.nome(),
                        requisicao.email(),
                        requisicao.senha()
                );

        UsuarioCadastradoResponse resposta =
                new UsuarioCadastradoResponse(
                        usuario.id(),
                        usuario.nome(),
                        usuario.email(),
                        usuario.ativo(),
                        usuario.criadoEm()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resposta);
    }
}
