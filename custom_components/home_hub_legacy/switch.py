from homeassistant.components.switch import SwitchEntity
from homeassistant.helpers.entity_platform import AddConfigEntryEntitiesCallback

from .entity import HomeHubEntity


async def async_setup_entry(hass, entry, async_add_entities: AddConfigEntryEntitiesCallback):
    coordinator = entry.runtime_data
    async_add_entities([HomeHubScreenSwitch(coordinator), HomeHubMotionSwitch(coordinator)])


class HomeHubScreenSwitch(HomeHubEntity, SwitchEntity):
    _attr_name = "Screen"

    def __init__(self, coordinator):
        super().__init__(coordinator)
        self._attr_unique_id = f"{coordinator.data['device_id']}-screen"

    @property
    def is_on(self):
        return bool(self.coordinator.data.get("screen_on"))

    async def async_turn_on(self, **kwargs):
        await self.coordinator.api.command("/api/screen/on")
        await self.coordinator.async_request_refresh()

    async def async_turn_off(self, **kwargs):
        await self.coordinator.api.command("/api/screen/off")
        await self.coordinator.async_request_refresh()


class HomeHubMotionSwitch(HomeHubEntity, SwitchEntity):
    _attr_name = "Motion sensing"

    def __init__(self, coordinator):
        super().__init__(coordinator)
        self._attr_unique_id = f"{coordinator.data['device_id']}-motion-enabled"

    @property
    def is_on(self):
        return bool(self.coordinator.data.get("motion_enabled"))

    async def async_turn_on(self, **kwargs):
        await self.coordinator.api.command("/api/motion/enable", {"enabled": "true"})
        await self.coordinator.async_request_refresh()

    async def async_turn_off(self, **kwargs):
        await self.coordinator.api.command("/api/motion/enable", {"enabled": "false"})
        await self.coordinator.async_request_refresh()
