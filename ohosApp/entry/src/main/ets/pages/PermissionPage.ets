import { HMRouter, HMRouterMgr } from '@hadss/hmrouter';
import { HarmonyRouteDispatcher } from '../navigation/HarmonyRouteDispatcher';
import { HarmonyNativeRouteParams } from '../navigation/HarmonyRouteParams';
import { HarmonyRouteStackCoordinator } from '../navigation/HarmonyRouteStackCoordinator';
import {
  NATIVE_PERMISSION_PAGE,
  NATIVE_PERMISSION_ROUTE_KEY
} from '../navigation/HarmonyRoutes';

@HMRouter({ pageUrl: NATIVE_PERMISSION_PAGE })
@Component
export struct PermissionPage {
  private readonly routeParams: HarmonyNativeRouteParams | null =
    HMRouterMgr.getCurrentParam() as HarmonyNativeRouteParams | null;
  private routeEntryId: number = -1;

  aboutToAppear(): void {
    const routeKey: string = this.routeParams?.routeKey ?? NATIVE_PERMISSION_ROUTE_KEY;
    this.routeEntryId = HarmonyRouteStackCoordinator.shared.register(routeKey);
  }

  aboutToDisappear(): void {
    HarmonyRouteStackCoordinator.shared.unregister(this.routeEntryId);
  }

  build() {
    Column({ space: 20 }) {
      Text('权限处理')
        .fontSize(28)
        .fontWeight(FontWeight.Bold)
      Text('HMRouter 原生权限页骨架已接入。具体权限申请将在对应业务阶段注册。')
        .fontSize(16)
        .fontColor('#687083')
      Text(`参数：${JSON.stringify(this.routeParams?.pageData ?? {})}`)
        .fontSize(14)
        .fontColor('#687083')
      Button('返回')
        .onClick(() => HarmonyRouteDispatcher.shared.back())
    }
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Start)
    .width('100%')
    .height('100%')
    .padding(24)
    .backgroundColor('#F5F6FA')
  }
}
