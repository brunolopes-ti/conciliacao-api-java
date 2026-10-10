\set ON_ERROR_STOP on

SET ROLE conciliacao_app;

BEGIN;

DO $$
DECLARE
    usuario_id BIGINT;
    total INTEGER;
BEGIN
    -- Confirma que o teste realmente esta executando
    -- com os privilegios da role da aplicacao.
    IF current_user <> 'conciliacao_app' THEN
        RAISE EXCEPTION
            'FALHOU: role atual deveria ser conciliacao_app, mas e %',
            current_user;
    END IF;

    -- SELECT deve ser permitido.
    IF NOT has_table_privilege(
        current_user,
        'public.usuarios',
        'SELECT'
    ) THEN
        RAISE EXCEPTION
            'FALHOU: conciliacao_app sem SELECT em usuarios';
    END IF;

    -- INSERT deve ser permitido.
    IF NOT has_table_privilege(
        current_user,
        'public.usuarios',
        'INSERT'
    ) THEN
        RAISE EXCEPTION
            'FALHOU: conciliacao_app sem INSERT em usuarios';
    END IF;

    -- UPDATE deve ser permitido.
    IF NOT has_table_privilege(
        current_user,
        'public.usuarios',
        'UPDATE'
    ) THEN
        RAISE EXCEPTION
            'FALHOU: conciliacao_app sem UPDATE em usuarios';
    END IF;

    -- DELETE nao deve ser permitido.
    IF has_table_privilege(
        current_user,
        'public.usuarios',
        'DELETE'
    ) THEN
        RAISE EXCEPTION
            'FALHOU: conciliacao_app possui DELETE em usuarios';
    END IF;

    -- Usuario valido deve ser aceito.
    INSERT INTO public.usuarios (
        nome,
        email,
        senha_hash
    )
    VALUES (
        'Usuario Teste',
        'usuario@teste.com',
        repeat('a', 60)
    )
    RETURNING id INTO usuario_id;

    IF usuario_id IS NULL THEN
        RAISE EXCEPTION
            'FALHOU: usuario valido nao foi criado';
    END IF;

    -- UPDATE deve funcionar.
    UPDATE public.usuarios
       SET nome = 'Usuario Atualizado'
     WHERE id = usuario_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'FALHOU: usuario nao foi atualizado';
    END IF;

    -- Email duplicado deve ser rejeitado.
    BEGIN
        INSERT INTO public.usuarios (
            nome,
            email,
            senha_hash
        )
        VALUES (
            'Usuario Duplicado',
            'usuario@teste.com',
            repeat('b', 60)
        );

        RAISE EXCEPTION
            'FALHOU: email duplicado foi aceito';

    EXCEPTION
        WHEN unique_violation THEN
            NULL;
    END;

    -- Email deve estar normalizado em minusculas.
    BEGIN
        INSERT INTO public.usuarios (
            nome,
            email,
            senha_hash
        )
        VALUES (
            'Email Maiusculo',
            'USUARIO2@TESTE.COM',
            repeat('c', 60)
        );

        RAISE EXCEPTION
            'FALHOU: email em maiusculas foi aceito';

    EXCEPTION
        WHEN check_violation THEN
            NULL;
    END;

    -- Hash curto deve ser rejeitado.
    BEGIN
        INSERT INTO public.usuarios (
            nome,
            email,
            senha_hash
        )
        VALUES (
            'Hash Curto',
            'hash@teste.com',
            '123'
        );

        RAISE EXCEPTION
            'FALHOU: hash curto foi aceito';

    EXCEPTION
        WHEN check_violation THEN
            NULL;
    END;

    -- Nome com espacos nas extremidades deve ser rejeitado.
    BEGIN
        INSERT INTO public.usuarios (
            nome,
            email,
            senha_hash
        )
        VALUES (
            ' Usuario Invalido ',
            'nome@teste.com',
            repeat('d', 60)
        );

        RAISE EXCEPTION
            'FALHOU: nome com espacos externos foi aceito';

    EXCEPTION
        WHEN check_violation THEN
            NULL;
    END;

    -- DELETE deve falhar por falta de permissao.
    BEGIN
        DELETE FROM public.usuarios
        WHERE id = usuario_id;

        RAISE EXCEPTION
            'FALHOU: DELETE foi permitido para conciliacao_app';

    EXCEPTION
        WHEN insufficient_privilege THEN
            NULL;
    END;

    -- O usuario valido deve continuar existindo.
    SELECT count(*)
      INTO total
      FROM public.usuarios
     WHERE email = 'usuario@teste.com';

    IF total <> 1 THEN
        RAISE EXCEPTION
            'FALHOU: quantidade inesperada de usuarios validos: %',
            total;
    END IF;

    RAISE NOTICE
        'Usuarios V7: PASSOU';
END;
$$;

ROLLBACK;

RESET ROLE;

\echo 'Contrato de usuarios V7: PASSOU'
