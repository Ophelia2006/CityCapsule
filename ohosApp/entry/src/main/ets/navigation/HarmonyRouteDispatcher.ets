import { common } from '@kit.AbilityKit';
import {
  HMRouterMgr,
  HMRouterPathCallback,
  HMRouterPathInfo,
  IHMAnimator
} from '@hadss/hmrouter';
import { KRRecord } from '@kuikly-open/render';
import { HarmonyBackToDecision, HarmonyBackToPolicy } from './HarmonyBackToPolicy';
import { HarmonyNativeRouteRegistry } from './HarmonyNativeRouteRegistry';
import { HarmonyRouteGuard, HarmonyRouteGuardResult } from './HarmonyRouteGuard';
import { HarmonyRouteLogEntry, HarmonyRouteLogger } from './HarmonyRouteLogger';
import {
  HarmonyKuiklyHostParams,
  HarmonyNativeRouteParams,
  HarmonyRouteFallbackParams
} from './HarmonyRouteParams';
import {
  HarmonyRouteAction,
  HarmonyRouteRequest,
  HarmonyRouteRequestDecoder,
  HarmonyRouteTargetType
} from './HarmonyRouteRequest';
import { HarmonyRouteStackCoordinator } from './HarmonyRouteStackCoordinator';
import { HarmonyRouteTransitions } from './HarmonyRouteTransitions';
import {
  HOME_PAGE_NAME,
  HOME_ROUTE_KEY,
  KUIKLY_HOST_PAGE,
  MAIN_NAVIGATION_ID,
  ROUTE_FALLBACK_PAGE
} from './HarmonyRoutes';

/**
 * The only HarmonyOS class allowed to choose between the Kuikly host and HMRouter-native pages.
 */
export class HarmonyRouteDispatcher {
  private readonly stackCoordinator: HarmonyRouteStackCoordinator;
  private readonly nativeRouteRegistry: HarmonyNativeRouteRegistry;
  private readonly guard: HarmonyRouteGuard;
  private readonly logger: HarmonyRouteLogger;

  constructor(
    stackCoordinator: HarmonyRouteStackCoordinator,
    nativeRouteRegistry: HarmonyNativeRouteRegistry,
    guard: HarmonyRouteGuard = HarmonyRouteGuard.shared,
    logger: HarmonyRouteLogger = HarmonyRouteLogger.shared
  ) {
    this.stackCoordinator = stackCoordinator;
    this.nativeRouteRegistry = nativeRouteRegistry;
    this.guard = guard;
    this.logger = logger;
  }

  openPage(context: common.UIAbilityContext, pageName: string, pageData: KRRecord): boolean {
    // HMRouter owns the UIAbility context after initialization, but this boundary keeps the
    // Kuikly adapter contract explicit and prevents calls after the Ability has been released.
    if (context === null) {
      return this.fail('', '', pageName, 'Router context is unavailable.');
    }
    try {
      const request: HarmonyRouteRequest = HarmonyRouteRequestDecoder.decode(pageName, pageData);
      this.logger.record(new HarmonyRouteLogEntry(
        'request', request.action, request.routeKey, request.target, 'decoded'
      ));
      return this.dispatch(request);
    } catch (error) {
      return this.fail('', '', pageName, this.errorMessage(error));
    }
  }

  back(): boolean {
    if (!HMRouterMgr.isInit) {
      return this.fail('pop', '', '', 'HMRouter is not initialized.');
    }
    this.logger.record(new HarmonyRouteLogEntry('back', 'pop', '', '', 'accepted'));
    HMRouterMgr.pop({ navigationId: MAIN_NAVIGATION_ID });
    return true;
  }

  recoverHome(): boolean {
    if (!HMRouterMgr.isInit) {
      return this.fail(HarmonyRouteAction.REPLACE, HOME_ROUTE_KEY, HOME_PAGE_NAME,
        'HMRouter is not initialized.');
    }
    return this.openKuikly(new HarmonyRouteRequest(
      HarmonyRouteAction.REPLACE,
      HOME_ROUTE_KEY,
      HarmonyRouteTargetType.KUIKLY,
      HOME_PAGE_NAME,
      {}
    ));
  }

  dispatch(request: HarmonyRouteRequest): boolean {
    if (!HMRouterMgr.isInit) {
      return this.fail(
        request.action,
        request.routeKey,
        request.target,
        'HMRouter is not initialized.'
      );
    }

    const guardResult: HarmonyRouteGuardResult = this.guard.checkRequest(request);
    if (!guardResult.allowed) {
      return this.fail(request.action, request.routeKey, request.target, guardResult.reason);
    }

    if (request.action === HarmonyRouteAction.BACK_TO) {
      return this.backTo(request);
    }

    switch (request.targetType) {
      case HarmonyRouteTargetType.KUIKLY:
        return this.openKuikly(request);
      case HarmonyRouteTargetType.NATIVE:
        return this.openNative(request);
      default:
        return this.fail(request.action, request.routeKey, request.target, 'Unknown target type.');
    }
  }

  private openKuikly(request: HarmonyRouteRequest): boolean {
    const params: HarmonyKuiklyHostParams = new HarmonyKuiklyHostParams(
      request.target,
      request.pageData,
      request.routeKey
    );
    this.openHmRouterPage(request, KUIKLY_HOST_PAGE, params);
    return true;
  }

