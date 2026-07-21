import {
  HMActionType,
  HMInterceptor,
  HMInterceptorAction,
  HMInterceptorInfo,
  HMRouterMgr,
  IHMAnimator,
  IHMInterceptor
} from '@hadss/hmrouter';
import { HarmonyRouteGuard, HarmonyRouteGuardResult } from './HarmonyRouteGuard';
import { HarmonyRouteLogEntry, HarmonyRouteLogger } from './HarmonyRouteLogger';
import { HarmonyRouteFallbackParams } from './HarmonyRouteParams';
import { HarmonyRouteTransitions } from './HarmonyRouteTransitions';
import {
  GLOBAL_ROUTE_INTERCEPTOR,
  MAIN_NAVIGATION_ID,
  ROUTE_FALLBACK_PAGE
} from './HarmonyRoutes';

@HMInterceptor({
  interceptorName: GLOBAL_ROUTE_INTERCEPTOR,
  priority: 100,
  global: true
})
export class HarmonyGlobalRouteInterceptor implements IHMInterceptor {
  handle(info: HMInterceptorInfo): HMInterceptorAction {
    const action: string = info.type.toString();
    const result: HarmonyRouteGuardResult = HarmonyRouteGuard.shared.checkHmRouterCall(
      action,
      info.routerPathInfo.navigationId,
      info.routerPathInfo.pageUrl
    );
    const target: string = info.routerPathInfo.pageUrl ?? info.targetName ?? '';
    if (result.allowed) {
      HarmonyRouteLogger.shared.record(new HarmonyRouteLogEntry(
        'interceptor', action, '', target, 'allowed'
      ));
      return HMInterceptorAction.DO_NEXT;
    }

    HarmonyRouteLogger.shared.rejected(new HarmonyRouteLogEntry(
      'interceptor', action, '', target, 'rejected', result.reason
    ));
    if (action !== HMActionType.POP && target !== ROUTE_FALLBACK_PAGE) {
      const animator: IHMAnimator | null = HarmonyRouteTransitions.pageOverride(ROUTE_FALLBACK_PAGE);
      HMRouterMgr.push({
        navigationId: MAIN_NAVIGATION_ID,
        pageUrl: ROUTE_FALLBACK_PAGE,
        param: new HarmonyRouteFallbackParams(target, '', result.reason),
        animator: animator ?? false,
        skipAllInterceptor: true
      });
    }
    return HMInterceptorAction.DO_REJECT;
  }
}
