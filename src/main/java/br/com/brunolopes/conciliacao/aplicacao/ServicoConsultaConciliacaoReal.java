package br.com.brunolopes.conciliacao.aplicacao;

import java.util.List;

import br.com.brunolopes.conciliacao.aplicacao.excecao.ConciliacaoNaoEncontradaException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.RequisicaoInvalidaException;
import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;
import br.com.brunolopes.conciliacao.modelo.consulta.ConciliacaoConsultada;
import br.com.brunolopes.conciliacao.modelo.consulta.ItemHistoricoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioConsultaConciliacao;

public final class ServicoConsultaConciliacaoReal
        implements ServicoConsultaConciliacao {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final RepositorioConsultaConciliacao repositorio;

    public ServicoConsultaConciliacaoReal(
            RepositorioConsultaConciliacao repositorio
    ) {
        if (repositorio == null) {
            throw new IllegalArgumentException(
                    "Repositorio de consulta e obrigatorio."
            );
        }

        this.repositorio = repositorio;
    }

    @Override
    public PaginaHistoricoConciliacao listar(
            int pagina,
            int tamanho,
            StatusExecucaoConciliacao status
    ) {
        validarPaginacao(
                pagina,
                tamanho
        );

        long deslocamento =
                calcularDeslocamento(
                        pagina,
                        tamanho
                );

        long totalElementos =
                repositorio.contar(
                        status
                );

        List<ItemHistoricoConciliacao> itens =
                repositorio.listar(
                        tamanho,
                        deslocamento,
                        status
                );

        long totalPaginas =
                calcularTotalPaginas(
                        totalElementos,
                        tamanho
                );

        return new PaginaHistoricoConciliacao(
                pagina,
                tamanho,
                totalElementos,
                totalPaginas,
                itens
        );
    }

    @Override
    public ConciliacaoConsultada buscarPorId(
            long conciliacaoId
    ) {
        if (conciliacaoId <= 0) {
            throw new RequisicaoInvalidaException(
                    "Identificador da conciliacao deve ser positivo."
            );
        }

        return repositorio.buscarPorId(
                        conciliacaoId
                )
                .orElseThrow(
                        ConciliacaoNaoEncontradaException::new
                );
    }

    private void validarPaginacao(
            int pagina,
            int tamanho
    ) {
        if (pagina < 0) {
            throw new RequisicaoInvalidaException(
                    "Pagina nao pode ser negativa."
            );
        }

        if (tamanho < 1
                || tamanho > TAMANHO_MAXIMO_PAGINA) {

            throw new RequisicaoInvalidaException(
                    "Tamanho deve estar entre 1 e "
                            + TAMANHO_MAXIMO_PAGINA
                            + "."
            );
        }
    }

    private long calcularDeslocamento(
            int pagina,
            int tamanho
    ) {
        try {
            return Math.multiplyExact(
                    (long) pagina,
                    (long) tamanho
            );

        } catch (ArithmeticException erro) {
            throw new RequisicaoInvalidaException(
                    "Pagina solicitada e muito grande."
            );
        }
    }

    private long calcularTotalPaginas(
            long totalElementos,
            int tamanho
    ) {
        if (totalElementos == 0) {
            return 0;
        }

        return (
                totalElementos
                        + tamanho
                        - 1
        ) / tamanho;
    }
}
