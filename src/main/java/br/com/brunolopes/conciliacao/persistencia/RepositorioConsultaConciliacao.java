package br.com.brunolopes.conciliacao.persistencia;

import java.util.List;
import java.util.Optional;

import br.com.brunolopes.conciliacao.modelo.consulta.ConciliacaoConsultada;
import br.com.brunolopes.conciliacao.modelo.consulta.ItemHistoricoConciliacao;
import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;

public interface RepositorioConsultaConciliacao {

    List<ItemHistoricoConciliacao> listar(
            int limite,
            long deslocamento,
            StatusExecucaoConciliacao status
    );

    long contar(
            StatusExecucaoConciliacao status
    );

    Optional<ConciliacaoConsultada> buscarPorId(
            long conciliacaoId
    );
}
