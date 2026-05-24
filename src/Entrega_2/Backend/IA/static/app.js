// ==========================================================================
// Lógica de Negócio do Dashboard Web - AlimempatIA
// ==========================================================================

const API_BACKEND_URL = "http://localhost:8080";
let socket = null;
let currentSessionId = null;
let currentJwtToken = null;
let statusPollingInterval = null;

// Referências do DOM
const screenAuth = document.getElementById("screen-auth");
const screenDashboard = document.getElementById("screen-dashboard");
const qrcodeBox = document.getElementById("qrcode-box");
const qrLoading = document.getElementById("qr-loading");
const authStatusMsg = document.getElementById("auth-status-msg");
const displaySessionId = document.getElementById("display-session-id");
const videoStream = document.getElementById("video-stream");
const videoPlaceholder = document.getElementById("video-placeholder");
const detectionsTbody = document.getElementById("detections-tbody");
const totalCountBadge = document.getElementById("total-count-badge");
const sumRecognized = document.getElementById("sum-recognized");
const sumUnrecognized = document.getElementById("sum-unrecognized");
const btnFinalizar = document.getElementById("btn-finalizar");

// ──────────────────────────────────────────────────────────
// Inicialização
// ──────────────────────────────────────────────────────────
document.addEventListener("DOMContentLoaded", () => {
    iniciarFluxoAutenticacao();
    
    // Evento de Finalizar
    btnFinalizar.addEventListener("click", finalizarSessaoIA);
});

// ──────────────────────────────────────────────────────────
// Fluxo 1: Geração de QR Code e Conexão Socket.IO
// ──────────────────────────────────────────────────────────
async function iniciarFluxoAutenticacao() {
    qrcodeBox.innerHTML = "";
    qrLoading.style.display = "flex";
    authStatusMsg.textContent = "Obtendo token temporário com o backend...";

    try {
        // Passo 1: Obter session_id temporário da API Node (porta 8080)
        const response = await fetch(`${API_BACKEND_URL}/api/qr-auth/generate`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error(`Erro ao gerar QR Token: ${response.status}`);
        }

        const data = await response.json();
        const sessionId = data.session_id;
        currentSessionId = sessionId;

        // Desenhar o QR Code na tela usando qrcode.js
        // O valor codificado é apenas o session_id, que o app mobile enviará no POST
        new QRCode(qrcodeBox, {
            text: sessionId,
            width: 200,
            height: 200,
            colorDark: "#0b0f19",
            colorLight: "#ffffff",
            correctLevel: QRCode.CorrectLevel.H
        });

        qrLoading.style.display = "none";
        authStatusMsg.textContent = "Aguardando leitura do celular...";

        // Passo 2: Conectar ao Socket.IO do backend Node.js (porta 8080)
        conectarWebSocket(sessionId);

    } catch (err) {
        console.error("Falha no login por QR Code:", err);
        authStatusMsg.textContent = "Erro ao carregar o QR Code. Tente recarregar a página.";
        authStatusMsg.style.color = "#ef4444";
    }
}

function conectarWebSocket(sessionId) {
    if (socket) {
        socket.disconnect();
    }

    socket = io(API_BACKEND_URL);

    socket.on("connect", () => {
        console.log("Conectado ao WebSocket da porta 8080. Entrando na sala:", sessionId);
        socket.emit("join-session", sessionId);
    });

    // Aguarda o sucesso da validação efetuada pelo celular
    socket.on("auth-success", async (data) => {
        console.log("Autenticação autorizada com sucesso via celular!", data);
        
        currentJwtToken = data.token;
        localStorage.setItem("web_token", currentJwtToken);

        authStatusMsg.textContent = "Acesso concedido! Iniciando sistema de visão computacional...";
        authStatusMsg.style.color = "#10b981";

        // Inicia a câmera e o YOLO no FastAPI local (porta 5001)
        await iniciarSessaoIA(currentJwtToken);
    });

    socket.on("connect_error", (error) => {
        console.error("Erro na conexão WebSocket:", error);
    });
}

// ──────────────────────────────────────────────────────────
// Fluxo 2: Comunicação com o FastAPI Local (Porta 5001)
// ──────────────────────────────────────────────────────────

async function iniciarSessaoIA(jwtToken) {
    try {
        // Envia o JWT para o FastAPI local iniciar a câmera + sessão
        const response = await fetch("/iniciar-jwt", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ token: jwtToken })
        });

        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.detail || "Erro desconhecido");
        }

        const data = await response.json();
        console.log("Sessão de contagem de IA iniciada:", data);

        // Transição de tela
        screenAuth.classList.remove("active");
        screenDashboard.classList.add("active");

        displaySessionId.textContent = `#${data.id_sessao}`;

        // Inicia o feed de vídeo (MJPEG)
        videoStream.src = "/video_feed";
        videoPlaceholder.style.display = "none";

        // Inicia polling para atualizar console de detecções e estado
        iniciarStatusPolling();

    } catch (err) {
        console.error("Falha ao iniciar sessão de IA no FastAPI:", err);
        alert(`Não foi possível iniciar a câmera/modelo de IA: ${err.message}`);
        // Retorna pro login
        iniciarFluxoAutenticacao();
    }
}

