package br.com.brunolopes.conciliacao.integracao;

import java.io.IOException;

import br.com.brunolopes.conciliacao.aplicacao.excecao.FalhaCobolException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.ResultadoInvalidoException;
import br.com.brunolopes.conciliacao.aplicacao.excecao.TimeoutCobolException;
import br.com.brunolopes.conciliacao.modelo.ResultadoConciliacao;

public final class ProcessadorConciliacaoCobolReal
        implements ProcessadorConciliacaoCobol {

    private final IntegradorExecucaoCobol integrador;

    public ProcessadorConciliacaoCobolReal(
            IntegradorExecucaoCobol integrador
    ) {
        if (integrador == null) {
            throw new IllegalArgumentException(
                    "Integrador COBOL e obrigatorio."
            );
        }

        this.integrador = integrador;
    }

    @Override
    public ResultadoConciliacao processar(
            SnapshotExecucaoCobol snapshot
    ) {
        if (snapshot == null) {
            throw new IllegalArgumentException(
                    "Snapshot e obrigatorio."
            );
        }

        try (DiretorioExecucaoCobol execucao =
                     DiretorioExecucaoCobol.criar()) {

            GeradorArquivosEntradaCobol.gerar(
                    execucao,
                    snapshot
            );

            ResultadoExecucaoCobol resultado =
                    integrador.executar(
                            execucao,
                            snapshot
                    );

            return resultado.resultado();

        } catch (TimeoutExecucaoCobolException erro) {
            throw new TimeoutCobolException(
                    "Tempo limite da execucao COBOL excedido.",
                    erro
            );

        } catch (FalhaExecucaoCobolException erro) {
            throw new FalhaCobolException(
                    "O motor COBOL terminou com falha.",
                    erro
            );

        } catch (ResultadoCobolInvalidoException erro) {
            throw new ResultadoInvalidoException(
                    "O resultado produzido pelo COBOL e invalido.",
                    erro
            );

        } catch (IOException erro) {
            throw new IllegalStateException(
                    "Falha ao limpar "
                            + "o diretorio temporario COBOL.",
                    erro
            );
        }
    }
}
