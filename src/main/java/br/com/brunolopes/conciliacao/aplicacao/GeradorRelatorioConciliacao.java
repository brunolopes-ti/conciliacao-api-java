package br.com.brunolopes.conciliacao.aplicacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import br.com.brunolopes.conciliacao.aplicacao.excecao.RelatorioIndisponivelException;
import br.com.brunolopes.conciliacao.modelo.ResumoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;
import br.com.brunolopes.conciliacao.modelo.consulta.ConciliacaoConsultada;
import br.com.brunolopes.conciliacao.modelo.consulta.DetalheConciliacaoConsultado;

public final class GeradorRelatorioConciliacao {

    private GeradorRelatorioConciliacao() {
    }

    public static String gerar(
            ConciliacaoConsultada conciliacao
    ) {
        if (conciliacao == null) {
            throw new IllegalArgumentException(
                    "Conciliacao e obrigatoria."
            );
        }

        if (conciliacao.status()
                != StatusExecucaoConciliacao.CONCLUIDA) {

            throw new RelatorioIndisponivelException();
        }

        if (conciliacao.resumo() == null) {
            throw new IllegalStateException(
                    "Conciliacao concluida sem resumo."
            );
        }

        StringBuilder relatorio =
                new StringBuilder();

        relatorio.append("RELATORIO DE CONCILIACAO\n");
        relatorio.append("ID: ")
                .append(conciliacao.id())
                .append('\n');

        relatorio.append("STATUS: ")
                .append(conciliacao.status().name())
                .append('\n');

        relatorio.append("VERSAO_RESULTADO: ")
                .append(conciliacao.resultadoVersao())
                .append('\n');

        relatorio.append('\n');

        adicionarResumo(
                relatorio,
                conciliacao.resumo()
        );

        relatorio.append('\n');
        relatorio.append("DETALHES\n");

        relatorio.append(
                "ORDEM | IDENTIFICADOR | VALOR_ESPERADO | "
                        + "VALOR_RECEBIDO | DIFERENCA | STATUS | "
                        + "QUANTIDADE_RECEBIMENTOS\n"
        );

        List<DetalheConciliacaoConsultado> detalhes =
                conciliacao.detalhes()
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        DetalheConciliacaoConsultado::ordem
                                )
                        )
                        .toList();

        for (DetalheConciliacaoConsultado detalhe
                : detalhes) {

            relatorio.append(detalhe.ordem())
                    .append(" | ")
                    .append(detalhe.identificador())
                    .append(" | ")
                    .append(formatarValor(
                            detalhe.valorEsperado()
                    ))
                    .append(" | ")
                    .append(formatarValor(
                            detalhe.valorRecebido()
                    ))
                    .append(" | ")
                    .append(formatarValor(
                            detalhe.diferenca()
                    ))
                    .append(" | ")
                    .append(detalhe.status().name())
                    .append(" | ")
                    .append(
                            detalhe.quantidadeRecebimentos()
                    )
                    .append('\n');
        }

        return relatorio.toString();
    }

    private static void adicionarResumo(
            StringBuilder relatorio,
            ResumoConciliacao resumo
    ) {
        relatorio.append("RESUMO\n");

        relatorio.append("CONFERIDOS: ")
                .append(resumo.conferidos())
                .append('\n');

        relatorio.append("ACIMA: ")
                .append(resumo.acima())
                .append('\n');

        relatorio.append("ABAIXO: ")
                .append(resumo.abaixo())
                .append('\n');

        relatorio.append("DUPLICADOS: ")
                .append(resumo.duplicados())
                .append('\n');

        relatorio.append("SEM_RECEBIMENTO: ")
                .append(resumo.semRecebimento())
                .append('\n');

        relatorio.append("SEM_PREVISAO: ")
                .append(resumo.semPrevisao())
                .append('\n');

        relatorio.append("TOTAL_ESPERADO: ")
                .append(
                        formatarValor(
                                resumo.totalEsperado()
                        )
                )
                .append('\n');

        relatorio.append("TOTAL_RECEBIDO: ")
                .append(
                        formatarValor(
                                resumo.totalRecebido()
                        )
                )
                .append('\n');

        relatorio.append("SALDO_GLOBAL: ")
                .append(
                        formatarValor(
                                resumo.saldoGlobal()
                        )
                )
                .append('\n');
    }

    private static String formatarValor(
            BigDecimal valor
    ) {
        if (valor == null) {
            return "-";
        }

        return valor
                .setScale(
                        2,
                        RoundingMode.UNNECESSARY
                )
                .toPlainString();
    }
}
