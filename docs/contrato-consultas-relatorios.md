# Contrato HTTP — Consultas, Histórico e Relatórios

## 1. Objetivo

Este documento define o contrato HTTP do Bloco 6 da API de conciliação.

O Bloco 5 já implementou:

- criação da execução;
- snapshot dos dados;
- processamento COBOL;
- validação do resultado;
- persistência da execução;
- persistência do resumo;
- persistência dos detalhes;
- registro de falhas.

O Bloco 6 adiciona leitura dos dados já persistidos.

Serão disponibilizadas três operações principais:

```text
GET /api/conciliacoes
GET /api/conciliacoes/{id}
GET /api/conciliacoes/{id}/relatorio
```

Nenhuma dessas operações executa novamente o COBOL.

---

## 2. Regra arquitetural principal

A fonte das consultas é o PostgreSQL.

```text
cliente
   ↓
API Java
   ↓
PostgreSQL
   ↓
dados persistidos
```

Não será utilizado:

```text
cliente
   ↓
arquivo resultado.tsv antigo

ou

cliente
   ↓
relatorio.txt antigo
```

Os arquivos produzidos durante a execução do COBOL continuam sendo
artefatos temporários.

O histórico e os relatórios serão reconstruídos a partir dos dados
persistidos.

---

# 3. Listar histórico de conciliações

## Endpoint

```http
GET /api/conciliacoes
```

O endpoint retorna o histórico de execuções em ordem da mais recente para
a mais antiga.

A ordenação será determinística:

```text
criada_em DESC
id DESC
```

---

## 3.1. Paginação

A listagem será paginada.

Parâmetros opcionais:

```text
pagina
tamanho
status
```

Valores padrão:

```text
pagina = 0
tamanho = 20
```

Limites:

```text
pagina >= 0
1 <= tamanho <= 100
```

Exemplo:

```http
GET /api/conciliacoes?pagina=0&tamanho=20
```

---

## 3.2. Filtro por status

O parâmetro `status` é opcional.

Exemplo:

```http
GET /api/conciliacoes?status=CONCLUIDA
```

Valores aceitos:

```text
CRIADA
EM_PROCESSAMENTO
CONCLUIDA
FALHOU
```

Também será permitido combinar filtro e paginação:

```http
GET /api/conciliacoes?pagina=0&tamanho=10&status=FALHOU
```

Status desconhecido será rejeitado com:

```http
400 Bad Request
```

Parâmetros HTTP desconhecidos também serão rejeitados com:

```http
400 Bad Request
```

---

## 3.3. Resposta da listagem

Exemplo:

```json
{
  "pagina": 0,
  "tamanho": 20,
  "totalElementos": 2,
  "totalPaginas": 1,
  "itens": [
    {
      "id": 12,
      "status": "CONCLUIDA",
      "criadaEm": "2026-10-02T11:30:00Z",
      "iniciadaEm": "2026-10-02T11:30:00Z",
      "finalizadaEm": "2026-10-02T11:30:02Z",
      "resultadoVersao": 1,
      "erroCodigo": null,
      "resumo": {
        "conferidos": 2,
        "acima": 1,
        "abaixo": 0,
        "duplicados": 0,
        "semRecebimento": 1,
        "semPrevisao": 0,
        "totalEsperado": 475.75,
        "totalRecebido": 455.75,
        "saldoGlobal": -20.00
      }
    },
    {
      "id": 11,
      "status": "FALHOU",
      "criadaEm": "2026-10-02T11:20:00Z",
      "iniciadaEm": "2026-10-02T11:20:00Z",
      "finalizadaEm": "2026-10-02T11:20:31Z",
      "resultadoVersao": null,
      "erroCodigo": "TIMEOUT_COBOL",
      "resumo": null
    }
  ]
}
```

---

## 3.4. Dados que não aparecem na listagem

A listagem não retorna:

- detalhes individuais da conciliação;
- snapshot completo;
- `erro_detalhe`;
- stdout do COBOL;
- stderr do COBOL;
- caminhos internos;
- arquivos temporários.

Os detalhes serão consultados pelo endpoint específico da execução.

---

# 4. Consultar uma conciliação por ID

## Endpoint

```http
GET /api/conciliacoes/{id}
```

Exemplo:

```http
GET /api/conciliacoes/12
```

O identificador deve ser inteiro positivo.

Valores inválidos serão rejeitados com:

```http
400 Bad Request
```

---

## 4.1. Conciliação concluída

Para uma execução `CONCLUIDA`, a resposta inclui:

