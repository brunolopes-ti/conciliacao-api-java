package br.com.brunolopes.conciliacao.integracao;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

/** Dados imutaveis e ordenados da execucao; ainda nao representa persistencia SQL. */
public record SnapshotExecucaoCobol(List<Item> cobrancas, List<Item> pagamentos) {
    public SnapshotExecucaoCobol {
        cobrancas = copiar(cobrancas);
        pagamentos = copiar(pagamentos);
        var ids = new HashSet<String>();
        for (Item item : cobrancas) {
            if (!ids.add(item.identificador())) {
                throw new IllegalArgumentException("Cobranca duplicada no snapshot.");
            }
        }
    }

    private static List<Item> copiar(List<Item> itens) {
        if (itens == null || itens.isEmpty() || itens.size() > 1000) {
            throw new IllegalArgumentException("Snapshot exige de 1 a 1000 itens por conjunto.");
        }
        return List.copyOf(itens);
    }

    public record Item(String identificador, BigDecimal valor) {
        public Item {
            if (identificador == null || identificador.isBlank()
                    || !identificador.equals(identificador.strip())
                    || identificador.codePointCount(0, identificador.length()) > 50
                    || identificador.codePoints().anyMatch(c -> Character.isISOControl(c) || c == ';')) {
                throw new IllegalArgumentException("Identificador invalido no snapshot.");
            }
            if (valor == null || valor.signum() < 0
                    || valor.compareTo(new BigDecimal("99999.99")) > 0) {
                throw new IllegalArgumentException("Valor invalido no snapshot.");
            }
            try {
                valor = valor.setScale(2, java.math.RoundingMode.UNNECESSARY);
            } catch (ArithmeticException erro) {
                throw new IllegalArgumentException("Valor deve representar centavos exatos.", erro);
            }
        }
    }
}
