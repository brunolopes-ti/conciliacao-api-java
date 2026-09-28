package br.com.brunolopes.conciliacao.api.erro;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import br.com.brunolopes.conciliacao.api.dto.ErroResponse;
import br.com.brunolopes.conciliacao.aplicacao.excecao.ConflitoDadosException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.FalhaCobolException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.ResultadoInvalidoException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.TimeoutCobolException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TratadorGlobalExcecoesTests {

    private final TratadorGlobalExcecoes tratador =
            new TratadorGlobalExcecoes();

    @Test
    void deveRetornar409ParaConflitoDeDados() {
        ResponseEntity<ErroResponse> resposta =
                tratador.tratarConflitoDados(
                        new ConflitoDadosException(
                                "Dados indisponiveis para conciliacao."));

        validar(
                resposta,
                HttpStatus.CONFLICT,
                "CONFLITO_DADOS",
                "Dados indisponiveis para conciliacao.");
    }

    @Test
    void deveRetornar422ParaResultadoInvalido() {
        ResponseEntity<ErroResponse> resposta =
                tratador.tratarResultadoInvalido(
                        new ResultadoInvalidoException(
                                "Resultado da conciliacao invalido."));

        validar(
                resposta,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "RESULTADO_INVALIDO",
                "Resultado da conciliacao invalido.");
    }

    @Test
    void deveRetornar502ParaFalhaCobol() {
        ResponseEntity<ErroResponse> resposta =
                tratador.tratarFalhaCobol(
                        new FalhaCobolException(
                                "Falha no processamento COBOL."));

        validar(
                resposta,
                HttpStatus.BAD_GATEWAY,
                "FALHA_COBOL",
                "Falha no processamento COBOL.");
    }

    @Test
    void deveRetornar504ParaTimeoutCobol() {
        ResponseEntity<ErroResponse> resposta =
                tratador.tratarTimeoutCobol(
                        new TimeoutCobolException(
                                "Tempo limite do COBOL excedido."));

        validar(
                resposta,
                HttpStatus.GATEWAY_TIMEOUT,
                "TIMEOUT_COBOL",
                "Tempo limite do COBOL excedido.");
    }

    @Test
    void deveOcultarDetalhesDoErroInterno() {
        ResponseEntity<ErroResponse> resposta =
                tratador.tratarErroInterno(
                        new RuntimeException(
                                "/tmp/segredo-interno"));

        validar(
                resposta,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ERRO_INTERNO",
                "Ocorreu uma falha interna no processamento.");
    }

    private void validar(
            ResponseEntity<ErroResponse> resposta,
            HttpStatus status,
            String codigo,
            String mensagem
    ) {
        assertEquals(status, resposta.getStatusCode());

        ErroResponse corpo = resposta.getBody();

        assertNotNull(corpo);
        assertEquals(codigo, corpo.codigo());
        assertEquals(mensagem, corpo.mensagem());
    }
}
