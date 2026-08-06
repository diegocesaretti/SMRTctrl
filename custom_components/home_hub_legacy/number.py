from homeassistant.components.number import NumberEntity, NumberMode
from homeassistant.helpers.entity_platform import AddConfigEntryEntitiesCallback

from .entity import HomeHubEntity


async def async_setup_entry(hass, entry, async_add_entities: AddConfigEntryEntitiesCallback):
    async_add_entities([HomeHubBrightnessNumber(entry.runtime_data)])


class HomeHubBrightnessNumber(HomeHubEntity, NumberEntity):
    _attr_name = "Screen brightness"
    _attr_native_min_value = 1
    _attr_native_max_value = 100
    _attr_native_step = 1
    _attr_mode = NumberMode.SLIDER

    def __init__(self, coordinator):
        super().__init__(coordinator)
        self._attr_unique_id = f"{coordinator.data['device_id']}-brightness"

    @property
    def native_value(self):
        return self.coordinator.data.get("brightness", 70)

    async def async_set_native_value(self, value):
        await self.coordinator.api.command(
            "/api/screen/brightness", {"level": str(round(value))}
        )
        await self.coordinator.async_request_refresh()
