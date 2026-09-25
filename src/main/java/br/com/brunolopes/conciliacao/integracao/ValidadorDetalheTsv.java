package br.com.brunolopes.conciliacao.integracao;

import java.math.BigDecimal;

import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

public final class ValidadorDetalheTsv {

    private ValidadorDetalheTsv() {
    }

    public static void validar(DetalheConciliacao detalhe) {
        if (detalhe == null) {
            throw new IllegalArgumentException(
                    "Detalhe do TSV nao pode ser nulo.");
        }

        if (detalhe.identificador() == null
                || detalhe.identificador().isEmpty()) {
            throw new IllegalArgumentException(
                    "Identificador do detalhe nao pode ser vazio.");
        }

        if (detalhe.status() == null) {
            throw new IllegalArgumentException(
                    "Status do detalhe nao pode ser nulo.");
        }

        switch (detalhe.status()) {
            case CONFERIDO ->
                    validarConferido(detalhe);

            case ACIMA_DO_ESPERADO ->
                    validarAcima(detalhe);

            case ABAIXO_DO_ESPERADO ->
                    validarAbaixo(detalhe);

            case DUPLICADO ->
                    validarDuplicado(detalhe);

            case SEM_RECEBIMENTO ->
                    validarSemRecebimento(detalhe);

            case SEM_PREVISAO ->
                    validarSemPrevisao(detalhe);
        }
    }

    private static void validarConferido(
            DetalheConciliacao detalhe
    ) {
        exigirValoresCompletos(detalhe);

        if (detalhe.diferenca().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException(
                    "CONFERIDO deve possuir diferenca igual a zero.");
        }

        exigirQuantidade(
                detalhe,
                1,
                "CONFERIDO");
    }

    private static void validarAcima(
            DetalheConciliacao detalhe
    ) {
        exigirValoresCompletos(detalhe);

        if (detalhe.diferenca().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "ACIMA_DO_ESPERADO deve possuir diferenca positiva.");
        }

        exigirQuantidade(
                detalhe,
                1,
                "ACIMA_DO_ESPERADO");
    }

    private static void validarAbaixo(
            DetalheConciliacao detalhe
    ) {
        exigirValoresCompletos(detalhe);

        if (detalhe.diferenca().compareTo(BigDecimal.ZERO) >= 0) {
            throw new IllegalArgumentException(
                    "ABAIXO_DO_ESPERADO deve possuir diferenca negativa.");
        }

        exigirQuantidade(
                detalhe,
                1,
                "ABAIXO_DO_ESPERADO");
    }

    private static void validarDuplicado(
            DetalheConciliacao detalhe
    ) {
        exigirValoresCompletos(detalhe);

        if (detalhe.quantidadeRecebimentos() <= 1) {
            throw new IllegalArgumentException(
                    "DUPLICADO deve possuir mais de um recebimento.");
        }
    }

    private static void validarSemRecebimento(
            DetalheConciliacao detalhe
    ) {
        if (detalhe.valorEsperado() == null) {
            throw new IllegalArgumentException(
                    "SEM_RECEBIMENTO exige valor esperado.");
        }

        if (detalhe.valorRecebido() != null) {
            throw new IllegalArgumentException(
                    "SEM_RECEBIMENTO nao deve possuir valor recebido.");
        }

        if (detalhe.diferenca() != null) {
            throw new IllegalArgumentException(
                    "SEM_RECEBIMENTO nao deve possuir diferenca.");
        }

        exigirQuantidade(
                detalhe,
                0,
                "SEM_RECEBIMENTO");
    }

    private static void validarSemPrevisao(
            DetalheConciliacao detalhe
    ) {
        if (detalhe.valorEsperado() != null) {
            throw new IllegalArgumentException(
                    "SEM_PREVISAO nao deve possuir valor esperado.");
        }

        if (detalhe.valorRecebido() == null) {
            throw new IllegalArgumentException(
                    "SEM_PREVISAO exige valor recebido.");
        }

        if (detalhe.diferenca() != null) {
            throw new IllegalArgumentException(
                    "SEM_PREVISAO nao deve possuir diferenca.");
        }

        exigirQuantidade(
                detalhe,
                1,
                "SEM_PREVISAO");
    }

    private static void exigirValoresCompletos(
            DetalheConciliacao detalhe
    ) {
        if (detalhe.valorEsperado() == null
                || detalhe.valorRecebido() == null
                || detalhe.diferenca() == null) {

            throw new IllegalArgumentException(
                    "Status exige esperado, recebido e diferenca.");
        }
    }

    private static void exigirQuantidade(
            DetalheConciliacao detalhe,
            int esperada,
            String status
    ) {
        if (detalhe.quantidadeRecebimentos() != esperada) {
            throw new IllegalArgumentException(
                    status
                    + " exige quantidade_recebimentos = "
                    + esperada
                    + ".");
        }
    }
}
