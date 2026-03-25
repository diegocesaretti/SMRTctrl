import socket
import struct
import logging
import async_timeout
import asyncio

from homeassistant.components.climate import ClimateEntity
from homeassistant.components.climate.const import (
    HVACMode,
    ClimateEntityFeature,
    FanEntityFeature,
)
from homeassistant.const import ATTR_TEMPERATURE, UnitOfTemperature

from .const import (
    DOMAIN,
    UDP_SEND_PORT,
    UDP_RECV_PORT,
    STATUS_CMD,
    MODE_MAP,
    HA_TO_BGH_MODE,
    FAN_MAP,
    HA_TO_BGH_FAN,
)

_LOGGER = logging.getLogger(__name__)

async def async_setup_entry(hass, entry, async_add_entities):
    """Set up BGH Smart AC from a config entry."""
    host = entry.data["host"]
    device_id = entry.data["device_id"]
    async_add_entities([BGHSmartAC(host, device_id)])

class BGHSmartAC(ClimateEntity):
    """Representation of a BGH Smart AC unit."""

    _attr_has_entity_name = True
    _attr_temperature_unit = UnitOfTemperature.CELSIUS
    _attr_hvac_modes = [
        HVACMode.OFF,
        HVACMode.COOL,
        HVACMode.HEAT,
        HVACMode.DRY,
        HVACMode.FAN_ONLY,
        HVACMode.AUTO,
    ]
    _attr_fan_modes = ["low", "medium", "high"]
    _attr_supported_features = (
        ClimateEntityFeature.TARGET_TEMPERATURE
        | ClimateEntityFeature.FAN_MODE
    )

    def __init__(self, host, device_id):
        self._host = host
        self._device_id = device_id
        self._attr_unique_id = device_id
        self._attr_name = f"BGH AC {device_id[-4:]}"
        
        self._current_temp = None
        self._target_temp = 24
        self._hvac_mode = HVACMode.OFF
        self._fan_mode = "low"

    @property
    def current_temperature(self):
        return self._current_temp

    @property
    def target_temperature(self):
        return self._target_temp

    @property
    def hvac_mode(self):
        return self._hvac_mode

    @property
    def fan_mode(self):
        return self._fan_mode

    async def async_update(self):
        """Fetch state from the device."""
        try:
            data = await self._send_udp_command(bytes.fromhex(STATUS_CMD))
            if data and len(data) >= 25:
                self._parse_status(data)
        except Exception as e:
            _LOGGER.error("Error updating BGH AC: %s", e)

    def _parse_status(self, data):
        """Parse the raw UDP response."""
        # Byte 18: Mode
        mode_raw = data[18]
        self._hvac_mode = MODE_MAP.get(mode_raw, HVACMode.OFF)
        
        # Byte 19: Fan
        fan_raw = data[19]
        self._fan_mode = FAN_MAP.get(fan_raw, "low")
        
        # Bytes 21-22: Current Temp (LE short / 100)
        curr_raw = struct.unpack("<h", data[21:23])[0]
        self._current_temp = curr_raw / 100.0
        
        # Bytes 23-24: Target Temp (LE short / 100)
        targ_raw = struct.unpack("<h", data[23:25])[0]
        self._target_temp = targ_raw / 100.0

    async def async_set_hvac_mode(self, hvac_mode):
        """Set new target hvac mode."""
        mode_byte = HA_TO_BGH_MODE.get(hvac_mode, 0)
        fan_byte = HA_TO_BGH_FAN.get(self._fan_mode, 1)
        
        # Construct Mode Command
        cmd_base = f"00000000000000{self._device_id}f60001610402000080"
        cmd = bytearray(bytes.fromhex(cmd_base))
        cmd[17] = mode_byte
        cmd[18] = fan_byte
        
        await self._send_udp_command(cmd)
        self._hvac_mode = hvac_mode

    async def async_set_fan_mode(self, fan_mode):
        """Set new target fan mode."""
        mode_byte = HA_TO_BGH_MODE.get(self._hvac_mode, 0)
        fan_byte = HA_TO_BGH_FAN.get(fan_mode, 1)
        
        cmd_base = f"00000000000000{self._device_id}f60001610402000080"
        cmd = bytearray(bytes.fromhex(cmd_base))
        cmd[17] = mode_byte
        cmd[18] = fan_byte
        
        await self._send_udp_command(cmd)
        self._fan_mode = fan_mode

    async def async_set_temperature(self, **kwargs):
        """Set new target temperature."""
        temp = kwargs.get(ATTR_TEMPERATURE)
        if temp is None:
            return

        mode_byte = HA_TO_BGH_MODE.get(self._hvac_mode, 0)
        fan_byte = HA_TO_BGH_FAN.get(self._fan_mode, 1)
        
        cmd_base = f"00000000000000{self._device_id}810001610100000000"
        cmd = bytearray(bytes.fromhex(cmd_base))
        cmd[17] = mode_byte
        cmd[18] = fan_byte
        
        temp_raw = int(temp * 100)
        struct.pack_into("<h", cmd, 20, temp_raw)
        
        await self._send_udp_command(cmd)
        self._target_temp = temp

    async def _send_udp_command(self, command):
        """Send a UDP packet and wait for response."""
        loop = asyncio.get_running_loop()
        transport, protocol = await loop.create_datagram_endpoint(
            lambda: BGHProtocol(command),
            remote_addr=(self._host, UDP_SEND_PORT)
        )
        try:
            async with async_timeout.timeout(2.0):
                return await protocol.on_response
        except asyncio.TimeoutError:
            return None
        finally:
            transport.close()

class BGHProtocol(asyncio.DatagramProtocol):
    def __init__(self, message):
        self.message = message
        self.on_response = asyncio.get_running_loop().create_future()

    def connection_made(self, transport):
        transport.sendto(self.message)

    def datagram_received(self, data, addr):
        if not self.on_response.done():
            self.on_response.set_result(data)

    def error_received(self, exc):
        if not self.on_response.done():
            self.on_response.set_exception(exc)
