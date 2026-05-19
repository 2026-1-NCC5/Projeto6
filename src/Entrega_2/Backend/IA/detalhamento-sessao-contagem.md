# Detalhamento — Sessao de Contagem via API

Data: 2026-05-17

Este documento descreve o processo completo para iniciar uma sessao de contagem de produtos, registrar as deteccoes da inteligencia artificial e finalizar a sessao utilizando a API REST do backend AlimempatIA.

---

## Visao geral do fluxo

O ciclo de vida de uma sessao de contagem segue tres etapas:

```
Autenticacao  →  Iniciar Sessao  →  Registrar Deteccoes  →  Finalizar Sessao
```

Cada sessao agrupa todas as deteccoes realizadas durante um periodo de contagem (por exemplo, enquanto a esteira esta em funcionamento ou enquanto o operador esta utilizando a camera). Ao final, a sessao e encerrada e todas as deteccoes ficam vinculadas a ela para consulta posterior.

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────────────────┐     ┌──────────────────┐
│   Login     │────>│  Iniciar Sessao │────>│  Registrar Deteccoes    │────>│ Finalizar Sessao │
│ POST /auth  │     │  POST /sessoes  │     │  POST /:id/deteccoes    │     │ POST /:id/final  │
│             │     │                 │     │  POST /:id/deteccoes/   │     │                  │
│ Retorna JWT │     │ Retorna         │     │       lote              │     │ Encerra a sessao │
│             │     │ id_sessao       │     │                         │     │                  │
└─────────────┘     └─────────────────┘     └─────────────────────────┘     └──────────────────┘
```

---

## Pre-requisitos

- Servidor backend rodando (`node src/server.js` na porta 8080).
- Banco de dados MySQL com as tabelas `sessoes`, `deteccoes` e `produtos` criadas conforme o arquivo `database/alimempatia_db.sql`.
- Um usuario cadastrado no sistema com `id_equipe` vinculado.

---

## Tabelas envolvidas

### sessoes

Controla o inicio e fim de cada ciclo de contagem.

| Coluna       | Tipo                           | Descricao                                 |
|--------------|--------------------------------|-------------------------------------------|
| id_sessao    | INT AUTO_INCREMENT             | Identificador unico da sessao             |
| id_usuario   | INT NOT NULL                   | Operador que iniciou a sessao             |
| id_equipe    | INT NOT NULL                   | Equipe do operador                        |
| data_inicio  | DATETIME DEFAULT NOW()         | Data/hora de inicio                       |
| data_fim     | DATETIME DEFAULT NULL          | Data/hora de finalizacao                  |
| status       | ENUM('ativa','finalizada')     | Estado atual da sessao                    |

### deteccoes

Armazena cada item detectado pela inteligencia artificial durante uma sessao.

| Coluna            | Tipo                                                           | Descricao                                     |
|-------------------|----------------------------------------------------------------|-----------------------------------------------|
| id_deteccao       | INT AUTO_INCREMENT                                             | Identificador unico da deteccao               |
| id_sessao         | INT NOT NULL                                                   | Sessao a qual pertence                        |
| id_produto        | INT DEFAULT NULL                                               | Produto correspondente (NULL se nao encontrado)|
| classe_detectada  | ENUM('arroz','feijao','macarrao','oleo','leite','outros')      | Classe identificada pelo modelo YOLO          |
| confianca         | DECIMAL(5,2) DEFAULT NULL                                      | Percentual de confianca da deteccao (0-100)   |
| reconhecido       | TINYINT(1) DEFAULT 1                                           | 1 = produto reconhecido, 0 = nao reconhecido  |
| imagem_url        | TEXT                                                           | URL ou caminho da imagem capturada            |
| detectado_em      | DATETIME DEFAULT NOW()                                         | Data/hora da deteccao                         |

### produtos (referencia)

Os SKUs utilizados nas deteccoes devem corresponder a produtos cadastrados nesta tabela.

| SKU       | Nome           | Categoria   |
|-----------|----------------|-------------|
| ARR-005   | Arroz 5kg      | Cereais     |
| ARR-001   | Arroz 1kg      | Cereais     |
| ARR-010   | Arroz 10kg     | Cereais     |
| SKU-LE-002| Feijao         | Leguminosas |
| FEI-001   | Feijao 1kg     | Leguminosas |
| FEI-005   | Feijao 5kg     | Leguminosas |
| SKU-MA-003| Macarrao       | Massas      |
| MAC-500   | Macarrao 500g  | Massas      |
| SKU-OL-004| Oleo           | Oleos       |
| OLE-900   | Oleo 900ml     | Oleos       |
| SKU-LA-005| Leite          | Laticinios  |
| LEI-001   | Leite 1L       | Laticinios  |

---

## Endpoints disponiveis

| Metodo | Endpoint                                | Descricao                              |
|--------|-----------------------------------------|----------------------------------------|
| POST   | `/api/auth/login`                       | Autentica o usuario e retorna o JWT    |
| POST   | `/api/sessoes`                          | Inicia uma nova sessao de contagem     |
| POST   | `/api/sessoes/:idSessao/deteccoes`      | Registra uma unica deteccao            |
| POST   | `/api/sessoes/:idSessao/deteccoes/lote` | Registra um lote de deteccoes de uma vez|
| POST   | `/api/sessoes/:idSessao/finalizar`      | Finaliza a sessao manualmente          |
| GET    | `/api/sessoes/:idSessao/deteccoes`      | Lista todas as deteccoes de uma sessao |

Todas as rotas de sessao exigem autenticacao via header `Authorization: Bearer <token>`.

---

## Passo a passo detalhado

### Etapa 1 — Autenticacao

Antes de qualquer operacao, e necessario obter um token JWT.

**Requisicao:**

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

```json
{
  "username": "pedro_op",
  "password": "123"
}
```

**Resposta (200):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 11,
    "name": "Pedro Henrique",
    "username": "pedro_op",
    "role": "Operador",
    "id_equipe": 2
  }
}
```

