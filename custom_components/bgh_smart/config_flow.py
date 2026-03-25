import voluptuous as vol
from homeassistant import config_entries
from homeassistant.const import CONF_HOST, CONF_MAC
import re

from .const import DOMAIN

class BGHSmartConfigFlow(config_entries.ConfigFlow, domain=DOMAIN):
    """Handle a config flow for BGH Smart AC."""

    VERSION = 1

    async def async_step_user(self, user_input=None):
        """Handle the initial step."""
        errors = {}
        if user_input is not None:
            host = user_input[CONF_HOST]
            mac = user_input[CONF_MAC].replace(":", "").replace("-", "").lower()
            
            if not re.match(r"^[0-9a-f]{12}$", mac):
                errors[CONF_MAC] = "invalid_mac"
            else:
                await self.async_set_unique_id(mac)
                self._abort_if_unique_id_configured()

                # Trust the user input and create the entry without verifying connection
                # This prevents "Failed to connect" errors if UDP packets are dropped
                return self.async_create_entry(
                    title=f"BGH AC ({host})",
                    data={"host": host, "device_id": mac}
                )

        return self.async_show_form(
            step_id="user",
            data_schema=vol.Schema({
                vol.Required(CONF_HOST): str,
                vol.Required(CONF_MAC): str,
            }),
            errors=errors,
        )
