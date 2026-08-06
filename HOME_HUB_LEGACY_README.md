# Home Hub Legacy

A deliberately lightweight Android 6 (API 23) dashboard and Home Assistant satellite.

## First alpha scope

- Home Assistant dashboard in a plain system WebView.
- Custom dashboard URL.
- Immersive kiosk mode and optional use as the Android Home launcher.
- Local authenticated HTTP API.
- Home Assistant custom integration.
- Screen wake / black-screen sleep / brightness control.
- Front-camera luminance motion sensing without CameraX or ML Kit.
- Built-in media playback or selection of an installed external player such as VLC.
- `media_player`, motion, battery, screen, brightness and dashboard controls in Home Assistant.
- Large on-screen messages, Android TTS, or both through three `notify` entities.
- Start on boot.

## Deliberate exclusion from alpha 1

Always-listening wake-word detection is not bundled yet. It will be added as an isolated optional module after the base kiosk is confirmed stable on the target Android 6 device. This avoids letting a native ML/audio library crash the entire dashboard again.

## Android setup

1. Install the debug APK.
2. Open settings from the three-dot button.
3. Set the full Home Assistant dashboard URL.
4. Copy the displayed local API port and token.
5. Optionally choose an external media player.
6. Enable camera motion only after the dashboard itself is stable.

For a stronger kiosk, select **Home Hub Legacy** as the device's default Home app.

## Home Assistant setup

Copy `custom_components/home_hub_legacy` into the Home Assistant `custom_components` directory and restart Home Assistant.

Then add **Home Hub Legacy** from Settings → Devices & services and enter:

- Android device IP
- Port (default `2323`)
- Token from the app

The integration creates a media player, screen and motion switches, motion and battery sensors, brightness control, dashboard buttons, and notification entities for overlay text, TTS, and combined assistant-style responses.
