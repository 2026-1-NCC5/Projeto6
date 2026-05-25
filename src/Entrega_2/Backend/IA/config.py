"""
config.py — Configurações centrais do microserviço AlimempatIA

Contém:
- Mapeamento das classes do modelo YOLO para os valores aceitos pela API
- Constantes de comportamento da câmera e da linha virtual de contagem
"""

import os
from dotenv import load_dotenv

load_dotenv()

# ──────────────────────────────────────────────────────────────
# Configurações da API backend (Node.js — localhost:8080)
# ──────────────────────────────────────────────────────────────
API_BASE_URL: str = os.getenv("API_BASE_URL", "http://localhost:8080/api")

# ──────────────────────────────────────────────────────────────
# Configurações do microserviço FastAPI (próprio da IA)
# ──────────────────────────────────────────────────────────────
SERVICO_PORTA: int = int(os.getenv("SERVICO_PORTA", "5001"))
SERVICO_HOST: str = os.getenv("SERVICO_HOST", "0.0.0.0")

# ──────────────────────────────────────────────────────────────
# Configurações do modelo YOLO
# ──────────────────────────────────────────────────────────────
MODELO_PATH: str = os.getenv(
    "MODELO_PATH",
    r"runs\detect\train-2\weights\best.pt"
)

# Confiança mínima para considerar uma detecção válida (0.0 – 1.0)
CONFIANCA_MINIMA: float = float(os.getenv("CONFIANCA_MINIMA", "0.5"))

# Índice da câmera: 0 = webcam padrão, 1 = DroidCam, etc.
CAMERA_INDEX: int = int(os.getenv("CAMERA_INDEX", "1"))

# Processar 1 a cada N frames (reduz carga de CPU/GPU)
FRAMES_SKIP: int = int(os.getenv("FRAMES_SKIP", "3"))

# ──────────────────────────────────────────────────────────────
# Linha virtual de contagem (Line Crossing)
#
# A linha é desenhada horizontalmente no frame a LINE_RATIO da
# altura total (0.5 = centro exato).
# Um objeto é contado UMA vez quando seu centro (cy) cruza a
# linha de um lado para o outro entre dois frames consecutivos.
# ──────────────────────────────────────────────────────────────
LINE_RATIO: float = float(os.getenv("LINE_RATIO", "0.5"))

# ──────────────────────────────────────────────────────────────
# Volumetria
# ──────────────────────────────────────────────────────────────
# Proporção de tamanho para calcular dimensões dos objetos (cm / pixel)
CM_POR_PIXEL: float = float(os.getenv("CM_POR_PIXEL", "0.1"))

# ──────────────────────────────────────────────────────────────
# Mapeamento: índice da classe YOLO → dados para a API
#
# Índices conforme dataset_yolo/dataset.yaml:
#   0 = Arroz
#   1 = Feijao
#   2 = Acucar
#   3 = Macarrao
#   4 = Fuba
#   5 = Oleo
#
# A coluna classe_detectada na API aceita (ENUM):
#   arroz | feijao | macarrao | oleo | leite | acucar | fuba | outros
# ──────────────────────────────────────────────────────────────
CLASSE_MAP: dict = {
    0: {
        "classe_base": "arroz",
        "variantes": [
            {"peso_kg": 1.0, "sku": "ARR-001", "classe_api": "arroz"},
            {"peso_kg": 5.0, "sku": "ARR-005", "classe_api": "arroz"},
            {"peso_kg": 10.0, "sku": "ARR-010", "classe_api": "arroz"}
        ]
    },
    1: {
        "classe_base": "feijao",
        "variantes": [
            {"peso_kg": 1.0, "sku": "FEI-001", "classe_api": "feijao"},
            {"peso_kg": 5.0, "sku": "FEI-005", "classe_api": "feijao"}
        ]
    },
    2: {
        "classe_base": "acucar",
        "variantes": [
            {"peso_kg": 1.0, "sku": "ACU-001", "classe_api": "acucar"},
            {"peso_kg": 5.0, "sku": "ACU-005", "classe_api": "acucar"}
        ]
    },
    3: {
        "classe_base": "macarrao",
        "variantes": [
            {"peso_kg": 0.5, "sku": "MAC-500", "classe_api": "macarrao"}
        ]
    },
    4: {
        "classe_base": "fuba",
        "variantes": [
            {"peso_kg": 0.5, "sku": "FUB-500", "classe_api": "fuba"}
        ]
    },
    5: {
        "classe_base": "oleo",
        "variantes": [
            {"peso_kg": 0.9, "sku": "OLE-900", "classe_api": "oleo"}
        ]
    },
}
