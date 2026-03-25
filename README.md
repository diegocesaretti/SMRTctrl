# BGH Smart AC Home Assistant Integration

This integration allows you to control BGH Smart AC units directly from Home Assistant using local UDP communication.

## Features
- **Local Control**: No cloud required.
- **HVAC Modes**: Cool, Heat, Dry, Fan, Auto, Off.
- **Fan Speed**: Low, Medium, High.
- **Temperature Control**: Set target temperature and view current temperature.
- **Auto Discovery**: Automatically finds the device ID from the IP address.

## Installation

### HACS (Recommended)
1. Open HACS in Home Assistant.
2. Click on "Integrations".
3. Click the three dots in the top right and select "Custom repositories".
4. Add this repository URL and select "Integration" as the category.
5. Search for "BGH Smart AC" and install.
6. Restart Home Assistant.

### Manual
1. Copy the `custom_components/bgh_smart` folder to your Home Assistant `config/custom_components/` directory.
2. Restart Home Assistant.

## Configuration
1. Go to **Settings** > **Devices & Services**.
2. Click **Add Integration**.
3. Search for **BGH Smart AC**.
4. Enter the IP address of your AC unit.
