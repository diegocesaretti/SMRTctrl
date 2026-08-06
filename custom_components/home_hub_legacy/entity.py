from __future__ import annotations

from homeassistant.helpers.device_registry import DeviceInfo
from homeassistant.helpers.update_coordinator import CoordinatorEntity

from .const import DOMAIN
from .coordinator import HomeHubCoordinator


class HomeHubEntity(CoordinatorEntity[HomeHubCoordinator]):
    _attr_has_entity_name = True

    def __init__(self, coordinator: HomeHubCoordinator) -> None:
        super().__init__(coordinator)
        data = coordinator.data
        self._attr_device_info = DeviceInfo(
            identifiers={(DOMAIN, data["device_id"])},
            name=data.get("device_name", "Home Hub Legacy"),
            manufacturer=data.get("manufacturer", "Android"),
            model=data.get("model", "Android display"),
            sw_version=data.get("app_version"),
        )
