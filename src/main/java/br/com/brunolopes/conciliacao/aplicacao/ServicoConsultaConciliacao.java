package br.com.brunolopes.conciliacao.aplicacao;

import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;
import br.com.brunolopes.conciliacao.modelo.consulta.ConciliacaoConsultada;

public interface ServicoConsultaConciliacao {

    PaginaHistoricoConciliacao listar(
            int pagina,
            int tamanho,
            StatusExecucaoConciliacao status
    );

    ConciliacaoConsultada buscarPorId(
            long conciliacaoId
    );
}
