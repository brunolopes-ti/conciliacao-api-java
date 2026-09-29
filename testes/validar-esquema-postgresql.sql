\set ON_ERROR_STOP on

BEGIN;

DO $$
DECLARE
    v_conciliacao_id BIGINT;
    v_cobranca_id BIGINT;
    v_pagamento_id BIGINT;
BEGIN
    /*
     * Identificadores com ";" devem ser rejeitados,
     * porque ";" e o separador dos CSVs enviados ao COBOL.
     */
    BEGIN
        INSERT INTO public.cobrancas (
            identificador,
            valor_esperado
        )
        VALUES (
            'INVALIDO;COBRANCA',
            10.00
        );

        RAISE EXCEPTION
            'TESTE FALHOU: cobranca com ponto e virgula foi aceita.';

    EXCEPTION
        WHEN check_violation THEN
            NULL;
    END;

    BEGIN
        INSERT INTO public.pagamentos (
            identificador_cobranca,
            valor_pago
        )
        VALUES (
            'INVALIDO;PAGAMENTO',
            10.00
        );

        RAISE EXCEPTION
            'TESTE FALHOU: pagamento com ponto e virgula foi aceito.';

    EXCEPTION
        WHEN check_violation THEN
            NULL;
    END;

    /*
     * Uma conciliacao nao pode nascer diretamente como CONCLUIDA.
     */
    BEGIN
        INSERT INTO public.conciliacoes (
            status
        )
        VALUES (
            'CONCLUIDA'
        );

        RAISE EXCEPTION
            'TESTE FALHOU: estado CONCLUIDA inconsistente foi aceito.';

    EXCEPTION
        WHEN check_violation THEN
            NULL;
    END;

    /*
     * Dados validos para o caminho positivo.
     */
    INSERT INTO public.cobrancas (
        identificador,
        valor_esperado
    )
    VALUES (
        'TESTE_MIGRACAO_001',
        100.00
    )
    RETURNING id
    INTO v_cobranca_id;

    INSERT INTO public.pagamentos (
        identificador_cobranca,
        valor_pago
    )
    VALUES (
        'TESTE_MIGRACAO_001',
        100.00
    )
    RETURNING id
    INTO v_pagamento_id;

    INSERT INTO public.conciliacoes (
        status
    )
    VALUES (
        'CRIADA'
    )
    RETURNING id
    INTO v_conciliacao_id;

    /*
     * A ordem do snapshot comeca em 1.
     */
    BEGIN
        INSERT INTO public.conciliacao_snapshot_cobrancas (
            conciliacao_id,
            ordem,
            origem_id,
            identificador,
            valor_esperado
        )
        VALUES (
            v_conciliacao_id,
            0,
            v_cobranca_id,
            'TESTE_MIGRACAO_001',
            100.00
        );

        RAISE EXCEPTION
            'TESTE FALHOU: ordem zero no snapshot foi aceita.';

    EXCEPTION
        WHEN check_violation THEN
            NULL;
    END;

    /*
     * saldo_global deve ser exatamente:
     * total_recebido - total_esperado.
     */
    BEGIN
        INSERT INTO public.conciliacao_resumos (
            conciliacao_id,
            versao,
            conferidos,
            acima,
            abaixo,
            duplicados,
            sem_recebimento,
            sem_previsao,
            total_esperado,
            total_recebido,
            saldo_global
        )
        VALUES (
            v_conciliacao_id,
            1,
            1,
            0,
            0,
            0,
            0,
            0,
            100.00,
            100.00,
            1.00
        );

        RAISE EXCEPTION
            'TESTE FALHOU: saldo global inconsistente foi aceito.';

    EXCEPTION
        WHEN check_violation THEN
            NULL;
    END;

    /*
     * SEM_PREVISAO nao pode possuir valor esperado.
     */
    BEGIN
        INSERT INTO public.conciliacao_detalhes (
            conciliacao_id,
            ordem,
            identificador,
            valor_esperado,
            valor_recebido,
            diferenca,
            status,
            quantidade_recebimentos
        )
        VALUES (
            v_conciliacao_id,
            1,
            'TESTE_MIGRACAO_001',
            100.00,
            100.00,
            NULL,
            'SEM_PREVISAO',
            1
        );

        RAISE EXCEPTION
            'TESTE FALHOU: detalhe SEM_PREVISAO inconsistente foi aceito.';

    EXCEPTION
        WHEN check_violation THEN
            NULL;
    END;

    /*
     * Caminho valido completo.
     */
    UPDATE public.conciliacoes
    SET
        status = 'EM_PROCESSAMENTO',
        iniciada_em = criada_em
    WHERE id = v_conciliacao_id;

    INSERT INTO public.conciliacao_snapshot_cobrancas (
        conciliacao_id,
        ordem,
        origem_id,
        identificador,
        valor_esperado
    )
    VALUES (
        v_conciliacao_id,
        1,
        v_cobranca_id,
        'TESTE_MIGRACAO_001',
        100.00
    );

    INSERT INTO public.conciliacao_snapshot_pagamentos (
        conciliacao_id,
        ordem,
        origem_id,
        identificador_cobranca,
        valor_pago
    )
    VALUES (
        v_conciliacao_id,
        1,
        v_pagamento_id,
        'TESTE_MIGRACAO_001',
        100.00
    );

    INSERT INTO public.conciliacao_resumos (
        conciliacao_id,
        versao,
        conferidos,
        acima,
        abaixo,
        duplicados,
        sem_recebimento,
        sem_previsao,
        total_esperado,
        total_recebido,
        saldo_global
    )
    VALUES (
        v_conciliacao_id,
        1,
        1,
        0,
        0,
        0,
        0,
        0,
        100.00,
        100.00,
        0.00
    );

    INSERT INTO public.conciliacao_detalhes (
        conciliacao_id,
        ordem,
        identificador,
        valor_esperado,
        valor_recebido,
        diferenca,
        status,
        quantidade_recebimentos
    )
    VALUES (
        v_conciliacao_id,
        1,
        'TESTE_MIGRACAO_001',
        100.00,
        100.00,
        0.00,
        'CONFERIDO',
        1
    );

    UPDATE public.conciliacoes
    SET
        status = 'CONCLUIDA',
        finalizada_em = GREATEST(
            clock_timestamp(),
            iniciada_em
        ),
        resultado_versao = 1
    WHERE id = v_conciliacao_id;

    IF NOT EXISTS (
        SELECT 1
        FROM public.conciliacoes
        WHERE id = v_conciliacao_id
          AND status = 'CONCLUIDA'
          AND resultado_versao = 1
    ) THEN
        RAISE EXCEPTION
            'TESTE FALHOU: fluxo valido nao terminou como CONCLUIDA.';
    END IF;

    RAISE NOTICE
        'VALIDACAO OK: constraints e fluxo positivo aprovados.';
END
$$;

ROLLBACK;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.cobrancas
        WHERE identificador = 'TESTE_MIGRACAO_001'
    ) THEN
        RAISE EXCEPTION
            'TESTE FALHOU: rollback deixou cobranca de teste.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.pagamentos
        WHERE identificador_cobranca = 'TESTE_MIGRACAO_001'
    ) THEN
        RAISE EXCEPTION
            'TESTE FALHOU: rollback deixou pagamento de teste.';
    END IF;

    RAISE NOTICE
        'VALIDACAO OK: rollback nao deixou dados de teste.';
END
$$;
