"""
servico_ia.py — Microserviço FastAPI da AlimempatIA

Expõe endpoints HTTP para controlar o ciclo de vida da detecção YOLO
integrada ao backend AlimempatIA (Node.js / localhost:8080).

Endpoints:
  POST /iniciar    → Recebe credenciais, autentica no backend, inicia sessão
                     e dispara a câmera + detecção em background.
  POST /finalizar  → Para a detecção, envia o lote acumulado e encerra a sessão.
  GET  /status     → Retorna o estado atual do serviço (idle / ativa).

Line Crossing:
  Uma linha horizontal virtual é desenhada a LINE_RATIO da altura do frame.
  Cada objeto rastreado (via YOLO tracking) é contado UMA única vez quando seu
  centro (cy) cruza essa linha entre dois frames consecutivos.
  Isso elimina a necessidade de deduplicação por tempo e garante que um produto
  só seja contado uma vez por passagem física pela linha.

Para iniciar:
  uvicorn servico_ia:app --host 0.0.0.0 --port 5001
  (ou configure via variáveis de ambiente em .env)
"""

import logging
import threading
import time
from contextlib import asynccontextmanager
from typing import Any

import cv2
import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from ultralytics import YOLO

from api_client import AlimempatIAClient
from config import (
    CAMERA_INDEX,
    CLASSE_MAP,
    CONFIANCA_MINIMA,
    FRAMES_SKIP,
    LINE_RATIO,
    MODELO_PATH,
    SERVICO_HOST,
    SERVICO_PORTA,
)

# ──────────────────────────────────────────────────────────────
# Logging
# ──────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("servico_ia")

# ──────────────────────────────────────────────────────────────
# Estado global do serviço (compartilhado entre threads)
# ──────────────────────────────────────────────────────────────
_state: dict[str, Any] = {
    "status": "idle",          # "idle" | "ativa"
    "id_sessao": None,
    "deteccoes": [],           # lista de dicts acumulados
    "cliente": None,           # instância de AlimempatIAClient
    "thread": None,            # thread da câmera
    "parar_evento": None,      # threading.Event para sinalizar parada
    "lock": threading.Lock(),  # protege deteccoes e status
}


# ──────────────────────────────────────────────────────────────
# Schemas Pydantic
# ──────────────────────────────────────────────────────────────
class IniciarRequest(BaseModel):
    username: str
    password: str


class FinalizarRequest(BaseModel):
    """Corpo opcional ao finalizar (pode ser enviado vazio)."""
    pass


# ──────────────────────────────────────────────────────────────
# FastAPI — lifecycle
# ──────────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Microserviço AlimempatIA iniciado. Aguardando POST /iniciar...")
    yield
    # Encerramento seguro: para câmera se ainda ativa
    if _state["status"] == "ativa" and _state["parar_evento"]:
        logger.info("Encerrando câmera por desligamento do serviço...")
        _state["parar_evento"].set()
        if _state["thread"]:
            _state["thread"].join(timeout=5)


app = FastAPI(
    title="AlimempatIA — Microserviço de Detecção",
    version="1.0.0",
    lifespan=lifespan,
)


