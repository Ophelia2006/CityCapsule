import { abilityAccessCtrl, Permissions } from '@kit.AbilityKit';
import { geoLocationManager } from '@kit.LocationKit';
import { BusinessError } from '@kit.BasicServicesKit';
import hilog from '@ohos.hilog';
import { KRAny, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';

export class KRLocationModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME: string = 'CCLocationModule';
  private static readonly METHOD: string = 'getCurrentLocation';
  private static readonly LOG_DOMAIN: number = 0x0000;
  private static readonly LOG_TAG: string = 'CityCapsuleLocation';
  private inFlight: boolean = false;
  private permissionRejectedOnce: boolean = false;

  syncMode(): boolean { return false; }

  call(method: string, _params: KRAny, callback: KuiklyRenderCallback): KRAny {
    if (method !== KRLocationModule.METHOD) {
      this.logWarn('unsupported_method', -1, 'unavailable');
      callback(response('unavailable', '当前定位方法不受支持。'));
      return null;
    }
    this.request(callback);
    return null;
  }

  private async request(callback: KuiklyRenderCallback): Promise<void> {
    if (this.inFlight) {
      this.logWarn('request_rejected_in_flight', -1, 'failure');
      callback(response('failure', '已有定位请求正在进行。'));
      return;
    }
    const context = this.controller?.getUIAbilityContext();
    if (context === null || context === undefined) {
      this.logWarn('context_unavailable', -1, 'unavailable');
      callback(response('unavailable', '当前页面不支持定位。'));
      return;
    }
    this.inFlight = true;
    let stage: LocationStage = 'permission_request';
    this.logInfo('request_started');
    try {
      const permissions: Permissions[] = [
        'ohos.permission.APPROXIMATELY_LOCATION',
        'ohos.permission.LOCATION'
      ];
      const atManager = abilityAccessCtrl.createAtManager();
      let granted: boolean;
      let dialogShown: boolean = true;
      if (this.permissionRejectedOnce) {
        this.logInfo('permission_settings_request_started');
        const settingResults = await atManager.requestPermissionOnSetting(context, permissions);
        granted = settingResults.some((value: number): boolean => value === 0);
        dialogShown = false;
      } else {
        const result = await atManager.requestPermissionsFromUser(context, permissions);
        granted = result.authResults.some((value: number): boolean => value === 0);
        dialogShown = result.dialogShownResults?.some(
          (value: boolean): boolean => value
        ) ?? true;
      }
      if (!granted) {
        const status = dialogShown ? 'permission_denied' : 'permission_permanently_denied';
        this.permissionRejectedOnce = true;
        this.logWarn('permission_rejected', -1, status);
        callback(response(status));
        return;
      }
      this.permissionRejectedOnce = false;
      this.logInfo('permission_granted');
      stage = 'service_check';
      if (!geoLocationManager.isLocationEnabled()) {
        this.logWarn('service_disabled', 3301100, 'service_disabled');
        callback(response('service_disabled'));
        return;
      }
      this.logInfo('service_enabled');
      stage = 'location_request';
      this.logInfo('location_request_started');
      const location = await geoLocationManager.getCurrentLocation({
        priority: geoLocationManager.LocationRequestPriority.FIRST_FIX,
        scenario: geoLocationManager.LocationRequestScenario.UNSET,
        timeoutMs: 10000
      });
      this.logInfo('location_request_succeeded');
      callback(JSON.stringify({
        'status': 'success',
        'message': '',
        'latitude': location.latitude,
        'longitude': location.longitude,
        'accuracyMeters': location.accuracy
      }));
    } catch (error) {
      const businessError = error as BusinessError;
      const mapped = mapLocationError(businessError.code, stage);
      this.logError(stage, businessError.code, mapped.status);
      callback(response(mapped.status, mapped.message));
    } finally {
      this.inFlight = false;
      this.logInfo('request_finished');
    }
  }

  private logInfo(stage: string): void {
    hilog.info(
      KRLocationModule.LOG_DOMAIN,
      KRLocationModule.LOG_TAG,
      'stage=%{public}s',
      stage
    );
  }

  private logWarn(stage: string, code: number, status: string): void {
    hilog.warn(
      KRLocationModule.LOG_DOMAIN,
      KRLocationModule.LOG_TAG,
      'stage=%{public}s code=%{public}d mappedStatus=%{public}s',
      stage,
      code,
      status
    );
  }

  private logError(stage: string, code: number, status: string): void {
    hilog.error(
      KRLocationModule.LOG_DOMAIN,
      KRLocationModule.LOG_TAG,
      'stage=%{public}s code=%{public}d mappedStatus=%{public}s',
      stage,
      code,
      status
    );
  }
}

type LocationStage = 'permission_request' | 'service_check' | 'location_request';

interface MappedLocationError {
  status: string;
  message: string;
}

export function mapLocationError(code: number, stage: LocationStage): MappedLocationError {
  switch (code) {
    case 201:
      return {
        status: 'permission_denied',
        message: '定位权限校验失败，请检查系统权限设置。'
      };
    case 801:
      return {
        status: 'unavailable',
        message: '当前设备不支持定位能力。'
      };
    case 3301000:
      return {
        status: 'unavailable',
        message: '系统定位服务暂时不可用。'
      };
    case 3301100:
      return {
        status: 'service_disabled',
        message: '系统定位服务已关闭。'
      };
    case 3301200:
      return {
        status: 'failure',
        message: '未能在限定时间内获得当前位置，请到开阔处重试。'
      };
    case 401:
      return {
        status: 'failure',
        message: '定位请求参数无效。'
      };
    default:
      return {
        status: stage === 'permission_request' ? 'permission_denied' : 'failure',
        message: stage === 'permission_request'
          ? '无法完成定位权限申请，请检查系统设置。'
          : '暂时无法获取当前位置。'
      };
  }
}

function response(status: string, message: string = ''): string {
  return JSON.stringify({ 'status': status, 'message': message });
}
