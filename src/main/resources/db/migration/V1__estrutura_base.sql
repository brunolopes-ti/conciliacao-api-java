CREATE TABLE public.cobrancas (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    identificador VARCHAR(50) NOT NULL,
    valor_esperado NUMERIC NOT NULL,

    CONSTRAINT cobrancas_pkey
        PRIMARY KEY (id),

    CONSTRAINT cobrancas_identificador_key
        UNIQUE (identificador),

    CONSTRAINT cobrancas_identificador_valido
        CHECK (
            identificador::text ~ '[^[:space:]]'::text
            AND identificador::text = btrim(identificador::text)
            AND identificador::text !~ '[[:cntrl:]]'::text
        ),

    CONSTRAINT cobrancas_valor_esperado_check
        CHECK (
            valor_esperado >= 0::numeric
        ),

    CONSTRAINT cobrancas_valor_valido
        CHECK (
            valor_esperado >= 0::numeric
            AND valor_esperado <= 99999.99
            AND scale(valor_esperado) <= 2
        )
);

CREATE TABLE public.pagamentos (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    identificador_cobranca VARCHAR(50) NOT NULL,
    valor_pago NUMERIC NOT NULL,

    CONSTRAINT pagamentos_pkey
        PRIMARY KEY (id),

    CONSTRAINT pagamentos_identificador_valido
        CHECK (
            identificador_cobranca::text ~ '[^[:space:]]'::text
            AND identificador_cobranca::text =
                btrim(identificador_cobranca::text)
            AND identificador_cobranca::text
                !~ '[[:cntrl:]]'::text
        ),

    CONSTRAINT pagamentos_valor_pago_check
        CHECK (
            valor_pago >= 0::numeric
        ),

    CONSTRAINT pagamentos_valor_valido
        CHECK (
            valor_pago >= 0::numeric
            AND valor_pago <= 99999.99
            AND scale(valor_pago) <= 2
        )
);

GRANT USAGE ON SCHEMA public
TO conciliacao_app;

GRANT SELECT, INSERT, UPDATE
ON TABLE
    public.cobrancas,
    public.pagamentos
TO conciliacao_app;

GRANT USAGE, SELECT
ON SEQUENCE
    public.cobrancas_id_seq,
    public.pagamentos_id_seq
TO conciliacao_app;
