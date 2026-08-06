from __future__ import annotations

from typing import Any

import voluptuous as vol

from homeassistant import config_entries
from homeassistant.const import CONF_HOST, CONF_NAME, CONF_PORT
from homeassistant.core import HomeAssistant
from homeassistant.helpers.aiohttp_client import async_get_clientsession

from .api import HomeHubApi, HomeHubApiError
from .const import CONF_TOKEN, DEFAULT_PORT, DOMAIN


async def validate_input(hass: HomeAssistant, data: dict[str, Any]) -> dict[str, Any]:
    api = HomeHubApi(
        async_get_clientsession(hass),
        data[CONF_HOST],
        data[CONF_PORT],
        data[CONF_TOKEN],
    )
    return await api.status()


class HomeHubLegacyConfigFlow(config_entries.ConfigFlow, domain=DOMAIN):
    VERSION = 1

    async def async_step_user(self, user_input: dict[str, Any] | None = None):
        errors: dict[str, str] = {}

        if user_input is not None:
            try:
                status = await validate_input(self.hass, user_input)
            except HomeHubApiError:
                errors["base"] = "cannot_connect"
            else:
                await self.async_set_unique_id(status["device_id"])
                self._abort_if_unique_id_configured()
                return self.async_create_entry(
                    title=user_input.get(CONF_NAME)
                    or status.get("device_name", "Home Hub"),
                    data={
                        CONF_HOST: user_input[CONF_HOST],
                        CONF_PORT: user_input[CONF_PORT],
                        CONF_TOKEN: user_input[CONF_TOKEN],
                    },
                )

        schema = vol.Schema(
            {
                vol.Required(CONF_HOST): str,
                vol.Required(CONF_PORT, default=DEFAULT_PORT): int,
                vol.Required(CONF_TOKEN): str,
                vol.Optional(CONF_NAME): str,
            }
        )
        return self.async_show_form(step_id="user", data_schema=schema, errors=errors)
