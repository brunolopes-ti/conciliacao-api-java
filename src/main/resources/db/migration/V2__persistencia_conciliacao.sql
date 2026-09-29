CREATE TABLE public.conciliacoes (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    status VARCHAR(20) NOT NULL,
    criada_em TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    iniciada_em TIMESTAMP WITH TIME ZONE,
    finalizada_em TIMESTAMP WITH TIME ZONE,
    resultado_versao SMALLINT,
    erro_codigo VARCHAR(50),
    erro_detalhe TEXT,

    CONSTRAINT conciliacoes_pkey
        PRIMARY KEY (id),

    CONSTRAINT conciliacoes_status_valido
        CHECK (
            status IN (
                'CRIADA',
                'EM_PROCESSAMENTO',
                'CONCLUIDA',
                'FALHOU'
            )
        ),

    CONSTRAINT conciliacoes_resultado_versao_valido
        CHECK (
            resultado_versao IS NULL
            OR resultado_versao = 1
        ),

    CONSTRAINT conciliacoes_erro_codigo_valido
        CHECK (
            erro_codigo IS NULL
            OR (
                erro_codigo <> ''
                AND erro_codigo = btrim(erro_codigo)
            )
        ),

    CONSTRAINT conciliacoes_erro_detalhe_limite
        CHECK (
            erro_detalhe IS NULL
            OR char_length(erro_detalhe) <= 4000
        ),

    CONSTRAINT conciliacoes_datas_validas
        CHECK (
            (
                iniciada_em IS NULL
                OR iniciada_em >= criada_em
            )
            AND
            (
                finalizada_em IS NULL
                OR finalizada_em >= COALESCE(
                    iniciada_em,
                    criada_em
                )
            )
        ),

    CONSTRAINT conciliacoes_estado_consistente
        CHECK (
            (
                status = 'CRIADA'
                AND iniciada_em IS NULL
                AND finalizada_em IS NULL
                AND resultado_versao IS NULL
                AND erro_codigo IS NULL
                AND erro_detalhe IS NULL
            )
            OR
            (
                status = 'EM_PROCESSAMENTO'
                AND iniciada_em IS NOT NULL
                AND finalizada_em IS NULL
                AND resultado_versao IS NULL
                AND erro_codigo IS NULL
                AND erro_detalhe IS NULL
            )
            OR
            (
                status = 'CONCLUIDA'
                AND iniciada_em IS NOT NULL
                AND finalizada_em IS NOT NULL
                AND resultado_versao = 1
                AND erro_codigo IS NULL
                AND erro_detalhe IS NULL
            )
            OR
            (
                status = 'FALHOU'
                AND iniciada_em IS NOT NULL
                AND finalizada_em IS NOT NULL
                AND resultado_versao IS NULL
                AND erro_codigo IS NOT NULL
            )
        )
);

CREATE TABLE public.conciliacao_snapshot_cobrancas (
    conciliacao_id BIGINT NOT NULL,
    ordem SMALLINT NOT NULL,
    origem_id BIGINT NOT NULL,
    identificador VARCHAR(50) NOT NULL,
    valor_esperado NUMERIC(7, 2) NOT NULL,

    CONSTRAINT conciliacao_snapshot_cobrancas_pkey
        PRIMARY KEY (
            conciliacao_id,
            ordem
        ),

    CONSTRAINT conciliacao_snapshot_cobrancas_execucao_fk
        FOREIGN KEY (conciliacao_id)
        REFERENCES public.conciliacoes (id)
        ON DELETE RESTRICT,

    CONSTRAINT conciliacao_snapshot_cobrancas_origem_unique
        UNIQUE (
            conciliacao_id,
            origem_id
        ),

    CONSTRAINT conciliacao_snapshot_cobrancas_identificador_unique
        UNIQUE (
            conciliacao_id,
            identificador
        ),

    CONSTRAINT conciliacao_snapshot_cobrancas_ordem_valida
        CHECK (
            ordem BETWEEN 1 AND 1000
        ),

    CONSTRAINT conciliacao_snapshot_cobrancas_origem_valida
        CHECK (
            origem_id > 0
        ),

    CONSTRAINT conciliacao_snapshot_cobrancas_identificador_valido
        CHECK (
            identificador::text ~ '[^[:space:]]'::text
            AND identificador::text = btrim(identificador::text)
            AND identificador::text !~ '[[:cntrl:]]'::text
            AND position(';' IN identificador) = 0
        ),

    CONSTRAINT conciliacao_snapshot_cobrancas_valor_valido
        CHECK (
            valor_esperado >= 0
            AND valor_esperado <= 99999.99
        )
);

