package br.com.brunolopes.conciliacao.integracao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import br.com.brunolopes.conciliacao.modelo.DetalheConciliacao;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusConciliacao;

/** Confere ocorrencias, ordem, primeiro pagamento e totais contra as entradas. */
public final class ValidadorSnapshotCobol {
    private ValidadorSnapshotCobol() { }

    public static void validar(SnapshotExecucaoCobol snapshot, ResultadoConciliacao resultado) {
        if (snapshot == null || resultado == null || resultado.versao() != 1) {
            throw new IllegalArgumentException("Snapshot e resultado versao 1 obrigatorios.");
        }
        ValidadorResultadoTsv.validar(resultado);
        var esperados = new ArrayList<DetalheConciliacao>();
        var ids = new HashSet<String>();
        for (var cobranca : snapshot.cobrancas()) {
            ids.add(cobranca.identificador());
            var recebimentos = snapshot.pagamentos().stream()
                    .filter(p -> p.identificador().equals(cobranca.identificador())).toList();
            if (recebimentos.isEmpty()) {
                esperados.add(new DetalheConciliacao(cobranca.identificador(), cobranca.valor(),
                        null, null, StatusConciliacao.SEM_RECEBIMENTO, 0));
                continue;
            }
            var primeiro = recebimentos.get(0).valor();
            var diferenca = primeiro.subtract(cobranca.valor());
            var status = recebimentos.size() > 1 ? StatusConciliacao.DUPLICADO
                    : diferenca.signum() == 0 ? StatusConciliacao.CONFERIDO
                    : diferenca.signum() > 0 ? StatusConciliacao.ACIMA_DO_ESPERADO
                    : StatusConciliacao.ABAIXO_DO_ESPERADO;
            esperados.add(new DetalheConciliacao(cobranca.identificador(), cobranca.valor(),
                    primeiro, diferenca, status, recebimentos.size()));
        }
        for (var pagamento : snapshot.pagamentos()) {
            if (!ids.contains(pagamento.identificador())) {
                esperados.add(new DetalheConciliacao(pagamento.identificador(), null,
                        pagamento.valor(), null, StatusConciliacao.SEM_PREVISAO, 1));
            }
        }
        if (esperados.size() != resultado.detalhes().size()) {
            throw new IllegalArgumentException("Quantidade de detalhes diverge do snapshot.");
        }
        for (int i = 0; i < esperados.size(); i++) {
            var esperado = esperados.get(i);
            var obtido = resultado.detalhes().get(i);
            if (!esperado.identificador().equals(obtido.identificador())
                    || esperado.status() != obtido.status()
                    || esperado.quantidadeRecebimentos() != obtido.quantidadeRecebimentos()
                    || !igual(esperado.valorEsperado(), obtido.valorEsperado())
                    || !igual(esperado.valorRecebido(), obtido.valorRecebido())
                    || !igual(esperado.diferenca(), obtido.diferenca())) {
                throw new IllegalArgumentException("Detalhe na posicao " + (i + 1)
                        + " diverge do snapshot.");
            }
        }
        var totalEsperado = snapshot.cobrancas().stream().map(SnapshotExecucaoCobol.Item::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalRecebido = snapshot.pagamentos().stream().map(SnapshotExecucaoCobol.Item::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!igual(totalEsperado, resultado.resumo().totalEsperado())
                || !igual(totalRecebido, resultado.resumo().totalRecebido())) {
            throw new IllegalArgumentException("Totais do resultado divergem do snapshot.");
        }
    }

    private static boolean igual(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
