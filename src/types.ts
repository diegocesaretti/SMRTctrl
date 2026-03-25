export enum ACMode {
  OFF = 0,
  COOL = 1,
  HEAT = 2,
  DRY = 3,
  FAN = 4,
  AUTO = 254,
}

export enum FanSpeed {
  LOW = 1,
  MEDIUM = 2,
  HIGH = 3,
}

export interface ACStatus {
  ip: string;
  deviceId: string;
  currentTemp: number;
  targetTemp: number;
  mode: ACMode;
  fanSpeed: FanSpeed;
  lastUpdated: number;
}

export const AC_MODES_LABELS: Record<ACMode, string> = {
  [ACMode.OFF]: "OFF",
  [ACMode.COOL]: "COOL",
  [ACMode.HEAT]: "HEAT",
  [ACMode.DRY]: "DRY",
  [ACMode.FAN]: "FAN",
  [ACMode.AUTO]: "AUTO",
};

export const FAN_SPEED_LABELS: Record<FanSpeed, string> = {
  [FanSpeed.LOW]: "LOW",
  [FanSpeed.MEDIUM]: "MEDIUM",
  [FanSpeed.HIGH]: "HIGH",
};
