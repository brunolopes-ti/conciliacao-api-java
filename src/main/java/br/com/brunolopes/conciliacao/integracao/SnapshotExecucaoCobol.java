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

    private static boolean espacoDoContrato(int cp) {
        return cp == 0x20
                || cp == 0x1680
                || (cp >= 0x2000 && cp <= 0x200A)
                || cp == 0x2028
                || cp == 0x2029
                || cp == 0x205F
                || cp == 0x3000;
    }

    private static void validarIdentificador(String identificador) {
        if (identificador == null || identificador.isEmpty()) {
            throw new IllegalArgumentException("Identificador invalido no snapshot.");
        }

        int quantidade = identificador.codePointCount(0, identificador.length());
        if (quantidade > 50) {
            throw new IllegalArgumentException("Identificador invalido no snapshot.");
        }

        int primeiro = identificador.codePointAt(0);
        int ultimo = identificador.codePointBefore(identificador.length());

        if (espacoDoContrato(primeiro) || espacoDoContrato(ultimo)) {
            throw new IllegalArgumentException("Identificador invalido no snapshot.");
        }

        boolean possuiVisivel = false;

        for (int posicao = 0; posicao < identificador.length();) {
            int cp = identificador.codePointAt(posicao);

            if (Character.isISOControl(cp) || cp == ';') {
                throw new IllegalArgumentException("Identificador invalido no snapshot.");
            }

            if (!espacoDoContrato(cp)) {
                possuiVisivel = true;
            }

            posicao += Character.charCount(cp);
        }

        if (!possuiVisivel) {
            throw new IllegalArgumentException("Identificador invalido no snapshot.");
        }
    }

    public record Item(String identificador, BigDecimal valor) {
        public Item {
            validarIdentificador(identificador);

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
