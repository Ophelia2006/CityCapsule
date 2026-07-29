import { describe, expect, it } from '@ohos/hypium';
import { HarmonyLaunchContract } from '../main/ets/navigation/HarmonyLaunchContract';
import {
  APP_SHELL_PAGE_NAME,
  APP_SHELL_ROUTE_KEY,
  LAUNCH_GATE_PAGE_NAME,
  LAUNCH_GATE_ROUTE_KEY
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
      expect(HarmonyLaunchContract.resolvePageName(APP_SHELL_PAGE_NAME))
        .assertEqual(APP_SHELL_PAGE_NAME);
      expect(HarmonyLaunchContract.resolveRouteKey(APP_SHELL_ROUTE_KEY, APP_SHELL_PAGE_NAME))
        .assertEqual(APP_SHELL_ROUTE_KEY);
    });

    it('missingRouteKeyFallsBackToResolvedPage', 0, () => {
      expect(HarmonyLaunchContract.resolveRouteKey(undefined, APP_SHELL_PAGE_NAME))
        .assertEqual(APP_SHELL_PAGE_NAME);
    });
  });
}
