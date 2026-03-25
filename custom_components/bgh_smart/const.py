from homeassistant.components.climate.const import (
    HVACMode,
    ClimateEntityFeature,
)

DOMAIN = "bgh_smart"
DEFAULT_NAME = "BGH Smart AC"

UDP_SEND_PORT = 20910
UDP_RECV_PORT = 20911

# Protocol Hex Commands
STATUS_CMD = "00000000000000accf23aa3190590001e4"

# BGH Modes to HA HVACModes
MODE_MAP = {
    0: HVACMode.OFF,
    1: HVACMode.COOL,
    2: HVACMode.HEAT,
    3: HVACMode.DRY,
    4: HVACMode.FAN_ONLY,
    254: HVACMode.AUTO,
}

HA_TO_BGH_MODE = {v: k for k, v in MODE_MAP.items()}

# Fan Speeds
FAN_MAP = {
    1: "low",
    2: "medium",
    3: "high",
}

HA_TO_BGH_FAN = {v: k for k, v in FAN_MAP.items()}
