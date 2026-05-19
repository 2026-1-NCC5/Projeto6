"""
api_client.py — Cliente HTTP para o backend AlimempatIA (Node.js / localhost:8080)

Encapsula todo o ciclo de vida de uma sessão de contagem:
  1. login()              → POST /api/auth/login
  2. iniciar_sessao()     → POST /api/sessoes
  3. registrar_lote()     → POST /api/sessoes/:id/deteccoes/lote
  4. finalizar_sessao()   → POST /api/sessoes/:id/finalizar
  5. listar_deteccoes()   → GET  /api/sessoes/:id/deteccoes
"""

import logging
import requests
from requests.exceptions import RequestException

from config import API_BASE_URL

logger = logging.getLogger(__name__)


class AlimempatIAClient:
    """
    Cliente HTTP que gerencia autenticação e comunicação com o
    backend Node.js do AlimempatIA.
    """

    def __init__(self) -> None:
        self._token: str | None = None
        self._id_sessao: int | None = None

    # ──────────────────────────────────────────────────────────
    # Propriedades de estado
    # ──────────────────────────────────────────────────────────

    @property
    def sessao_ativa(self) -> bool:
        return self._id_sessao is not None

    @property
    def id_sessao(self) -> int | None:
        return self._id_sessao

    # ──────────────────────────────────────────────────────────
    # Autenticação
    # ──────────────────────────────────────────────────────────

    def login(self, username: str, password: str) -> dict:
        """
        Autentica o usuário no backend e armazena o JWT internamente.

        Returns:
            Payload de resposta do backend (contém 'token' e 'user').
        Raises:
            ValueError: se as credenciais forem rejeitadas (401/403).
            RuntimeError: em caso de falha de conexão.
        """
        url = f"{API_BASE_URL}/auth/login"
        try:
            resp = requests.post(
                url,
                json={"username": username, "password": password},
                timeout=10,
            )
        except RequestException as exc:
            raise RuntimeError(f"Falha de conexão com o backend: {exc}") from exc

        if resp.status_code in (401, 403):
            raise ValueError(f"Credenciais inválidas ou não autorizadas: {resp.text}")
        if not resp.ok:
            raise RuntimeError(f"Erro inesperado no login [{resp.status_code}]: {resp.text}")

        data = resp.json()
        self._token = data["token"]
        logger.info("[AlimempatIAClient] Login bem-sucedido. Usuário: %s", data["user"]["username"])
        return data

    # ──────────────────────────────────────────────────────────
    # Gerenciamento de sessão
    # ──────────────────────────────────────────────────────────

    def iniciar_sessao(self) -> dict:
        """
        Cria uma nova sessão de contagem no backend.

        Returns:
            Payload de resposta (contém 'id_sessao').
        Raises:
            RuntimeError: se não houver token ou ocorrer erro HTTP.
        """
        self._garantir_autenticado()
        url = f"{API_BASE_URL}/sessoes"
        try:
            resp = requests.post(url, headers=self._headers(), timeout=10)
        except RequestException as exc:
            raise RuntimeError(f"Falha ao iniciar sessão: {exc}") from exc

        if not resp.ok:
            raise RuntimeError(f"Erro ao iniciar sessão [{resp.status_code}]: {resp.text}")

        data = resp.json()
        self._id_sessao = data["id_sessao"]
        logger.info("[AlimempatIAClient] Sessão iniciada: id_sessao=%d", self._id_sessao)
        return data

    def registrar_lote(self, deteccoes: list[dict], finalizar: bool = False) -> dict:
        """
        Envia um lote de detecções para a sessão ativa.

        Args:
            deteccoes: lista de dicts com 'classe_detectada', 'confianca' e 'sku' (opcional).
            finalizar: se True, encerra a sessão após inserir o lote.

        Returns:
            Payload de resposta com resumo do lote.
        Raises:
            RuntimeError: se não houver sessão ativa ou ocorrer erro HTTP.
        """
        self._garantir_sessao_ativa()
        url = f"{API_BASE_URL}/sessoes/{self._id_sessao}/deteccoes/lote"
        payload = {"deteccoes": deteccoes, "finalizar_sessao": finalizar}
        try:
            resp = requests.post(url, json=payload, headers=self._headers(), timeout=15)
        except RequestException as exc:
            raise RuntimeError(f"Falha ao enviar lote de detecções: {exc}") from exc

        if not resp.ok:
            raise RuntimeError(f"Erro ao registrar lote [{resp.status_code}]: {resp.text}")

        data = resp.json()
        if finalizar:
            self._id_sessao = None  # Sessão encerrada
        logger.info("[AlimempatIAClient] Lote registrado: %s detecções.", data.get("deteccoes_inseridas"))
        return data

    def finalizar_sessao(self) -> dict:
        """
        Encerra a sessão ativa manualmente (sem envio de lote).

        Returns:
            Payload de resposta do backend.
        Raises:
            RuntimeError: se não houver sessão ativa ou ocorrer erro HTTP.
        """
        self._garantir_sessao_ativa()
        url = f"{API_BASE_URL}/sessoes/{self._id_sessao}/finalizar"
        try:
            resp = requests.post(url, headers=self._headers(), timeout=10)
        except RequestException as exc:
            raise RuntimeError(f"Falha ao finalizar sessão: {exc}") from exc

        if not resp.ok:
            raise RuntimeError(f"Erro ao finalizar sessão [{resp.status_code}]: {resp.text}")

        data = resp.json()
        logger.info("[AlimempatIAClient] Sessão finalizada: id_sessao=%d", self._id_sessao)
        self._id_sessao = None
        return data

    def listar_deteccoes(self, id_sessao: int | None = None) -> dict:
        """
        Consulta as detecções de uma sessão.

        Args:
            id_sessao: ID da sessão. Se None, usa a sessão atual.

        Returns:
            Payload com resumo e lista de detecções.
        """
        sid = id_sessao or self._id_sessao
        if sid is None:
            raise RuntimeError("Nenhuma sessão especificada ou ativa.")
        url = f"{API_BASE_URL}/sessoes/{sid}/deteccoes"
        try:
            resp = requests.get(url, headers=self._headers(), timeout=10)
        except RequestException as exc:
            raise RuntimeError(f"Falha ao consultar detecções: {exc}") from exc

        if not resp.ok:
            raise RuntimeError(f"Erro ao listar detecções [{resp.status_code}]: {resp.text}")

        return resp.json()

    # ──────────────────────────────────────────────────────────
    # Utilitários internos
    # ──────────────────────────────────────────────────────────

    def _headers(self) -> dict:
        return {"Authorization": f"Bearer {self._token}"}

    def _garantir_autenticado(self) -> None:
        if not self._token:
            raise RuntimeError("Cliente não autenticado. Chame login() primeiro.")

    def _garantir_sessao_ativa(self) -> None:
        self._garantir_autenticado()
        if self._id_sessao is None:
            raise RuntimeError("Nenhuma sessão ativa. Chame iniciar_sessao() primeiro.")
