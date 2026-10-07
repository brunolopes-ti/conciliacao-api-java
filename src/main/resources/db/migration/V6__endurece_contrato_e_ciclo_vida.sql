-- Revisao operacional: alinha identificadores com o motor COBOL e reforca
-- invariantes que antes dependiam apenas do codigo Java.
-- V1-V5 permanecem imutaveis.

-- Mesmo conjunto de espacos Unicode reconhecido por entrada-segura.c:
-- U+0020, U+1680, U+2000..U+200A, U+2028, U+2029, U+205F, U+3000.
ALTER TABLE public.cobrancas
    ADD CONSTRAINT cobrancas_bordas_unicode_v6_validas
    CHECK (identificador = btrim(identificador,
        U&'\0020\1680\2000\2001\2002\2003\2004\2005\2006\2007\2008\2009\200A\2028\2029\205F\3000'));

ALTER TABLE public.pagamentos
    ADD CONSTRAINT pagamentos_bordas_unicode_v6_validas
    CHECK (identificador_cobranca = btrim(identificador_cobranca,
        U&'\0020\1680\2000\2001\2002\2003\2004\2005\2006\2007\2008\2009\200A\2028\2029\205F\3000'));

ALTER TABLE public.conciliacao_snapshot_cobrancas
    ADD CONSTRAINT snapshot_cobrancas_bordas_unicode_v6_validas
    CHECK (identificador = btrim(identificador,
        U&'\0020\1680\2000\2001\2002\2003\2004\2005\2006\2007\2008\2009\200A\2028\2029\205F\3000'));

ALTER TABLE public.conciliacao_snapshot_pagamentos
    ADD CONSTRAINT snapshot_pagamentos_bordas_unicode_v6_validas
    CHECK (identificador_cobranca = btrim(identificador_cobranca,
        U&'\0020\1680\2000\2001\2002\2003\2004\2005\2006\2007\2008\2009\200A\2028\2029\205F\3000'));

ALTER TABLE public.conciliacao_detalhes
    ADD CONSTRAINT detalhes_bordas_unicode_v6_validas
    CHECK (identificador = btrim(identificador,
        U&'\0020\1680\2000\2001\2002\2003\2004\2005\2006\2007\2008\2009\200A\2028\2029\205F\3000'));

-- Snapshot e imutavel depois que a execucao deixa EM_PROCESSAMENTO.
CREATE FUNCTION public.validar_snapshot_em_processamento()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    estado VARCHAR(20);
BEGIN
    SELECT status
      INTO estado
      FROM public.conciliacoes
     WHERE id = NEW.conciliacao_id;

    IF estado IS DISTINCT FROM 'EM_PROCESSAMENTO' THEN
        RAISE EXCEPTION
            USING ERRCODE = '23514',
                  MESSAGE = 'snapshot exige conciliacao EM_PROCESSAMENTO';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER snapshot_cobrancas_exige_processamento
BEFORE INSERT ON public.conciliacao_snapshot_cobrancas
FOR EACH ROW
EXECUTE FUNCTION public.validar_snapshot_em_processamento();

CREATE TRIGGER snapshot_pagamentos_exige_processamento
BEFORE INSERT ON public.conciliacao_snapshot_pagamentos
FOR EACH ROW
EXECUTE FUNCTION public.validar_snapshot_em_processamento();

-- Uma execucao CONCLUIDA precisa possuir resumo e pelo menos um detalhe.
-- O trigger e diferido para permitir que resumo/detalhes sejam gravados antes
-- da mudanca final de status dentro da mesma transacao.
CREATE FUNCTION public.validar_conclusao_com_resultado()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.status = 'CONCLUIDA' THEN
        IF NOT EXISTS (
            SELECT 1
              FROM public.conciliacao_resumos r
             WHERE r.conciliacao_id = NEW.id
        ) THEN
            RAISE EXCEPTION
                USING ERRCODE = '23514',
                      MESSAGE = 'conciliacao CONCLUIDA exige resumo';
        END IF;

        IF NOT EXISTS (
            SELECT 1
              FROM public.conciliacao_detalhes d
             WHERE d.conciliacao_id = NEW.id
        ) THEN
            RAISE EXCEPTION
                USING ERRCODE = '23514',
                      MESSAGE = 'conciliacao CONCLUIDA exige detalhes';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE CONSTRAINT TRIGGER conciliacoes_conclusao_exige_resultado
AFTER INSERT OR UPDATE ON public.conciliacoes
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION public.validar_conclusao_com_resultado();
