import { ACMode, FanSpeed } from "../types";

export function getStatusHex(): string {
  return "00000000000000accf23aa3190590001e4";
}

export function getModeHex(deviceId: string, mode: ACMode, fan: FanSpeed): string {
  // Base: 00000000000000 + deviceId + f60001610402000080
  // Index 17 (byte 18) is mode, Index 18 (byte 19) is fan
  const base = `00000000000000${deviceId}f60001610402000080`;
  const bytes = Buffer.from(base, "hex");
  bytes[17] = mode;
  bytes[18] = fan;
  return bytes.toString("hex");
}

export function getTempHex(deviceId: string, mode: ACMode, fan: FanSpeed, temp: number): string {
  // Base: 00000000000000 + deviceId + 810001610100000000
  // Index 17=mode, 18=fan, 20-21=temp (little-endian short * 100)
  const base = `00000000000000${deviceId}810001610100000000`;
  const bytes = Buffer.from(base, "hex");
  bytes[17] = mode;
  bytes[18] = fan;
  
  const tempRaw = Math.round(temp * 100);
  bytes[20] = tempRaw & 0xff;
  bytes[21] = (tempRaw >> 8) & 0xff;
  
  return bytes.toString("hex");
}

export function parseStatus(hex: string) {
  if (hex.length < 50) return null;
  const bytes = Buffer.from(hex, "hex");
  
  const deviceId = hex.substring(14, 26);
  const mode = bytes[18];
  const fan = bytes[19];
  
  // Little-endian short at 21 and 23
  const currentTemp = bytes.readInt16LE(21) / 100;
  const targetTemp = bytes.readInt16LE(23) / 100;
  
  return {
    deviceId,
    mode,
    fan,
    currentTemp,
    targetTemp
  };
}