O campo `token` deve ser armazenado e enviado em todas as requisicoes seguintes no header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

---

### Etapa 2 — Iniciar uma sessao de contagem

O operador (ou o sistema automatizado) inicia uma nova sessao. O backend vincula automaticamente o `id_usuario` e `id_equipe` extraidos do token JWT.

**Requisicao:**

```
POST http://localhost:8080/api/sessoes
Authorization: Bearer <token>
```

Nenhum corpo e necessario. O backend identifica o usuario pelo token.

**Resposta (201):**

```json
{
  "message": "Sessao de contagem iniciada com sucesso",
  "id_sessao": 1,
  "id_usuario": 11,
  "id_equipe": 2,
  "status": "ativa",
  "data_inicio": "2026-05-17T23:59:00.000Z"
}
```

> **Importante:** Guarde o valor de `id_sessao` retornado. Ele sera usado em todas as requisicoes de deteccao.

---

### Etapa 3 — Registrar deteccoes

Existem duas formas de registrar as deteccoes: individualmente (uma por vez) ou em lote (todas de uma vez). Recomenda-se o envio em lote por questoes de performance.

#### Opcao A — Registro individual

Para cada produto detectado pela camera ou modelo YOLO, envie uma requisicao.

**Requisicao:**

```
POST http://localhost:8080/api/sessoes/1/deteccoes
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "classe_detectada": "arroz",
  "confianca": 94.50,
  "sku": "ARR-005",
  "imagem_url": "http://exemplo.com/imagens/deteccao_001.jpg"
}
```

**Campos do body:**

| Campo             | Tipo    | Obrigatorio | Descricao                                                                 |
|-------------------|---------|-------------|---------------------------------------------------------------------------|
| classe_detectada  | string  | Sim         | Classe identificada pelo modelo: arroz, feijao, macarrao, oleo, leite, outros |
| confianca         | number  | Nao         | Percentual de confianca da deteccao (0 a 100). Padrao: 0.0                |
| sku               | string  | Nao         | SKU do produto para vinculacao direta. Se ausente, busca pelo nome da classe |
| reconhecido       | boolean | Nao         | Se o produto foi reconhecido. Padrao: true se o produto for encontrado    |
| imagem_url        | string  | Nao         | URL ou caminho da imagem capturada no momento da deteccao                 |

