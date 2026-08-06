from homeassistant.components.sensor import SensorDeviceClass, SensorEntity
from homeassistant.const import PERCENTAGE
from homeassistant.helpers.entity_platform import AddConfigEntryEntitiesCallback

from .entity import HomeHubEntity


async def async_setup_entry(hass, entry, async_add_entities: AddConfigEntryEntitiesCallback):
    async_add_entities([HomeHubBatterySensor(entry.runtime_data)])


class HomeHubBatterySensor(HomeHubEntity, SensorEntity):
    _attr_name = "Battery"
    _attr_device_class = SensorDeviceClass.BATTERY
    _attr_native_unit_of_measurement = PERCENTAGE

    def __init__(self, coordinator):
        super().__init__(coordinator)
        self._attr_unique_id = f"{coordinator.data['device_id']}-battery"

    @property
    def native_value(self):
        return self.coordinator.data.get("battery_level")

    @property
    def extra_state_attributes(self):
        return {"charging": self.coordinator.data.get("charging", False)}
