import {
  NATIVE_FILE_IMPORT_PAGE,
  NATIVE_PERMISSION_PAGE
} from './HarmonyRoutes';

/** Maps shared native route targets to HMRouter-only page URLs. */
export class HarmonyNativeRouteRegistry {
  resolve(path: string): string | null {
    switch (path) {
      case '/native/permission':
        return NATIVE_PERMISSION_PAGE;
      case '/native/file-import':
        return NATIVE_FILE_IMPORT_PAGE;
      default:
        return null;
    }
  }

  static readonly shared: HarmonyNativeRouteRegistry = new HarmonyNativeRouteRegistry();
}
