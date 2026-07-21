import { describe, expect, it } from '@ohos/hypium';
import { IHMAnimator } from '@hadss/hmrouter';
import { HarmonyRouteTransitions } from '../main/ets/navigation/HarmonyRouteTransitions';
import { KUIKLY_HOST_PAGE, NATIVE_FILE_IMPORT_PAGE } from '../main/ets/navigation/HarmonyRoutes';

export default function harmonyRouteTransitionsTest() {
  describe('HarmonyRouteTransitions', () => {
    it('definesGlobalStandardTransition', 0, () => {
      const effect: IHMAnimator.Effect = HarmonyRouteTransitions.standard();

      expect(effect.effectOption.duration).assertEqual(260);
      expect(effect.effectOption.direction).assertEqual(IHMAnimator.Direction.RIGHT_TO_LEFT);
    });

    it('overridesOnlySelectedPages', 0, () => {
      expect(HarmonyRouteTransitions.pageOverride(NATIVE_FILE_IMPORT_PAGE) !== null).assertTrue();
      expect(HarmonyRouteTransitions.pageOverride(KUIKLY_HOST_PAGE) === null).assertTrue();
    });
  });
}
