package br.com.brunolopes.conciliacao.integracao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

public final class InterpretadorResultadoTsv {

    private static final int VERSAO_SUPORTADA = 1;

    private InterpretadorResultadoTsv() {
    }

    public static ResultadoConciliacao interpretar(List<String> linhas) {
        if (linhas == null || linhas.isEmpty()) {
            throw new IllegalArgumentException(
                    "Resultado TSV vazio.");
        }

        for (String linha : linhas) {
            if (linha == null || linha.isEmpty()) {
                throw new IllegalArgumentException(
                        "Resultado TSV contem linha vazia.");
            }
        }

        int versao = interpretarVersao(linhas.get(0));

        if (linhas.size() < 2) {
            throw new IllegalArgumentException(
                    "Resultado TSV nao possui resumo.");
        }

        List<DetalheConciliacao> detalhes = new ArrayList<>();

        for (int indice = 1; indice < linhas.size() - 1; indice++) {
            detalhes.add(
                    interpretarDetalhe(linhas.get(indice)));
        }

        ResumoConciliacao resumo =
                interpretarResumo(linhas.get(linhas.size() - 1));

        ResultadoConciliacao resultado =
                new ResultadoConciliacao(
                        versao,
                        detalhes,
                        resumo);

        ValidadorResultadoTsv.validar(resultado);

        return resultado;
    }

    private static int interpretarVersao(String linha) {
        String[] campos = separar(linha);

        if (campos.length != 2
                || !"VERSAO".equals(campos[0])) {
            throw new IllegalArgumentException(
                    "Linha VERSAO invalida.");
        }

        int versao = lerInteiroNaoNegativo(
                campos[1],
                "versao");

        if (versao != VERSAO_SUPORTADA) {
            throw new IllegalArgumentException(
                    "Versao do TSV nao suportada: " + versao);
        }

        return versao;
    }

    private static DetalheConciliacao interpretarDetalhe(
            String linha
    ) {
        String[] campos = separar(linha);

        if (campos.length != 7
                || !"DETALHE".equals(campos[0])) {
            throw new IllegalArgumentException(
                    "Linha DETALHE invalida.");
        }

        String identificador = campos[1];

        if (identificador.isEmpty()) {
            throw new IllegalArgumentException(
                    "Identificador vazio no DETALHE.");
        }

        BigDecimal valorEsperado =
                lerValorOpcional(campos[2]);

        BigDecimal valorRecebido =
                lerValorOpcional(campos[3]);

        BigDecimal diferenca =
                lerDiferencaOpcional(campos[4]);

        StatusConciliacao status =
                lerStatus(campos[5]);

        int quantidadeRecebimentos =
                lerInteiroNaoNegativo(
                        campos[6],
                        "quantidade_recebimentos");

        DetalheConciliacao detalhe =
                new DetalheConciliacao(
                        identificador,
                        valorEsperado,
                        valorRecebido,
                        diferenca,
                        status,
                        quantidadeRecebimentos);

        ValidadorDetalheTsv.validar(detalhe);

        return detalhe;
    }

    private static ResumoConciliacao interpretarResumo(
            String linha
    ) {
        String[] campos = separar(linha);

        if (campos.length != 10
                || !"RESUMO".equals(campos[0])) {
            throw new IllegalArgumentException(
                    "Linha RESUMO invalida.");
        }

        return new ResumoConciliacao(
                lerInteiroNaoNegativo(
                        campos[1], "conferidos"),
                lerInteiroNaoNegativo(
                        campos[2], "acima"),
                lerInteiroNaoNegativo(
                        campos[3], "abaixo"),
                lerInteiroNaoNegativo(
                        campos[4], "duplicados"),
                lerInteiroNaoNegativo(
                        campos[5], "sem_recebimento"),
                lerInteiroNaoNegativo(
                        campos[6], "sem_previsao"),
                ConversorMonetarioTsv.lerTotal(campos[7]),
                ConversorMonetarioTsv.lerTotal(campos[8]),
                ConversorMonetarioTsv.lerSaldo(campos[9]));
    }

    private static String[] separar(String linha) {
        return linha.split("\t", -1);
    }

    private static BigDecimal lerValorOpcional(
            String texto
    ) {
        if (texto.isEmpty()) {
            return null;
        }

        return ConversorMonetarioTsv.lerValor(texto);
    }

    private static BigDecimal lerDiferencaOpcional(
            String texto
    ) {
        if (texto.isEmpty()) {
            return null;
        }

        return ConversorMonetarioTsv.lerDiferenca(texto);
    }

    private static StatusConciliacao lerStatus(
            String texto
    ) {
        try {
            return StatusConciliacao.valueOf(texto);
        } catch (IllegalArgumentException erro) {
            throw new IllegalArgumentException(
                    "Status desconhecido no TSV: " + texto,
                    erro);
        }
    }

    private static int lerInteiroNaoNegativo(
            String texto,
            String campo
    ) {
        if (texto == null || !texto.matches("[0-9]+")) {
            throw new IllegalArgumentException(
                    "Inteiro invalido no campo " + campo + ".");
        }

        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException erro) {
            throw new IllegalArgumentException(
                    "Inteiro fora do limite no campo "
                    + campo + ".",
                    erro);
        }
    }
}