  private openNative(request: HarmonyRouteRequest): boolean {
    const pageUrl: string | null = this.nativeRouteRegistry.resolve(request.target);
    if (pageUrl === null) {
      return this.fail(
        request.action,
        request.routeKey,
        request.target,
        `Native route '${request.target}' is not registered.`
      );
    }
    const params: HarmonyNativeRouteParams = new HarmonyNativeRouteParams(
      request.target,
      request.pageData,
      request.routeKey
    );
    this.openHmRouterPage(request, pageUrl, params);
    return true;
  }

  private openHmRouterPage(request: HarmonyRouteRequest, pageUrl: string, params: Object): void {
    const pathInfo: HMRouterPathInfo = {
      navigationId: MAIN_NAVIGATION_ID,
      pageUrl: pageUrl,
      param: params
    };
    const animator: IHMAnimator | null = HarmonyRouteTransitions.pageOverride(pageUrl);
    if (animator !== null) {
      pathInfo.animator = animator;
    }
    const callback: HMRouterPathCallback = {
      onArrival: () => {
        this.logger.record(new HarmonyRouteLogEntry(
          'arrival', request.action, request.routeKey, request.target, 'success'
        ));
      },
      onLost: () => {
        const reason: string = `HMRouter page '${pageUrl}' could not be found.`;
        this.logger.failed(new HarmonyRouteLogEntry(
          'lost', request.action, request.routeKey, request.target, 'failed', reason
        ));
        this.showFallback(request.target, request.routeKey, reason);
      }
    };
    this.logger.record(new HarmonyRouteLogEntry(
      'dispatch', request.action, request.routeKey, request.target, 'accepted'
    ));
    if (request.action === HarmonyRouteAction.REPLACE) {
      HMRouterMgr.replace(pathInfo, callback);
      return;
    }
    HMRouterMgr.push(pathInfo, callback);
  }

  private backTo(request: HarmonyRouteRequest): boolean {
    const closedCount: number = this.stackCoordinator.backTo(request.routeKey);
    if (HarmonyBackToPolicy.decide(closedCount) === HarmonyBackToDecision.REPLACE_TARGET) {
      this.logger.record(new HarmonyRouteLogEntry(
        'backTo',
        HarmonyRouteAction.BACK_TO,
        request.routeKey,
        request.target,
        'replace_missing_target',
        `Target '${request.routeKey}' is absent from the Harmony route stack.`
      ));
      return this.replaceMissingBackToTarget(request);
    }
    this.logger.record(new HarmonyRouteLogEntry(
      'backTo', HarmonyRouteAction.BACK_TO, request.routeKey, request.target, 'accepted'
    ));
    if (closedCount === 0) {
      return true;
    }
    HMRouterMgr.pop(
      { navigationId: MAIN_NAVIGATION_ID },
      closedCount - 1
    );
    return true;
  }

  private replaceMissingBackToTarget(request: HarmonyRouteRequest): boolean {
    const replacement: HarmonyRouteRequest = new HarmonyRouteRequest(
      HarmonyRouteAction.REPLACE,
      request.routeKey,
      request.targetType,
      request.target,
      request.pageData
    );
    const guardResult: HarmonyRouteGuardResult = this.guard.checkRequest(replacement);
    if (!guardResult.allowed) {
      return this.fail(
        replacement.action,
        replacement.routeKey,
        replacement.target,
        guardResult.reason
      );
    }
    switch (replacement.targetType) {
      case HarmonyRouteTargetType.KUIKLY:
        return this.openKuikly(replacement);
      case HarmonyRouteTargetType.NATIVE:
        return this.openNative(replacement);
      default:
        return this.fail(
          replacement.action,
          replacement.routeKey,
          replacement.target,
          'Unknown target type.'
        );
    }
  }

  private fail(action: string, routeKey: string, target: string, reason: string): boolean {
    this.logger.failed(new HarmonyRouteLogEntry(
      'route_failure', action, routeKey, target, 'fallback', reason
    ));
    if (HMRouterMgr.isInit) {
      this.showFallback(target, routeKey, reason);
    }
    return false;
  }

  private showFallback(failedTarget: string, failedRouteKey: string, reason: string): void {
    const pathInfo: HMRouterPathInfo = {
      navigationId: MAIN_NAVIGATION_ID,
      pageUrl: ROUTE_FALLBACK_PAGE,
      param: new HarmonyRouteFallbackParams(failedTarget, failedRouteKey, reason)
    };
    const animator: IHMAnimator | null = HarmonyRouteTransitions.pageOverride(ROUTE_FALLBACK_PAGE);
    if (animator !== null) {
      pathInfo.animator = animator;
    }
    HMRouterMgr.push(pathInfo, {
      onArrival: () => {
        this.logger.record(new HarmonyRouteLogEntry(
          'fallback', 'push', failedRouteKey, failedTarget, 'visible', reason
        ));
      },
      onLost: () => {
        this.logger.failed(new HarmonyRouteLogEntry(
          'fallback', 'push', failedRouteKey, failedTarget, 'lost',
          'The fallback page itself is not registered.'
        ));
      }
    });
  }

  private errorMessage(error: Object): string {
    if (error instanceof Error) {
      return error.message;
    }
    return JSON.stringify(error) ?? 'Unknown route error.';
  }

  static readonly shared: HarmonyRouteDispatcher = new HarmonyRouteDispatcher(
    HarmonyRouteStackCoordinator.shared,
    HarmonyNativeRouteRegistry.shared
  );
}
