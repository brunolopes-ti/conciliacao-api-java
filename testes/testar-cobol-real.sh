#!/usr/bin/env bash
set -euo pipefail
raiz_java="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
raiz_cobol="${1:-$HOME/projetos/cobol-conciliacao}"
raiz_cobol="$(cd -- "$raiz_cobol" && pwd)"
command -v cobc >/dev/null || { echo 'GnuCOBOL (cobc) nao encontrado.' >&2; exit 1; }
pasta_compilacao="$(mktemp -d)"
trap 'rm -rf -- "$pasta_compilacao"' EXIT
cobc -x -free -o "$pasta_compilacao/conciliacao" \
    "$raiz_cobol/conciliacao.cob" \
    "$raiz_cobol/validar-monetario.cob" \
    "$raiz_cobol/entrada-segura.c" \
    "$raiz_cobol/relatorio-seguro.c"
cd -- "$raiz_java"
COBOL_EXECUTAVEL_TESTE="$pasta_compilacao/conciliacao" bash ./mvnw test
