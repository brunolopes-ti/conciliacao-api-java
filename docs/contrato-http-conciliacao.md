# Contrato HTTP — Conciliação

## 1. Objetivo

Este documento define o contrato HTTP da primeira versão da API responsável
por iniciar uma conciliação de pagamentos.

O contrato foi definido antes da implementação do controller e dos DTOs para
que a camada HTTP siga as regras de negócio e de integração já estabelecidas.

A execução completa ainda dependerá das etapas futuras de persistência,
snapshot PostgreSQL e geração dos arquivos de entrada do motor COBOL.

---

## 2. Fluxo da conciliação

A API não recebe arquivos CSV, caminhos locais ou dados brutos de cobranças e
pagamentos enviados pelo cliente.

O fluxo planejado é:

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

A primeira versão não exige corpo.

O cliente não informa:

- caminho de arquivo;
- caminho do executável COBOL;
- `esperados.csv`;
- `recebidos.csv`;
- cobranças individuais;
- pagamentos individuais;
- diretório temporário.

Essas informações pertencem ao backend.

---

## 4. Resposta de sucesso

Quando uma nova execução for criada e concluída com sucesso:

```http
201 Created
```

Formato planejado:

```json
{
  "id": 123,
  "status": "CONCLUIDA",
  "resumo": {
    "conferidos": 10,
    "acima": 1,
    "abaixo": 2,
    "duplicados": 1,
    "semRecebimento": 3,
    "semPrevisao": 2,
    "totalEsperado": 1500.00,
    "totalRecebido": 1475.00,
    "saldoGlobal": -25.00
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

O campo `id` será gerado pela camada de persistência, ainda não implementada.

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
| `502 Bad Gateway` | Falha ou resposta inválida do motor COBOL |
| `504 Gateway Timeout` | Motor COBOL excedeu o tempo permitido |

Os códigos serão refinados durante a implementação da camada de serviço caso
surjam situações de negócio que exijam tratamento específico.

---

## 8. Limites de responsabilidade

A definição deste contrato HTTP não significa que o fluxo completo de negócio
já esteja implementado.

Ainda pertencem às próximas etapas:

- conexão com PostgreSQL;
- criação do snapshot persistente;
- geração de `esperados.csv` e `recebidos.csv` a partir do snapshot;
- persistência da execução;
- persistência dos resultados;
- persistência ou armazenamento do relatório;
- consulta de conciliações anteriores.

O endpoint não deve contornar essas etapas aceitando arquivos fornecidos pelo
cliente.

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
