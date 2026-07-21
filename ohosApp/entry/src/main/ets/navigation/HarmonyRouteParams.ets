import { KRRecord } from '@kuikly-open/render';

export class HarmonyKuiklyHostParams {
  readonly pageName: string;
  readonly pageData: KRRecord;
  readonly routeKey: string;

  constructor(pageName: string, pageData: KRRecord, routeKey: string) {
    this.pageName = pageName;
    this.pageData = pageData;
    this.routeKey = routeKey;
  }
}

export class HarmonyNativeRouteParams {
  readonly path: string;
  readonly pageData: KRRecord;
  readonly routeKey: string;

  constructor(path: string, pageData: KRRecord, routeKey: string) {
    this.path = path;
    this.pageData = pageData;
    this.routeKey = routeKey;
  }
}

/** Contains diagnostics only; business parameters are deliberately excluded. */
export class HarmonyRouteFallbackParams {
  readonly failedTarget: string;
  readonly failedRouteKey: string;
  readonly reason: string;

  constructor(failedTarget: string, failedRouteKey: string, reason: string) {
    this.failedTarget = failedTarget;
    this.failedRouteKey = failedRouteKey;
    this.reason = reason;
  }
}
