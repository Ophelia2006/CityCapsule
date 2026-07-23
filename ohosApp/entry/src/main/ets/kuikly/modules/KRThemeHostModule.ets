import {
  KRAny,
  KuiklyRenderBaseModule,
  KuiklyRenderCallback
} from '@kuikly-open/render';
import { HarmonyThemeHost } from '../../designsystem/HarmonyThemeHost';
import {
  HarmonyThemeHostRequest,
  HarmonyThemeProtocol
} from '../../designsystem/HarmonyThemeProtocol';

export class KRThemeHostModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME: string = HarmonyThemeProtocol.MODULE_NAME;

  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback): KRAny {
    if (method !== HarmonyThemeProtocol.METHOD_APPLY_APPEARANCE) {
      callback(JSON.stringify({ success: false, message: 'unknown_method' }));
      return null;
    }

    try {
      const rawParams = typeof params === 'string' ? params : JSON.stringify(params);
      const request = JSON.parse(rawParams) as HarmonyThemeHostRequest;
      if (request.protocolVersion !== HarmonyThemeProtocol.VERSION ||
        typeof request.isDark !== 'boolean') {
        callback(JSON.stringify({ success: false, message: 'invalid_request' }));
        return null;
      }
      HarmonyThemeHost.shared.applyAppearance(request.isDark);
      callback(JSON.stringify({ success: true }));
    } catch (error) {
      callback(JSON.stringify({ success: false, message: 'invalid_json' }));
    }
    return null;
  }
}
