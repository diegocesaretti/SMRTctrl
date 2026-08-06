from homeassistant.components.button import ButtonEntity
from homeassistant.helpers.entity_platform import AddConfigEntryEntitiesCallback

from .entity import HomeHubEntity


async def async_setup_entry(hass, entry, async_add_entities: AddConfigEntryEntitiesCallback):
    coordinator = entry.runtime_data
    async_add_entities([ReloadDashboardButton(coordinator), OpenSettingsButton(coordinator)])


class ReloadDashboardButton(HomeHubEntity, ButtonEntity):
    _attr_name = "Reload dashboard"

    def __init__(self, coordinator):
        super().__init__(coordinator)
        self._attr_unique_id = f"{coordinator.data['device_id']}-reload"

    async def async_press(self):
        await self.coordinator.api.command("/api/dashboard/reload")


class OpenSettingsButton(HomeHubEntity, ButtonEntity):
    _attr_name = "Open settings"

    def __init__(self, coordinator):
        super().__init__(coordinator)
        self._attr_unique_id = f"{coordinator.data['device_id']}-settings"

    async def async_press(self):
        await self.coordinator.api.command("/api/settings/open")
