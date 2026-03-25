import voluptuous as vol
from homeassistant import config_entries
from homeassistant.const import CONF_HOST
import socket

from .const import DOMAIN, UDP_SEND_PORT, STATUS_CMD

class BGHSmartConfigFlow(config_entries.ConfigFlow, domain=DOMAIN):
    """Handle a config flow for BGH Smart AC."""

    VERSION = 1

    async def async_step_user(self, user_input=None):
        """Handle the initial step."""
        errors = {}
        if user_input is not None:
            host = user_input[CONF_HOST]
            device_id = await self._discover_device_id(host)
            
            if device_id:
                return self.async_create_entry(
                    title=f"BGH AC ({host})",
                    data={"host": host, "device_id": device_id}
                )
            else:
                errors["base"] = "cannot_connect"

        return self.async_show_form(
            step_id="user",
            data_schema=vol.Schema({vol.Required(CONF_HOST): str}),
            errors=errors,
        )

    async def _discover_device_id(self, host):
        """Try to get the device ID from the host."""
        # This implements the 'requestStatus' logic from Processing
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            sock.settimeout(2.0)
            sock.sendto(bytes.fromhex(STATUS_CMD), (host, UDP_SEND_PORT))
            data, addr = sock.recvfrom(1024)
            if len(data) >= 7:
                # Bytes 1-6 are the device ID
                return data[1:7].hex()
        except:
            return None
        finally:
            sock.close()
        return None
