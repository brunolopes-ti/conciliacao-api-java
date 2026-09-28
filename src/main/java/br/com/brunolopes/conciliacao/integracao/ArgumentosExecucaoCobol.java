package br.com.brunolopes.conciliacao.integracao;

import java.nio.file.Path;
import java.util.List;

public record ArgumentosExecucaoCobol(
        Path esperados,
        Path recebidos,
        Path relatorio,
        Path resultado
) {

    public ArgumentosExecucaoCobol {
        if (esperados == null
                || recebidos == null
                || relatorio == null
                || resultado == null) {

            throw new IllegalArgumentException(
                    "Os quatro caminhos da execucao COBOL sao obrigatorios.");
        }
    }

    public static ArgumentosExecucaoCobol de(
            DiretorioExecucaoCobol execucao
    ) {
        if (execucao == null) {
            throw new IllegalArgumentException(
                    "Diretorio da execucao COBOL e obrigatorio.");
        }

        return new ArgumentosExecucaoCobol(
                execucao.esperados(),
                execucao.recebidos(),
                execucao.relatorio(),
                execucao.resultado());
    }

    public List<String> comoLista() {
        return List.of(
                esperados.toString(),
                recebidos.toString(),
                relatorio.toString(),
                resultado.toString());
    }
}