- dados da execução;
- resumo;
- detalhes.

Exemplo:

```json
{
  "id": 12,
  "status": "CONCLUIDA",
  "criadaEm": "2026-10-02T11:30:00Z",
  "iniciadaEm": "2026-10-02T11:30:00Z",
  "finalizadaEm": "2026-10-02T11:30:02Z",
  "resultadoVersao": 1,
  "erroCodigo": null,
  "resumo": {
    "conferidos": 1,
    "acima": 0,
    "abaixo": 0,
    "duplicados": 0,
    "semRecebimento": 0,
    "semPrevisao": 0,
    "totalEsperado": 100.00,
    "totalRecebido": 100.00,
    "saldoGlobal": 0.00
  },
  "detalhes": [
    {
      "ordem": 1,
      "identificador": "P001",
      "valorEsperado": 100.00,
      "valorRecebido": 100.00,
      "diferenca": 0.00,
      "status": "CONFERIDO",
      "quantidadeRecebimentos": 1
    }
  ]
}
```

Os detalhes serão sempre ordenados por:

```text
ordem ASC
```

---

## 4.2. Execução que falhou

Uma execução `FALHOU` continua podendo ser consultada.

Exemplo:

```json
{
  "id": 13,
  "status": "FALHOU",
  "criadaEm": "2026-10-02T11:40:00Z",
  "iniciadaEm": "2026-10-02T11:40:00Z",
  "finalizadaEm": "2026-10-02T11:40:31Z",
  "resultadoVersao": null,
  "erroCodigo": "TIMEOUT_COBOL",
  "resumo": null,
  "detalhes": []
}
```

O campo interno:

```text
erro_detalhe
```

não será exposto pela API.

Ele continua disponível apenas para diagnóstico interno.

---

## 4.3. Execução ainda em processamento

Uma execução `CRIADA` ou `EM_PROCESSAMENTO` também poderá ser consultada.

Enquanto não existir resultado persistido:

```json
{
  "id": 14,
  "status": "EM_PROCESSAMENTO",
  "criadaEm": "2026-10-02T11:50:00Z",
  "iniciadaEm": "2026-10-02T11:50:00Z",
  "finalizadaEm": null,
  "resultadoVersao": null,
  "erroCodigo": null,
  "resumo": null,
  "detalhes": []
}
```

Consultar uma execução em processamento não espera o COBOL terminar.

A API retorna o estado persistido naquele momento.

---

# 5. Conciliação inexistente

Quando o ID solicitado não existir:

```http
404 Not Found
```

Resposta:

```json
{
  "codigo": "CONCILIACAO_NAO_ENCONTRADA",
  "mensagem": "Conciliacao nao encontrada."
}
```

Nenhum registro será criado durante uma consulta.

---

# 6. Gerar relatório de uma conciliação

## Endpoint

```http
GET /api/conciliacoes/{id}/relatorio
```

O relatório será produzido pelo backend Java utilizando exclusivamente os
dados persistidos.

Não será reutilizado o `relatorio.txt` temporário produzido durante a
execução COBOL.

---

## 6.1. Execução concluída

Para uma conciliação `CONCLUIDA`:

```http
200 OK
```

Content-Type:

```text
text/plain; charset=UTF-8
```

A resposta será enviada como arquivo:

```text
conciliacao-{id}.txt
```

Exemplo:

```text
RELATORIO DE CONCILIACAO

ID: 12
STATUS: CONCLUIDA
CRIADA_EM: 2026-10-02T11:30:00Z
INICIADA_EM: 2026-10-02T11:30:00Z
FINALIZADA_EM: 2026-10-02T11:30:02Z
VERSAO_RESULTADO: 1

RESUMO
CONFERIDOS: 1
ACIMA: 0
ABAIXO: 0
DUPLICADOS: 0
SEM_RECEBIMENTO: 0
SEM_PREVISAO: 0
TOTAL_ESPERADO: 100.00
TOTAL_RECEBIDO: 100.00
SALDO_GLOBAL: 0.00

DETALHES
ORDEM | IDENTIFICADOR | VALOR_ESPERADO | VALOR_RECEBIDO | DIFERENCA | STATUS | QUANTIDADE_RECEBIMENTOS
1 | P001 | 100.00 | 100.00 | 0.00 | CONFERIDO | 1
```

Valores inexistentes nos detalhes serão representados no relatório por:

```text
-
```

Exemplo:

```text
2 | P002 | 200.00 | - | - | SEM_RECEBIMENTO | 0
```

