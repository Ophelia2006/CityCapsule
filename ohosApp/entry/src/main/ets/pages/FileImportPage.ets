import { HMRouter, HMRouterMgr } from '@hadss/hmrouter';
import { HarmonyRouteDispatcher } from '../navigation/HarmonyRouteDispatcher';
import { HarmonyNativeRouteParams } from '../navigation/HarmonyRouteParams';
import { HarmonyRouteStackCoordinator } from '../navigation/HarmonyRouteStackCoordinator';
import {
  NATIVE_FILE_IMPORT_PAGE,
  NATIVE_FILE_IMPORT_ROUTE_KEY
} from '../navigation/HarmonyRoutes';

@HMRouter({ pageUrl: NATIVE_FILE_IMPORT_PAGE })
@Component
export struct FileImportPage {
  private readonly routeParams: HarmonyNativeRouteParams | null =
    HMRouterMgr.getCurrentParam() as HarmonyNativeRouteParams | null;
  private routeEntryId: number = -1;

  aboutToAppear(): void {
    const routeKey: string = this.routeParams?.routeKey ?? NATIVE_FILE_IMPORT_ROUTE_KEY;
    this.routeEntryId = HarmonyRouteStackCoordinator.shared.register(routeKey);
  }

  aboutToDisappear(): void {
    HarmonyRouteStackCoordinator.shared.unregister(this.routeEntryId);
  }

  build() {
    Column({ space: 20 }) {
      Text('文件导入')
        .fontSize(28)
        .fontWeight(FontWeight.Bold)
      Text('HMRouter 原生文件导入页骨架已接入。文件选择器将在对应业务阶段实现。')
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
