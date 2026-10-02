package br.com.brunolopes.conciliacao.aplicacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.brunolopes.conciliacao.aplicacao.excecao.ConciliacaoNaoEncontradaException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.RequisicaoInvalidaException;
import br.com.brunolopes.conciliacao.modelo.StatusExecucaoConciliacao;
import br.com.brunolopes.conciliacao.modelo.consulta.ConciliacaoConsultada;
import br.com.brunolopes.conciliacao.modelo.consulta.ItemHistoricoConciliacao;
import br.com.brunolopes.conciliacao.persistencia.RepositorioConsultaConciliacao;

class ServicoConsultaConciliacaoRealTests {

    @Test
    void deveCalcularPaginacaoEConsultarRepositorio() {
        RepositorioFake repositorio =
                new RepositorioFake();

        repositorio.total = 47;

        repositorio.itens =
                List.of(
                        criarItem(
                                30,
                                StatusExecucaoConciliacao.CONCLUIDA
                        )
                );

        ServicoConsultaConciliacaoReal servico =
                new ServicoConsultaConciliacaoReal(
                        repositorio
                );

        PaginaHistoricoConciliacao resultado =
                servico.listar(
                        2,
                        20,
                        null
                );

        assertEquals(
                2,
                resultado.pagina()
        );

        assertEquals(
                20,
                resultado.tamanho()
        );

        assertEquals(
                47,
                resultado.totalElementos()
        );

        assertEquals(
                3,
                resultado.totalPaginas()
        );

        assertEquals(
                1,
                resultado.itens().size()
        );

        assertEquals(
                20,
                repositorio.ultimoLimite
        );

        assertEquals(
                40,
                repositorio.ultimoDeslocamento
        );

        assertEquals(
                null,
                repositorio.ultimoStatus
        );
    }

    @Test
    void deveRepassarFiltroDeStatusAoRepositorio() {
        RepositorioFake repositorio =
                new RepositorioFake();

        repositorio.total = 2;

        repositorio.itens =
                List.of(
                        criarItem(
                                8,
                                StatusExecucaoConciliacao.FALHOU
                        )
                );

        ServicoConsultaConciliacaoReal servico =
                new ServicoConsultaConciliacaoReal(
                        repositorio
                );

        PaginaHistoricoConciliacao resultado =
                servico.listar(
                        0,
                        10,
                        StatusExecucaoConciliacao.FALHOU
                );

        assertEquals(
                StatusExecucaoConciliacao.FALHOU,
                repositorio.ultimoStatus
        );

        assertEquals(
                1,
                resultado.totalPaginas()
        );
    }

    @Test
    void deveRetornarZeroPaginasQuandoNaoHaResultados() {
        RepositorioFake repositorio =
                new RepositorioFake();

        repositorio.total = 0;
        repositorio.itens = List.of();

        ServicoConsultaConciliacaoReal servico =
                new ServicoConsultaConciliacaoReal(
                        repositorio
                );

        PaginaHistoricoConciliacao resultado =
                servico.listar(
                        0,
                        20,
                        null
                );

        assertEquals(
                0,
                resultado.totalElementos()
        );

        assertEquals(
                0,
                resultado.totalPaginas()
        );

        assertEquals(
                0,
                resultado.itens().size()
        );
    }

    @Test
    void deveRejeitarPaginaNegativa() {
        ServicoConsultaConciliacaoReal servico =
                new ServicoConsultaConciliacaoReal(
                        new RepositorioFake()
                );

        assertThrows(
                RequisicaoInvalidaException.class,
                () ->
                        servico.listar(
                                -1,
                                20,
                                null
                        )
        );
    }

    @Test
    void deveRejeitarTamanhoInvalido() {
        ServicoConsultaConciliacaoReal servico =
                new ServicoConsultaConciliacaoReal(
                        new RepositorioFake()
                );

        assertThrows(
                RequisicaoInvalidaException.class,
                () ->
                        servico.listar(
                                0,
                                0,
                                null
                        )
        );

        assertThrows(
                RequisicaoInvalidaException.class,
                () ->
                        servico.listar(
                                0,
                                101,
                                null
                        )
        );
    }

    @Test
    void deveBuscarConciliacaoPorId() {
        RepositorioFake repositorio =
                new RepositorioFake();

        repositorio.conciliacao =
                Optional.of(
                        criarConciliacao(
                                25
                        )
                );

        ServicoConsultaConciliacaoReal servico =
                new ServicoConsultaConciliacaoReal(
                        repositorio
                );

        ConciliacaoConsultada resultado =
                servico.buscarPorId(
                        25
                );

        assertEquals(
                25,
                resultado.id()
        );

        assertEquals(
                25,
                repositorio.ultimoIdBuscado
        );
    }

    @Test
    void deveInformarQuandoConciliacaoNaoExiste() {
        RepositorioFake repositorio =
                new RepositorioFake();

        repositorio.conciliacao =
                Optional.empty();

        ServicoConsultaConciliacaoReal servico =
                new ServicoConsultaConciliacaoReal(
                        repositorio
                );

        assertThrows(
                ConciliacaoNaoEncontradaException.class,
                () ->
                        servico.buscarPorId(
                                999
                        )
        );
    }

    @Test
    void deveRejeitarIdInvalido() {
        RepositorioFake repositorio =
                new RepositorioFake();

        ServicoConsultaConciliacaoReal servico =
                new ServicoConsultaConciliacaoReal(
                        repositorio
                );

        assertThrows(
                RequisicaoInvalidaException.class,
                () ->
                        servico.buscarPorId(
                                0
                        )
        );

        assertEquals(
                0,
                repositorio.quantidadeBuscasPorId
        );
    }

    @Test
    void deveRejeitarRepositorioNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ServicoConsultaConciliacaoReal(
                                null
                        )
        );
    }

    private ItemHistoricoConciliacao criarItem(
            long id,
            StatusExecucaoConciliacao status
    ) {
        return new ItemHistoricoConciliacao(
                id,
                status,
                OffsetDateTime.parse(
                        "2026-10-02T12:00:00Z"
                ),
                null,
                null,
                null,
                null,
                null
        );
    }

    private ConciliacaoConsultada criarConciliacao(
            long id
    ) {
        return new ConciliacaoConsultada(
                id,
                StatusExecucaoConciliacao.CRIADA,
                OffsetDateTime.parse(
                        "2026-10-02T12:00:00Z"
                ),
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    private static class RepositorioFake
            implements RepositorioConsultaConciliacao {

        private long total;

        private List<ItemHistoricoConciliacao> itens =
                new ArrayList<>();

        private Optional<ConciliacaoConsultada> conciliacao =
                Optional.empty();

        private int ultimoLimite;
        private long ultimoDeslocamento;
        private StatusExecucaoConciliacao ultimoStatus;
        private long ultimoIdBuscado;
        private int quantidadeBuscasPorId;

        @Override
        public List<ItemHistoricoConciliacao> listar(
                int limite,
                long deslocamento,
                StatusExecucaoConciliacao status
        ) {
            ultimoLimite = limite;
            ultimoDeslocamento = deslocamento;
            ultimoStatus = status;

            return itens;
        }

        @Override
        public long contar(
                StatusExecucaoConciliacao status
        ) {
            ultimoStatus = status;

            return total;
        }

        @Override
        public Optional<ConciliacaoConsultada> buscarPorId(
                long conciliacaoId
        ) {
            ultimoIdBuscado = conciliacaoId;
            quantidadeBuscasPorId++;

            return conciliacao;
        }
    }
}
