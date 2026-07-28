import {
  KRAny,
  KuiklyRenderBaseModule,
  KuiklyRenderCallback
} from '@kuikly-open/render';

export class KRLocaleModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME: string = 'CCLocaleModule';
  private static readonly METHOD_FORMAT_LOCAL_DATE: string = 'formatLocalDate';

  syncMode(): boolean {
    return true;
  }

  call(method: string, params: KRAny, _callback: KuiklyRenderCallback): KRAny {
    if (method !== KRLocaleModule.METHOD_FORMAT_LOCAL_DATE) {
      return '';
    }
    try {
      const request = JSON.parse(params as string) as LocalDateRequest;
      if (request.epochMs < 0) {
        return '';
      }
      return formatLocalCalendarDate(request.epochMs);
    } catch (_) {
      return '';
    }
  }
}

interface LocalDateRequest {
  epochMs: number;
}

export function formatLocalCalendarDate(epochMs: number): string {
  const date = new Date(epochMs);
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  return `${year}-${month}-${day}`;
}
