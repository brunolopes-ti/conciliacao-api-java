package br.com.brunolopes.conciliacao.integracao;

import java.math.BigDecimal;

import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

public final class ValidadorResultadoTsv {

    private static final int MAXIMO_DETALHES = 2000;

    private ValidadorResultadoTsv() {
    }

    public static void validar(ResultadoConciliacao resultado) {
        if (resultado == null) {
            throw new IllegalArgumentException(
                    "Resultado da conciliacao nao pode ser nulo.");
        }

        validarQuantidadeDetalhes(resultado);
        validarDiferencas(resultado);
        validarContadores(resultado);
        validarTotalEsperado(resultado);
        validarSaldoGlobal(resultado.resumo());
    }

    private static void validarQuantidadeDetalhes(
            ResultadoConciliacao resultado
    ) {
        if (resultado.detalhes().size() > MAXIMO_DETALHES) {
            throw new IllegalArgumentException(
                    "Resultado TSV excede o limite de "
                    + MAXIMO_DETALHES
                    + " registros DETALHE.");
        }
    }

    private static void validarDiferencas(
            ResultadoConciliacao resultado
    ) {
        for (DetalheConciliacao detalhe : resultado.detalhes()) {
            if (detalhe.valorEsperado() == null
                    || detalhe.valorRecebido() == null
                    || detalhe.diferenca() == null) {
                continue;
            }

            BigDecimal diferencaCalculada =
                    detalhe.valorRecebido()
                            .subtract(detalhe.valorEsperado());

            if (detalhe.diferenca()
                    .compareTo(diferencaCalculada) != 0) {

                throw new IllegalArgumentException(
                        "Diferenca do detalhe nao corresponde "
                        + "a valor_recebido - valor_esperado.");
            }
        }
    }

    private static void validarContadores(
            ResultadoConciliacao resultado
    ) {
        ResumoConciliacao resumo = resultado.resumo();

        validarContador(
                resultado,
                StatusConciliacao.CONFERIDO,
                resumo.conferidos(),
                "conferidos");

        validarContador(
                resultado,
                StatusConciliacao.ACIMA_DO_ESPERADO,
                resumo.acima(),
                "acima");

        validarContador(
                resultado,
                StatusConciliacao.ABAIXO_DO_ESPERADO,
                resumo.abaixo(),
                "abaixo");

        validarContador(
                resultado,
                StatusConciliacao.DUPLICADO,
                resumo.duplicados(),
                "duplicados");

        validarContador(
                resultado,
                StatusConciliacao.SEM_RECEBIMENTO,
                resumo.semRecebimento(),
                "sem_recebimento");

        validarContador(
                resultado,
                StatusConciliacao.SEM_PREVISAO,
                resumo.semPrevisao(),
                "sem_previsao");
    }

    private static void validarContador(
            ResultadoConciliacao resultado,
            StatusConciliacao status,
            int informado,
            String nome
    ) {
        long calculado = resultado.detalhes()
                .stream()
                .filter(detalhe -> detalhe.status() == status)
                .count();

        if (calculado != informado) {
            throw new IllegalArgumentException(
                    "Contador " + nome
                    + " do RESUMO nao corresponde aos detalhes.");
        }
    }

    private static void validarTotalEsperado(
            ResultadoConciliacao resultado
    ) {
        BigDecimal totalCalculado = BigDecimal.ZERO;

        for (DetalheConciliacao detalhe : resultado.detalhes()) {
            if (detalhe.valorEsperado() != null) {
                totalCalculado =
                        totalCalculado.add(
                                detalhe.valorEsperado());
            }
        }

        if (totalCalculado.compareTo(
                resultado.resumo().totalEsperado()) != 0) {

            throw new IllegalArgumentException(
                    "Total esperado do RESUMO "
                    + "nao corresponde aos detalhes.");
        }
    }

    private static void validarSaldoGlobal(
            ResumoConciliacao resumo
    ) {
        BigDecimal saldoCalculado =
                resumo.totalRecebido()
                        .subtract(resumo.totalEsperado());

        if (saldoCalculado.compareTo(
                resumo.saldoGlobal()) != 0) {

            throw new IllegalArgumentException(
                    "Saldo global do RESUMO invalido.");
        }
    }
}
