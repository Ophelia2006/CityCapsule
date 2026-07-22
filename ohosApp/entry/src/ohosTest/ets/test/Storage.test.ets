import { common } from '@kit.AbilityKit';
import { preferences } from '@kit.ArkData';
import { abilityDelegatorRegistry } from '@kit.TestKit';
import { describe, expect, it } from '@ohos/hypium';
import { MMKV } from '@tencent/mmkv';
import { HarmonyMmkvStorage } from '../../../main/ets/storage/HarmonyMmkvStorage';
import {
  HarmonyMigrationContract,
  HarmonyMigrationOutcome,
  HarmonyPreferencesLegacySettingsSource,
  HarmonyStorageMigrator
} from '../../../main/ets/storage/HarmonyStorageMigration';
import { HarmonyStorageProtocol } from '../../../main/ets/storage/HarmonyStorageProtocol';

export default function storageTest(): void {
  describe('MMKVPersistenceTest', () => {
    it('valueSurvivesMemoryCacheReload', 0, () => {
      const applicationContext = testApplicationContext();
      MMKV.initialize(applicationContext);
      const mmapId = 'cc_harmony_device_test';
      const key = 'diagnostics.persistence_probe';
      const first = MMKV.mmkvWithID(mmapId, MMKV.SINGLE_PROCESS_MODE);
      first.removeValueForKey(key);

      expect(first.encodeString(key, 'harmony-mmkv-ok')).assertTrue();
      first.clearMemoryCache();
      const reopened = MMKV.mmkvWithID(mmapId, MMKV.SINGLE_PROCESS_MODE);

      expect(reopened.decodeString(key)).assertEqual('harmony-mmkv-ok');
      reopened.removeValueForKey(key);
    });

    it('preferencesThemeMigratesIntoMmkv', 0, () => {
      const applicationContext = testApplicationContext();
      const initialization = HarmonyMmkvStorage.shared.initialize(applicationContext);
      expect(initialization.success).assertTrue();
      const meta = HarmonyMmkvStorage.shared.store(HarmonyStorageProtocol.STORE_META);
      const target = HarmonyMmkvStorage.shared.store(HarmonyStorageProtocol.STORE_PREFERENCES);
      expect(meta !== undefined).assertTrue();
      expect(target !== undefined).assertTrue();
      if (meta === undefined || target === undefined) {
        return;
      }
      const typeKey = HarmonyMigrationContract.TYPE_METADATA_PREFIX +
        HarmonyMigrationContract.TARGET_THEME_MODE;
      meta.remove(HarmonyMigrationContract.META_SCHEMA_VERSION);
      meta.remove(HarmonyMigrationContract.META_STATE);
      meta.remove(HarmonyMigrationContract.META_ATTEMPTS);
      meta.remove(HarmonyMigrationContract.META_LAST_ERROR);
      target.remove(HarmonyMigrationContract.TARGET_THEME_MODE);
      target.remove(typeKey);
      const legacy = preferences.getPreferencesSync(applicationContext, {
        name: HarmonyMigrationContract.LEGACY_SETTINGS_STORE
      });
      legacy.putSync(HarmonyMigrationContract.LEGACY_THEME_MODE, 'dark');

      const result = new HarmonyStorageMigrator(
        HarmonyMmkvStorage.shared,
        new HarmonyPreferencesLegacySettingsSource(applicationContext)
      ).migrate();

      expect(result.outcome).assertEqual(HarmonyMigrationOutcome.COMPLETED);
      expect(target.read(HarmonyMigrationContract.TARGET_THEME_MODE)).assertEqual('dark');
      expect(target.read(typeKey)).assertEqual('string');
      expect(meta.read(HarmonyMigrationContract.META_STATE))
        .assertEqual(HarmonyMigrationContract.STATE_COMPLETED);
      expect(legacy.hasSync(HarmonyMigrationContract.LEGACY_THEME_MODE)).assertFalse();

      target.remove(HarmonyMigrationContract.TARGET_THEME_MODE);
      target.remove(typeKey);
      meta.remove(HarmonyMigrationContract.META_SCHEMA_VERSION);
      meta.remove(HarmonyMigrationContract.META_STATE);
      meta.remove(HarmonyMigrationContract.META_ATTEMPTS);
      meta.remove(HarmonyMigrationContract.META_LAST_ERROR);
    });
  });
}

function testApplicationContext(): common.ApplicationContext {
  return abilityDelegatorRegistry.getAbilityDelegator()
    .getAppContext()
    .getApplicationContext();
}
