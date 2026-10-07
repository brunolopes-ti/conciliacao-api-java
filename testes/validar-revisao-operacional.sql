\set ON_ERROR_STOP on
BEGIN;

-- U+2007 nas bordas deve ser rejeitado pelo banco, como Java e COBOL.
DO $$
BEGIN
    BEGIN
        INSERT INTO public.cobrancas (identificador, valor_esperado)
        VALUES (U&'\2007', 1.00);
        RAISE EXCEPTION 'falha: U+2007 isolado foi aceito';
    EXCEPTION
        WHEN check_violation THEN NULL;
    END;
END;
$$;

-- Snapshot nao pode ser acrescentado depois da conclusao.
DO $$
DECLARE
    cid BIGINT;
BEGIN
    INSERT INTO public.conciliacoes (
        status, iniciada_em
    ) VALUES (
        'EM_PROCESSAMENTO', clock_timestamp()
    ) RETURNING id INTO cid;

    INSERT INTO public.conciliacao_resumos (
        conciliacao_id, versao, conferidos, acima, abaixo, duplicados,
        sem_recebimento, sem_previsao, total_esperado, total_recebido,
        saldo_global
    ) VALUES (
        cid, 1, 1, 0, 0, 0, 0, 0, 1.00, 1.00, 0.00
    );

    INSERT INTO public.conciliacao_detalhes (
        conciliacao_id, ordem, identificador, valor_esperado,
        valor_recebido, diferenca, status, quantidade_recebimentos
    ) VALUES (
        cid, 1, 'OK', 1.00, 1.00, 0.00, 'CONFERIDO', 1
    );

    UPDATE public.conciliacoes
       SET status = 'CONCLUIDA',
           finalizada_em = clock_timestamp(),
           resultado_versao = 1
     WHERE id = cid;

    BEGIN
        INSERT INTO public.conciliacao_snapshot_cobrancas (
            conciliacao_id, ordem, origem_id, identificador, valor_esperado
        ) VALUES (
            cid, 1, 999999, 'TARDIO', 1.00
        );
        RAISE EXCEPTION 'falha: snapshot tardio foi aceito';
    EXCEPTION
        WHEN check_violation THEN NULL;
    END;
END;
$$;

-- CONCLUIDA sem resumo/detalhes deve falhar ao forcar constraints diferidas.
DO $$
DECLARE
    cid BIGINT;
BEGIN
    BEGIN
        INSERT INTO public.conciliacoes (
            status, iniciada_em, finalizada_em, resultado_versao
        ) VALUES (
            'CONCLUIDA', clock_timestamp(), clock_timestamp(), 1
        ) RETURNING id INTO cid;

        SET CONSTRAINTS conciliacoes_conclusao_exige_resultado IMMEDIATE;
        RAISE EXCEPTION 'falha: CONCLUIDA sem resultado foi aceita';
    EXCEPTION
        WHEN check_violation THEN
            SET CONSTRAINTS ALL DEFERRED;
    END;
END;
$$;

ROLLBACK;
\echo 'Revisao operacional SQL: PASSOU'
