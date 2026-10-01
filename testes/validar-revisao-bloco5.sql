\set ON_ERROR_STOP on
BEGIN;
DO $$
DECLARE
    execucao BIGINT;
    estado TEXT;
    tabela TEXT;
    coluna TEXT;
    valor_coluna TEXT;
    espaco TEXT;
    identificador_teste TEXT;
    checks INTEGER := 0;
BEGIN
    BEGIN
        INSERT INTO public.conciliacoes(status, iniciada_em, finalizada_em)
        VALUES ('CONCLUIDA', clock_timestamp(), clock_timestamp());
        RAISE EXCEPTION 'FALHOU: CONCLUIDA sem versao aceita';
    EXCEPTION WHEN check_violation THEN checks := checks + 1;
    END;

    INSERT INTO public.conciliacoes(status, iniciada_em)
    VALUES ('EM_PROCESSAMENTO', clock_timestamp()) RETURNING id INTO execucao;
    FOREACH estado IN ARRAY ARRAY['CONFERIDO','ACIMA_DO_ESPERADO','ABAIXO_DO_ESPERADO'] LOOP
        BEGIN
            INSERT INTO public.conciliacao_detalhes
                (conciliacao_id, ordem, identificador, valor_esperado,
                 valor_recebido, diferenca, status, quantidade_recebimentos)
            VALUES (execucao, 1, 'AUDIT', 10, 20, NULL, estado, 1);
            RAISE EXCEPTION 'FALHOU: diferenca nula aceita para %', estado;
        EXCEPTION WHEN check_violation THEN checks := checks + 1;
        END;
    END LOOP;

    FOREACH tabela IN ARRAY ARRAY['cobrancas', 'pagamentos'] LOOP
        coluna := CASE WHEN tabela = 'cobrancas' THEN 'identificador' ELSE 'identificador_cobranca' END;
        valor_coluna := CASE WHEN tabela = 'cobrancas' THEN 'valor_esperado' ELSE 'valor_pago' END;
        FOREACH espaco IN ARRAY ARRAY[U&'\1680', U&'\2000', U&'\2001', U&'\2002',
            U&'\2003', U&'\2004', U&'\2005', U&'\2006', U&'\2008', U&'\2009',
            U&'\200A', U&'\2028', U&'\2029', U&'\205F', U&'\3000'] LOOP
            FOREACH identificador_teste IN ARRAY ARRAY[espaco || 'AUDIT', 'AUDIT' || espaco] LOOP
                BEGIN
                    EXECUTE format('INSERT INTO public.%I(%I,%I) VALUES ($1,10)', tabela, coluna, valor_coluna)
                        USING identificador_teste;
                    RAISE EXCEPTION 'FALHOU: espaco Unicode na borda aceito em %', tabela;
                EXCEPTION WHEN check_violation THEN checks := checks + 1;
                END;
            END LOOP;
        END LOOP;
    END LOOP;

    -- Estados legitimos com diferenca preenchida continuam aceitos.
    INSERT INTO public.conciliacao_detalhes
        (conciliacao_id, ordem, identificador, valor_esperado,
         valor_recebido, diferenca, status, quantidade_recebimentos)
    VALUES (execucao, 1, 'AUDIT1', 10, 10, 0, 'CONFERIDO', 1),
           (execucao, 2, 'AUDIT2', 10, 20, 10, 'ACIMA_DO_ESPERADO', 1),
           (execucao, 3, 'AUDIT3', 20, 10, -10, 'ABAIXO_DO_ESPERADO', 1),
           (execucao, 4, 'AUDIT4', 10, NULL, NULL, 'SEM_RECEBIMENTO', 0),
           (execucao, 5, 'AUDIT5', NULL, 10, NULL, 'SEM_PREVISAO', 1);
    RAISE NOTICE 'PASSOU: % rejeicoes e cinco detalhes validos', checks;
END $$;
ROLLBACK;
