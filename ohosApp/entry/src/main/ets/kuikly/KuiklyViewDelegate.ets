import {
  IKuiklyViewDelegate,
  KRRenderModuleExportCreator,
  KRRenderViewExportCreator
} from '@kuikly-open/render';
import { KRMyView } from './components/KRMyView';
import { KRAmapView } from './components/KRAmapView';
import { KRBridgeModule } from './modules/KRBridgeModule';
import { KRMyModule } from './modules/KRMyModule';
import { KRStorageModule } from './modules/KRStorageModule';
import { KRThemeHostModule } from './modules/KRThemeHostModule';
import { KRMediaModule } from './modules/KRMediaModule';
import { KRLocaleModule } from './modules/KRLocaleModule';
import { KRDataArchiveModule } from './modules/KRDataArchiveModule';
import { KRLocationModule } from './modules/KRLocationModule';
import { KRExternalNavigationModule } from './modules/KRExternalNavigationModule';
import hilog from '@ohos.hilog';

export class KuiklyViewDelegate extends IKuiklyViewDelegate {
  getCustomRenderViewCreatorRegisterMap(): Map<string, KRRenderViewExportCreator> {
    const map: Map<string, KRRenderViewExportCreator> = new Map();
    map.set(KRMyView.VIEW_NAME, () => new KRMyView());
    map.set(KRAmapView.VIEW_NAME, () => {
      hilog.info(0x0000, 'CityCapsuleMap', '%{public}s', 'platform_view_creator_invoked');
      return new KRAmapView();
    });
    return map;
  }

  getCustomRenderModuleCreatorRegisterMap(): Map<string, KRRenderModuleExportCreator> {
    const map: Map<string, KRRenderModuleExportCreator> = new Map();
    map.set(KRBridgeModule.MODULE_NAME, () => new KRBridgeModule());
    map.set(KRMyModule.MODULE_NAME, () => new KRMyModule())
    map.set(KRStorageModule.MODULE_NAME, () => new KRStorageModule())
    map.set(KRThemeHostModule.MODULE_NAME, () => new KRThemeHostModule())
    map.set(KRMediaModule.MODULE_NAME, () => new KRMediaModule())
    map.set(KRLocaleModule.MODULE_NAME, () => new KRLocaleModule())
    map.set(KRDataArchiveModule.MODULE_NAME, () => new KRDataArchiveModule())
    map.set(KRLocationModule.MODULE_NAME, () => new KRLocationModule())
    map.set(KRExternalNavigationModule.MODULE_NAME, () => new KRExternalNavigationModule())
    return map;
  }
}
