import { IKRRouterAdapter, KRRecord } from '@kuikly-open/render';
import common from '@ohos.app.ability.common';
import { HarmonyRouteDispatcher } from '../../navigation/HarmonyRouteDispatcher';

export class RouterAdapter implements IKRRouterAdapter {
  openPage(context: common.UIAbilityContext, pageName: string, pageData: KRRecord): void {
    HarmonyRouteDispatcher.shared.openPage(context, pageName, pageData);
  }

  closePage(context: common.UIAbilityContext): void {
    HarmonyRouteDispatcher.shared.back();
  }
}
