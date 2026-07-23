export class HarmonyThemeProtocol {
  static readonly VERSION: number = 1;

  static readonly KEY_PROTOCOL_VERSION: string = 'themeProtocolVersion';
  static readonly KEY_THEME_MODE: string = 'themeMode';
  static readonly KEY_SYSTEM_DARK: string = 'systemDark';
  static readonly KEY_RESOLVED_DARK: string = 'resolvedDark';
  static readonly KEY_KUIKLY_NIGHT_MODE: string = 'isNightMode';

  static readonly MODULE_NAME: string = 'CCThemeHostModule';
  static readonly METHOD_APPLY_APPEARANCE: string = 'applyAppearance';
  static readonly FIELD_PROTOCOL_VERSION: string = 'protocolVersion';
  static readonly FIELD_THEME_MODE: string = 'themeMode';
  static readonly FIELD_IS_DARK: string = 'isDark';

  static readonly EVENT_THEME_DID_CHANGED: string = 'themeDidChanged';

  static readonly MODE_SYSTEM: string = 'system';
  static readonly MODE_LIGHT: string = 'light';
  static readonly MODE_DARK: string = 'dark';
  static readonly THEME_STORAGE_KEY: string = 'settings.theme_mode';
}

export interface HarmonyThemeBootstrap {
  themeMode: string;
  systemDark: boolean;
  resolvedDark: boolean;
}

export interface HarmonyThemeHostRequest {
  protocolVersion: number;
  themeMode: string;
  isDark: boolean;
}

export function normalizeHarmonyThemeMode(value: string | undefined): string {
  if (value === HarmonyThemeProtocol.MODE_LIGHT || value === HarmonyThemeProtocol.MODE_DARK) {
    return value;
  }
  return HarmonyThemeProtocol.MODE_SYSTEM;
}

export function resolveHarmonyTheme(mode: string, systemDark: boolean): boolean {
  if (mode === HarmonyThemeProtocol.MODE_LIGHT) {
    return false;
  }
  if (mode === HarmonyThemeProtocol.MODE_DARK) {
    return true;
  }
  return systemDark;
}
