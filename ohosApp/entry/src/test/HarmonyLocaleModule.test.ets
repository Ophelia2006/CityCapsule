import { describe, expect, it } from '@ohos/hypium';
import { formatLocalCalendarDate } from '../main/ets/kuikly/modules/KRLocaleModule';

export default function harmonyLocaleModuleTest() {
  describe('HarmonyLocaleModule', () => {
    it('formatsTheDeviceLocalCalendarDateWithStableDigits', 0, () => {
      const localMidday = new Date(2024, 0, 2, 12, 0, 0, 0).getTime();

      expect(formatLocalCalendarDate(localMidday)).assertEqual('2024-01-02');
    });
  });
}
