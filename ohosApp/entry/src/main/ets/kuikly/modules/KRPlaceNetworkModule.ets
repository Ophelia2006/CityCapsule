import { http } from '@kit.NetworkKit';
import { KRAny, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';
import { AMAP_WEB_SERVICE_KEY } from '../../config/AmapLocalConfig';

export class KRPlaceNetworkModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME: string = 'CCPlaceNetworkModule';
  private static readonly SEARCH: string = 'searchPlaces';
  private static readonly REVERSE: string = 'reverseGeocode';
  private static readonly WALKING: string = 'walkingRoute';

  syncMode(): boolean { return false; }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback): KRAny {
    if (AMAP_WEB_SERVICE_KEY.trim().length === 0) {
      callback(JSON.stringify({ status: 'unavailable', message: '未配置高德 Web 服务 Key。' }));
      return null;
    }
    const request = this.decodeRequest(params);
    if (request === undefined) {
      callback(JSON.stringify({ status: 'failure', message: '网络请求参数无效。' }));
      return null;
    }
    let url = '';
    if (method === KRPlaceNetworkModule.SEARCH) {
      url = this.searchUrl(request);
    } else if (method === KRPlaceNetworkModule.REVERSE) {
      url = this.reverseUrl(request);
    } else if (method === KRPlaceNetworkModule.WALKING) {
      url = this.walkingUrl(request);
    }
    if (url.length === 0) {
      callback(JSON.stringify({ status: 'failure', message: '网络请求参数无效。' }));
      return null;
    }
    const client = http.createHttp();
    client.request(url, {
      method: http.RequestMethod.GET,
      connectTimeout: 8000,
      readTimeout: 10000,
      header: { 'Accept': 'application/json' }
    }).then((result: http.HttpResponse): void => {
      callback(JSON.stringify({ status: 'success', body: String(result.result) }));
    }).catch((): void => {
      callback(JSON.stringify({ status: 'failure', message: '网络请求失败，请稍后重试。' }));
    }).finally((): void => client.destroy());
    return null;
  }

  private searchUrl(request: Record<string, KRAny>): string {
    const query = String(request['query'] ?? '').trim();
    const city = String(request['city'] ?? '').trim();
    const location = String(request['location'] ?? '').trim();
    const endpoint = location.length > 0 && query.length === 0 ? 'around' : 'text';
    const values: string[] = [
      this.param('key', AMAP_WEB_SERVICE_KEY), this.param('output', 'JSON'),
      this.param('extensions', 'all'),
      this.param('offset', String(Math.min(20, Math.max(1, Number(request['pageSize'] ?? 12))))),
      this.param('page', String(Math.max(1, Number(request['page'] ?? 1))))
    ];
    if (query.length > 0) values.push(this.param('keywords', query));
    if (city.length > 0) {
      values.push(this.param('city', city));
      values.push(this.param('citylimit', 'true'));
    }
    if (location.length > 0) {
      values.push(this.param('location', location));
      values.push(this.param('radius', '10000'));
      values.push(this.param('sortrule', 'distance'));
    }
    return `https://restapi.amap.com/v3/place/${endpoint}?${values.join('&')}`;
  }

  private decodeRequest(params: KRAny): Record<string, KRAny> | undefined {
    if (typeof params === 'string') {
      try {
        return JSON.parse(params) as Record<string, KRAny>;
      } catch (_) {
        return undefined;
      }
    }
    if (typeof params === 'object' && params !== null) {
      return params as Record<string, KRAny>;
    }
    return undefined;
  }

  private reverseUrl(request: Record<string, KRAny>): string {
    const location = String(request['location'] ?? '').trim();
    if (location.length === 0) return '';
    const values: string[] = [
      this.param('key', AMAP_WEB_SERVICE_KEY), this.param('output', 'JSON'),
      this.param('extensions', 'base'), this.param('location', location)
    ];
    return `https://restapi.amap.com/v3/geocode/regeo?${values.join('&')}`;
  }

  private walkingUrl(request: Record<string, KRAny>): string {
    const origin = String(request['origin'] ?? '').trim();
    const destination = String(request['destination'] ?? '').trim();
    if (origin.length === 0 || destination.length === 0) return '';
    const values: string[] = [
      this.param('key', AMAP_WEB_SERVICE_KEY), this.param('output', 'JSON'),
      this.param('origin', origin), this.param('destination', destination)
    ];
    return `https://restapi.amap.com/v3/direction/walking?${values.join('&')}`;
  }

  private param(key: string, value: string): string {
    return `${encodeURIComponent(key)}=${encodeURIComponent(value)}`;
  }
}
