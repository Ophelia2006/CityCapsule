import { describe, expect, it } from '@ohos/hypium';
import { HarmonyRouteGuard, HarmonyRouteGuardResult } from '../main/ets/navigation/HarmonyRouteGuard';
import {
  HarmonyRouteAction,
  HarmonyRouteRequest,
  HarmonyRouteTargetType
} from '../main/ets/navigation/HarmonyRouteRequest';
import {
  KUIKLY_HOST_PAGE,
  LAUNCH_GATE_PAGE_NAME,
  MAIN_NAVIGATION_ID,
  ONBOARDING_PAGE_NAME,
  PLACE_DETAIL_PAGE_NAME,
  PLACE_EDITOR_PAGE_NAME,
  PLACE_LIST_PAGE_NAME,
  PROFILE_PAGE_NAME,
  FAVORITES_PAGE_NAME,
  CAPSULE_EDITOR_PAGE_NAME,
  CAPSULE_DETAIL_PAGE_NAME,
  TIMELINE_PAGE_NAME,
  GALLERY_PAGE_NAME,
  ROUTE_FALLBACK_PAGE
} from '../main/ets/navigation/HarmonyRoutes';

export default function harmonyRouteGuardTest() {
  describe('HarmonyRouteGuard', () => {
    const guard: HarmonyRouteGuard = new HarmonyRouteGuard();

    it('allowsAvailableKuiklyPage', 0, () => {
      const result: HarmonyRouteGuardResult = guard.checkRequest(new HarmonyRouteRequest(
        HarmonyRouteAction.PUSH,
        'settings',
        HarmonyRouteTargetType.KUIKLY,
        'settings',
        {}
      ));

      expect(result.allowed).assertTrue();
    });

    it('allowsLocalProfileAndOnboardingPages', 0, () => {
      [LAUNCH_GATE_PAGE_NAME, ONBOARDING_PAGE_NAME, PROFILE_PAGE_NAME].forEach(
        (pageName: string) => {
          const result: HarmonyRouteGuardResult = guard.checkRequest(
            new HarmonyRouteRequest(
              HarmonyRouteAction.PUSH,
              pageName,
              HarmonyRouteTargetType.KUIKLY,
              pageName,
              {}
            )
          );
          expect(result.allowed).assertTrue();
        }
      );
    });

    it('allowsRegisteredPlacePages', 0, () => {
      [
        PLACE_LIST_PAGE_NAME,
        PLACE_DETAIL_PAGE_NAME,
        PLACE_EDITOR_PAGE_NAME,
        FAVORITES_PAGE_NAME
      ].forEach((pageName: string) => {
        const result: HarmonyRouteGuardResult = guard.checkRequest(new HarmonyRouteRequest(
          HarmonyRouteAction.PUSH,
          pageName,
          HarmonyRouteTargetType.KUIKLY,
          pageName,
          {}
        ));

        expect(result.allowed).assertTrue();
      });
    });

    it('allowsRegisteredRecordPages', 0, () => {
      [
        CAPSULE_EDITOR_PAGE_NAME,
        CAPSULE_DETAIL_PAGE_NAME,
        TIMELINE_PAGE_NAME,
        GALLERY_PAGE_NAME
      ].forEach((pageName: string) => {
        const result: HarmonyRouteGuardResult = guard.checkRequest(new HarmonyRouteRequest(
          HarmonyRouteAction.PUSH,
          pageName,
          HarmonyRouteTargetType.KUIKLY,
          pageName,
          {}
        ));

        expect(result.allowed).assertTrue();
      });
    });

    it('allowsRegisteredHmRouterPages', 0, () => {
      expect(guard.checkHmRouterCall(
        'push', MAIN_NAVIGATION_ID, KUIKLY_HOST_PAGE
      ).allowed).assertTrue();
      expect(guard.checkHmRouterCall(
        'push', MAIN_NAVIGATION_ID, ROUTE_FALLBACK_PAGE
      ).allowed).assertTrue();
    });

    it('rejectsForeignNavigationContainer', 0, () => {
      const result: HarmonyRouteGuardResult = guard.checkHmRouterCall(
        'push', 'foreign_navigation', KUIKLY_HOST_PAGE
      );

      expect(result.allowed).assertFalse();
      expect(result.reason).assertContain('outside');
    });

    it('allowsPopWithoutDestination', 0, () => {
      expect(guard.checkHmRouterCall(
        'pop', MAIN_NAVIGATION_ID, undefined
      ).allowed).assertTrue();
    });
  });
}
