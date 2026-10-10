-- Bloco 7: base de usuarios para autenticacao da API.
-- A associacao entre usuario e conciliacao sera adicionada em etapa posterior.

CREATE TABLE public.usuarios (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT usuarios_pkey
        PRIMARY KEY (id),

    CONSTRAINT usuarios_nome_valido
        CHECK (
            nome = btrim(nome)
            AND char_length(nome) BETWEEN 2 AND 100
        ),

    CONSTRAINT usuarios_email_valido
        CHECK (
            email = lower(btrim(email))
            AND char_length(email) BETWEEN 3 AND 254
            AND position('@' IN email) > 1
        ),

    CONSTRAINT usuarios_senha_hash_valida
        CHECK (
            char_length(senha_hash) BETWEEN 50 AND 100
        )
);

CREATE UNIQUE INDEX usuarios_email_lower_unique
    ON public.usuarios (lower(email));

GRANT SELECT, INSERT, UPDATE
ON TABLE public.usuarios
TO conciliacao_app;

GRANT USAGE, SELECT
ON SEQUENCE public.usuarios_id_seq
TO conciliacao_app;
