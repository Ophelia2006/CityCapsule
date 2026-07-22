import {
  KRAny,
  KuiklyRenderBaseModule,
  KuiklyRenderCallback
} from '@kuikly-open/render';
import { HarmonyStorageDispatcher } from '../../storage/HarmonyStorageDispatcher';
import { HarmonyStorageProtocol } from '../../storage/HarmonyStorageProtocol';

export class KRStorageModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME: string = HarmonyStorageProtocol.MODULE_NAME;

  private readonly dispatcher: HarmonyStorageDispatcher = new HarmonyStorageDispatcher();

  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback): KRAny {
    const rawParams = typeof params === 'string' ? params : JSON.stringify(params);
    const response = this.dispatcher.dispatch(method, rawParams);
    callback(JSON.stringify(response));
    return null;
  }
}