# ──────────────────────────────────────────────────────────────
# Thread de câmera + YOLO + Line Crossing
# ──────────────────────────────────────────────────────────────
def _thread_deteccao(modelo_path: str, parar_evento: threading.Event) -> None:
    """
    Roda em background: captura frames da câmera, executa inferência
    YOLO com tracking e aplica a lógica de Line Crossing para acumular
    detecções sem duplicatas.

    A thread lê e escreve em _state["deteccoes"] com proteção por lock.
    Encerra quando parar_evento é sinalizado.
    """
    logger.info("[Thread] Carregando modelo YOLO: %s", modelo_path)
    try:
        model = YOLO(modelo_path)
    except Exception as exc:
        logger.error("[Thread] Falha ao carregar modelo: %s", exc)
        with _state["lock"]:
            _state["status"] = "idle"
        return

    logger.info("[Thread] Abrindo câmera (índice %d)...", CAMERA_INDEX)
    cap = cv2.VideoCapture(CAMERA_INDEX)
    cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)

    if not cap.isOpened():
        logger.error("[Thread] Não foi possível abrir a câmera.")
        with _state["lock"]:
            _state["status"] = "idle"
        cap.release()
        return

    logger.info("[Thread] Câmera pronta. Detecção em andamento...")

    # ── Line Crossing ──────────────────────────────────────────
    # Rastreia, para cada track_id, em qual lado da linha o objeto
    # estava no frame anterior: True = acima, False = abaixo, None = desconhecido
    posicao_anterior: dict[int, bool | None] = {}

    contador_frames = 0
    frame_anotado = None

    while not parar_evento.is_set():
        ret, frame = cap.read()
        if not ret:
            logger.warning("[Thread] Falha na leitura do frame. Tentando novamente...")
            time.sleep(0.05)
            continue

        contador_frames += 1

        # Processa 1 a cada FRAMES_SKIP frames
        if contador_frames % FRAMES_SKIP != 0:
            if frame_anotado is not None:
                cv2.imshow("AlimempatIA — Deteccao", frame_anotado)
            if cv2.waitKey(1) & 0xFF == ord("q"):
                logger.info("[Thread] Tecla 'q' pressionada pelo operador.")
                parar_evento.set()
                break
            continue

        altura, largura = frame.shape[:2]
        linha_y = int(altura * LINE_RATIO)

        # Inferência com tracking persistente (botsort ou bytetrack)
        resultados = model.track(
            frame,
            persist=True,
            stream=True,
            verbose=False,
            conf=CONFIANCA_MINIMA,
        )

        for resultado in resultados:
            frame_anotado = resultado.plot()

            # Desenha a linha virtual
            cv2.line(frame_anotado, (0, linha_y), (largura, linha_y), (0, 255, 255), 2)
            cv2.putText(
                frame_anotado,
                "Linha de Contagem",
                (10, linha_y - 8),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.55,
                (0, 255, 255),
                1,
            )

            if resultado.boxes is None or resultado.boxes.id is None:
                continue

            boxes = resultado.boxes
            for i, track_id_tensor in enumerate(boxes.id):
                track_id = int(track_id_tensor.item())
                classe_idx = int(boxes.cls[i].item())
                confianca = float(boxes.conf[i].item())

                if classe_idx not in CLASSE_MAP:
                    continue  # Classe desconhecida — ignora

                # Centro Y da bounding box
                cy = float(boxes.xyxy[i][1].item() + boxes.xyxy[i][3].item()) / 2.0
                acima = cy < linha_y

                prev = posicao_anterior.get(track_id)

                if prev is not None and prev != acima:
                    # ── Cruzamento detectado! ──────────────────
                    mapeamento = CLASSE_MAP[classe_idx]
                    deteccao = {
                        "classe_detectada": mapeamento["classe_api"],
                        "confianca": round(confianca * 100, 2),
                    }
                    if mapeamento["sku"]:
                        deteccao["sku"] = mapeamento["sku"]

                    with _state["lock"]:
                        _state["deteccoes"].append(deteccao)

                    logger.info(
                        "[LineCrossing] track_id=%d | classe=%s | confiança=%.1f%%",
                        track_id,
                        mapeamento["classe_api"],
                        confianca * 100,
                    )

                posicao_anterior[track_id] = acima

        # Exibe frame na tela do operador
        if frame_anotado is not None:
            cv2.imshow("AlimempatIA — Deteccao", frame_anotado)

        if cv2.waitKey(1) & 0xFF == ord("q"):
            logger.info("[Thread] Tecla 'q' pressionada pelo operador.")
            parar_evento.set()
            break

    cap.release()
    cv2.destroyAllWindows()
    logger.info("[Thread] Câmera encerrada.")


# ──────────────────────────────────────────────────────────────
# Endpoints
# ──────────────────────────────────────────────────────────────

