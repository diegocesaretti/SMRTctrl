from __future__ import annotations

from typing import Any

from homeassistant.components import media_source
from homeassistant.components.media_player import (
    MediaPlayerEntity,
    MediaPlayerEntityFeature,
    MediaPlayerState,
    MediaType,
    async_process_play_media_url,
)
from homeassistant.helpers.entity_platform import AddConfigEntryEntitiesCallback

from .entity import HomeHubEntity


async def async_setup_entry(hass, entry, async_add_entities: AddConfigEntryEntitiesCallback):
    async_add_entities([HomeHubMediaPlayer(entry.runtime_data)])


class HomeHubMediaPlayer(HomeHubEntity, MediaPlayerEntity):
    _attr_name = None
    _attr_supported_features = (
        MediaPlayerEntityFeature.PLAY_MEDIA
        | MediaPlayerEntityFeature.PLAY
        | MediaPlayerEntityFeature.PAUSE
        | MediaPlayerEntityFeature.STOP
        | MediaPlayerEntityFeature.VOLUME_SET
    )
    _attr_assumed_state = True

    def __init__(self, coordinator):
        super().__init__(coordinator)
        self._attr_unique_id = f"{coordinator.data['device_id']}-media-player"

    @property
    def state(self):
        state = self.coordinator.data.get("media_state", "idle")
        if state == "playing":
            return MediaPlayerState.PLAYING
        if state == "paused":
            return MediaPlayerState.PAUSED
        return MediaPlayerState.IDLE

    @property
    def media_content_id(self):
        return self.coordinator.data.get("media_url") or None

    @property
    def media_title(self):
        return self.coordinator.data.get("media_title") or None

    @property
    def volume_level(self):
        return self.coordinator.data.get("volume", 50) / 100

    async def async_play_media(
        self,
        media_type: MediaType | str,
        media_id: str,
        **kwargs: Any,
    ) -> None:
        if media_source.is_media_source_id(media_id):
            item = await media_source.async_resolve_media(self.hass, media_id, self.entity_id)
            media_id = async_process_play_media_url(self.hass, item.url)

        mime = str(media_type)
        if media_type == MediaType.MUSIC:
            mime = "audio/*"
        elif media_type == MediaType.VIDEO:
            mime = "video/*"

        await self.coordinator.api.command(
            "/api/media/play",
            {
                "url": media_id,
                "mime": mime,
                "title": kwargs.get("title", ""),
            },
        )
        await self.coordinator.async_request_refresh()

    async def async_media_pause(self):
        await self.coordinator.api.command("/api/media/pause")
        await self.coordinator.async_request_refresh()

    async def async_media_play(self):
        await self.coordinator.api.command("/api/media/resume")
        await self.coordinator.async_request_refresh()

    async def async_media_stop(self):
        await self.coordinator.api.command("/api/media/stop")
        await self.coordinator.async_request_refresh()

    async def async_set_volume_level(self, volume):
        await self.coordinator.api.command(
            "/api/media/volume", {"level": str(round(volume * 100))}
        )
        await self.coordinator.async_request_refresh()
