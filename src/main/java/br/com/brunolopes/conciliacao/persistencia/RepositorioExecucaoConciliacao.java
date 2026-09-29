package br.com.brunolopes.conciliacao.persistencia;

public interface RepositorioExecucaoConciliacao {

    long criar();

    void iniciar(long conciliacaoId);

    default long criarEmProcessamento() {
        long conciliacaoId =
                criar();

        iniciar(
                conciliacaoId
        );

        return conciliacaoId;
    }

    void falhar(
            long conciliacaoId,
            String codigoErro,
            String detalheErro
    );
}