function iniciarStatusPolling() {
    if (statusPollingInterval) {
        clearInterval(statusPollingInterval);
    }

    // Busca o estado das detecções a cada 500ms para manter o console super fluido
    statusPollingInterval = setInterval(buscarEstadoIA, 500);
}

async function buscarEstadoIA() {
    try {
        const response = await fetch("/estado");
        if (!response.ok) return;

        const data = await response.json();
        
        // Atualiza a tabela de detecções na interface
        atualizarConsoleTabela(data.deteccoes || []);

    } catch (err) {
        console.warn("Erro ao buscar estado da IA:", err);
    }
}

function atualizarConsoleTabela(deteccoes) {
    if (deteccoes.length === 0) {
        detectionsTbody.innerHTML = `
            <tr class="empty-row">
                <td colspan="5">Nenhum item detectado na linha de contagem até o momento.</td>
            </tr>
        `;
        totalCountBadge.textContent = "0 itens";
        sumRecognized.textContent = "0";
        sumUnrecognized.textContent = "0";
        return;
    }

    let html = "";
    let reconhecidosCount = 0;
    let naoReconhecidosCount = 0;

    // Renderiza cada linha de detecção de trás para frente (mais recente no topo)
    // Clonamos o array para não reverter o original
    [...deteccoes].reverse().forEach(det => {
        const confianca = det.confianca ? `${det.confianca}%` : "N/A";
        const sku = det.sku || `<span style="color: var(--text-muted)">Sem SKU</span>`;
        const dimensoes = (det.largura_cm && det.altura_cm) ? `${det.largura_cm}x${det.altura_cm} cm` : "N/A";
        const peso = det.peso_kg ? `${det.peso_kg} kg` : "N/A";

        // Classifica se é reconhecido ou desconhecido
        let isReconhecido = true;
        if (det.classe_detectada === "outros") {
            isReconhecido = false;
        }

        if (isReconhecido) {
            reconhecidosCount++;
        } else {
            naoReconhecidosCount++;
        }

        const corStatus = isReconhecido ? "var(--success)" : "var(--warning)";

        html += `
            <tr>
                <td style="font-weight: 600; color: ${corStatus}">${capitalizeFirstLetter(det.classe_detectada)}</td>
                <td>${sku}</td>
                <td><span class="fps-badge" style="background: rgba(139, 92, 246, 0.15); color: #c4b5fd;">${confianca}</span></td>
                <td>${dimensoes}</td>
                <td>${peso}</td>
            </tr>
        `;
    });

    detectionsTbody.innerHTML = html;
    totalCountBadge.textContent = `${deteccoes.length} ${deteccoes.length === 1 ? 'item' : 'itens'}`;
    sumRecognized.textContent = reconhecidosCount;
    sumUnrecognized.textContent = naoReconhecidosCount;
}

async function finalizarSessaoIA() {
    if (!confirm("Deseja realmente encerrar a sessão de contagem atual?")) {
        return;
    }

    clearInterval(statusPollingInterval);
    btnFinalizar.disabled = true;
    btnFinalizar.textContent = "Finalizando...";

    try {
        const response = await fetch("/finalizar", {
            method: "POST"
        });

        if (!response.ok) {
            throw new Error("Falha ao finalizar sessão no FastAPI");
        }

        const data = await response.json();
        console.log("Sessão finalizada com sucesso:", data);
        alert(`Sessão de contagem finalizada!\nTotal de itens contados: ${data.deteccoes_enviadas}`);

    } catch (err) {
        console.error("Erro ao finalizar sessão:", err);
        alert("Erro ao finalizar sessão no servidor. O estado será resetado localmente.");
    } finally {
        // Limpar estados locais
        currentJwtToken = null;
        currentSessionId = null;
        localStorage.removeItem("web_token");
        
        // Resetar vídeo e UI
        videoStream.src = "";
        videoPlaceholder.style.display = "flex";
        
        btnFinalizar.disabled = false;
        btnFinalizar.innerHTML = `
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect></svg>
            Finalizar Sessão
        `;

        // Voltar para a tela de autenticação por QR
        screenDashboard.classList.remove("active");
        screenAuth.classList.add("active");

        iniciarFluxoAutenticacao();
    }
}

// Utilitários
function capitalizeFirstLetter(string) {
    if (!string) return "";
    return string.charAt(0).toUpperCase() + string.slice(1);
}
