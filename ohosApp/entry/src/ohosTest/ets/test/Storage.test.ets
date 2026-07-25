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

const TYPE_KEY_PREFIX: string = '__cc_type__.';
const PLACE_CATALOG_KEY: string = 'places.catalog';
const FAVORITE_IDS_KEY: string = 'favorites.place_ids';
const PLACE_CATALOG_JSON: string =
  '{"schemaVersion":1,"seedVersion":1,"places":[{"schemaVersion":1,"id":"device_place",' +
  '"name":"Device Place","city":"Shanghai","category":"culture","tags":["device"],' +
  '"createdAtEpochMs":1000,"updatedAtEpochMs":1000}]}';
const FAVORITE_IDS_JSON: string =
  '{"schemaVersion":1,"placeIds":["device_place"]}';

class RawStoredValue {
  readonly value: string | undefined;
  readonly valueType: string | undefined;

  constructor(value: string | undefined, valueType: string | undefined) {
    this.value = value;
    this.valueType = valueType;
  }
}

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

    it('placeCatalogAndFavoritesSurviveMemoryCacheReload', 0, () => {
      const applicationContext = testApplicationContext();
      MMKV.initialize(applicationContext);
      const first = MMKV.mmkvWithID(
        HarmonyStorageProtocol.STORE_PREFERENCES,
        MMKV.SINGLE_PROCESS_MODE
      );
      const originalCatalog: RawStoredValue = snapshot(first, PLACE_CATALOG_KEY);
      const originalFavorites: RawStoredValue = snapshot(first, FAVORITE_IDS_KEY);

      try {
        expect(first.encodeString(PLACE_CATALOG_KEY, PLACE_CATALOG_JSON)).assertTrue();
        expect(first.encodeString(
          TYPE_KEY_PREFIX + PLACE_CATALOG_KEY,
          HarmonyStorageProtocol.TYPE_JSON_OBJECT
        )).assertTrue();
        expect(first.encodeString(FAVORITE_IDS_KEY, FAVORITE_IDS_JSON)).assertTrue();
        expect(first.encodeString(
          TYPE_KEY_PREFIX + FAVORITE_IDS_KEY,
          HarmonyStorageProtocol.TYPE_JSON_OBJECT
        )).assertTrue();

        first.clearMemoryCache();
        const reopened = MMKV.mmkvWithID(
          HarmonyStorageProtocol.STORE_PREFERENCES,
          MMKV.SINGLE_PROCESS_MODE
        );
        expect(reopened.decodeString(PLACE_CATALOG_KEY)).assertEqual(PLACE_CATALOG_JSON);
        expect(reopened.decodeString(TYPE_KEY_PREFIX + PLACE_CATALOG_KEY))
          .assertEqual(HarmonyStorageProtocol.TYPE_JSON_OBJECT);
        expect(reopened.decodeString(FAVORITE_IDS_KEY)).assertEqual(FAVORITE_IDS_JSON);
        expect(reopened.decodeString(TYPE_KEY_PREFIX + FAVORITE_IDS_KEY))
          .assertEqual(HarmonyStorageProtocol.TYPE_JSON_OBJECT);

        restore(reopened, PLACE_CATALOG_KEY, originalCatalog);
        restore(reopened, FAVORITE_IDS_KEY, originalFavorites);
      } catch (error) {
        const reopened = MMKV.mmkvWithID(
          HarmonyStorageProtocol.STORE_PREFERENCES,
          MMKV.SINGLE_PROCESS_MODE
        );
        restore(reopened, PLACE_CATALOG_KEY, originalCatalog);
        restore(reopened, FAVORITE_IDS_KEY, originalFavorites);
        const message: string = error instanceof Error
          ? error.message
          : 'Unknown place persistence test failure.';
        throw new Error(message);
      }
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

function snapshot(mmkv: MMKV, key: string): RawStoredValue {
  return new RawStoredValue(
    mmkv.decodeString(key),
    mmkv.decodeString(TYPE_KEY_PREFIX + key)
  );
}

function restore(mmkv: MMKV, key: string, original: RawStoredValue): void {
  mmkv.removeValueForKey(key);
  mmkv.removeValueForKey(TYPE_KEY_PREFIX + key);
  if (original.value !== undefined) {
    mmkv.encodeString(key, original.value);
  }
  if (original.valueType !== undefined) {
    mmkv.encodeString(TYPE_KEY_PREFIX + key, original.valueType);
  }
}
