import { KRRecord } from '@kuikly-open/render';
import { HarmonyRouteProtocol } from './HarmonyRoutes';

export enum HarmonyRouteAction {
  PUSH = 'push',
  REPLACE = 'replace',
  BACK_TO = 'backTo'
}

export enum HarmonyRouteTargetType {
  KUIKLY = 'kuikly',
  NATIVE = 'native'
}

export class HarmonyRouteRequest {
  action: HarmonyRouteAction;
  routeKey: string;
  targetType: HarmonyRouteTargetType;
  target: string;
  pageData: KRRecord;

  constructor(
    action: HarmonyRouteAction,
    routeKey: string,
    targetType: HarmonyRouteTargetType,
    target: string,
    pageData: KRRecord
  ) {
    this.action = action;
    this.routeKey = routeKey;
    this.targetType = targetType;
    this.target = target;
    this.pageData = pageData;
  }
}

/** Decodes the wire contract emitted by shared KuiklyAppNavigator. */
export class HarmonyRouteRequestDecoder {
  static decode(pageName: string, pageData: KRRecord): HarmonyRouteRequest {
    const target: string = pageName.trim();
    if (target.length === 0) {
      throw new Error('Harmony route target must not be blank.');
    }

    const action: HarmonyRouteAction = HarmonyRouteRequestDecoder.decodeAction(
      HarmonyRouteRequestDecoder.stringValue(pageData[HarmonyRouteProtocol.PARAM_ACTION])
    );
    const targetType: HarmonyRouteTargetType = HarmonyRouteRequestDecoder.decodeTargetType(
      target,
      HarmonyRouteRequestDecoder.stringValue(pageData[HarmonyRouteProtocol.PARAM_TARGET_TYPE])
    );
    const routeKeyValue: string = HarmonyRouteRequestDecoder.stringValue(
      pageData[HarmonyRouteProtocol.PARAM_ROUTE_KEY]
    ).trim();
    const routeKey: string = routeKeyValue.length > 0 ? routeKeyValue : target;

    return new HarmonyRouteRequest(
      action,
      routeKey,
      targetType,
      target,
      HarmonyRouteRequestDecoder.businessData(pageData)
    );
  }

  private static decodeAction(value: string): HarmonyRouteAction {
    switch (value.trim()) {
      case '':
      case HarmonyRouteAction.PUSH:
        return HarmonyRouteAction.PUSH;
      case HarmonyRouteAction.REPLACE:
        return HarmonyRouteAction.REPLACE;
      case HarmonyRouteAction.BACK_TO:
        return HarmonyRouteAction.BACK_TO;
      default:
        throw new Error(`Unsupported Harmony route action '${value}'.`);
    }
  }

  private static decodeTargetType(target: string, value: string): HarmonyRouteTargetType {
    switch (value.trim()) {
      case '':
        return target.startsWith('/native/')
          ? HarmonyRouteTargetType.NATIVE
          : HarmonyRouteTargetType.KUIKLY;
      case HarmonyRouteProtocol.TARGET_KUIKLY:
        return HarmonyRouteTargetType.KUIKLY;
      case HarmonyRouteProtocol.TARGET_NATIVE:
        if (!target.startsWith('/native/')) {
          throw new Error("Harmony native route target must start with '/native/'.");
        }
        return HarmonyRouteTargetType.NATIVE;
      default:
        throw new Error(`Unsupported Harmony route target type '${value}'.`);
    }
  }

  private static businessData(pageData: KRRecord): KRRecord {
    const result: KRRecord = {};
    Object.keys(pageData).forEach((key: string) => {
      if (!HarmonyRouteProtocol.isReservedKey(key)) {
        result[key] = pageData[key];
      }
    });
    return result;
  }

  private static stringValue(value: Object | null | undefined): string {
    return typeof value === 'string' ? value : '';
  }
}
