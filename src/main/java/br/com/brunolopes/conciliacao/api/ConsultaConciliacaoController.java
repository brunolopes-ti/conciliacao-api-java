package br.com.brunolopes.conciliacao.api;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.brunolopes.conciliacao.api.dto.ConciliacaoConsultadaResponse;
import br.com.brunolopes.conciliacao.api.dto.PaginaHistoricoConciliacaoResponse;
import br.com.brunolopes.conciliacao.api.mapeamento.ConsultaConciliacaoResponseMapper;
import br.com.brunolopes.conciliacao.api.mapeamento.HistoricoConciliacaoResponseMapper;
import br.com.brunolopes.conciliacao.aplicacao.GeradorRelatorioConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.PaginaHistoricoConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.ServicoConsultaConciliacao;
import br.com.brunolopes.conciliacao.aplicacao.excecao.RequisicaoInvalidaException;
import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;
import br.com.brunolopes.conciliacao.modelo.consulta.ConciliacaoConsultada;

@RestController
@RequestMapping("/api/conciliacoes")
@ConditionalOnProperty(
        name = "conciliacao.persistencia.habilitada",
        havingValue = "true",
        matchIfMissing = true
)
public class ConsultaConciliacaoController {

    private static final Set<String> PARAMETROS_PERMITIDOS =
            Set.of(
                    "pagina",
                    "tamanho",
                    "status"
            );

    private final ServicoConsultaConciliacao servico;

    public ConsultaConciliacaoController(
            ServicoConsultaConciliacao servico
    ) {
        this.servico = servico;
    }

    @GetMapping
    public ResponseEntity<PaginaHistoricoConciliacaoResponse> listar(
            HttpServletRequest requisicao
    ) {
        validarParametros(
                requisicao
        );

        int pagina =
                lerInteiro(
                        requisicao,
                        "pagina",
                        0
                );

        int tamanho =
                lerInteiro(
                        requisicao,
                        "tamanho",
                        20
                );

        StatusExecucaoConciliacao status =
                lerStatus(
                        requisicao.getParameter(
                                "status"
                        )
                );

        PaginaHistoricoConciliacao resultado =
                servico.listar(
                        pagina,
                        tamanho,
                        status
                );

        return ResponseEntity.ok(
                HistoricoConciliacaoResponseMapper.mapear(
                        resultado
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConciliacaoConsultadaResponse> buscarPorId(
            @PathVariable String id,
            HttpServletRequest requisicao
    ) {
        validarSemParametros(
                requisicao
        );

        long conciliacaoId =
                lerId(
                        id
                );

        ConciliacaoConsultada resultado =
                servico.buscarPorId(
                        conciliacaoId
                );

        return ResponseEntity.ok(
                ConsultaConciliacaoResponseMapper.mapear(
                        resultado
                )
        );
    }

    @GetMapping(
            value = "/{id}/relatorio",
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> gerarRelatorio(
            @PathVariable String id,
            HttpServletRequest requisicao
    ) {
        validarSemParametros(
                requisicao
        );

        long conciliacaoId =
                lerId(
                        id
                );

        ConciliacaoConsultada conciliacao =
                servico.buscarPorId(
                        conciliacaoId
                );

        String relatorio =
                GeradorRelatorioConciliacao.gerar(
                        conciliacao
                );

        MediaType tipoConteudo =
                new MediaType(
                        "text",
                        "plain",
                        StandardCharsets.UTF_8
                );

        return ResponseEntity.ok()
                .contentType(
                        tipoConteudo
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"conciliacao-"
                                + conciliacaoId
                                + ".txt\""
                )
                .body(
                        relatorio
                );
    }

    private void validarParametros(
            HttpServletRequest requisicao
    ) {
        Map<String, String[]> parametros =
                requisicao.getParameterMap();

        for (Map.Entry<String, String[]> entrada
                : parametros.entrySet()) {

            String nome =
                    entrada.getKey();

            if (!PARAMETROS_PERMITIDOS.contains(
                    nome
            )) {
                throw new RequisicaoInvalidaException(
                        "Parametro nao permitido: "
                                + nome
                                + "."
                );
            }

            String[] valores =
                    entrada.getValue();

            if (valores == null
                    || valores.length != 1) {

                throw new RequisicaoInvalidaException(
                        "Parametro deve ser informado uma unica vez: "
                                + nome
                                + "."
                );
            }
        }
    }

    private void validarSemParametros(
            HttpServletRequest requisicao
    ) {
        if (!requisicao.getParameterMap().isEmpty()) {
            throw new RequisicaoInvalidaException(
                    "A consulta nao aceita parametros."
            );
        }
    }

    private int lerInteiro(
            HttpServletRequest requisicao,
            String nome,
            int valorPadrao
    ) {
        String valor =
                requisicao.getParameter(
                        nome
                );

        if (valor == null) {
            return valorPadrao;
        }

        try {
            return Integer.parseInt(
                    valor
            );

        } catch (NumberFormatException erro) {
            throw new RequisicaoInvalidaException(
                    "Parametro "
                            + nome
                            + " deve ser um numero inteiro."
            );
        }
    }

    private StatusExecucaoConciliacao lerStatus(
            String valor
    ) {
        if (valor == null) {
            return null;
        }

        if (valor.isBlank()) {
            throw new RequisicaoInvalidaException(
                    "Status nao pode ser vazio."
            );
        }

        try {
            return StatusExecucaoConciliacao.valueOf(
                    valor
            );

        } catch (IllegalArgumentException erro) {
            throw new RequisicaoInvalidaException(
                    "Status de conciliacao invalido."
            );
        }
    }

    private long lerId(
            String valor
    ) {
        try {
            long id =
                    Long.parseLong(
                            valor
                    );

            if (id <= 0) {
                throw new RequisicaoInvalidaException(
                        "Identificador da conciliacao deve ser positivo."
                );
            }

            return id;

        } catch (NumberFormatException erro) {
            throw new RequisicaoInvalidaException(
                    "Identificador da conciliacao deve ser um numero inteiro positivo."
            );
        }
    }
}
