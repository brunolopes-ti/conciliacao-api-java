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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TratadorGlobalExcecoesTests {

    private final TratadorGlobalExcecoes tratador =
            new TratadorGlobalExcecoes();

    @Test
    void deveRetornar503ParaCapacidadeEsgotada() {
        var resposta = tratador.tratarCapacidadeEsgotada(
                new br.com.brunolopes.conciliacao.aplicacao.excecao.CapacidadeEsgotadaException());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resposta.getStatusCode());
        assertEquals("CAPACIDADE_ESGOTADA", resposta.getBody().codigo());
    }

    @Test
    void deveRetornar409ParaConflitoDeDados() {
        String detalheInterno =
                "/tmp/dados-internos";

        ResponseEntity<ErroResponse> resposta =
                tratador.tratarConflitoDados(
                        new ConflitoDadosException(
                                detalheInterno));

        validar(
                resposta,
                HttpStatus.CONFLICT,
                "CONFLITO_DADOS",
                "O estado atual dos dados impede iniciar a conciliacao.");

        assertFalse(
                resposta.getBody()
                        .mensagem()
                        .contains(detalheInterno));
    }

    @Test
    void deveRetornar422ParaResultadoInvalido() {
        String detalheInterno =
                "resultado.tsv em /tmp/execucao";

        ResponseEntity<ErroResponse> resposta =
                tratador.tratarResultadoInvalido(
                        new ResultadoInvalidoException(
                                detalheInterno));

        validar(
                resposta,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "RESULTADO_INVALIDO",
                "O resultado da conciliacao nao passou pelas validacoes.");

        assertFalse(
                resposta.getBody()
                        .mensagem()
                        .contains(detalheInterno));
    }

    @Test
    void deveRetornar502ParaFalhaCobol() {
        String detalheInterno =
                "/home/usuario/bin/conciliacao exit=1";

        ResponseEntity<ErroResponse> resposta =
                tratador.tratarFalhaCobol(
                        new FalhaCobolException(
                                detalheInterno));

        validar(
                resposta,
                HttpStatus.BAD_GATEWAY,
                "FALHA_COBOL",
                "O motor COBOL nao concluiu o processamento.");

        assertFalse(
                resposta.getBody()
                        .mensagem()
                        .contains(detalheInterno));
    }

    @Test
    void deveRetornar504ParaTimeoutCobol() {
        String detalheInterno =
                "timeout executando /home/usuario/conciliacao";

        ResponseEntity<ErroResponse> resposta =
                tratador.tratarTimeoutCobol(
                        new TimeoutCobolException(
                                detalheInterno));

        validar(
                resposta,
                HttpStatus.GATEWAY_TIMEOUT,
                "TIMEOUT_COBOL",
                "O processamento da conciliacao excedeu o tempo permitido.");

        assertFalse(
                resposta.getBody()
                        .mensagem()
                        .contains(detalheInterno));
    }

    @Test
    void deveOcultarDetalhesDoErroInterno() {
        String detalheInterno =
                "/tmp/segredo-interno";

        ResponseEntity<ErroResponse> resposta =
                tratador.tratarErroInterno(
                        new RuntimeException(
                                detalheInterno));

        validar(
                resposta,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ERRO_INTERNO",
                "Ocorreu uma falha interna no processamento.");

        assertFalse(
                resposta.getBody()
                        .mensagem()
                        .contains(detalheInterno));
    }

    private void validar(
            ResponseEntity<ErroResponse> resposta,
            HttpStatus status,
            String codigo,
            String mensagem
    ) {
        assertEquals(
                status,
                resposta.getStatusCode());

        ErroResponse corpo =
                resposta.getBody();

        assertNotNull(corpo);
        assertEquals(codigo, corpo.codigo());
        assertEquals(mensagem, corpo.mensagem());
    }
}
