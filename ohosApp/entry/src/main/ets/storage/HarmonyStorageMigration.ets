import { common } from '@kit.AbilityKit';
import { preferences } from '@kit.ArkData';
import {
  HarmonyMmkvStorage,
  HarmonyStorageProvider,
  HarmonyStringStore
} from './HarmonyMmkvStorage';
import { HarmonyStorageProtocol } from './HarmonyStorageProtocol';

export class HarmonyMigrationContract {
  static readonly CURRENT_SCHEMA_VERSION: number = 1;
  static readonly MAX_ATTEMPTS: number = 3;

  static readonly META_SCHEMA_VERSION: string = 'storage.schema_version';
  static readonly META_STATE: string = 'storage.migration_state';
  static readonly META_ATTEMPTS: string = 'storage.migration_attempts';
  static readonly META_LAST_ERROR: string = 'storage.migration_last_error';

  static readonly STATE_RUNNING: string = 'running';
  static readonly STATE_COMPLETED: string = 'completed';
  static readonly STATE_FAILED: string = 'failed';

  static readonly TYPE_METADATA_PREFIX: string = '__cc_type__.';
  static readonly LEGACY_SETTINGS_STORE: string = 'city_capsule_settings';
  static readonly LEGACY_THEME_MODE: string = 'theme_mode';
  static readonly TARGET_THEME_MODE: string = 'settings.theme_mode';
  static readonly TARGET_THEME_MODE_TYPE: string = 'string';
}

export class HarmonyMigrationOutcome {
  static readonly COMPLETED: string = 'completed';
  static readonly ALREADY_COMPLETED: string = 'already_completed';
  static readonly RETRY_EXHAUSTED: string = 'retry_exhausted';
  static readonly STORAGE_UNAVAILABLE: string = 'storage_unavailable';
  static readonly FAILED: string = 'failed';
}

export interface HarmonyLegacyThemeValue {
  present: boolean;
  value?: string;
}

export interface HarmonyLegacySettingsSource {
  readThemeMode(): HarmonyLegacyThemeValue;

  clearThemeMode(): void;
}

export interface HarmonyMigrationResult {
  outcome: string;
  attempt: number;
  migratedTheme: boolean;
  reason?: string;
}

export class HarmonyPreferencesLegacySettingsSource implements HarmonyLegacySettingsSource {
  private readonly legacyPreferences: preferences.Preferences;

  constructor(context: common.ApplicationContext) {
    this.legacyPreferences = preferences.getPreferencesSync(context, {
      name: HarmonyMigrationContract.LEGACY_SETTINGS_STORE
    });
  }

  readThemeMode(): HarmonyLegacyThemeValue {
    const present = this.legacyPreferences.hasSync(HarmonyMigrationContract.LEGACY_THEME_MODE);
    if (!present) {
      return { present: false };
    }
    const rawValue = this.legacyPreferences.getSync(HarmonyMigrationContract.LEGACY_THEME_MODE, '');
    return {
      present: true,
      value: typeof rawValue === 'string' ? rawValue : undefined
    };
  }

  clearThemeMode(): void {
    this.legacyPreferences.deleteSync(HarmonyMigrationContract.LEGACY_THEME_MODE);
    this.legacyPreferences.flush().catch(() => {
      // Cleanup is best-effort and will be retried safely because the MMKV target wins.
    });
  }
}

export class HarmonyStorageMigrator {
  private readonly provider: HarmonyStorageProvider;
  private readonly legacySource: HarmonyLegacySettingsSource;

  constructor(
    provider: HarmonyStorageProvider = HarmonyMmkvStorage.shared,
    legacySource: HarmonyLegacySettingsSource
  ) {
    this.provider = provider;
    this.legacySource = legacySource;
  }

