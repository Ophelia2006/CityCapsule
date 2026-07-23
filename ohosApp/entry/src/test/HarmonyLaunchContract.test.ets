import { describe, expect, it } from '@ohos/hypium';
import { HarmonyLaunchContract } from '../main/ets/navigation/HarmonyLaunchContract';
import {
  LAUNCH_GATE_PAGE_NAME,
  LAUNCH_GATE_ROUTE_KEY,
  PROFILE_PAGE_NAME,
  PROFILE_ROUTE_KEY
} from '../main/ets/navigation/HarmonyRoutes';

export default function harmonyLaunchContractTest(): void {
  describe('HarmonyLaunchContract', () => {
    it('hostWithoutParamsStartsAtLaunchGate', 0, () => {
      const pageName: string = HarmonyLaunchContract.resolvePageName(undefined);

      expect(pageName).assertEqual(LAUNCH_GATE_PAGE_NAME);
      expect(HarmonyLaunchContract.resolveRouteKey(undefined, pageName))
        .assertEqual(LAUNCH_GATE_ROUTE_KEY);
    });

    it('blankParamsCannotBypassLaunchGate', 0, () => {
      expect(HarmonyLaunchContract.resolvePageName('   '))
        .assertEqual(LAUNCH_GATE_PAGE_NAME);
    });

    it('explicitBusinessRouteRemainsUnchanged', 0, () => {
      expect(HarmonyLaunchContract.resolvePageName(PROFILE_PAGE_NAME))
        .assertEqual(PROFILE_PAGE_NAME);
      expect(HarmonyLaunchContract.resolveRouteKey(PROFILE_ROUTE_KEY, PROFILE_PAGE_NAME))
        .assertEqual(PROFILE_ROUTE_KEY);
    });

    it('missingRouteKeyFallsBackToResolvedPage', 0, () => {
      expect(HarmonyLaunchContract.resolveRouteKey(undefined, PROFILE_PAGE_NAME))
        .assertEqual(PROFILE_PAGE_NAME);
    });
  });
}