---

## 6.2. Relatório determinístico

Para os mesmos dados persistidos, o relatório deve sempre possuir o mesmo
conteúdo funcional.

Os detalhes serão ordenados por:

```text
ordem ASC
```

Valores monetários serão apresentados com:

```text
duas casas decimais
ponto como separador decimal
```

Codificação:

```text
UTF-8
```

Quebra de linha:

```text
LF
```

---

# 7. Relatório indisponível

O relatório completo só existe para uma execução:

```text
CONCLUIDA
```

Se a execução estiver:

```text
CRIADA
EM_PROCESSAMENTO
FALHOU
```

o endpoint retorna:

```http
409 Conflict
```

Resposta:

```json
{
  "codigo": "RELATORIO_INDISPONIVEL",
  "mensagem": "Relatorio disponivel apenas para conciliacao concluida."
}
```

---

# 8. Segurança das respostas

As consultas nunca devem expor:

- senha de banco;
- `erro_detalhe`;
- stack trace;
- stdout bruto do COBOL;
- stderr bruto do COBOL;
- caminho do executável COBOL;
- diretório temporário;
- caminho dos CSVs;
- dados de configuração internos.

`erroCodigo` pode ser exposto porque representa uma classificação pública da
falha.

---

# 9. Consistência entre execução e resultado

As regras existentes no banco continuam sendo respeitadas.

Uma execução `CONCLUIDA` deve possuir:

- `resultado_versao`;
- resumo;
- detalhes compatíveis com o resultado persistido.

Uma execução `FALHOU` não possui resultado persistido completo.

O Bloco 6 não corrige silenciosamente inconsistências do banco.

Se uma inconsistência estrutural inesperada for encontrada durante a leitura,
a API deve tratá-la como erro interno em vez de fabricar um resultado.

---

# 10. Consultas são somente leitura

Os três endpoints deste documento são operações de leitura.

```text
GET /api/conciliacoes
GET /api/conciliacoes/{id}
GET /api/conciliacoes/{id}/relatorio
```

Eles não podem:

- criar uma conciliação;
- alterar status;
- executar COBOL;
- atualizar snapshot;
- modificar resumo;
- modificar detalhes;
- apagar registros.

---

# 11. Banco e migrations

O Bloco 6 utilizará inicialmente o esquema já criado nos Blocos anteriores.

As migrations:

```text
V1
V2
V3
V4
V5
```

permanecem imutáveis.

Nenhuma migration antiga será editada.

Caso uma alteração de banco seja realmente necessária durante a implementação,
ela deverá ser adicionada como:

```text
V6 ou superior
```

Somente será criada migration nova se houver necessidade técnica comprovada.

---

# 12. Códigos HTTP do Bloco 6

| Código | Uso |
| --- | --- |
| `200 OK` | Consulta ou relatório realizado com sucesso |
| `400 Bad Request` | ID, paginação, filtro ou parâmetro inválido |
| `404 Not Found` | Conciliação não existe |
| `409 Conflict` | Relatório solicitado para execução não concluída |
| `500 Internal Server Error` | Falha interna inesperada ou inconsistência estrutural |

---

# 13. Novos códigos públicos de erro

O Bloco 6 introduzirá:

```text
CONCILIACAO_NAO_ENCONTRADA
RELATORIO_INDISPONIVEL
```

Os códigos já existentes continuam válidos para as operações anteriores.

---

# 14. Responsabilidades por camada

A implementação deverá manter separação entre responsabilidades.

```text
Controller
    ↓
Serviço de consulta
    ↓
Repositório de consulta
    ↓
PostgreSQL
```

Para relatório:

```text
Controller
    ↓
Serviço de consulta
    ↓
Repositório de consulta
    ↓
dados persistidos
    ↓
Gerador de relatório
    ↓
TXT
```

O Controller não deve possuir SQL.

O repositório não deve conhecer regras HTTP.

O gerador de relatório não deve consultar diretamente o banco.

---

# 15. Resultado esperado ao final do Bloco 6

Ao final deste bloco, será possível:

```text
POST /api/conciliacoes
        ↓
executar nova conciliação

GET /api/conciliacoes
        ↓
consultar histórico paginado

GET /api/conciliacoes/{id}
        ↓
consultar execução, resumo e detalhes

GET /api/conciliacoes/{id}/relatorio
        ↓
baixar relatório reconstruído
```

Isso permitirá que o frontend Angular futuro consulte o histórico e os
resultados sem depender de arquivos temporários do processamento COBOL.