  migrate(): HarmonyMigrationResult {
    if (!this.provider.isReady()) {
      return this.result(HarmonyMigrationOutcome.STORAGE_UNAVAILABLE, 0, false, 'storage_unavailable');
    }
    const meta = this.provider.store(HarmonyStorageProtocol.STORE_META);
    const target = this.provider.store(HarmonyStorageProtocol.STORE_PREFERENCES);
    if (meta === undefined || target === undefined) {
      return this.result(HarmonyMigrationOutcome.STORAGE_UNAVAILABLE, 0, false, 'storage_unavailable');
    }

    const schemaVersion = this.parseNonNegative(meta.read(HarmonyMigrationContract.META_SCHEMA_VERSION));
    const state = meta.read(HarmonyMigrationContract.META_STATE) ?? '';
    const previousAttempts = this.parseNonNegative(meta.read(HarmonyMigrationContract.META_ATTEMPTS));
    if (schemaVersion >= HarmonyMigrationContract.CURRENT_SCHEMA_VERSION &&
      state === HarmonyMigrationContract.STATE_COMPLETED) {
      return this.result(HarmonyMigrationOutcome.ALREADY_COMPLETED, previousAttempts, false);
    }
    if (state === HarmonyMigrationContract.STATE_FAILED &&
      previousAttempts >= HarmonyMigrationContract.MAX_ATTEMPTS) {
      return this.result(
        HarmonyMigrationOutcome.RETRY_EXHAUSTED,
        previousAttempts,
        false,
        meta.read(HarmonyMigrationContract.META_LAST_ERROR)
      );
    }

    const attempt = previousAttempts + 1;
    if (!meta.write(HarmonyMigrationContract.META_ATTEMPTS, attempt.toString()) ||
      !meta.write(HarmonyMigrationContract.META_STATE, HarmonyMigrationContract.STATE_RUNNING)) {
      return this.result(HarmonyMigrationOutcome.FAILED, attempt, false, 'meta_write_failed');
    }

    try {
      const targetKey = HarmonyMigrationContract.TARGET_THEME_MODE;
      const targetTypeKey = HarmonyMigrationContract.TYPE_METADATA_PREFIX + targetKey;
      const targetAlreadyExists = target.contains(targetKey);
      const legacy = this.legacySource.readThemeMode();
      let migratedTheme = false;

      if (targetAlreadyExists) {
        if (!target.write(targetTypeKey, HarmonyMigrationContract.TARGET_THEME_MODE_TYPE)) {
          return this.fail(meta, attempt, 'target_type_write_failed');
        }
      } else {
        const normalized = this.normalizeThemeMode(legacy.value);
        if (legacy.present && normalized !== undefined) {
          if (!target.write(targetTypeKey, HarmonyMigrationContract.TARGET_THEME_MODE_TYPE)) {
            return this.fail(meta, attempt, 'target_type_write_failed');
          }
          if (!target.write(targetKey, normalized)) {
            return this.fail(meta, attempt, 'target_write_failed');
          }
          if (target.read(targetKey) !== normalized) {
            return this.fail(meta, attempt, 'target_verify_failed');
          }
          migratedTheme = true;
        }
      }

      if (legacy.present) {
        try {
          this.legacySource.clearThemeMode();
        } catch (_) {
          // Cleanup is best-effort. Completion remains safe because an existing MMKV target wins.
        }
      }
      if (!meta.write(
        HarmonyMigrationContract.META_SCHEMA_VERSION,
        HarmonyMigrationContract.CURRENT_SCHEMA_VERSION.toString()
      )) {
        return this.fail(meta, attempt, 'meta_write_failed');
      }
      meta.remove(HarmonyMigrationContract.META_LAST_ERROR);
      if (!meta.write(HarmonyMigrationContract.META_STATE, HarmonyMigrationContract.STATE_COMPLETED)) {
        return this.fail(meta, attempt, 'meta_write_failed');
      }
      return this.result(HarmonyMigrationOutcome.COMPLETED, attempt, migratedTheme);
    } catch (_) {
      return this.fail(meta, attempt, 'unexpected_failure');
    }
  }

  private fail(meta: HarmonyStringStore, attempt: number, reason: string): HarmonyMigrationResult {
    meta.write(HarmonyMigrationContract.META_LAST_ERROR, reason);
    meta.write(HarmonyMigrationContract.META_STATE, HarmonyMigrationContract.STATE_FAILED);
    return this.result(HarmonyMigrationOutcome.FAILED, attempt, false, reason);
  }

  private result(
    outcome: string,
    attempt: number,
    migratedTheme: boolean,
    reason?: string
  ): HarmonyMigrationResult {
    return {
      outcome: outcome,
      attempt: attempt,
      migratedTheme: migratedTheme,
      reason: reason
    };
  }

  private parseNonNegative(value?: string): number {
    if (value === undefined || value.length === 0) {
      return 0;
    }
    const parsed = Number.parseInt(value);
    return Number.isNaN(parsed) ? 0 : Math.max(0, parsed);
  }

  private normalizeThemeMode(value?: string): string | undefined {
    if (value === undefined) {
      return undefined;
    }
    const candidate = value.trim().toLowerCase();
    if (candidate === 'system' || candidate === 'light' || candidate === 'dark') {
      return candidate;
    }
    return undefined;
  }
}
