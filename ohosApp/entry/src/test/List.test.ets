import localUnitTest from './LocalUnit.test';
import harmonyRouteRequestDecoderTest from './HarmonyRouteRequestDecoder.test';
import harmonyRouteStackCoordinatorTest from './HarmonyRouteStackCoordinator.test';
import harmonyRouteGuardTest from './HarmonyRouteGuard.test';
import harmonyRouteLoggerTest from './HarmonyRouteLogger.test';
import harmonyRouteTransitionsTest from './HarmonyRouteTransitions.test';
import harmonyStorageDispatcherTest from './HarmonyStorageDispatcher.test';
import harmonyStorageMigrationTest from './HarmonyStorageMigration.test';
import harmonyThemeProtocolTest from './HarmonyThemeProtocol.test';
import harmonyLaunchContractTest from './HarmonyLaunchContract.test';

export default function testsuite() {
  localUnitTest();
  harmonyRouteRequestDecoderTest();
  harmonyRouteStackCoordinatorTest();
  harmonyRouteGuardTest();
  harmonyRouteLoggerTest();
  harmonyRouteTransitionsTest();
  harmonyStorageDispatcherTest();
  harmonyStorageMigrationTest();
  harmonyThemeProtocolTest();
  harmonyLaunchContractTest();
}
