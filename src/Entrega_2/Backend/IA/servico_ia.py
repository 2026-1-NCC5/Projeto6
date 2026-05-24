"""
servico_ia.py — Microserviço FastAPI da AlimempatIA

Expõe endpoints HTTP para controlar o ciclo de vida da detecção YOLO
integrada ao backend AlimempatIA (Node.js / localhost:8080).

Endpoints:
  POST /iniciar               → Recebe credenciais, autentica no backend, inicia sessão
                                e dispara a câmera + detecção em background.
  POST /pausar                → Para a câmera e entra em modo de auditoria.
  DELETE /deteccoes/{indice}  → Remove uma detecção da memória e seu frame temporário.
  GET  /deteccoes/{indice}/frame → Retorna a imagem do frame de uma detecção.
  POST /enviar_auditoria      → Envia o lote auditado ao backend e encerra a sessão.
  POST /finalizar             → Descarta todas as detecções e encerra a sessão.
  GET  /status                → Retorna o estado atual do serviço (idle / ativa / auditoria).

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

import asyncio
import logging
import os
import tempfile
import threading
import time
from contextlib import asynccontextmanager
from typing import Any

import cv2
import uvicorn
from fastapi import FastAPI, HTTPException, Request, Response
from fastapi.responses import JSONResponse, StreamingResponse, FileResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
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
    CM_POR_PIXEL,
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
    "status": "idle",          # "idle" | "ativa" | "auditoria"
    "id_sessao": None,
    "deteccoes": [],           # lista de dicts acumulados
    "cliente": None,           # instância de AlimempatIAClient
    "thread": None,            # thread da câmera
    "parar_evento": None,      # threading.Event para sinalizar parada
    "frame_atual": None,       # último frame processado em formato bytes JPEG
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


class AtualizarPesoRequest(BaseModel):
    """Corpo para atualizar o peso de uma detecção durante a auditoria."""
    peso_kg: float


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

app.mount("/static", StaticFiles(directory="static"), name="static")
templates = Jinja2Templates(directory="templates")


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
                    
                    # Cálculo da volumetria
                    x1, y1, x2, y2 = boxes.xyxy[i].tolist()
                    largura_px = x2 - x1
                    altura_px = y2 - y1
                    
                    largura_cm = largura_px * CM_POR_PIXEL
                    altura_cm = altura_px * CM_POR_PIXEL
                    
                    # TODO: Definir a lógica de cálculo de peso do alimento a partir 
                    # de largura_cm e altura_cm.
                    # Exemplo temporário:
                    peso_kg = 1.0  # Placeholder (pode ser 1 ou 5 dependendo das dimensões)
                    
                    classe_api_final = mapeamento["classe_api"]
                    if classe_api_final in ["feijao", "acucar", "arroz"]:
                        if peso_kg > 2.5:
                            classe_api_final = f"{classe_api_final}_5kg"
                        else:
                            classe_api_final = f"{classe_api_final}_1kg"

                    deteccao = {
                        "classe_detectada": classe_api_final,
                        "confianca": round(confianca * 100, 2),
                        "largura_cm": round(largura_cm, 2),
                        "altura_cm": round(altura_cm, 2),
                        "peso_kg": peso_kg
                    }
                    if mapeamento["sku"]:
                        deteccao["sku"] = mapeamento["sku"]

                    # Salva o frame anotado completo em arquivo temporário
                    # para ser enviado como imagem ao backend.
                    # O arquivo será deletado pelo api_client após o envio.
                    if frame_anotado is not None:
                        try:
                            fd, frame_path = tempfile.mkstemp(suffix=".jpg", prefix="det_")
                            os.close(fd)  # fecha o descriptor; cv2.imwrite abre por conta
                            cv2.imwrite(frame_path, frame_anotado)
                            deteccao["frame_path"] = frame_path
                        except Exception as exc_img:
                            logger.warning(
                                "[LineCrossing] Falha ao salvar frame temporário: %s", exc_img
                            )

                    with _state["lock"]:
                        _state["deteccoes"].append(deteccao)

                    logger.info(
                        "[LineCrossing] track_id=%d | classe=%s | confiança=%.1f%% | vol=%.1fx%.1fcm",
                        track_id,
                        classe_api_final,
                        confianca * 100,
                        largura_cm,
                        altura_cm
                    )

                posicao_anterior[track_id] = acima

        # Armazena frame na memória para o stream do navegador
        if frame_anotado is not None:
            ret_encode, jpeg = cv2.imencode(".jpg", frame_anotado)
            if ret_encode:
                with _state["lock"]:
                    _state["frame_atual"] = jpeg.tobytes()

    cap.release()
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
        login_data = await asyncio.to_thread(cliente.login, body.username, body.password)
    except ValueError as exc:
        raise HTTPException(status_code=401, detail=str(exc))
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc))

    # 2. Iniciar sessão no backend
    try:
        sessao_data = await asyncio.to_thread(cliente.iniciar_sessao)
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

    # Aguarda a thread encerrar sem bloquear o event loop (máx. 5s)
    thread_ref = _state.get("thread")
    if thread_ref:
        await asyncio.to_thread(thread_ref.join, 5)

    # Envia o lote ao backend (finaliza sessão automaticamente)
    try:
        if deteccoes_snapshot:
            resultado = await asyncio.to_thread(cliente.registrar_lote, deteccoes_snapshot, True)
        else:
            # Nenhuma detecção — apenas finaliza a sessão
            resultado = await asyncio.to_thread(cliente.finalizar_sessao)
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


@app.post("/pausar", summary="Pausar câmera e entrar em modo de auditoria")
async def pausar():
    """
    Para a thread de câmera e muda o status para 'auditoria'.
    As detecções acumuladas ficam na memória para revisão.
    A sessão no backend permanece aberta.

    Returns:
        JSON com número de detecções pendentes de revisão.
    """
    logger.info("[DEBUG] Rota /pausar acessada pelo frontend!")
    with _state["lock"]:
        if _state["status"] != "ativa":
            raise HTTPException(
                status_code=409,
                detail="Nenhuma sessão ativa para pausar.",
            )
        # Sinaliza parada da câmera
        _state["parar_evento"].set()
        deteccoes_pendentes = len(_state["deteccoes"])

    # Aguarda thread da câmera encerrar sem bloquear o event loop (máx. 5s)
    thread_ref = _state.get("thread")
    if thread_ref:
        await asyncio.to_thread(thread_ref.join, 5)

    with _state["lock"]:
        _state["status"] = "auditoria"
        _state["thread"] = None
        _state["frame_atual"] = None

    logger.info("Câmera pausada. Modo de auditoria ativado. %d detecções pendentes.", deteccoes_pendentes)

    return JSONResponse(
        status_code=200,
        content={
            "message": "Câmera pausada. Sistema em modo de auditoria.",
            "status": "auditoria",
            "deteccoes_pendentes": deteccoes_pendentes,
        },
    )


@app.delete("/deteccoes/{indice}", summary="Remover detecção da lista de auditoria")
async def remover_deteccao(indice: int):
    """
    Remove a detecção no índice informado da lista em memória.
    O arquivo de frame temporário associado (frame_path) também é deletado do disco.

    Args:
        indice: posição (0-based) da detecção a ser removida.

    Returns:
        JSON com confirmação e detecções restantes.
    """
    with _state["lock"]:
        if _state["status"] != "auditoria":
            raise HTTPException(
                status_code=409,
                detail="A remoção de detecções só é permitida durante a auditoria.",
            )
        if indice < 0 or indice >= len(_state["deteccoes"]):
            raise HTTPException(
                status_code=404,
                detail=f"Índice {indice} inválido. Total de detecções: {len(_state['deteccoes'])}.",
            )

        deteccao_removida = _state["deteccoes"].pop(indice)
        restantes = len(_state["deteccoes"])

    # Deleta frame temporário do disco (fora do lock para não bloquear)
    frame_path = deteccao_removida.get("frame_path")
    if frame_path and os.path.isfile(frame_path):
        try:
            os.remove(frame_path)
            logger.info("[Auditoria] Frame temporário removido: %s", frame_path)
        except OSError as exc:
            logger.warning("[Auditoria] Falha ao remover frame temporário %s: %s", frame_path, exc)

    logger.info(
        "[Auditoria] Detecção %d removida (%s). Restam %d itens.",
        indice,
        deteccao_removida.get("classe_detectada", "?"),
        restantes,
    )

    return JSONResponse(
        status_code=200,
        content={
            "message": "Detecção removida com sucesso.",
            "indice_removido": indice,
            "classe_removida": deteccao_removida.get("classe_detectada"),
            "deteccoes_restantes": restantes,
        },
    )


@app.patch("/deteccoes/{indice}/peso", summary="Atualizar o peso de uma detecção durante a auditoria")
async def atualizar_peso_deteccao(indice: int, body: AtualizarPesoRequest):
    """
    Atualiza o campo peso_kg de uma detecção específica enquanto o sistema
    estiver em modo de auditoria. Permite ao operador corrigir manualmente
    o peso estimado pelo modelo.

    Args:
        indice: posição (0-based) da detecção a ser editada.
        body: objeto JSON contendo o novo valor de peso_kg (float).

    Returns:
        JSON com confirmação e o novo peso registrado.
    """
    with _state["lock"]:
        if _state["status"] != "auditoria":
            raise HTTPException(
                status_code=409,
                detail="A edição de peso só é permitida durante a auditoria.",
            )
        if indice < 0 or indice >= len(_state["deteccoes"]):
            raise HTTPException(
                status_code=404,
                detail=f"Índice {indice} inválido. Total de detecções: {len(_state['deteccoes'])}.",
            )
        if body.peso_kg <= 0:
            raise HTTPException(
                status_code=422,
                detail="O peso deve ser um valor positivo.",
            )

        _state["deteccoes"][indice]["peso_kg"] = round(body.peso_kg, 3)
        classe = _state["deteccoes"][indice].get("classe_detectada", "?")

    logger.info(
        "[Auditoria] Peso da detecção %d (%s) atualizado para %.3f kg.",
        indice,
        classe,
        body.peso_kg,
    )

    return JSONResponse(
        status_code=200,
        content={
            "message": "Peso atualizado com sucesso.",
            "indice": indice,
            "peso_kg": round(body.peso_kg, 3),
        },
    )


@app.post("/retomar", summary="Retomar a contagem após uma auditoria parcial")
async def retomar_contagem():
    """
    Sai do modo de auditoria e reinicia a câmera + YOLO, mantendo as
    detecções já acumuladas na sessão atual. Permite que o operador
    revise, corrija e depois retorne à esteira para continuar contando.

    Returns:
        JSON confirmando a retomada e o número de detecções já registradas.
    """
    with _state["lock"]:
        if _state["status"] != "auditoria":
            raise HTTPException(
                status_code=409,
                detail="O sistema não está em modo de auditoria.",
            )
        deteccoes_acumuladas = len(_state["deteccoes"])

    # Cria novo evento de parada e nova thread da câmera
    parar_evento = threading.Event()
    thread = threading.Thread(
        target=_thread_deteccao,
        args=(MODELO_PATH, parar_evento),
        daemon=True,
        name="thread-deteccao",
    )

    with _state["lock"]:
        _state["status"] = "ativa"
        _state["parar_evento"] = parar_evento
        _state["thread"] = thread
        _state["frame_atual"] = None

    thread.start()

    logger.info(
        "[Retomar] Câmera reiniciada. Sessão %s continuada com %d detecções acumuladas.",
        _state.get("id_sessao"),
        deteccoes_acumuladas,
    )

    return JSONResponse(
        status_code=200,
        content={
            "message": "Contagem retomada. Câmera ativa novamente.",
            "status": "ativa",
            "deteccoes_acumuladas": deteccoes_acumuladas,
        },
    )


@app.get("/deteccoes/{indice}/frame", summary="Retornar imagem do frame de uma detecção")
async def obter_frame_deteccao(indice: int):
    """
    Serve a imagem JPEG do frame capturado no momento da detecção.
    Usado pelo frontend para exibir o preview durante a auditoria.

    Args:
        indice: posição (0-based) da detecção.

    Returns:
        Imagem JPEG do frame ou 404 se não disponível.
    """
    with _state["lock"]:
        if indice < 0 or indice >= len(_state["deteccoes"]):
            raise HTTPException(status_code=404, detail="Índice de detecção inválido.")
        frame_path = _state["deteccoes"][indice].get("frame_path")

    if not frame_path or not os.path.isfile(frame_path):
        raise HTTPException(status_code=404, detail="Frame não disponível para esta detecção.")

    return FileResponse(frame_path, media_type="image/jpeg")


@app.post("/enviar_auditoria", summary="Confirmar e enviar lote auditado ao backend")
async def enviar_auditoria():
    """
    Envia o lote de detecções que sobrou após a auditoria ao backend Node.js,
    finalizando a sessão de contagem.

    Returns:
        JSON com resumo do envio.
    """
    with _state["lock"]:
        if _state["status"] != "auditoria":
            raise HTTPException(
                status_code=409,
                detail="O envio de auditoria só é permitido no modo de auditoria.",
            )
        deteccoes_snapshot = list(_state["deteccoes"])
        cliente: AlimempatIAClient = _state["cliente"]

    try:
        if deteccoes_snapshot:
            resultado = await asyncio.to_thread(cliente.registrar_lote, deteccoes_snapshot, True)
        else:
            # Nenhuma detecção restante — apenas finaliza a sessão
            resultado = await asyncio.to_thread(cliente.finalizar_sessao)
            resultado["deteccoes_inseridas"] = 0
            resultado["resumo"] = {"total": 0, "reconhecidos": 0, "nao_reconhecidos": 0}
    except RuntimeError as exc:
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
        _state["frame_atual"] = None

    logger.info(
        "[Auditoria] Lote auditado enviado. Detecções confirmadas: %d",
        resultado.get("deteccoes_inseridas", 0),
    )

    return JSONResponse(
        status_code=200,
        content={
            "message": "Lote auditado enviado com sucesso.",
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


class IniciarJWTRequest(BaseModel):
    token: str


@app.post("/iniciar-jwt", summary="Iniciar sessão de contagem via Token JWT direto")
async def iniciar_jwt(body: IniciarJWTRequest):
    """
    Recebe um token JWT pré-existente (obtido via autenticação de QR Code),
    inicia a sessão de contagem no backend Node.js e dispara a câmera.
    """
    with _state["lock"]:
        if _state["status"] == "ativa":
            raise HTTPException(
                status_code=409,
                detail="Já existe uma sessão de contagem ativa. Finalize-a antes de iniciar uma nova.",
            )

    # Instancia o cliente com o token fornecido
    cliente = AlimempatIAClient(token=body.token)
    try:
        sessao_data = await asyncio.to_thread(cliente.iniciar_sessao)
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc))

    # Preparar evento de parada e thread da câmera
    parar_evento = threading.Event()
    thread = threading.Thread(
        target=_thread_deteccao,
        args=(MODELO_PATH, parar_evento),
        daemon=True,
        name="thread-deteccao",
    )

    # Atualizar estado global
    with _state["lock"]:
        _state["status"] = "ativa"
        _state["id_sessao"] = sessao_data["id_sessao"]
        _state["deteccoes"] = []
        _state["cliente"] = cliente
        _state["thread"] = thread
        _state["parar_evento"] = parar_evento
        _state["frame_atual"] = None

    thread.start()

    logger.info(
        "Sessão iniciada via JWT. id_sessao=%d",
        sessao_data["id_sessao"],
    )

    return JSONResponse(
        status_code=201,
        content={
            "message": "Sessão de contagem iniciada. Câmera ativa.",
            "id_sessao": sessao_data["id_sessao"],
            "status": "ativa",
        },
    )


@app.get("/", summary="Renderizar a interface gráfica web")
async def root_page(request: Request):
    """
    Exibe a interface de login por QR Code e Dashboard de Contagem.
    """
    return templates.TemplateResponse("index.html", {"request": request})


async def gerar_frames():
    """
    Gerador contínuo para o streaming de vídeo MJPEG.
    """
    while True:
        with _state["lock"]:
            frame = _state.get("frame_atual")
            status = _state.get("status")

        if status != "ativa":
            # Quando inativo, aguarda um tempo para liberar a CPU
            await asyncio.sleep(0.2)
            continue

        if frame is not None:
            yield (b'--frame\r\n'
                   b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n')
        
        # Limitador de FPS (~25 fps)
        await asyncio.sleep(0.04)


@app.get("/video_feed", summary="Stream de vídeo com as detecções em tempo real")
async def video_feed():
    """
    Fornece o feed MJPEG ao navegador.
    """
    return StreamingResponse(
        gerar_frames(),
        media_type="multipart/x-mixed-replace; boundary=frame"
    )


@app.get("/estado", summary="Consultar estado e detecções acumuladas em tempo real")
async def estado():
    """
    Retorna o estado detalhado do serviço, incluindo as detecções
    acumuladas para preencher o console web em tempo real.
    """
    with _state["lock"]:
        # Injeta o índice de cada detecção e omite frame_path (dado interno de servidor)
        deteccoes_publicas = []
        for i, det in enumerate(_state["deteccoes"]):
            det_publico = {k: v for k, v in det.items() if k != "frame_path"}
            det_publico["indice"] = i
            det_publico["tem_frame"] = bool(det.get("frame_path") and os.path.isfile(det.get("frame_path", "")))
            deteccoes_publicas.append(det_publico)

        return {
            "status": _state["status"],
            "id_sessao": _state["id_sessao"],
            "deteccoes": deteccoes_publicas,
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