**Resposta (201):**

```json
{
  "message": "Deteccao registrada com sucesso",
  "id_deteccao": 1,
  "id_sessao": 1,
  "id_produto": 1,
  "classe_detectada": "arroz",
  "confianca": 94.50,
  "reconhecido": 1,
  "detectado_em": "2026-05-17T23:59:30.000Z"
}
```

#### Opcao B — Registro em lote (recomendado)

Envia todas as deteccoes acumuladas em uma unica requisicao. Este endpoint executa um unico INSERT SQL para todas as deteccoes e atualiza o estoque de cada produto reconhecido somando a quantidade correta.

**Requisicao:**

```
POST http://localhost:8080/api/sessoes/1/deteccoes/lote
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "deteccoes": [
    {
      "classe_detectada": "arroz",
      "confianca": 94.50,
      "sku": "ARR-005"
    },
    {
      "classe_detectada": "arroz",
      "confianca": 91.20,
      "sku": "ARR-001"
    },
    {
      "classe_detectada": "feijao",
      "confianca": 88.30,
      "sku": "FEI-001"
    },
    {
      "classe_detectada": "macarrao",
      "confianca": 85.10
    },
    {
      "classe_detectada": "oleo",
      "confianca": 79.60,
      "sku": "OLE-900"
    },
    {
      "classe_detectada": "outros",
      "confianca": 45.00,
      "reconhecido": false
    }
  ],
  "finalizar_sessao": true
}
```

**Campos do body:**

| Campo              | Tipo    | Obrigatorio | Descricao                                                        |
|--------------------|---------|-------------|------------------------------------------------------------------|
| deteccoes          | array   | Sim         | Lista de objetos de deteccao (mesmo formato do registro individual)|
| finalizar_sessao   | boolean | Nao         | Se `true`, finaliza a sessao automaticamente apos inserir o lote |

Cada objeto dentro do array `deteccoes` aceita os mesmos campos descritos na Opcao A.

**Resposta (201) — com `finalizar_sessao: true`:**

```json
{
  "message": "Lote registrado e sessao finalizada com sucesso",
  "id_sessao": 1,
  "deteccoes_inseridas": 6,
  "resumo": {
    "total": 6,
    "reconhecidos": 5,
    "nao_reconhecidos": 1,
    "erros": []
  },
  "sessao_finalizada": true,
  "data_fim": "2026-05-18T00:01:00.000Z"
}
```

**Resposta (201) — com `finalizar_sessao: false` ou ausente:**

```json
{
  "message": "Lote de deteccoes registrado com sucesso",
  "id_sessao": 1,
  "deteccoes_inseridas": 6,
  "resumo": {
    "total": 6,
    "reconhecidos": 5,
    "nao_reconhecidos": 1,
    "erros": []
  },
  "sessao_finalizada": false
}
```

---

### Etapa 4 — Finalizar a sessao (manual)

Se a sessao nao foi finalizada automaticamente pelo envio em lote, finalize-a manualmente.

**Requisicao:**

```
POST http://localhost:8080/api/sessoes/1/finalizar
Authorization: Bearer <token>
```

**Resposta (200):**

```json
{
  "message": "Sessao finalizada com sucesso",
  "id_sessao": 1,
  "status": "finalizada",
  "data_fim": "2026-05-18T00:02:00.000Z"
}
```

---

### Etapa 5 — Consultar deteccoes de uma sessao

Apos a sessao ser finalizada (ou enquanto estiver ativa), e possivel consultar todas as deteccoes registradas.

**Requisicao:**

```
GET http://localhost:8080/api/sessoes/1/deteccoes
Authorization: Bearer <token>
```

**Resposta (200):**

