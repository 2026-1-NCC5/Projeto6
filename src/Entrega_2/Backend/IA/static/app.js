// ==========================================================================
// Lógica de Negócio do Dashboard Web - AlimempatIA
// ==========================================================================

const API_BACKEND_URL = "http://localhost:8080";
let socket = null;
let currentSessionId = null;
let currentJwtToken = null;
let statusPollingInterval = null;

// Estado local da interface: "live" | "auditoria" | "idle"
let uiMode = "idle";
let lastDetectionCount = -1; // Controle de re-render incremental
let lastUiModeRendered = "";  // Evita re-render desnecessário ao trocar de modo

// Referências do DOM
const screenAuth = document.getElementById("screen-auth");
const screenDashboard = document.getElementById("screen-dashboard");
const qrcodeBox = document.getElementById("qrcode-box");
const qrLoading = document.getElementById("qr-loading");
const authStatusMsg = document.getElementById("auth-status-msg");
const displaySessionId = document.getElementById("display-session-id");
const dashboardTitle = document.getElementById("dashboard-title");
const liveIndicator = document.getElementById("live-indicator");
const videoStream = document.getElementById("video-stream");
const videoPlaceholder = document.getElementById("video-placeholder");
const videoPausedOverlay = document.getElementById("video-paused-overlay");
const detectionsTbody = document.getElementById("detections-tbody");
const totalCountBadge = document.getElementById("total-count-badge");
const sumRecognized = document.getElementById("sum-recognized");
const sumUnrecognized = document.getElementById("sum-unrecognized");
const btnPausar = document.getElementById("btn-pausar");
const btnFinalizar = document.getElementById("btn-finalizar");
const auditBanner = document.getElementById("audit-banner");
const btnDescartarAuditoria = document.getElementById("btn-descartar-auditoria");
const btnConfirmarEnvio = document.getElementById("btn-confirmar-envio");
const cameraStatusBadge = document.getElementById("camera-status-badge");
const modalDescartar = document.getElementById("modal-descartar");
const modalCancelar = document.getElementById("modal-cancelar");
const modalConfirmarDescartar = document.getElementById("modal-confirmar-descartar");
const colActionsHeaders = document.querySelectorAll(".audit-only");

// ──────────────────────────────────────────────────────────
// Inicialização
// ──────────────────────────────────────────────────────────
document.addEventListener("click", (e) => {
    console.log("[DEBUG GLOBAL] Clique no elemento:", e.target.tagName, "ID:", e.target.id, "Class:", e.target.className);
});

document.addEventListener("DOMContentLoaded", () => {
    console.log("[DEBUG] DOMContentLoaded: anexando ouvintes de eventos.");
    iniciarFluxoAutenticacao();

    btnPausar.addEventListener("click", pausarParaAuditoria);
    btnFinalizar.addEventListener("click", () => abrirModalDescartar());
    btnDescartarAuditoria.addEventListener("click", () => abrirModalDescartar());
    btnConfirmarEnvio.addEventListener("click", confirmarEEnviarLote);

    // Modal de descarte
    modalCancelar.addEventListener("click", fecharModalDescartar);
    modalConfirmarDescartar.addEventListener("click", descartarSessao);
    modalDescartar.addEventListener("click", (e) => {
        if (e.target === modalDescartar) fecharModalDescartar();
    });
});

// ──────────────────────────────────────────────────────────
// Fluxo 1: Geração de QR Code e Conexão Socket.IO
// ──────────────────────────────────────────────────────────
async function iniciarFluxoAutenticacao() {
    qrcodeBox.innerHTML = "";
    qrLoading.style.display = "flex";
    authStatusMsg.textContent = "Obtendo token temporário com o backend...";

    try {
        const response = await fetch(`${API_BACKEND_URL}/api/qr-auth/generate`, {
            method: "POST",
            headers: { "Content-Type": "application/json" }
        });

        if (!response.ok) {
            throw new Error(`Erro ao gerar QR Token: ${response.status}`);
        }

        const data = await response.json();
        const sessionId = data.session_id;
        currentSessionId = sessionId;

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

    socket.on("auth-success", async (data) => {
        console.log("Autenticação autorizada com sucesso via celular!", data);
        currentJwtToken = data.token;
        localStorage.setItem("web_token", currentJwtToken);

        authStatusMsg.textContent = "Acesso concedido! Iniciando sistema de visão computacional...";
        authStatusMsg.style.color = "#10b981";

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
        const response = await fetch("/iniciar-jwt", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ token: jwtToken })
        });

        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.detail || "Erro desconhecido");
        }

        const data = await response.json();
        console.log("Sessão de contagem de IA iniciada:", data);

        // Transição de tela e modo LIVE
        screenAuth.classList.remove("active");
        screenDashboard.classList.add("active");
        displaySessionId.textContent = `#${data.id_sessao}`;

        ativarModoLive();

    } catch (err) {
        console.error("Falha ao iniciar sessão de IA no FastAPI:", err);
        alert(`Não foi possível iniciar a câmera/modelo de IA: ${err.message}`);
        iniciarFluxoAutenticacao();
    }
}

