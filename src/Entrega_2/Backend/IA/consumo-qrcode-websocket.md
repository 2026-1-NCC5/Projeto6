# Guia de Consumo: Autenticação via QR Code e WebSocket

Este documento detalha como as duas interfaces (Web e Mobile) devem se comunicar com a API para executar o fluxo de autenticação por QR Code.

---

## 1. Interface Web (O Site)

A página Web é responsável por gerar o QR Code, exibi-lo na tela e aguardar a aprovação do Mobile via WebSocket.

### Passo 1: Gerar o Token Temporário
Ao carregar a página de login por QR Code, a Web deve chamar a API para gerar um `session_id`.

**Requisição:**
```http
POST /api/qr-auth/generate
Content-Type: application/json
```

**Resposta de Sucesso (201):**
```json
{
  "session_id": "c3f8b8a0-1234-4b5c-a9d0-ef1234567890",
  "expires_in": 120
}
```
**Ação:** Pegue o valor de `session_id` e gere um QR Code visual na tela (utilizando bibliotecas como `qrcode.react` ou `qrcode.js`).

### Passo 2: Conectar no WebSocket e Aguardar
Imediatamente após receber o `session_id`, a Web deve abrir uma conexão Socket.IO com o servidor e entrar na sala correta.

**Exemplo em JavaScript (Web):**
```javascript
import { io } from "socket.io-client";

// 1. Conecta no servidor WebSocket
const socket = io("http://localhost:8080"); 

// 2. Entra na sala usando o session_id gerado
const sessionId = "c3f8b8a0-1234-4b5c-a9d0-ef1234567890";
socket.emit("join-session", sessionId);

// 3. Aguarda o evento de sucesso emitido pelo Backend
socket.on("auth-success", (data) => {
    console.log("Sucesso! O mobile autorizou.", data);
    
    // Salva o novo JWT exclusivo da Web
    localStorage.setItem("web_token", data.token);
    
    // Redireciona o usuário para a sessão de contagem
    window.location.href = "/sessao-contagem";
});
```

---

## 2. Aplicativo Mobile

O aplicativo já possui um usuário autenticado (com um token JWT salvo). O papel do app é ler o QR Code e enviar o `session_id` para a API, autorizando o acesso.

### Passo Único: Escanear e Autorizar
Quando a câmera do Mobile ler o QR Code (que contém o texto do `session_id`), o app deve fazer um POST para a API enviando o próprio JWT no Header.

**Requisição:**
```http
POST /api/qr-auth/authenticate
Authorization: Bearer <SEU_JWT_MOBILE_AQUI>
Content-Type: application/json

{
    "session_id": "c3f8b8a0-1234-4b5c-a9d0-ef1234567890"
}
```

**Resposta de Sucesso (200):**
```json
{
    "message": "Sessão Web autenticada com sucesso!"
}
```
**Ação:** Ao receber o status `200 OK`, o App Mobile pode exibir uma mensagem de "Acesso Liberado" ou "Sucesso" na tela e fechar a câmera. O servidor já terá notificado o site automaticamente.

---

## 3. Resumo do Fluxo no Backend

1. Web chama `/generate` -> Backend salva no banco MySQL (`web_auth_tokens`) como "pendente".
2. Web conecta no `Socket.IO` e se inscreve no evento daquele `session_id`.
3. Mobile chama `/authenticate` -> Backend valida no banco, troca status para "autenticado", gera um novo JWT.
4. Backend faz `io.to(session_id).emit('auth-success', { token: ... })`.
5. Web recebe o evento, salva o token e redireciona.
6. A cada 5 minutos, o Backend limpa os tokens do banco MySQL que não foram utilizados e expiraram.