CREATE TABLE public.conciliacao_snapshot_pagamentos (
    conciliacao_id BIGINT NOT NULL,
    ordem SMALLINT NOT NULL,
    origem_id BIGINT NOT NULL,
    identificador_cobranca VARCHAR(50) NOT NULL,
    valor_pago NUMERIC(7, 2) NOT NULL,

    CONSTRAINT conciliacao_snapshot_pagamentos_pkey
        PRIMARY KEY (
            conciliacao_id,
            ordem
        ),

    CONSTRAINT conciliacao_snapshot_pagamentos_execucao_fk
        FOREIGN KEY (conciliacao_id)
        REFERENCES public.conciliacoes (id)
        ON DELETE RESTRICT,

    CONSTRAINT conciliacao_snapshot_pagamentos_origem_unique
        UNIQUE (
            conciliacao_id,
            origem_id
        ),

    CONSTRAINT conciliacao_snapshot_pagamentos_ordem_valida
        CHECK (
            ordem BETWEEN 1 AND 1000
        ),

    CONSTRAINT conciliacao_snapshot_pagamentos_origem_valida
        CHECK (
            origem_id > 0
        ),

    CONSTRAINT conciliacao_snapshot_pagamentos_identificador_valido
        CHECK (
            identificador_cobranca::text ~ '[^[:space:]]'::text
            AND identificador_cobranca::text =
                btrim(identificador_cobranca::text)
            AND identificador_cobranca::text
                !~ '[[:cntrl:]]'::text
            AND position(
                ';'
                IN identificador_cobranca
            ) = 0
        ),

    CONSTRAINT conciliacao_snapshot_pagamentos_valor_valido
        CHECK (
            valor_pago >= 0
            AND valor_pago <= 99999.99
        )
);

CREATE TABLE public.conciliacao_resumos (
    conciliacao_id BIGINT NOT NULL,
    versao SMALLINT NOT NULL,
    conferidos INTEGER NOT NULL,
    acima INTEGER NOT NULL,
    abaixo INTEGER NOT NULL,
    duplicados INTEGER NOT NULL,
    sem_recebimento INTEGER NOT NULL,
    sem_previsao INTEGER NOT NULL,
    total_esperado NUMERIC(10, 2) NOT NULL,
    total_recebido NUMERIC(10, 2) NOT NULL,
    saldo_global NUMERIC(10, 2) NOT NULL,

    CONSTRAINT conciliacao_resumos_pkey
        PRIMARY KEY (conciliacao_id),

    CONSTRAINT conciliacao_resumos_execucao_fk
        FOREIGN KEY (conciliacao_id)
        REFERENCES public.conciliacoes (id)
        ON DELETE RESTRICT,

    CONSTRAINT conciliacao_resumos_versao_valida
        CHECK (
            versao = 1
        ),

    CONSTRAINT conciliacao_resumos_contadores_validos
        CHECK (
            conferidos >= 0
            AND acima >= 0
            AND abaixo >= 0
            AND duplicados >= 0
            AND sem_recebimento >= 0
            AND sem_previsao >= 0
            AND (
                conferidos
                + acima
                + abaixo
                + duplicados
                + sem_recebimento
                + sem_previsao
            ) <= 2000
        ),

    CONSTRAINT conciliacao_resumos_total_esperado_valido
        CHECK (
            total_esperado >= 0
            AND total_esperado <= 99999990.00
        ),

    CONSTRAINT conciliacao_resumos_total_recebido_valido
        CHECK (
            total_recebido >= 0
            AND total_recebido <= 99999990.00
        ),

    CONSTRAINT conciliacao_resumos_saldo_valido
        CHECK (
            saldo_global >= -99999990.00
            AND saldo_global <= 99999990.00
            AND saldo_global =
                total_recebido - total_esperado
        )
);

