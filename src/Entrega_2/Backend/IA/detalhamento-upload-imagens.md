# Envio de Imagens no Endpoint de Detecções em Lote

Data: 2026-05-23

Este documento descreve como o endpoint de registro em lote passou a aceitar imagens
e como o cliente (microserviço Python / aplicativo Android) deve ser atualizado para
enviar os frames capturados durante a contagem.

---

## O que mudou

O endpoint `POST /api/sessoes/:idSessao/deteccoes/lote` agora aceita duas formas de envio:

| Modo | Content-Type | Quando usar |
|------|-------------|-------------|
| **Com imagens** (novo) | `multipart/form-data` | Microserviço IA — envia dados + frames |
| **Sem imagens** (inalterado) | `application/json` | Clientes que não possuem frames |

> A compatibilidade retroativa foi mantida. Clientes que ainda enviam JSON puro continuam funcionando sem qualquer alteração.

---

## Formato multipart/form-data (com imagens)

### Campos de texto

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|------------|-----------|
| `deteccoes` | string (JSON serializado) | Sim | Array de detecções serializado como string JSON |
| `finalizar_sessao` | string `"true"` ou `"false"` | Não | Finaliza a sessão ao inserir o lote. Padrão: `"false"` |

> **Atenção:** Em multipart, campos de texto chegam como `string`. Por isso `deteccoes`
> deve ser serializado com `JSON.stringify()` / `json.dumps()` antes de ser enviado,
> e `finalizar_sessao` deve ser a string `"true"` ou `"false"`, não um booleano.

### Arquivos (imagens)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `imagens` | arquivo(s) | Um arquivo por detecção, **na mesma ordem** do array `deteccoes` |

**Regras de associação por índice:**

```
deteccoes[0]  ←→  imagens[0]  (primeira imagem enviada)
deteccoes[1]  ←→  imagens[1]  (segunda imagem enviada)
deteccoes[N]  ←→  imagens[N]  (N-ésima imagem enviada)
```

- Se uma detecção não tiver imagem correspondente, `imagem_url` será `null`.
- **O campo de arquivo deve sempre se chamar `imagens`** — com este nome exato.
- Tipos aceitos: `image/jpeg`, `image/png`, `image/webp`
- Tamanho máximo por arquivo: **5 MB**

### Como a API armazena as imagens

As imagens são salvas em disco no servidor:

```
backend/
└── uploads/
    └── deteccoes/
        ├── sessao-7_det-0_1716508800000.jpg
        ├── sessao-7_det-1_1716508800001.jpg
        └── sessao-7_det-2_1716508800002.png
```

O caminho relativo (`uploads/deteccoes/{filename}`) é gravado na coluna `imagem_url`
da tabela `deteccoes`. A imagem pode ser consultada via HTTP:

```
GET http://localhost:8080/uploads/deteccoes/sessao-7_det-0_1716508800000.jpg
```

---

## Exemplo — Python (microserviço IA)

```python
import json
import requests

BASE_URL = "http://localhost:8080/api"

# 1. Autenticar
login = requests.post(f"{BASE_URL}/auth/login", json={"username": "pedro_op", "password": "123"})
token = login.json()["token"]
headers = {"Authorization": f"Bearer {token}"}

# 2. Iniciar sessão
sessao = requests.post(f"{BASE_URL}/sessoes", headers=headers)
id_sessao = sessao.json()["id_sessao"]

# 3. Acumular detecções e frames durante o processamento YOLO
resultados = [
    {"classe_detectada": "arroz",   "confianca": 94.5, "sku": "ARR-005", "frame_path": "/tmp/frame_0.jpg"},
    {"classe_detectada": "feijao",  "confianca": 88.2, "sku": "FEI-001", "frame_path": "/tmp/frame_1.jpg"},
    {"classe_detectada": "macarrao","confianca": 85.1,                    "frame_path": "/tmp/frame_2.jpg"},
]

# 4. Preparar payload — deteccoes sem o campo frame_path
deteccoes = [
    {"classe_detectada": r["classe_detectada"], "confianca": r["confianca"], "sku": r.get("sku")}
    for r in resultados
]

# 5. Preparar arquivos — mesma ordem do array deteccoes
#    O campo DEVE se chamar "imagens"
files = [
    ("imagens", (f"frame_{i}.jpg", open(r["frame_path"], "rb"), "image/jpeg"))
    for i, r in enumerate(resultados)
]

# 6. Enviar — deteccoes como JSON serializado, imagens como arquivos
response = requests.post(
    f"{BASE_URL}/sessoes/{id_sessao}/deteccoes/lote",
    headers=headers,
    data={
        "deteccoes": json.dumps(deteccoes),  # string JSON, não objeto
        "finalizar_sessao": "true"           # string, não booleano
    },
    files=files
)

print(response.json())
```

---

## Exemplo — cURL

