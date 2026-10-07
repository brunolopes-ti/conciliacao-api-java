# Contrato HTTP — Conciliação

## 1. Objetivo

Este documento define o contrato HTTP da primeira versão da API responsável
por iniciar uma conciliação de pagamentos.

O contrato foi definido antes da implementação do controller e dos DTOs para
que a camada HTTP siga as regras de negócio e de integração já estabelecidas.

O fluxo com PostgreSQL, snapshot persistente e geração de arquivos COBOL está
implementado até o bloco 5.

---

## 2. Fluxo da conciliação

A API não recebe arquivos CSV, caminhos locais ou dados brutos de cobranças e
pagamentos enviados pelo cliente.

O fluxo implementado é:

```text
requisição HTTP
    ↓
backend Java
    ↓
PostgreSQL
    ↓
snapshot imutável
    ↓
geração de esperados.csv e recebidos.csv
    ↓
motor COBOL
    ↓
resultado.tsv e relatorio.txt
    ↓
validação interna
    ↓
validação contra o snapshot
    ↓
persistência
    ↓
resposta HTTP
```

Os arquivos utilizados pelo COBOL são artefatos internos e temporários do
backend.

---

## 3. Criar uma conciliação

### Endpoint

```http
POST /api/conciliacoes
```

### Corpo da requisição

A primeira versão não aceita corpo na requisição.

Qualquer corpo enviado ao endpoint será rejeitado com:

```http
400 Bad Request
```

O cliente também não informa:

- caminho de arquivo;
- caminho do executável COBOL;
- `esperados.csv`;
- `recebidos.csv`;
- cobranças individuais;
- pagamentos individuais;
- diretório temporário.

Essas informações pertencem ao backend.

Parâmetros de requisição inesperados também serão rejeitados com
`400 Bad Request`.

---

## 4. Resposta de sucesso

Quando uma nova execução for criada e concluída com sucesso:

```http
201 Created
```

Formato da resposta:

```json
{
  "id": 123,
  "status": "CONCLUIDA",
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

O campo `id` é gerado pela camada de persistência.

---

## 5. Status da execução

Os status da execução da conciliação são diferentes dos status de cada detalhe.

Status reservados para a execução:

```text
CRIADA
EM_PROCESSAMENTO
CONCLUIDA
FALHOU
```

Status dos detalhes continuam sendo os definidos pelo contrato de integração
com o COBOL:

```text
CONFERIDO
ACIMA_DO_ESPERADO
ABAIXO_DO_ESPERADO
DUPLICADO
SEM_RECEBIMENTO
SEM_PREVISAO
```

---

## 6. Respostas de erro

A API utilizará respostas de erro padronizadas.

Formato:

```json
{
  "codigo": "TIMEOUT_COBOL",
  "mensagem": "O processamento da conciliacao excedeu o tempo permitido."
}
```

O cliente não deve receber:

- stack trace;
- stdout ou stderr bruto do processo;
- caminhos internos do servidor;
- caminho do executável COBOL;
- diretórios temporários;
- detalhes internos de configuração.

---

## 7. Códigos HTTP

| Código | Uso |
| --- | --- |
| `201 Created` | Conciliação criada e concluída com sucesso |
| `400 Bad Request` | Requisição HTTP inválida |
| `409 Conflict` | Estado atual dos dados impede iniciar a conciliação |
| `422 Unprocessable Entity` | Resultado produzido não passou pelas validações |
| `500 Internal Server Error` | Falha interna inesperada |
| `502 Bad Gateway` | Falha na execução do motor COBOL |
| `503 Service Unavailable` | Espera por vaga esgotada (`CAPACIDADE_ESGOTADA`) |
| `504 Gateway Timeout` | Motor COBOL excedeu o tempo permitido |

Os códigos serão refinados durante a implementação da camada de serviço caso
surjam situações de negócio que exijam tratamento específico.

---

## 8. Limites de responsabilidade

A execução e os resultados já são persistidos. Continuam pendentes os endpoints
de consulta e download. Os relatórios futuros serão reconstruídos dos dados
persistidos; TXT e TSV são temporários.

O POST permanece síncrono. O timeout do motor não inclui consultas SQL.
A espera por uma vaga tem prazo separado e configurável (1000 ms por padrão).
Quando ele termina, a resposta é 503, sem execução do motor para essa chamada.
O snapshot e o registro de execução já criados permanecem, com estado FALHOU.
O cliente não deve repetir automaticamente um POST cujo resultado ficou incerto:
a versão atual não implementa chave de idempotência.

Detalhes técnicos persistidos em `erro_detalhe` são internos e não pertencem à
resposta pública de erro.

---

## 9. Regra arquitetural principal

A fonte da conciliação será controlada pelo backend.

```text
PostgreSQL → snapshot → arquivos internos → COBOL
```

Não será utilizado:

```text
cliente → upload de CSV → COBOL
```

Essa regra preserva a correspondência entre os dados originais, os arquivos
entregues ao COBOL e o resultado posteriormente validado contra o mesmo
snapshot.

## Politica de repeticao e idempotencia

`POST /api/conciliacoes` representa o comando de iniciar uma nova execucao.
Cada chamada aceita cria uma nova conciliacao historica, mesmo quando os dados
de origem nao mudaram. Portanto o endpoint nao e idempotente nesta versao.

Se o cliente perder a resposta, ele deve consultar o historico antes de decidir
por uma nova execucao. Retry automatico do POST nao e recomendado. Suporte a
`Idempotency-Key` nao faz parte do contrato atual.