// ──────────────────────────────────────────────────────────
// Máquina de estados de UI
// ──────────────────────────────────────────────────────────

function ativarModoLive() {
    uiMode = "live";

    // Cabeçalho
    dashboardTitle.textContent = "Painel de Contagem Ativo";
    liveIndicator.textContent = "● LIVE";
    liveIndicator.className = "live-indicator";

    // Botões
    btnPausar.style.display = "inline-flex";
    btnFinalizar.style.display = "inline-flex";
    btnFinalizar.innerHTML = `
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect></svg>
        Descartar Sessão
    `;
    btnFinalizar.disabled = false;

    // Câmera
    videoStream.src = "/video_feed";
    videoPlaceholder.style.display = "none";
    videoPausedOverlay.style.display = "none";
    cameraStatusBadge.textContent = "Detectando";
    cameraStatusBadge.className = "fps-badge";

    // Banner de auditoria
    auditBanner.style.display = "none";

    // Colunas de ação
    colActionsHeaders.forEach(el => el.style.display = "none");

    // Polling de estado
    iniciarStatusPolling();
}

function ativarModoAuditoria(deteccoesPendentes) {
    uiMode = "auditoria";

    // Para o polling e o vídeo
    clearInterval(statusPollingInterval);
    videoStream.src = "";
    videoPlaceholder.style.display = "none";
    videoPausedOverlay.style.display = "flex";

    // Cabeçalho
    dashboardTitle.textContent = "Modo de Auditoria";
    liveIndicator.textContent = "⏸ PAUSADO";
    liveIndicator.className = "live-indicator audit-mode";
    cameraStatusBadge.textContent = "Câmera Pausada";
    cameraStatusBadge.className = "fps-badge badge-paused";

    // Botões do header — esconde o pausar, deixa o descartar
    btnPausar.style.display = "none";

    // Banner de auditoria
    auditBanner.style.display = "flex";

    // Colunas de ação — exibe a coluna "Ação"
    colActionsHeaders.forEach(el => el.style.display = "");

    // Recarrega a tabela no modo de auditoria para mostrar imagens e botões
    buscarEstadoIA();
}

function voltarParaAuth() {
    uiMode = "idle";
    clearInterval(statusPollingInterval);

    currentJwtToken = null;
    currentSessionId = null;
    localStorage.removeItem("web_token");

    videoStream.src = "";
    videoPlaceholder.style.display = "flex";
    videoPausedOverlay.style.display = "none";
    detectionsTbody.innerHTML = `<tr class="empty-row"><td colspan="7">Nenhum item detectado na linha de contagem até o momento.</td></tr>`;
    totalCountBadge.textContent = "0 itens";
    sumRecognized.textContent = "0";
    sumUnrecognized.textContent = "0";
    auditBanner.style.display = "none";
    colActionsHeaders.forEach(el => el.style.display = "none");
    btnPausar.style.display = "inline-flex";
    btnPausar.disabled = false;
    btnFinalizar.disabled = false;

    // Reseta rastreadores para próxima sessão
    lastDetectionCount = -1;
    lastUiModeRendered = "";

    screenDashboard.classList.remove("active");
    screenAuth.classList.add("active");

    iniciarFluxoAutenticacao();
}

// ──────────────────────────────────────────────────────────
// Ações do operador
// ──────────────────────────────────────────────────────────

async function pausarParaAuditoria() {
    console.log("[DEBUG] Função pausarParaAuditoria() ativada pelo clique!");
    btnPausar.disabled = true;
    btnPausar.textContent = "Pausando...";

    try {
        console.log("[DEBUG] Iniciando fetch POST /pausar...");
        const response = await fetch("/pausar", { method: "POST" });
        console.log("[DEBUG] fetch concluído. Status HTTP:", response.status);
        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.detail || "Erro ao pausar");
        }
        const data = await response.json();
        console.log("Câmera pausada. Entrando em modo de auditoria:", data);
        ativarModoAuditoria(data.deteccoes_pendentes);
    } catch (err) {
        console.error("Erro ao pausar para auditoria:", err);
        alert(`Não foi possível pausar a câmera: ${err.message}`);
        btnPausar.disabled = false;
        btnPausar.innerHTML = `
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>
            Pausar para Auditoria
        `;
    }
}

async function removerDeteccao(indice) {
    try {
        const response = await fetch(`/deteccoes/${indice}`, { method: "DELETE" });
        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.detail || "Erro ao remover detecção");
        }
        const data = await response.json();
        console.log(`Detecção ${indice} removida. Restam: ${data.deteccoes_restantes}`);
        // Força re-render imediato ao alterar o count manualmente
        lastDetectionCount = -1;
        await buscarEstadoIA();
    } catch (err) {
        console.error(`Erro ao remover detecção ${indice}:`, err);
        alert(`Não foi possível remover o item: ${err.message}`);
    }
}