```bash
curl -X POST http://localhost:8080/api/sessoes/7/deteccoes/lote \
  -H "Authorization: Bearer <token>" \
  -F 'deteccoes=[{"classe_detectada":"arroz","confianca":94.5,"sku":"ARR-005"},{"classe_detectada":"feijao","confianca":88.2,"sku":"FEI-001"}]' \
  -F 'finalizar_sessao=true' \
  -F 'imagens=@/caminho/para/frame_arroz.jpg' \
  -F 'imagens=@/caminho/para/frame_feijao.jpg'
```

> **Importante:** `-F 'imagens=@arquivo'` deve ser repetido para cada arquivo.
> A ordem dos `-F 'imagens=...'` deve corresponder à ordem dos objetos em `deteccoes`.

---

## Exemplo — Kotlin / Android (Retrofit + OkHttp)

Para o aplicativo Android, a requisição deve ser montada com `MultipartBody`:

```kotlin
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

// Dados acumulados durante a sessão
data class DeteccaoItem(
    val classe_detectada: String,
    val confianca: Double,
    val sku: String? = null
)

data class FrameDeteccao(
    val deteccao: DeteccaoItem,
    val frameFile: File          // imagem do frame capturado
)

// Montar a requisição multipart
fun enviarLoteComImagens(
    token: String,
    idSessao: Int,
    frames: List<FrameDeteccao>,
    finalizarSessao: Boolean
) {
    val gson = com.google.gson.Gson()
    val deteccoesJson = gson.toJson(frames.map { it.deteccao })

    val builder = MultipartBody.Builder().setType(MultipartBody.FORM)

    // Campo de texto: deteccoes como JSON string
    builder.addFormDataPart(
        "deteccoes",
        deteccoesJson
    )

    // Campo de texto: finalizar_sessao como string
    builder.addFormDataPart(
        "finalizar_sessao",
        finalizarSessao.toString()
    )

    // Arquivos: um por detecção, na mesma ordem, com o nome de campo "imagens"
    frames.forEachIndexed { index, frame ->
        val requestBody = frame.frameFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        builder.addFormDataPart(
            "imagens",                          // nome do campo — DEVE ser "imagens"
            "frame_${index}.jpg",               // nome do arquivo (pode ser qualquer string)
            requestBody
        )
    }

    val requestBody = builder.build()

    // Enviar via Retrofit ou OkHttp
    // ...
}
```

> **Nota para Android:** o Retrofit não suporta nativamente `List<MultipartBody.Part>`
> com nome de campo repetido via anotações `@Part`. Use `MultipartBody.Builder`
> diretamente no OkHttp, ou monte a lista de parts no repositório antes de chamar
> a API.

---

## Resposta da API (sem alteração)

A resposta permanece idêntica ao comportamento anterior:

```json
{
  "message": "Lote registrado e sessão finalizada com sucesso",
  "id_sessao": 7,
  "deteccoes_inseridas": 3,
  "resumo": {
    "total": 3,
    "reconhecidos": 2,
    "nao_reconhecidos": 1,
    "erros": []
  },
  "sessao_finalizada": true,
  "data_fim": "2026-05-23T23:59:00.000Z"
}
```

Para verificar se a imagem foi gravada, consulte as detecções da sessão:

```
GET /api/sessoes/7/deteccoes
Authorization: Bearer <token>
```

O campo `imagem_url` de cada detecção retornará o caminho relativo:

```json
{
  "id_deteccao": 42,
  "classe_detectada": "arroz",
  "imagem_url": "uploads/deteccoes/sessao-7_det-0_1716508800000.jpg",
  ...
}
```

E a imagem pode ser obtida via:

```
GET http://localhost:8080/uploads/deteccoes/sessao-7_det-0_1716508800000.jpg
```

---

## Erros possíveis

| Código | Situação |
|--------|----------|
| `400` | Campo `deteccoes` ausente, array vazio, ou JSON inválido |
| `400` | Sessão já finalizada |
| `401` | Token JWT ausente ou mal formatado |
| `403` | Token JWT inválido ou expirado |
| `404` | Sessão não encontrada |
| `500` | Falha ao salvar arquivo ou erro no banco de dados |

Se o tipo de arquivo enviado não for JPEG, PNG ou WebP, o multer rejeitará a
requisição antes de ela chegar ao controller, retornando `500` com a mensagem de erro
do filtro.

---

## Arquivos modificados na API

| Arquivo | Alteração |
|---------|-----------|
| `package.json` | Adicionada dependência `multer` |
| `src/middlewares/uploadMiddleware.js` | **Novo** — configura multer (storage, filtro, limite) |
| `src/routes/sessaoRoutes.js` | Encadeado `upload.array('imagens', 50)` na rota de lote |
| `src/controllers/sessaoController.js` | Parse duplo de `deteccoes`; associação de imagens por índice |
| `src/server.js` | Adicionado `express.static` para servir `uploads/` |
| `.gitignore` | Adicionado `uploads/` |
