-- Flyway executa esta migration em transacao. Dados legados invalidos
-- impedem a aplicacao; devem ser investigados, nunca corrigidos silenciosamente.
ALTER TABLE public.conciliacoes
    ADD CONSTRAINT conciliacoes_concluida_exige_versao
    CHECK (status <> 'CONCLUIDA' OR resultado_versao IS NOT NULL);

ALTER TABLE public.conciliacao_detalhes
    ADD CONSTRAINT conciliacao_detalhes_exige_diferenca
    CHECK (
        status NOT IN ('CONFERIDO', 'ACIMA_DO_ESPERADO', 'ABAIXO_DO_ESPERADO')
        OR diferenca IS NOT NULL
    );

-- Espacos Unicode removidos por String.strip()/Character.isWhitespace.
-- Os controles ASCII ja sao rejeitados pelas constraints existentes.
ALTER TABLE public.cobrancas
    ADD CONSTRAINT cobrancas_bordas_unicode_validas
    CHECK (identificador = btrim(identificador,
        U&'\0020\1680\2000\2001\2002\2003\2004\2005\2006\2008\2009\200A\2028\2029\205F\3000'));
ALTER TABLE public.pagamentos
    ADD CONSTRAINT pagamentos_bordas_unicode_validas
    CHECK (identificador_cobranca = btrim(identificador_cobranca,
        U&'\0020\1680\2000\2001\2002\2003\2004\2005\2006\2008\2009\200A\2028\2029\205F\3000'));
ALTER TABLE public.conciliacao_snapshot_cobrancas
    ADD CONSTRAINT snapshot_cobrancas_bordas_unicode_validas
    CHECK (identificador = btrim(identificador,
        U&'\0020\1680\2000\2001\2002\2003\2004\2005\2006\2008\2009\200A\2028\2029\205F\3000'));
ALTER TABLE public.conciliacao_snapshot_pagamentos
    ADD CONSTRAINT snapshot_pagamentos_bordas_unicode_validas
    CHECK (identificador_cobranca = btrim(identificador_cobranca,
        U&'\0020\1680\2000\2001\2002\2003\2004\2005\2006\2008\2009\200A\2028\2029\205F\3000'));
ALTER TABLE public.conciliacao_detalhes
    ADD CONSTRAINT detalhes_bordas_unicode_validas
    CHECK (identificador = btrim(identificador,
        U&'\0020\1680\2000\2001\2002\2003\2004\2005\2006\2008\2009\200A\2028\2029\205F\3000'));