CREATE TABLE public.conciliacao_detalhes (
    conciliacao_id BIGINT NOT NULL,
    ordem SMALLINT NOT NULL,
    identificador VARCHAR(50) NOT NULL,
    valor_esperado NUMERIC(7, 2),
    valor_recebido NUMERIC(7, 2),
    diferenca NUMERIC(7, 2),
    status VARCHAR(25) NOT NULL,
    quantidade_recebimentos SMALLINT NOT NULL,

    CONSTRAINT conciliacao_detalhes_pkey
        PRIMARY KEY (
            conciliacao_id,
            ordem
        ),

    CONSTRAINT conciliacao_detalhes_execucao_fk
        FOREIGN KEY (conciliacao_id)
        REFERENCES public.conciliacoes (id)
        ON DELETE RESTRICT,

    CONSTRAINT conciliacao_detalhes_ordem_valida
        CHECK (
            ordem BETWEEN 1 AND 2000
        ),

    CONSTRAINT conciliacao_detalhes_identificador_valido
        CHECK (
            identificador::text ~ '[^[:space:]]'::text
            AND identificador::text = btrim(identificador::text)
            AND identificador::text !~ '[[:cntrl:]]'::text
            AND position(';' IN identificador) = 0
        ),

    CONSTRAINT conciliacao_detalhes_status_valido
        CHECK (
            status IN (
                'CONFERIDO',
                'ACIMA_DO_ESPERADO',
                'ABAIXO_DO_ESPERADO',
                'DUPLICADO',
                'SEM_RECEBIMENTO',
                'SEM_PREVISAO'
            )
        ),

    CONSTRAINT conciliacao_detalhes_quantidade_valida
        CHECK (
            quantidade_recebimentos
                BETWEEN 0 AND 1000
        ),

    CONSTRAINT conciliacao_detalhes_valor_esperado_valido
        CHECK (
            valor_esperado IS NULL
            OR (
                valor_esperado >= 0
                AND valor_esperado <= 99999.99
            )
        ),

    CONSTRAINT conciliacao_detalhes_valor_recebido_valido
        CHECK (
            valor_recebido IS NULL
            OR (
                valor_recebido >= 0
                AND valor_recebido <= 99999.99
            )
        ),

    CONSTRAINT conciliacao_detalhes_diferenca_valida
        CHECK (
            diferenca IS NULL
            OR (
                diferenca >= -99999.99
                AND diferenca <= 99999.99
            )
        ),

    CONSTRAINT conciliacao_detalhes_calculo_valido
        CHECK (
            valor_esperado IS NULL
            OR valor_recebido IS NULL
            OR diferenca IS NULL
            OR diferenca =
                valor_recebido - valor_esperado
        ),

    CONSTRAINT conciliacao_detalhes_estado_valido
        CHECK (
            (
                status = 'CONFERIDO'
                AND valor_esperado IS NOT NULL
                AND valor_recebido IS NOT NULL
                AND diferenca = 0
                AND quantidade_recebimentos = 1
            )
            OR
            (
                status = 'ACIMA_DO_ESPERADO'
                AND valor_esperado IS NOT NULL
                AND valor_recebido IS NOT NULL
                AND diferenca > 0
                AND quantidade_recebimentos = 1
            )
            OR
            (
                status = 'ABAIXO_DO_ESPERADO'
                AND valor_esperado IS NOT NULL
                AND valor_recebido IS NOT NULL
                AND diferenca < 0
                AND quantidade_recebimentos = 1
            )
            OR
            (
                status = 'DUPLICADO'
                AND valor_esperado IS NOT NULL
                AND valor_recebido IS NOT NULL
                AND diferenca IS NOT NULL
                AND quantidade_recebimentos
                    BETWEEN 2 AND 1000
            )
            OR
            (
                status = 'SEM_RECEBIMENTO'
                AND valor_esperado IS NOT NULL
                AND valor_recebido IS NULL
                AND diferenca IS NULL
                AND quantidade_recebimentos = 0
            )
            OR
            (
                status = 'SEM_PREVISAO'
                AND valor_esperado IS NULL
                AND valor_recebido IS NOT NULL
                AND diferenca IS NULL
                AND quantidade_recebimentos = 1
            )
        )
);

CREATE INDEX conciliacoes_status_iniciada_idx
    ON public.conciliacoes (
        status,
        iniciada_em
    );

CREATE INDEX conciliacoes_criada_em_idx
    ON public.conciliacoes (
        criada_em DESC
    );

CREATE INDEX conciliacao_snapshot_pagamentos_identificador_idx
    ON public.conciliacao_snapshot_pagamentos (
        conciliacao_id,
        identificador_cobranca,
        ordem
    );

CREATE INDEX conciliacao_detalhes_status_idx
    ON public.conciliacao_detalhes (
        conciliacao_id,
        status
    );

GRANT SELECT, INSERT, UPDATE
ON TABLE public.conciliacoes
TO conciliacao_app;

GRANT SELECT, INSERT
ON TABLE
    public.conciliacao_snapshot_cobrancas,
    public.conciliacao_snapshot_pagamentos,
    public.conciliacao_resumos,
    public.conciliacao_detalhes
TO conciliacao_app;

GRANT USAGE, SELECT
ON SEQUENCE public.conciliacoes_id_seq
TO conciliacao_app;