async function confirmarEEnviarLote() {
    btnConfirmarEnvio.disabled = true;
    btnConfirmarEnvio.textContent = "Enviando...";

    try {
        const response = await fetch("/enviar_auditoria", { method: "POST" });
        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.detail || "Erro ao enviar lote");
        }
        const data = await response.json();
        console.log("Lote auditado enviado com sucesso:", data);
        alert(`✅ Lote enviado com sucesso!\nTotal de itens confirmados: ${data.deteccoes_enviadas}`);
        voltarParaAuth();
    } catch (err) {
        console.error("Erro ao enviar lote auditado:", err);
        alert(`Erro ao enviar o lote: ${err.message}`);
        btnConfirmarEnvio.disabled = false;
        btnConfirmarEnvio.innerHTML = `
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            Confirmar e Enviar
        `;
    }
}

function abrirModalDescartar() {
    modalDescartar.style.display = "flex";
}

function fecharModalDescartar() {
    modalDescartar.style.display = "none";
}

async function descartarSessao() {
    fecharModalDescartar();
    btnFinalizar.disabled = true;

    try {
        const response = await fetch("/finalizar", { method: "POST" });
        if (!response.ok) {
            console.warn("Erro ao finalizar sessão via /finalizar. Resetando localmente.");
        }
        const data = await response.json().catch(() => ({}));
        console.log("Sessão descartada:", data);
    } catch (err) {
        console.warn("Erro de rede ao descartar sessão:", err);
    } finally {
        voltarParaAuth();
    }
}

// ──────────────────────────────────────────────────────────
// Polling e Renderização
// ──────────────────────────────────────────────────────────

function iniciarStatusPolling() {
    if (statusPollingInterval) {
        clearInterval(statusPollingInterval);
    }
    statusPollingInterval = setInterval(buscarEstadoIA, 500);
}

async function buscarEstadoIA() {
    try {
        const response = await fetch("/estado");
        if (!response.ok) return;
        const data = await response.json();
        const newCount = (data.deteccoes || []).length;

        // Só re-renderiza quando há itens novos ou o modo de UI mudou
        const modoMudou = uiMode !== lastUiModeRendered;
        const itemNovo  = newCount !== lastDetectionCount;

        if (itemNovo || modoMudou) {
            lastDetectionCount = newCount;
            lastUiModeRendered = uiMode;
            atualizarConsoleTabela(data.deteccoes || [], data.status);
        }

    } catch (err) {
        console.warn("Erro ao buscar estado da IA:", err);
    }
}

function atualizarConsoleTabela(deteccoes, status) {
    const modoAuditoria = (uiMode === "auditoria");

    if (deteccoes.length === 0) {
        const colspan = modoAuditoria ? 7 : 6;
        detectionsTbody.innerHTML = `
            <tr class="empty-row">
                <td colspan="${colspan}">Nenhum item detectado na linha de contagem até o momento.</td>
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

    // Mais recente no topo
    [...deteccoes].reverse().forEach(det => {
        const confianca = det.confianca ? `${det.confianca}%` : "N/A";
        const sku = det.sku || `<span style="color: var(--text-muted)">Sem SKU</span>`;
        const dimensoes = (det.largura_cm && det.altura_cm) ? `${det.largura_cm}×${det.altura_cm} cm` : "N/A";
        const peso = det.peso_kg ? `${det.peso_kg} kg` : "N/A";

        const isReconhecido = det.classe_detectada !== "outros";
        if (isReconhecido) reconhecidosCount++;
        else naoReconhecidosCount++;

        const corStatus = isReconhecido ? "var(--success)" : "var(--warning)";

        // Thumbnail do frame
        let frameCel = `<td class="col-frame"><span class="no-frame">—</span></td>`;
        if (det.tem_frame) {
            frameCel = `
                <td class="col-frame">
                    <img
                        class="frame-thumb"
                        src="/deteccoes/${det.indice}/frame"
                        alt="Frame da detecção ${det.indice}"
                        loading="lazy"
                    >
                </td>
            `;
        }

        // Botão de remover (somente em modo de auditoria)
        const acaoCell = modoAuditoria
            ? `<td class="col-actions audit-only">
                    <button
                        class="btn-remove"
                        data-indice="${det.indice}"
                        title="Remover este item"
                        onclick="removerDeteccao(${det.indice})"
                    >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    </button>
               </td>`
            : `<td class="col-actions audit-only" style="display:none;"></td>`;

        html += `
            <tr>
                ${frameCel}
                <td style="font-weight: 600; color: ${corStatus}">${capitalizeFirstLetter(det.classe_detectada)}</td>
                <td>${sku}</td>
                <td><span class="fps-badge" style="background: rgba(139, 92, 246, 0.15); color: #c4b5fd;">${confianca}</span></td>
                <td>${dimensoes}</td>
                <td>${peso}</td>
                ${acaoCell}
            </tr>
        `;
    });

    detectionsTbody.innerHTML = html;
    totalCountBadge.textContent = `${deteccoes.length} ${deteccoes.length === 1 ? 'item' : 'itens'}`;
    sumRecognized.textContent = reconhecidosCount;
    sumUnrecognized.textContent = naoReconhecidosCount;
}

// Utilitários
function capitalizeFirstLetter(string) {
    if (!string) return "";
    return string.charAt(0).toUpperCase() + string.slice(1);
}
