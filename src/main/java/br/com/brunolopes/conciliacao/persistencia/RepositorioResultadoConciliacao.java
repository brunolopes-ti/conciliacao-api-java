package br.com.brunolopes.conciliacao.persistencia;

import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;

public interface RepositorioResultadoConciliacao {

    void persistirEConcluir(
            long conciliacaoId,
            ResultadoConciliacao resultado
    );
}
