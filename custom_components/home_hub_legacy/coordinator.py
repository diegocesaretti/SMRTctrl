from __future__ import annotations

from typing import Any

from homeassistant.config_entries import ConfigEntry
from homeassistant.const import CONF_HOST, CONF_PORT
from homeassistant.core import HomeAssistant
from homeassistant.helpers.aiohttp_client import async_get_clientsession
from homeassistant.helpers.update_coordinator import DataUpdateCoordinator, UpdateFailed

from .api import HomeHubApi, HomeHubApiError
from .const import CONF_TOKEN, LOGGER, UPDATE_INTERVAL


class HomeHubCoordinator(DataUpdateCoordinator[dict[str, Any]]):
    config_entry: ConfigEntry

    def __init__(self, hass: HomeAssistant, entry: ConfigEntry) -> None:
        self.api = HomeHubApi(
            async_get_clientsession(hass),
            entry.data[CONF_HOST],
            entry.data[CONF_PORT],
            entry.data[CONF_TOKEN],
        )
        super().__init__(
            hass,
            LOGGER,
            config_entry=entry,
            name=entry.title,
            update_interval=UPDATE_INTERVAL,
        )

    async def _async_update_data(self) -> dict[str, Any]:
        try:
            return await self.api.status()
        except HomeHubApiError as error:
            raise UpdateFailed(str(error)) from error
