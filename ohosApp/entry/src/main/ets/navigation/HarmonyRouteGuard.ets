import { HarmonyRouteAction, HarmonyRouteRequest, HarmonyRouteTargetType } from './HarmonyRouteRequest';
import { HarmonyRouteCatalog, MAIN_NAVIGATION_ID } from './HarmonyRoutes';

export class HarmonyRouteGuardResult {
  readonly allowed: boolean;
  readonly reason: string;

  constructor(allowed: boolean, reason: string = '') {
    this.allowed = allowed;
    this.reason = reason;
  }
}

/** Pure routing policy shared by the dispatcher and the HMRouter interceptor. */
export class HarmonyRouteGuard {
  checkRequest(request: HarmonyRouteRequest): HarmonyRouteGuardResult {
    if (request.routeKey.trim().length === 0) {
      return new HarmonyRouteGuardResult(false, 'Route key must not be blank.');
    }
    if (request.action === HarmonyRouteAction.BACK_TO) {
      return new HarmonyRouteGuardResult(true);
    }
    if (
      request.targetType === HarmonyRouteTargetType.KUIKLY &&
      !HarmonyRouteCatalog.isAvailableKuiklyPage(request.target)
    ) {
      return new HarmonyRouteGuardResult(
        false,
        `Kuikly page '${request.target}' is not available in this build.`
      );
    }
    return new HarmonyRouteGuardResult(true);
  }

  checkHmRouterCall(
    action: string,
    navigationId: string | undefined,
    pageUrl: string | undefined
  ): HarmonyRouteGuardResult {
    if (navigationId !== undefined && navigationId !== MAIN_NAVIGATION_ID) {
      return new HarmonyRouteGuardResult(
        false,
        `Navigation '${navigationId}' is outside the CityCapsule route container.`
      );
    }
    if (action === 'pop') {
      return new HarmonyRouteGuardResult(true);
    }
    const target: string = pageUrl ?? '';
    if (!HarmonyRouteCatalog.isRegisteredHmRouterPage(target)) {
      return new HarmonyRouteGuardResult(false, `HMRouter page '${target}' is not registered.`);
    }
    return new HarmonyRouteGuardResult(true);
  }

  static readonly shared: HarmonyRouteGuard = new HarmonyRouteGuard();
}
