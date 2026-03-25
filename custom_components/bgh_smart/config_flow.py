import voluptuous as vol
from homeassistant import config_entries
from homeassistant.const import CONF_HOST, CONF_MAC
import socket
import re

from .const import DOMAIN, UDP_SEND_PORT

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

                # Verify connection by sending a status request
                is_valid = await self.hass.async_add_executor_job(
                    self._verify_connection, host, mac
                )
                
                if is_valid:
                    return self.async_create_entry(
                        title=f"BGH AC ({host})",
                        data={"host": host, "device_id": mac}
                    )
                else:
                    errors["base"] = "cannot_connect"

        return self.async_show_form(
            step_id="user",
            data_schema=vol.Schema({
                vol.Required(CONF_HOST): str,
                vol.Required(CONF_MAC): str,
            }),
            errors=errors,
        )

    def _verify_connection(self, host, mac):
        """Verify we can communicate with the device."""
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            sock.settimeout(5.0)
            
            # Construct status command with the provided MAC
            cmd_hex = f"00000000000000{mac}590001e4"
            sock.sendto(bytes.fromhex(cmd_hex), (host, UDP_SEND_PORT))
            
            data, addr = sock.recvfrom(1024)
            if len(data) >= 25:
                return True
        except Exception:
            return False
        finally:
            sock.close()
        return False
