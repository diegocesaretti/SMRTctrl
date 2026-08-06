from __future__ import annotations

import asyncio
from typing import Any

from aiohttp import ClientError, ClientSession


class HomeHubApiError(Exception):
    """Communication error with the Android hub."""


class HomeHubApi:
    def __init__(self, session: ClientSession, host: str, port: int, token: str) -> None:
        self._session = session
        self._base_url = f"http://{host}:{port}"
        self._headers = {"X-Home-Hub-Token": token}

    async def request(
        self,
        method: str,
        path: str,
        data: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        try:
            async with asyncio.timeout(10):
                async with self._session.request(
                    method,
                    f"{self._base_url}{path}",
                    headers=self._headers,
                    data=data,
                ) as response:
                    payload = await response.json(content_type=None)
                    if response.status >= 400:
                        raise HomeHubApiError(
                            payload.get("error", f"HTTP {response.status}")
                        )
                    return payload
        except (TimeoutError, ClientError, ValueError) as error:
            raise HomeHubApiError(str(error)) from error

    async def status(self) -> dict[str, Any]:
        return await self.request("GET", "/api/status")

    async def command(
        self,
        path: str,
        data: dict[str, Any] | None = None,
    ) -> None:
        await self.request("POST", path, data)
