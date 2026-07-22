import { describe, expect, it } from '@ohos/hypium';
import {
  HarmonyLegacySettingsSource,
  HarmonyLegacyThemeValue,
  HarmonyMigrationContract,
  HarmonyMigrationOutcome,
  HarmonyStorageMigrator
} from '../main/ets/storage/HarmonyStorageMigration';
import {
  HarmonyStorageProvider,
  HarmonyStringStore
} from '../main/ets/storage/HarmonyMmkvStorage';
import { HarmonyStorageProtocol } from '../main/ets/storage/HarmonyStorageProtocol';

export default function harmonyStorageMigrationTest(): void {
  describe('HarmonyStorageMigrator', () => {
    it('validLegacyThemeMigratesAndCleansSource', 0, () => {
      const provider = new MigrationFakeProvider();
      const source = new MigrationFakeLegacySource(' DARK ');

      const result = new HarmonyStorageMigrator(provider, source).migrate();

      expect(result.outcome).assertEqual(HarmonyMigrationOutcome.COMPLETED);
      expect(result.migratedTheme).assertTrue();
      expect(source.cleared).assertTrue();
      expect(provider.preferences.read(HarmonyMigrationContract.TARGET_THEME_MODE)).assertEqual('dark');
      expect(provider.meta.read(HarmonyMigrationContract.META_STATE))
        .assertEqual(HarmonyMigrationContract.STATE_COMPLETED);
    });

    it('existingTargetWinsOverLegacyValue', 0, () => {
      const provider = new MigrationFakeProvider();
      provider.preferences.write(HarmonyMigrationContract.TARGET_THEME_MODE, 'light');
      const source = new MigrationFakeLegacySource('dark');

      const result = new HarmonyStorageMigrator(provider, source).migrate();

      expect(result.outcome).assertEqual(HarmonyMigrationOutcome.COMPLETED);
      expect(result.migratedTheme).assertFalse();
      expect(provider.preferences.read(HarmonyMigrationContract.TARGET_THEME_MODE)).assertEqual('light');
      expect(source.cleared).assertTrue();
    });

    it('invalidLegacyCompletesWithoutTargetValue', 0, () => {
      const provider = new MigrationFakeProvider();
      const source = new MigrationFakeLegacySource('amoled');

      const result = new HarmonyStorageMigrator(provider, source).migrate();

      expect(result.outcome).assertEqual(HarmonyMigrationOutcome.COMPLETED);
      expect(provider.preferences.contains(HarmonyMigrationContract.TARGET_THEME_MODE)).assertFalse();
      expect(source.cleared).assertTrue();
    });

    it('failedWriteRetriesOnNextRun', 0, () => {
      const provider = new MigrationFakeProvider();
      provider.preferences.failKey = HarmonyMigrationContract.TARGET_THEME_MODE;
      const source = new MigrationFakeLegacySource('dark');
      const migrator = new HarmonyStorageMigrator(provider, source);

      const failed = migrator.migrate();
      const failedState = provider.meta.read(HarmonyMigrationContract.META_STATE);
      provider.preferences.failKey = undefined;
      const retried = migrator.migrate();

      expect(failed.outcome).assertEqual(HarmonyMigrationOutcome.FAILED);
      expect(failedState).assertEqual(HarmonyMigrationContract.STATE_FAILED);
      expect(retried.outcome).assertEqual(HarmonyMigrationOutcome.COMPLETED);
      expect(retried.attempt).assertEqual(2);
    });

    it('exhaustedFailureDoesNotReadLegacyAgain', 0, () => {
      const provider = new MigrationFakeProvider();
      provider.meta.write(HarmonyMigrationContract.META_STATE, HarmonyMigrationContract.STATE_FAILED);
      provider.meta.write(
        HarmonyMigrationContract.META_ATTEMPTS,
        HarmonyMigrationContract.MAX_ATTEMPTS.toString()
      );
      const source = new MigrationFakeLegacySource('dark');

      const result = new HarmonyStorageMigrator(provider, source).migrate();

      expect(result.outcome).assertEqual(HarmonyMigrationOutcome.RETRY_EXHAUSTED);
      expect(source.readCount).assertEqual(0);
      expect(source.cleared).assertFalse();
    });
  });
}

class MigrationFakeLegacySource implements HarmonyLegacySettingsSource {
  value: string | undefined;
  present: boolean = true;
  cleared: boolean = false;
  readCount: number = 0;

  constructor(value?: string) {
    this.value = value;
  }

  readThemeMode(): HarmonyLegacyThemeValue {
    this.readCount += 1;
    return {
      present: this.present,
      value: this.value
    };
  }

  clearThemeMode(): void {
    this.cleared = true;
    this.present = false;
    this.value = undefined;
  }
}

class MigrationFakeProvider implements HarmonyStorageProvider {
  readonly meta: MigrationFakeStringStore = new MigrationFakeStringStore();
  readonly preferences: MigrationFakeStringStore = new MigrationFakeStringStore();
  private readonly cache: MigrationFakeStringStore = new MigrationFakeStringStore();

  isReady(): boolean {
    return true;
  }

  store(storeId: string): HarmonyStringStore | undefined {
    if (storeId === HarmonyStorageProtocol.STORE_META) {
      return this.meta;
    }
    if (storeId === HarmonyStorageProtocol.STORE_PREFERENCES) {
      return this.preferences;
    }
    if (storeId === HarmonyStorageProtocol.STORE_CACHE) {
      return this.cache;
    }
    return undefined;
  }
}

class MigrationFakeStringStore implements HarmonyStringStore {
  private readonly values: Map<string, string> = new Map<string, string>();
  failKey: string | undefined = undefined;

  contains(key: string): boolean {
    return this.values.has(key);
  }

  read(key: string): string | undefined {
    return this.values.get(key);
  }

  write(key: string, value: string): boolean {
    if (key === this.failKey) {
      return false;
    }
    this.values.set(key, value);
    return true;
  }

  remove(key: string): void {
    this.values.delete(key);
  }
}

