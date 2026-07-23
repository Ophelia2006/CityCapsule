import { HMRouter, HMRouterMgr } from '@hadss/hmrouter';
import { ErrorReason, KRRecord, Kuikly } from '@kuikly-open/render';
import { KuiklyViewDelegate } from '../kuikly/KuiklyViewDelegate';
import globalNativeManager from '../kuikly/MyNativeManager';
import { HarmonyKuiklyHostParams } from '../navigation/HarmonyRouteParams';
import { HarmonyRouteStackCoordinator } from '../navigation/HarmonyRouteStackCoordinator';
import { HarmonyThemeHost } from '../designsystem/HarmonyThemeHost';
import { HarmonyThemeProtocol } from '../designsystem/HarmonyThemeProtocol';
import {
  HOME_PAGE_NAME,
  HOME_ROUTE_KEY,
  KUIKLY_HOST_PAGE
} from '../navigation/HarmonyRoutes';

@HMRouter({ pageUrl: KUIKLY_HOST_PAGE })
@Component
export struct KuiklyHostPage {
  private readonly routeParams: HarmonyKuiklyHostParams | null =
    HMRouterMgr.getCurrentParam() as HarmonyKuiklyHostParams | null;
  private readonly pageName: string = this.routeParams?.pageName ?? HOME_PAGE_NAME;
  private readonly pageData: KRRecord = this.createPageData();
  private readonly routeKey: string = this.routeParams?.routeKey ?? HOME_ROUTE_KEY;
  private readonly kuiklyViewDelegate: KuiklyViewDelegate = new KuiklyViewDelegate();
  private routeEntryId: number = -1;
  private themeListenerId: number = -1;
  private pageVisible: boolean = false;
  @State private exception: string = '';

  aboutToAppear(): void {
    AppStorage.setOrCreate<UIContext>('context', this.getUIContext());
    this.routeEntryId = HarmonyRouteStackCoordinator.shared.register(this.routeKey);
    HarmonyThemeHost.shared.removeListener(this.themeListenerId);
    this.themeListenerId = HarmonyThemeHost.shared.addListener((isDark: boolean): void => {
      const eventData: KRRecord = {};
      eventData[HarmonyThemeProtocol.KEY_KUIKLY_NIGHT_MODE] = isDark;
      this.kuiklyViewDelegate.sendEvent(HarmonyThemeProtocol.EVENT_THEME_DID_CHANGED, eventData);
    });
  }

  aboutToDisappear(): void {
    this.updatePageVisibility(false);
    HarmonyRouteStackCoordinator.shared.unregister(this.routeEntryId);
    HarmonyThemeHost.shared.removeListener(this.themeListenerId);
  }

  private updatePageVisibility(visible: boolean): void {
    if (this.pageVisible === visible) {
      return;
    }
    this.pageVisible = visible;
    if (visible) {
      this.kuiklyViewDelegate.pageDidAppear();
    } else {
      this.kuiklyViewDelegate.pageDidDisappear();
    }
  }

  private createPageData(): KRRecord {
    const pageData: KRRecord = this.routeParams?.pageData ?? {};
    const theme = HarmonyThemeHost.shared.bootstrap();
    pageData[HarmonyThemeProtocol.KEY_PROTOCOL_VERSION] = HarmonyThemeProtocol.VERSION;
    pageData[HarmonyThemeProtocol.KEY_THEME_MODE] = theme.themeMode;
    pageData[HarmonyThemeProtocol.KEY_SYSTEM_DARK] = theme.systemDark;
    pageData[HarmonyThemeProtocol.KEY_RESOLVED_DARK] = theme.resolvedDark;
    pageData[HarmonyThemeProtocol.KEY_KUIKLY_NIGHT_MODE] = theme.systemDark;
    return pageData;
  }

  build() {
    Stack() {
      Kuikly({
        pageName: this.pageName,
        pageData: this.pageData,
        delegate: this.kuiklyViewDelegate,
        nativeManager: globalNativeManager,
        onRenderException: (exception: Error, reason: ErrorReason) => {
          this.exception = `${reason}: ${exception.name}\n${exception.message}`;
        }
      })
      if (this.exception.length > 0) {
        Text(this.exception)
          .fontColor(Color.Red)
          .padding(16)
      }
    }
    .width('100%')
    .height('100%')
    .expandSafeArea([SafeAreaType.KEYBOARD])
    .onVisibleAreaChange([0.0, 1.0], (isVisible: boolean, currentRatio: number) => {
      this.updatePageVisibility(isVisible && currentRatio > 0);
    })
  }
}
