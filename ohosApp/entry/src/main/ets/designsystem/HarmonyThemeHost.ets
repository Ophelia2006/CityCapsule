import { common, ConfigurationConstant } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import window from '@ohos.window';
import { HarmonyMmkvStorage } from '../storage/HarmonyMmkvStorage';
import { HarmonyStorageProtocol } from '../storage/HarmonyStorageProtocol';
import {
  HarmonyThemeBootstrap,
  HarmonyThemeProtocol,
  normalizeHarmonyThemeMode,
  resolveHarmonyTheme
} from './HarmonyThemeProtocol';

export type HarmonyThemeListener = (isDark: boolean) => void;

/** HarmonyOS boundary for bootstrap state, configuration changes and system bars. */
export class HarmonyThemeHost {
  static readonly shared: HarmonyThemeHost = new HarmonyThemeHost();

  private mainWindow: window.Window | undefined = undefined;
  private abilityContext: common.UIAbilityContext | undefined = undefined;
  private systemDark: boolean = false;
  private nextListenerId: number = 1;
  private readonly listeners: Map<number, HarmonyThemeListener> = new Map<number, HarmonyThemeListener>();

  private constructor() {
  }

  attach(mainWindow: window.Window, context: common.UIAbilityContext): void {
    this.mainWindow = mainWindow;
    this.abilityContext = context;
    this.systemDark = this.readSystemDark(context);
    this.applyAppearance(this.bootstrap().resolvedDark);
  }

  detach(): void {
    this.listeners.clear();
    this.mainWindow = undefined;
    this.abilityContext = undefined;
  }

  bootstrap(): HarmonyThemeBootstrap {
    const persistedMode = HarmonyMmkvStorage.shared
      .store(HarmonyStorageProtocol.STORE_PREFERENCES)
      ?.read(HarmonyThemeProtocol.THEME_STORAGE_KEY);
    const themeMode = normalizeHarmonyThemeMode(persistedMode);
    return {
      themeMode: themeMode,
      systemDark: this.systemDark,
      resolvedDark: resolveHarmonyTheme(themeMode, this.systemDark)
    };
  }

  onConfigurationUpdate(): void {
    if (this.abilityContext === undefined) {
      return;
    }
    const latestSystemDark = this.readSystemDark(this.abilityContext);
    if (latestSystemDark !== this.systemDark) {
      this.systemDark = latestSystemDark;
      this.listeners.forEach((listener: HarmonyThemeListener) => {
        listener(latestSystemDark);
      });
    }
    this.applyAppearance(this.bootstrap().resolvedDark);
  }

  refreshAppearance(): void {
    this.applyAppearance(this.bootstrap().resolvedDark);
  }

  addListener(listener: HarmonyThemeListener): number {
    const listenerId = this.nextListenerId;
    this.nextListenerId += 1;
    this.listeners.set(listenerId, listener);
    listener(this.systemDark);
    return listenerId;
  }

  removeListener(listenerId: number): void {
    this.listeners.delete(listenerId);
  }

  applyAppearance(isDark: boolean): void {
    if (this.mainWindow === undefined) {
      return;
    }
    this.mainWindow.setWindowSystemBarProperties({
      statusBarColor: '#00000000',
      navigationBarColor: isDark ? '#FF11131A' : '#FFF5F6FA',
      statusBarContentColor: isDark ? '#FFFFFFFF' : '#FF1F2430',
      navigationBarContentColor: isDark ? '#FFFFFFFF' : '#FF1F2430'
    }).catch((error: Error) => {
      hilog.warn(
        0x0000,
        'CityCapsuleTheme',
        'Failed to apply system bars: %{public}s',
        error.message
      );
    });
  }

  private readSystemDark(context: common.UIAbilityContext): boolean {
    return context.config.colorMode === ConfigurationConstant.ColorMode.COLOR_MODE_DARK;
  }
}
