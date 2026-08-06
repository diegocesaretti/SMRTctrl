from homeassistant.components.binary_sensor import BinarySensorDeviceClass, BinarySensorEntity
from homeassistant.helpers.entity_platform import AddConfigEntryEntitiesCallback

from .entity import HomeHubEntity


async def async_setup_entry(hass, entry, async_add_entities: AddConfigEntryEntitiesCallback):
    async_add_entities([HomeHubMotionSensor(entry.runtime_data)])


class HomeHubMotionSensor(HomeHubEntity, BinarySensorEntity):
    _attr_name = "Motion"
    _attr_device_class = BinarySensorDeviceClass.MOTION

    def __init__(self, coordinator):
        super().__init__(coordinator)
        self._attr_unique_id = f"{coordinator.data['device_id']}-motion"

    @property
    def is_on(self):
        return bool(self.coordinator.data.get("motion"))
