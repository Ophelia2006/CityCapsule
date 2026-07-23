import { describe, expect, it } from '@ohos/hypium';
import {
  HarmonyThemeProtocol,
  normalizeHarmonyThemeMode,
  resolveHarmonyTheme
} from '../main/ets/designsystem/HarmonyThemeProtocol';

export default function harmonyThemeProtocolTest() {
  describe('HarmonyThemeProtocol', () => {
    it('normalizesMissingAndInvalidValuesToSystem', 0, () => {
      expect(normalizeHarmonyThemeMode(undefined)).assertEqual(HarmonyThemeProtocol.MODE_SYSTEM);
      expect(normalizeHarmonyThemeMode('invalid')).assertEqual(HarmonyThemeProtocol.MODE_SYSTEM);
    });

    it('forcedModesOverrideSystemAppearance', 0, () => {
      expect(resolveHarmonyTheme(HarmonyThemeProtocol.MODE_LIGHT, true)).assertFalse();
      expect(resolveHarmonyTheme(HarmonyThemeProtocol.MODE_DARK, false)).assertTrue();
    });

    it('systemModeTracksPlatformAppearance', 0, () => {
      expect(resolveHarmonyTheme(HarmonyThemeProtocol.MODE_SYSTEM, true)).assertTrue();
      expect(resolveHarmonyTheme(HarmonyThemeProtocol.MODE_SYSTEM, false)).assertFalse();
    });

    it('wireContractStaysAlignedWithSharedVersionOne', 0, () => {
      expect(HarmonyThemeProtocol.VERSION).assertEqual(1);
      expect(HarmonyThemeProtocol.KEY_PROTOCOL_VERSION).assertEqual('themeProtocolVersion');
      expect(HarmonyThemeProtocol.KEY_THEME_MODE).assertEqual('themeMode');
      expect(HarmonyThemeProtocol.KEY_SYSTEM_DARK).assertEqual('systemDark');
      expect(HarmonyThemeProtocol.KEY_RESOLVED_DARK).assertEqual('resolvedDark');
      expect(HarmonyThemeProtocol.KEY_KUIKLY_NIGHT_MODE).assertEqual('isNightMode');
      expect(HarmonyThemeProtocol.MODULE_NAME).assertEqual('CCThemeHostModule');
      expect(HarmonyThemeProtocol.METHOD_APPLY_APPEARANCE).assertEqual('applyAppearance');
      expect(HarmonyThemeProtocol.EVENT_THEME_DID_CHANGED).assertEqual('themeDidChanged');
    });
  });
}