@app.post("/iniciar", summary="Iniciar sessão de contagem")
async def iniciar(body: IniciarRequest):
    """
    Recebe as credenciais do usuário, autentica no backend Node.js,
    abre uma sessão de contagem e dispara a câmera + YOLO em background.

    Body:
        username (str): Nome de usuário cadastrado no sistema.
        password (str): Senha do usuário.

    Returns:
        JSON com id_sessao e status 'ativa'.
    """
    with _state["lock"]:
        if _state["status"] == "ativa":
            raise HTTPException(
                status_code=409,
                detail="Já existe uma sessão de contagem ativa. Finalize-a antes de iniciar uma nova.",
            )

    # 1. Autenticar no backend
    cliente = AlimempatIAClient()
    try:
        login_data = cliente.login(body.username, body.password)
    except ValueError as exc:
        raise HTTPException(status_code=401, detail=str(exc))
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc))

    # 2. Iniciar sessão no backend
    try:
        sessao_data = cliente.iniciar_sessao()
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc))

    # 3. Preparar evento de parada e thread da câmera
    parar_evento = threading.Event()
    thread = threading.Thread(
        target=_thread_deteccao,
        args=(MODELO_PATH, parar_evento),
        daemon=True,
        name="thread-deteccao",
    )

    # 4. Atualizar estado global
    with _state["lock"]:
        _state["status"] = "ativa"
        _state["id_sessao"] = sessao_data["id_sessao"]
        _state["deteccoes"] = []
        _state["cliente"] = cliente
        _state["thread"] = thread
        _state["parar_evento"] = parar_evento

    thread.start()

    logger.info(
        "Sessão iniciada pelo usuário '%s'. id_sessao=%d",
        login_data["user"]["username"],
        sessao_data["id_sessao"],
    )

    return JSONResponse(
        status_code=201,
        content={
            "message": "Sessão de contagem iniciada. Câmera ativa.",
            "id_sessao": sessao_data["id_sessao"],
            "usuario": login_data["user"]["username"],
            "status": "ativa",
        },
    )


@app.post("/finalizar", summary="Finalizar sessão e enviar detecções")
async def finalizar(_body: FinalizarRequest = None):
    """
    Para a câmera, coleta todas as detecções acumuladas e as envia
    em lote ao backend, encerrando a sessão.

    Returns:
        JSON com resumo do lote (total, reconhecidos, não reconhecidos).
    """
    with _state["lock"]:
        if _state["status"] != "ativa":
            raise HTTPException(
                status_code=409,
                detail="Nenhuma sessão ativa no momento.",
            )
        # Sinaliza parada e coleta snapshot das detecções
        _state["parar_evento"].set()
        deteccoes_snapshot = list(_state["deteccoes"])
        cliente: AlimempatIAClient = _state["cliente"]

    # Aguarda a thread encerrar (máx. 5s)
    if _state["thread"]:
        _state["thread"].join(timeout=5)

    # Envia o lote ao backend (finaliza sessão automaticamente)
    try:
        if deteccoes_snapshot:
            resultado = cliente.registrar_lote(deteccoes_snapshot, finalizar=True)
        else:
            # Nenhuma detecção — apenas finaliza a sessão
            resultado = cliente.finalizar_sessao()
            resultado["deteccoes_inseridas"] = 0
            resultado["resumo"] = {"total": 0, "reconhecidos": 0, "nao_reconhecidos": 0}
    except RuntimeError as exc:
        # Reseta estado mesmo em erro para permitir nova sessão
        with _state["lock"]:
            _state["status"] = "idle"
            _state["id_sessao"] = None
            _state["deteccoes"] = []
            _state["cliente"] = None
        raise HTTPException(status_code=503, detail=str(exc))

    # Reseta estado
    with _state["lock"]:
        _state["status"] = "idle"
        _state["id_sessao"] = None
        _state["deteccoes"] = []
        _state["cliente"] = None
        _state["thread"] = None
        _state["parar_evento"] = None

    logger.info(
        "Sessão finalizada. Detecções enviadas: %d",
        resultado.get("deteccoes_inseridas", 0),
    )

    return JSONResponse(
        status_code=200,
        content={
            "message": "Sessão finalizada com sucesso.",
            "deteccoes_enviadas": resultado.get("deteccoes_inseridas", 0),
            "resumo": resultado.get("resumo", {}),
            "sessao_finalizada": True,
        },
    )


@app.get("/status", summary="Consultar estado do serviço")
async def status():
    """
    Retorna o estado atual do microserviço.

    Returns:
        JSON com status ('idle' ou 'ativa'), id_sessao e total de
        detecções acumuladas até o momento.
    """
    with _state["lock"]:
        return {
            "status": _state["status"],
            "id_sessao": _state["id_sessao"],
            "deteccoes_acumuladas": len(_state["deteccoes"]),
        }


# ──────────────────────────────────────────────────────────────
# Inicialização direta (python servico_ia.py)
# ──────────────────────────────────────────────────────────────
if __name__ == "__main__":
    uvicorn.run(
        "servico_ia:app",
        host=SERVICO_HOST,
        port=SERVICO_PORTA,
        reload=False,
    )
