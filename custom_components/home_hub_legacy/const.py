from datetime import timedelta
import logging

from homeassistant.const import Platform

DOMAIN = "home_hub_legacy"
DEFAULT_PORT = 2323
CONF_TOKEN = "token"
UPDATE_INTERVAL = timedelta(seconds=4)
LOGGER = logging.getLogger(__package__)
PLATFORMS = [
    Platform.BINARY_SENSOR,
    Platform.BUTTON,
    Platform.MEDIA_PLAYER,
    Platform.NOTIFY,
    Platform.NUMBER,
    Platform.SENSOR,
    Platform.SWITCH,
]