```json
{
  "id_sessao": 1,
  "resumo": {
    "total": 6,
    "reconhecidos": 5,
    "nao_reconhecidos": 1
  },
  "deteccoes": [
    {
      "id_deteccao": 1,
      "id_sessao": 1,
      "classe_detectada": "arroz",
      "confianca": "94.50",
      "reconhecido": 1,
      "imagem_url": null,
      "detectado_em": "2026-05-17T23:59:30.000Z",
      "produto_nome": "Arroz 5kg",
      "produto_sku": "ARR-005"
    },
    {
      "id_deteccao": 2,
      "id_sessao": 1,
      "classe_detectada": "arroz",
      "confianca": "91.20",
      "reconhecido": 1,
      "imagem_url": null,
      "detectado_em": "2026-05-17T23:59:31.000Z",
      "produto_nome": "Arroz 1kg",
      "produto_sku": "ARR-001"
    },
    {
      "id_deteccao": 6,
      "id_sessao": 1,
      "classe_detectada": "outros",
      "confianca": "45.00",
      "reconhecido": 0,
      "imagem_url": null,
      "detectado_em": "2026-05-17T23:59:35.000Z",
      "produto_nome": null,
      "produto_sku": null
    }
  ]
}
```

---

## Resolucao de produtos

A API utiliza a seguinte logica para vincular uma deteccao a um produto cadastrado:

1. Se o campo `sku` for enviado, busca o produto diretamente pelo SKU na tabela `produtos`.
2. Se o campo `sku` nao for enviado, tenta localizar um produto cujo nome contenha a `classe_detectada` (busca parcial, case-insensitive).
3. Se nenhum produto for encontrado, a deteccao e registrada com `id_produto = NULL` e `reconhecido = 0`.

No envio em lote, a resolucao e otimizada: todos os SKUs sao buscados com uma unica query `WHERE sku IN (...)` e todas as classes com uma unica query `WHERE LOWER(nome) LIKE ...`, evitando consultas individuais por deteccao.

---

## Atualizacao automatica do estoque

Quando uma deteccao e vinculada a um produto reconhecido (`id_produto` nao nulo):

- No registro individual: o campo `quantidade` do produto e incrementado em 1.
- No registro em lote: o campo `quantidade` de cada produto e incrementado pela quantidade total de vezes que ele apareceu no lote.

A coluna `atualizado_em` do produto tambem e atualizada para o momento da operacao.

---

## Codigos de erro

| Codigo | Situacao                                                        |
|--------|-----------------------------------------------------------------|
| 400    | Campo obrigatorio ausente (`classe_detectada`) ou array vazio   |
| 400    | Sessao ja finalizada (tentativa de registrar em sessao encerrada)|
| 400    | Usuario sem equipe vinculada                                    |
| 401    | Token JWT nao enviado ou mal formatado                          |
| 403    | Token JWT invalido ou expirado                                  |
| 404    | Sessao nao encontrada                                           |
| 500    | Erro interno do servidor ou falha no banco de dados             |

---

## Exemplo completo com cURL

### 1. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"pedro_op","password":"123"}'
```

### 2. Iniciar sessao

```bash
curl -X POST http://localhost:8080/api/sessoes \
  -H "Authorization: Bearer <token>"
```

### 3. Enviar lote de deteccoes e finalizar

```bash
curl -X POST http://localhost:8080/api/sessoes/1/deteccoes/lote \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "finalizar_sessao": true,
    "deteccoes": [
      {"classe_detectada":"arroz","confianca":94.5,"sku":"ARR-005"},
      {"classe_detectada":"feijao","confianca":88.2,"sku":"FEI-001"},
      {"classe_detectada":"macarrao","confianca":85.1},
      {"classe_detectada":"oleo","confianca":79.6,"sku":"OLE-900"},
      {"classe_detectada":"outros","confianca":45.0,"reconhecido":false}
    ]
  }'
```

### 4. Consultar deteccoes

```bash
curl http://localhost:8080/api/sessoes/1/deteccoes \
  -H "Authorization: Bearer <token>"
```

---

## Exemplo de integracao com script Python (YOLO)

Abaixo, um exemplo de como um script Python que utiliza o modelo YOLOv8 poderia enviar as deteccoes para a API apos processar um conjunto de imagens:

```python
import requests

BASE_URL = "http://localhost:8080/api"

