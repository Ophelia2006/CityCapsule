import { HMRouter, HMRouterMgr } from '@hadss/hmrouter';
import { HarmonyRouteDispatcher } from '../navigation/HarmonyRouteDispatcher';
import { HarmonyRouteFallbackParams } from '../navigation/HarmonyRouteParams';
import { HarmonyRouteStackCoordinator } from '../navigation/HarmonyRouteStackCoordinator';
import { ROUTE_FALLBACK_KEY, ROUTE_FALLBACK_PAGE } from '../navigation/HarmonyRoutes';

@HMRouter({ pageUrl: ROUTE_FALLBACK_PAGE })
@Component
export struct RouteFallbackPage {
  private readonly routeParams: HarmonyRouteFallbackParams | null =
    HMRouterMgr.getCurrentParam() as HarmonyRouteFallbackParams | null;
  private routeEntryId: number = -1;

  aboutToAppear(): void {
    this.routeEntryId = HarmonyRouteStackCoordinator.shared.register(ROUTE_FALLBACK_KEY);
  }

  aboutToDisappear(): void {
    HarmonyRouteStackCoordinator.shared.unregister(this.routeEntryId);
  }

  build() {
    Column({ space: 18 }) {
      Text('页面暂时无法打开')
        .fontSize(28)
        .fontWeight(FontWeight.Bold)
        .fontColor('#1F2430')
      Text('路由已被安全拦截，应用仍可继续使用。')
        .fontSize(16)
        .fontColor('#687083')
      Text(`目标：${this.routeParams?.failedTarget ?? 'unknown'}`)
        .fontSize(14)
        .fontColor('#687083')
      Text(`原因：${this.routeParams?.reason ?? 'Route unavailable.'}`)
        .fontSize(14)
        .fontColor('#A13D3D')
      Row({ space: 12 }) {
        Button('返回上一页')
          .onClick(() => HarmonyRouteDispatcher.shared.back())
        Button('返回首页')
          .onClick(() => HarmonyRouteDispatcher.shared.recoverHome())
      }
    }
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Start)
    .width('100%')
    .height('100%')
    .padding(24)
    .backgroundColor('#FFF8F5')
  }
}
