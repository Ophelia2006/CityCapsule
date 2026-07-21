import { AttributeUpdater } from '@kit.ArkUI';
import { HMNavigation, HMRouterMgr } from '@hadss/hmrouter';
import { HarmonyRouteTransitions } from '../navigation/HarmonyRouteTransitions';
import { KUIKLY_HOST_PAGE, MAIN_NAVIGATION_ID } from '../navigation/HarmonyRoutes';

class MainNavigationModifier extends AttributeUpdater<NavigationAttribute> {
  initializeModifier(instance: NavigationAttribute): void {
    instance.hideNavBar(true);
  }
}

@Entry
@Component
struct Index {
  private readonly navigationModifier: MainNavigationModifier = new MainNavigationModifier();

  aboutToAppear(): void {
    AppStorage.setOrCreate<UIContext>('context', this.getUIContext());
    HMRouterMgr.setDefaultNavigationId(MAIN_NAVIGATION_ID);
  }

  build() {
    Column() {
      HMNavigation({
        navigationId: MAIN_NAVIGATION_ID,
        homePageUrl: KUIKLY_HOST_PAGE,
        options: {
          standardAnimator: HarmonyRouteTransitions.standard(),
          dialogAnimator: HarmonyRouteTransitions.dialog(),
          modifier: this.navigationModifier
        }
      })
    }
    .width('100%')
    .height('100%')
  }
}
