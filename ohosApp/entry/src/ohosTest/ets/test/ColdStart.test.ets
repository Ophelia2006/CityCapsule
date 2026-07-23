import { abilityDelegatorRegistry } from '@kit.TestKit';
import { describe, expect, it } from '@ohos/hypium';
import { MMKV } from '@tencent/mmkv';
import { HarmonyLaunchContract } from '../../../main/ets/navigation/HarmonyLaunchContract';
import {
  LAUNCH_GATE_PAGE_NAME,
  LAUNCH_GATE_ROUTE_KEY
} from '../../../main/ets/navigation/HarmonyRoutes';
import { HarmonyStorageProtocol } from '../../../main/ets/storage/HarmonyStorageProtocol';

const PROFILE_KEY: string = 'profile.local_profile';
const COMPLETION_KEY: string = 'onboarding.completed_version';
const TYPE_KEY_PREFIX: string = '__cc_type__.';
const DEFAULT_PROFILE_JSON: string =
  '{"schemaVersion":1,"displayName":"城市漫游者","avatarPreset":"sky"}';

class RawStoredValue {
  readonly value: string | undefined;
  readonly valueType: string | undefined;

  constructor(value: string | undefined, valueType: string | undefined) {
    this.value = value;
    this.valueType = valueType;
  }
}

export default function coldStartTest(): void {
  describe('ColdStartDeviceTest', () => {
    it('parameterlessHarmonyHostStartsAtLaunchGate', 0, () => {
      const pageName: string = HarmonyLaunchContract.resolvePageName(undefined);

      expect(pageName).assertEqual(LAUNCH_GATE_PAGE_NAME);
      expect(HarmonyLaunchContract.resolveRouteKey(undefined, pageName))
        .assertEqual(LAUNCH_GATE_ROUTE_KEY);
    });

    it('completedProfileMarkersSurviveMmkvReopen', 0, () => {
      const applicationContext = abilityDelegatorRegistry.getAbilityDelegator()
        .getAppContext()
        .getApplicationContext();
      MMKV.initialize(applicationContext);
      const first = MMKV.mmkvWithID(
        HarmonyStorageProtocol.STORE_PREFERENCES,
        MMKV.SINGLE_PROCESS_MODE
      );
      const originalProfile: RawStoredValue = snapshot(first, PROFILE_KEY);
      const originalCompletion: RawStoredValue = snapshot(first, COMPLETION_KEY);

      try {
        expect(first.encodeString(PROFILE_KEY, DEFAULT_PROFILE_JSON)).assertTrue();
        expect(first.encodeString(
          TYPE_KEY_PREFIX + PROFILE_KEY,
          HarmonyStorageProtocol.TYPE_JSON_OBJECT
        )).assertTrue();
        expect(first.encodeString(COMPLETION_KEY, '1')).assertTrue();
        expect(first.encodeString(
          TYPE_KEY_PREFIX + COMPLETION_KEY,
          HarmonyStorageProtocol.TYPE_LONG
        )).assertTrue();

        first.clearMemoryCache();
        const reopened = MMKV.mmkvWithID(
          HarmonyStorageProtocol.STORE_PREFERENCES,
          MMKV.SINGLE_PROCESS_MODE
        );

        expect(reopened.decodeString(PROFILE_KEY)).assertEqual(DEFAULT_PROFILE_JSON);
        expect(reopened.decodeString(TYPE_KEY_PREFIX + PROFILE_KEY))
          .assertEqual(HarmonyStorageProtocol.TYPE_JSON_OBJECT);
        expect(reopened.decodeString(COMPLETION_KEY)).assertEqual('1');
        expect(reopened.decodeString(TYPE_KEY_PREFIX + COMPLETION_KEY))
          .assertEqual(HarmonyStorageProtocol.TYPE_LONG);

        restore(reopened, PROFILE_KEY, originalProfile);
        restore(reopened, COMPLETION_KEY, originalCompletion);
      } catch (error) {
        const reopened = MMKV.mmkvWithID(
          HarmonyStorageProtocol.STORE_PREFERENCES,
          MMKV.SINGLE_PROCESS_MODE
        );
        restore(reopened, PROFILE_KEY, originalProfile);
        restore(reopened, COMPLETION_KEY, originalCompletion);
        const message: string = error instanceof Error
          ? error.message
          : 'Unknown cold-start persistence test failure.';
        throw new Error(message);
      }
    });
  });
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
