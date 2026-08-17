import { http } from '@kit.NetworkKit';
import { KRAny, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';
import { AMAP_WEB_SERVICE_KEY } from '../../config/AmapLocalConfig';

export class KRPlaceNetworkModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME: string = 'CCPlaceNetworkModule';
  private static readonly SEARCH: string = 'searchPlaces';
  private static readonly REVERSE: string = 'reverseGeocode';

  syncMode(): boolean { return false; }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback): KRAny {
    if (AMAP_WEB_SERVICE_KEY.trim().length === 0) {
      callback(JSON.stringify({ status: 'unavailable', message: '未配置高德 Web 服务 Key。' }));
      return null;
    }
    const request = params as Record<string, KRAny>;
    let url = '';
    if (method === KRPlaceNetworkModule.SEARCH) {
      url = this.searchUrl(request);
    } else if (method === KRPlaceNetworkModule.REVERSE) {
      url = this.reverseUrl(request);
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
      this.param('extensions', 'all'), this.param('offset', '20'), this.param('page', '1')
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

  private reverseUrl(request: Record<string, KRAny>): string {
    const location = String(request['location'] ?? '').trim();
    if (location.length === 0) return '';
    const values: string[] = [
      this.param('key', AMAP_WEB_SERVICE_KEY), this.param('output', 'JSON'),
      this.param('extensions', 'base'), this.param('location', location)
    ];
    return `https://restapi.amap.com/v3/geocode/regeo?${values.join('&')}`;
  }

  private param(key: string, value: string): string {
    return `${encodeURIComponent(key)}=${encodeURIComponent(value)}`;
  }
}
