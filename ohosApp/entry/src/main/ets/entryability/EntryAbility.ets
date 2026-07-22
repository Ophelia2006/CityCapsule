import AbilityConstant from '@ohos.app.ability.AbilityConstant';
import hilog from '@ohos.hilog';
import UIAbility from '@ohos.app.ability.UIAbility';
import Want from '@ohos.app.ability.Want';
import window from '@ohos.window';
import { KuiklyRenderAdapterManager } from '@kuikly-open/render';
import { HMRouterMgr } from '@hadss/hmrouter';
import { LogAdapter } from '../kuikly/adapter/LogAdapter';
import { RouterAdapter } from '../kuikly/adapter/RouterAdapter';
import { HarmonyRouteLogger } from '../navigation/HarmonyRouteLogger';
import { HarmonyRouteStackCoordinator } from '../navigation/HarmonyRouteStackCoordinator';
import { HarmonyMmkvStorage } from '../storage/HarmonyMmkvStorage';
import {
  HarmonyPreferencesLegacySettingsSource,
  HarmonyStorageMigrator
} from '../storage/HarmonyStorageMigration';

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onCreate');
    const storageInitialization = HarmonyMmkvStorage.shared.initialize(
      this.context.getApplicationContext()
    );
    if (!storageInitialization.success) {
      hilog.error(
        0x0000,
        'CityCapsuleStorage',
        'Storage initialization failed: %{public}s',
        storageInitialization.message ?? 'unknown error'
      );
    } else {
      try {
        const migration = new HarmonyStorageMigrator(
          HarmonyMmkvStorage.shared,
          new HarmonyPreferencesLegacySettingsSource(this.context.getApplicationContext())
        ).migrate();
        hilog.info(
          0x0000,
          'CityCapsuleStorage',
          'Storage migration outcome=%{public}s, attempt=%{public}d, migratedTheme=%{public}s, reason=%{public}s',
          migration.outcome,
          migration.attempt,
          migration.migratedTheme.toString(),
          migration.reason ?? ''
        );
      } catch (error) {
        const message = error instanceof Error ? error.message : 'legacy_source_unavailable';
        hilog.error(
          0x0000,
          'CityCapsuleStorage',
          'Storage migration skipped safely: %{public}s',
          message
        );
      }
    }
  }

  onDestroy(): void {
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onDestroy');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    // Main window is created, set main page for this ability
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onWindowStageCreate');
    windowStage.getMainWindowSync().setWindowLayoutFullScreen(true);

    KuiklyRenderAdapterManager.krLogAdapter = new LogAdapter();
    KuiklyRenderAdapterManager.krRouterAdapter = new RouterAdapter();

    HMRouterMgr.init({
      context: this.context,
      logLevel: 'DEBUG',
      customLog: HarmonyRouteLogger.shared
    }).then(() => {
      windowStage.loadContent('pages/Index', (err, data) => {
        if (err.code) {
          hilog.error(0x0000, 'testTag', 'Failed to load the content. Cause: %{public}s', JSON.stringify(err) ?? '');
          return;
        }
        hilog.info(0x0000, 'testTag', 'Succeeded in loading the content. Data: %{public}s', JSON.stringify(data) ?? '');
      });
    }).catch((error: Error) => {
      hilog.error(0x0000, 'testTag', 'Failed to initialize HMRouter: %{public}s', error.message);
    });
  }

  onWindowStageDestroy(): void {
    // Main window is destroyed, release UI related resources
    HarmonyRouteStackCoordinator.shared.clear();
    HMRouterMgr.clearDefaultNavigationId();
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onWindowStageDestroy');
  }

  onForeground(): void {
    // Ability has brought to foreground
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onForeground');
  }

  onBackground(): void {
    // Ability has back to background
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onBackground');
  }
}
