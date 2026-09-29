ALTER TABLE public.cobrancas
    ADD CONSTRAINT cobrancas_identificador_sem_separador
    CHECK (
        position(';' IN identificador) = 0
    )
    NOT VALID;

ALTER TABLE public.cobrancas
    VALIDATE CONSTRAINT cobrancas_identificador_sem_separador;


ALTER TABLE public.pagamentos
    ADD CONSTRAINT pagamentos_identificador_sem_separador
    CHECK (
        position(';' IN identificador_cobranca) = 0
    )
    NOT VALID;

ALTER TABLE public.pagamentos
    VALIDATE CONSTRAINT pagamentos_identificador_sem_separador;


GRANT USAGE, SELECT
ON SEQUENCE
    public.cobrancas_id_seq,
    public.pagamentos_id_seq
TO conciliacao_app;
