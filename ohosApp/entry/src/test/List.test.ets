import localUnitTest from './LocalUnit.test';
import harmonyRouteRequestDecoderTest from './HarmonyRouteRequestDecoder.test';
import harmonyRouteStackCoordinatorTest from './HarmonyRouteStackCoordinator.test';
import harmonyRouteGuardTest from './HarmonyRouteGuard.test';
import harmonyRouteLoggerTest from './HarmonyRouteLogger.test';
import harmonyRouteTransitionsTest from './HarmonyRouteTransitions.test';

export default function testsuite() {
  localUnitTest();
  harmonyRouteRequestDecoderTest();
  harmonyRouteStackCoordinatorTest();
  harmonyRouteGuardTest();
  harmonyRouteLoggerTest();
  harmonyRouteTransitionsTest();
}
