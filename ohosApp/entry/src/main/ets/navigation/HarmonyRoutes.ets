/**
 * HarmonyOS route constants.
 *
 * Shared route targets remain platform-neutral. HMRouter page URLs live only in the
 * HarmonyOS entry module and must never leak into shared feature code.
 */
export const MAIN_NAVIGATION_ID: string = 'city_capsule_main_navigation';

export const KUIKLY_HOST_PAGE: string = 'citycapsule/kuikly-host';
export const NATIVE_PERMISSION_PAGE: string = 'citycapsule/native/permission';
export const NATIVE_FILE_IMPORT_PAGE: string = 'citycapsule/native/file-import';
export const ROUTE_FALLBACK_PAGE: string = 'citycapsule/route-fallback';

export const GLOBAL_ROUTE_INTERCEPTOR: string = 'citycapsule_global_route_interceptor';

export const LAUNCH_GATE_ROUTE_KEY: string = 'launch_gate';
export const LAUNCH_GATE_PAGE_NAME: string = 'launch_gate';
export const ONBOARDING_ROUTE_KEY: string = 'onboarding';
export const ONBOARDING_PAGE_NAME: string = 'onboarding';
export const HOME_ROUTE_KEY: string = 'home';
export const HOME_PAGE_NAME: string = 'home';
export const PROFILE_ROUTE_KEY: string = 'profile';
export const PROFILE_PAGE_NAME: string = 'profile';
export const NATIVE_PERMISSION_ROUTE_KEY: string = 'native_permission';
export const NATIVE_FILE_IMPORT_ROUTE_KEY: string = 'native_file_import';
export const ROUTE_FALLBACK_KEY: string = 'route_fallback';
export const ROUTER_DIAGNOSTICS_PAGE_NAME: string = 'router';
export const IMAGE_ADAPTER_DIAGNOSTICS_PAGE_NAME: string = 'image_adapter';

/**
 * HarmonyOS cannot import Kotlin route constants, so this catalog mirrors only the
 * platform boundary that has actually been registered in the current milestone.
 */
export class HarmonyRouteCatalog {
  static isAvailableKuiklyPage(pageName: string): boolean {
    switch (pageName) {
      case LAUNCH_GATE_PAGE_NAME:
      case ONBOARDING_PAGE_NAME:
      case HOME_PAGE_NAME:
      case PROFILE_PAGE_NAME:
      case 'settings':
      case ROUTER_DIAGNOSTICS_PAGE_NAME:
      case IMAGE_ADAPTER_DIAGNOSTICS_PAGE_NAME:
        return true;
      default:
        return false;
    }
  }

  static isRegisteredHmRouterPage(pageUrl: string): boolean {
    switch (pageUrl) {
      case KUIKLY_HOST_PAGE:
      case NATIVE_PERMISSION_PAGE:
      case NATIVE_FILE_IMPORT_PAGE:
      case ROUTE_FALLBACK_PAGE:
        return true;
      default:
        return false;
    }
  }
}

export class HarmonyRouteProtocol {
  static readonly RESERVED_PREFIX: string = '__cc_';
  static readonly PARAM_ACTION: string = '__cc_route_action';
  static readonly PARAM_ROUTE_KEY: string = '__cc_route_key';
  static readonly PARAM_TARGET_TYPE: string = '__cc_target_type';

  static readonly TARGET_KUIKLY: string = 'kuikly';
  static readonly TARGET_NATIVE: string = 'native';

  static isReservedKey(key: string): boolean {
    return key.startsWith(HarmonyRouteProtocol.RESERVED_PREFIX);
  }
}
