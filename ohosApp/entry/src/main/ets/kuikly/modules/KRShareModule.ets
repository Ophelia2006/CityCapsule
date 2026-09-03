import { systemShare } from '@kit.ShareKit';
import { uniformTypeDescriptor as utd } from '@kit.ArkData';
import { BusinessError } from '@kit.BasicServicesKit';
import { KRAny, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';

export class KRShareModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME: string = 'CCShareModule';
  syncMode(): boolean { return false; }
  call(method: string, params: KRAny, callback: KuiklyRenderCallback): KRAny {
    if (method !== 'shareText') { callback(JSON.stringify({ success: false })); return null; }
    const context = this.controller?.getUIAbilityContext();
    if (!context) { callback(JSON.stringify({ success: false })); return null; }
    const request = params as Record<string, KRAny>;
    const sharedData = new systemShare.SharedData({
      utd: utd.UniformDataType.TEXT,
      content: request['text'] as string,
      title: request['title'] as string
    });
    new systemShare.ShareController(sharedData).show(context, {
      selectionMode: systemShare.SelectionMode.SINGLE,
      previewMode: systemShare.SharePreviewMode.DETAIL
    }).then(() => callback(JSON.stringify({ success: true }))).catch((_error: BusinessError) => callback(JSON.stringify({ success: false })));
    return null;
  }
}
