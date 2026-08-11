import { BusinessError } from '@kit.BasicServicesKit';
import { KRAny, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';

export class KRExternalNavigationModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME: string = 'CCExternalNavigationModule';
  syncMode(): boolean { return false; }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback): KRAny {
    if (method !== 'open') { callback(JSON.stringify({ status: 'unsupported' })); return null; }
    const context = this.controller?.getUIAbilityContext();
    if (context === null || context === undefined) { callback(JSON.stringify({ status: 'unsupported' })); return null; }
    const request = params as Record<string, KRAny>;
    const latitude = request['latitude'] as number;
    const longitude = request['longitude'] as number;
    const name = encodeURIComponent(request['placeName'] as string);
    context.startAbility({
      action: 'ohos.want.action.viewData',
      uri: `geo:0,0?q=${latitude},${longitude}(${name})`
    }).then(() => callback(JSON.stringify({ status: 'opened' })))
      .catch((error: BusinessError) => callback(JSON.stringify({
        status: error.code === 16000019 ? 'no_compatible_app' : 'failure',
        message: '无法打开外部地图。'
      })));
    return null;
  }
}
