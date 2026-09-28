package br.com.brunolopes.conciliacao.api.erro;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.brunolopes.conciliacao.api.dto.CodigoErroApi;
import br.com.brunolopes.conciliacao.api.dto.ErroResponse;
import br.com.brunolopes.conciliacao.aplicacao.excecao.ConflitoDadosException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.FalhaCobolException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.RequisicaoInvalidaException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.ResultadoInvalidoException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.TimeoutCobolException;

@RestControllerAdvice
public class TratadorGlobalExcecoes {

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ErroResponse> tratarRequisicaoInvalida(
            RequisicaoInvalidaException erro
    ) {
        return responder(
                HttpStatus.BAD_REQUEST,
                CodigoErroApi.REQUISICAO_INVALIDA,
                erro.getMessage());
    }

    @ExceptionHandler(ConflitoDadosException.class)
    public ResponseEntity<ErroResponse> tratarConflitoDados(
            ConflitoDadosException erro
    ) {
        return responder(
                HttpStatus.CONFLICT,
                CodigoErroApi.CONFLITO_DADOS,
                "O estado atual dos dados impede iniciar a conciliacao.");
    }

    @ExceptionHandler(ResultadoInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarResultadoInvalido(
            ResultadoInvalidoException erro
    ) {
        return responder(
                HttpStatus.UNPROCESSABLE_ENTITY,
                CodigoErroApi.RESULTADO_INVALIDO,
                "O resultado da conciliacao nao passou pelas validacoes.");
    }

    @ExceptionHandler(FalhaCobolException.class)
    public ResponseEntity<ErroResponse> tratarFalhaCobol(
            FalhaCobolException erro
    ) {
        return responder(
                HttpStatus.BAD_GATEWAY,
                CodigoErroApi.FALHA_COBOL,
                "O motor COBOL nao concluiu o processamento.");
    }

    @ExceptionHandler(TimeoutCobolException.class)
    public ResponseEntity<ErroResponse> tratarTimeoutCobol(
            TimeoutCobolException erro
    ) {
        return responder(
                HttpStatus.GATEWAY_TIMEOUT,
                CodigoErroApi.TIMEOUT_COBOL,
                "O processamento da conciliacao excedeu o tempo permitido.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInterno(
            Exception erro
    ) {
        return responder(
                HttpStatus.INTERNAL_SERVER_ERROR,
                CodigoErroApi.ERRO_INTERNO,
                "Ocorreu uma falha interna no processamento.");
    }

    private ResponseEntity<ErroResponse> responder(
            HttpStatus status,
            CodigoErroApi codigo,
            String mensagem
    ) {
        ErroResponse corpo =
                new ErroResponse(
                        codigo.name(),
                        mensagem);

        return ResponseEntity
                .status(status)
                .body(corpo);
    }
}