# 1. Autenticar
login = requests.post(f"{BASE_URL}/auth/login", json={
    "username": "pedro_op",
    "password": "123"
})
token = login.json()["token"]
headers = {"Authorization": f"Bearer {token}"}

# 2. Iniciar sessao
sessao = requests.post(f"{BASE_URL}/sessoes", headers=headers)
id_sessao = sessao.json()["id_sessao"]
print(f"Sessao iniciada: {id_sessao}")

# 3. Executar deteccoes com YOLO (exemplo simplificado)
# Suponha que o modelo ja processou as imagens e gerou os resultados:
resultados_yolo = [
    {"classe_detectada": "arroz",    "confianca": 94.5, "sku": "ARR-005"},
    {"classe_detectada": "arroz",    "confianca": 92.1, "sku": "ARR-001"},
    {"classe_detectada": "feijao",   "confianca": 88.3, "sku": "FEI-001"},
    {"classe_detectada": "macarrao", "confianca": 85.0},
    {"classe_detectada": "oleo",     "confianca": 79.6, "sku": "OLE-900"},
    {"classe_detectada": "leite",    "confianca": 90.2, "sku": "LEI-001"},
    {"classe_detectada": "outros",   "confianca": 42.0, "reconhecido": False},
]

# 4. Enviar lote e finalizar sessao
resposta = requests.post(
    f"{BASE_URL}/sessoes/{id_sessao}/deteccoes/lote",
    headers=headers,
    json={
        "deteccoes": resultados_yolo,
        "finalizar_sessao": True
    }
)

resultado = resposta.json()
print(f"Deteccoes inseridas: {resultado['deteccoes_inseridas']}")
print(f"Reconhecidos: {resultado['resumo']['reconhecidos']}")
print(f"Nao reconhecidos: {resultado['resumo']['nao_reconhecidos']}")
print(f"Sessao finalizada: {resultado['sessao_finalizada']}")
```

---

## Exemplo de integracao com aplicativo Android (Kotlin)

Para integrar no aplicativo Android, utilize Retrofit ou qualquer cliente HTTP. Exemplo conceitual com Retrofit:

```kotlin
// 1. Iniciar sessao
val sessaoResponse = api.iniciarSessao(token)
val idSessao = sessaoResponse.id_sessao

// 2. Acumular deteccoes durante o uso da camera
val deteccoes = mutableListOf<Deteccao>()

// Cada vez que o modelo YOLO detecta um produto:
deteccoes.add(Deteccao(
    classe_detectada = "arroz",
    confianca = 94.5,
    sku = "ARR-005"
))

// 3. Ao encerrar a contagem, enviar tudo de uma vez
val loteRequest = LoteDeteccoesRequest(
    deteccoes = deteccoes,
    finalizar_sessao = true
)
val resultado = api.enviarLoteDeteccoes(token, idSessao, loteRequest)
```

---

## Arquivos de codigo relacionados

| Arquivo                                    | Descricao                              |
|--------------------------------------------|----------------------------------------|
| `src/controllers/sessaoController.js`      | Logica de negocio das sessoes          |
| `src/routes/sessaoRoutes.js`               | Definicao das rotas HTTP               |
| `src/server.js`                            | Registro da rota `/api/sessoes`        |
| `src/middlewares/authMiddleware.js`         | Validacao do token JWT                 |
| `database/alimempatia_db.sql`              | Estrutura das tabelas sessoes e deteccoes |

---

## Observacoes

- Somente sessoes com status `ativa` aceitam novas deteccoes. Tentativas de registrar em sessoes finalizadas retornam erro 400.
- O campo `classe_detectada` na tabela `deteccoes` e um ENUM com valores fixos: `arroz`, `feijao`, `macarrao`, `oleo`, `leite`, `outros`. Certifique-se de que o modelo YOLO mapeia suas classes para um destes valores antes de enviar.
- Deteccoes com `reconhecido = 0` indicam itens que a IA nao conseguiu vincular a um produto cadastrado. Esses registros permanecem no banco para analise posterior.
- O envio em lote e a forma recomendada para cenarios de producao, pois reduz o numero de requisicoes HTTP e queries SQL.
