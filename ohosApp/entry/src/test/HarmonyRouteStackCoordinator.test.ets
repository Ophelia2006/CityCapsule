import { describe, expect, it } from '@ohos/hypium';
import { HarmonyBackToDecision, HarmonyBackToPolicy } from '../main/ets/navigation/HarmonyBackToPolicy';
import { HarmonyRouteStackCoordinator } from '../main/ets/navigation/HarmonyRouteStackCoordinator';

export default function harmonyRouteStackCoordinatorTest(): void {
  describe('HarmonyRouteStackCoordinator', () => {
    it('backToClosesOnlyEntriesAboveTarget', 0, () => {
      const coordinator = new HarmonyRouteStackCoordinator();
      coordinator.register('home');
      coordinator.register('settings');
      coordinator.register('place_detail');

      expect(coordinator.backTo('home')).assertEqual(2);
      expect(coordinator.snapshotRouteKeys().join(',')).assertEqual('home');
    });

    it('backToUsesLatestMatchingRouteInstance', 0, () => {
      const coordinator = new HarmonyRouteStackCoordinator();
      coordinator.register('home');
      coordinator.register('settings');
      coordinator.register('home');
      coordinator.register('place_detail');

      expect(coordinator.backTo('home')).assertEqual(1);
      expect(coordinator.snapshotRouteKeys().join(',')).assertEqual('home,settings,home');
    });

    it('backToCurrentRouteIsNoOp', 0, () => {
      const coordinator = new HarmonyRouteStackCoordinator();
      coordinator.register('home');

      expect(coordinator.backTo('home')).assertEqual(0);
      expect(coordinator.snapshotRouteKeys().join(',')).assertEqual('home');
    });

    it('missingTargetDoesNotMutateStack', 0, () => {
      const coordinator = new HarmonyRouteStackCoordinator();
      coordinator.register('home');
      coordinator.register('settings');

      expect(coordinator.backTo('missing')).assertEqual(-1);
      expect(coordinator.snapshotRouteKeys().join(',')).assertEqual('home,settings');
    });

    it('missingBackToTargetIsRecoveredWithReplace', 0, () => {
      const coordinator = new HarmonyRouteStackCoordinator();
      coordinator.register('settings');

      const decision: HarmonyBackToDecision = HarmonyBackToPolicy.decide(
        coordinator.backTo('home')
      );

      expect(decision).assertEqual(HarmonyBackToDecision.REPLACE_TARGET);
      expect(coordinator.snapshotRouteKeys().join(',')).assertEqual('settings');
    });

    it('existingBackToTargetCompletesWithoutReplace', 0, () => {
      const coordinator = new HarmonyRouteStackCoordinator();
      coordinator.register('home');
      coordinator.register('settings');

      const decision: HarmonyBackToDecision = HarmonyBackToPolicy.decide(
        coordinator.backTo('home')
      );

      expect(decision).assertEqual(HarmonyBackToDecision.COMPLETE);
      expect(coordinator.snapshotRouteKeys().join(',')).assertEqual('home');
    });

    it('pushBackReplaceAndBackToFollowPageLifecycle', 0, () => {
      const coordinator = new HarmonyRouteStackCoordinator();
      coordinator.register('home');
      const settingsId: number = coordinator.register('settings');
      coordinator.unregister(settingsId); // back
      const detailId: number = coordinator.register('place_detail');
      coordinator.register('settings');
      coordinator.unregister(detailId); // replace, after replacement page appeared
      coordinator.register('gallery');

      expect(coordinator.snapshotRouteKeys().join(',')).assertEqual('home,settings,gallery');
      expect(coordinator.backTo('settings')).assertEqual(1);
      expect(coordinator.snapshotRouteKeys().join(',')).assertEqual('home,settings');
    });

    it('unregisterAndClearAreIdempotent', 0, () => {
      const coordinator = new HarmonyRouteStackCoordinator();
      const homeId: number = coordinator.register('home');

      coordinator.unregister(homeId);
      coordinator.unregister(homeId);
      expect(coordinator.snapshotRouteKeys().length).assertEqual(0);

      coordinator.register('home');
      coordinator.clear();
      coordinator.clear();
      expect(coordinator.snapshotRouteKeys().length).assertEqual(0);
    });
  });
}
