from dataclasses import dataclass

from homeassistant.components.notify import NotifyEntity, NotifyEntityDescription
from homeassistant.helpers.entity_platform import AddConfigEntryEntitiesCallback

from .entity import HomeHubEntity


@dataclass(frozen=True, kw_only=True)
class HomeHubNotifyDescription(NotifyEntityDescription):
    endpoint: str


NOTIFIERS = (
    HomeHubNotifyDescription(key="overlay", name="Screen message", endpoint="/api/text"),
    HomeHubNotifyDescription(key="tts", name="Text to speech", endpoint="/api/tts"),
    HomeHubNotifyDescription(
        key="response", name="Assistant response", endpoint="/api/response"
    ),
)


async def async_setup_entry(hass, entry, async_add_entities: AddConfigEntryEntitiesCallback):
    coordinator = entry.runtime_data
    async_add_entities(
        HomeHubNotifyEntity(coordinator, description)
        for description in NOTIFIERS
    )


class HomeHubNotifyEntity(HomeHubEntity, NotifyEntity):
    entity_description: HomeHubNotifyDescription

    def __init__(self, coordinator, description):
        HomeHubEntity.__init__(self, coordinator)
        NotifyEntity.__init__(self)
        self.entity_description = description
        self._attr_unique_id = f"{coordinator.data['device_id']}-{description.key}"

    async def async_send_message(self, message: str, title: str | None = None):
        await self.coordinator.api.command(
            self.entity_description.endpoint,
            {"text": message, "duration": "10"},
        )
