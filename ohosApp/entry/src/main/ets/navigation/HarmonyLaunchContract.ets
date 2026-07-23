import {
  LAUNCH_GATE_PAGE_NAME,
  LAUNCH_GATE_ROUTE_KEY
} from './HarmonyRoutes';

/**
 * HarmonyOS launcher contract.
 *
 * HMNavigation creates its home Kuikly host without business params. That one
 * case must enter LaunchGate; explicit HMRouter requests keep their page and
 * stable route key unchanged.
 */
export class HarmonyLaunchContract {
  static readonly DEFAULT_PAGE_NAME: string = LAUNCH_GATE_PAGE_NAME;
  static readonly DEFAULT_ROUTE_KEY: string = LAUNCH_GATE_ROUTE_KEY;

  static resolvePageName(requestedPageName: string | undefined): string {
    const normalized: string = requestedPageName?.trim() ?? '';
    return normalized.length > 0
      ? normalized
      : HarmonyLaunchContract.DEFAULT_PAGE_NAME;
  }

  static resolveRouteKey(
    requestedRouteKey: string | undefined,
    resolvedPageName: string
  ): string {
    const normalized: string = requestedRouteKey?.trim() ?? '';
    return normalized.length > 0 ? normalized : resolvedPageName;
  }
}
