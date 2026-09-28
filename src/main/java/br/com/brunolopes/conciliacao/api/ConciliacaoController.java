package br.com.brunolopes.conciliacao.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.brunolopes.conciliacao.api.dto.ConciliacaoResponse;
import br.com.brunolopes.conciliacao.api.mapeamento.ConciliacaoResponseMapper;
import br.com.brunolopes.conciliacao.aplicacao.ResultadoServicoConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.ServicoConciliacao;

@RestController
@RequestMapping("/api/conciliacoes")
@ConditionalOnBean(ServicoConciliacao.class)
public class ConciliacaoController {

    private final ServicoConciliacao servicoConciliacao;

    public ConciliacaoController(
            ServicoConciliacao servicoConciliacao
    ) {
        this.servicoConciliacao = servicoConciliacao;
    }

    @PostMapping
    public ResponseEntity<ConciliacaoResponse> criar() {
        ResultadoServicoConciliacao resultado =
                servicoConciliacao.executar();

        ConciliacaoResponse resposta =
                ConciliacaoResponseMapper.mapear(resultado);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resposta);
    }
}
