import { Context } from '@kit.AbilityKit';
import { ComponentContent } from '@kit.ArkUI';
import hilog from '@ohos.hilog';
import { KRAny, KuiklyRenderBaseView, KuiklyRenderCallback } from '@kuikly-open/render';
import {
  AMap,
  CameraUpdateFactory,
  LatLng,
  MapView,
  MapViewCreateCallback,
  MapViewComponent,
  MapViewManager,
  MapsInitializer,
  Marker,
  MarkerOptions,
  PolylineOptions
} from '@amap/amap_lbs_map3d';
import {
  AMapPrivacyAgreeStatus,
  AMapPrivacyInfoStatus,
  AMapPrivacyShowStatus
} from '@amap/amap_lbs_common';
import { AMAP_HARMONY_API_KEY } from '../../config/AmapLocalConfig';

interface MapMarkerPayload {
  placeId: string;
  title: string;
  latitude: number;
  longitude: number;
}

interface MapStatePayload {
  markers: MapMarkerPayload[];
  camera?: MapCameraPayload;
  currentLocation?: MapPointPayload;
  showCurrentLocation?: boolean;
  trackPoints?: MapPointPayload[];
  plannedTrackPoints?: MapPointPayload[];
}

interface MapPointPayload {
  latitude: number;
  longitude: number;
}

interface MapCameraPayload {
  latitude: number;
  longitude: number;
  zoom: number;
}

interface MapEventPayload {
  type: string;
  reason?: string;
  message?: string;
  placeId?: string;
}

@Observed
export class KRAmapView extends KuiklyRenderBaseView {
  static readonly VIEW_NAME = 'CCAmapView';
  private eventCallback: KuiklyRenderCallback | null = null;
  private encodedState: string = '';
  private privacyAccepted: boolean = false;
  private initialized: boolean = false;
  private mapView: MapView | null = null;
  private aMap: AMap | null = null;
  private markerPlaces: Map<string, string> = new Map();
  private mapCreatedCallback: MapViewCreateCallback | null = null;
  private hostContext: Context | null = null;
  private mapMountListener: ((enabled: boolean) => void) | null = null;
  private sizeListener: ((width: number, height: number) => void) | null = null;

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    this.logStage('create_ark_ui_view');
    const uiContext = this.getUIContext() as UIContext;
    return new ComponentContent<KuiklyRenderBaseView>(
      uiContext,
      wrapBuilder<[KuiklyRenderBaseView]>(createAmapView),
      this
    );
  }

  setProp(propKey: string, propValue: KRAny | KuiklyRenderCallback): boolean {
    switch (propKey) {
      case 'mapState':
        this.encodedState = propValue as string;
        this.renderState();
        return true;
      case 'privacyAccepted':
        this.privacyAccepted = propValue as boolean;
        this.logStage(`privacy_received accepted=${this.privacyAccepted}`);
        this.tryInitialize();
        return true;
      case 'onMapEvent':
        this.eventCallback = propValue as KuiklyRenderCallback;
        this.logStage('callback_received');
        this.tryInitialize();
        return true;
      default:
        return super.setProp(propKey, propValue);
    }
  }

  attach(context: Context): void {
    this.hostContext = context;
    this.logStage('context_attached');
    this.tryInitialize();
  }

  isMapComponentEnabled(): boolean {
    return this.initialized;
  }

  setMapMountListener(listener: ((enabled: boolean) => void) | null): void {
    this.mapMountListener = listener;
    this.logStage(`mount_listener enabled=${listener !== null} initialized=${this.initialized}`);
    if (listener) listener(this.initialized);
  }

  setSizeListener(listener: ((width: number, height: number) => void) | null): void {
    this.sizeListener = listener;
    if (listener) listener(this.getViewWidth(), this.getViewHeight());
  }

  sizeDidChanged(width: number, height: number): void {
    super.sizeDidChanged(width, height);
    this.logStage(`size_changed width=${width} height=${height}`);
    this.sizeListener?.(width, height);
  }

  private tryInitialize(): void {
    const context = this.hostContext;
    if (this.initialized || !this.privacyAccepted || !context || !this.eventCallback) {
      this.logStage(
        `initialization_deferred initialized=${this.initialized} privacy=${this.privacyAccepted} ` +
        `context=${context !== null} callback=${this.eventCallback !== null}`
      );
      return;
    }
    if (AMAP_HARMONY_API_KEY.trim().length === 0) {
      this.emit({ type: 'unavailable', reason: 'missingConfiguration', message: '未配置 HarmonyOS 高德地图 Key' });
      return;
    }
    try {
      MapsInitializer.updatePrivacyShow(
        AMapPrivacyShowStatus.DidShow,
        AMapPrivacyInfoStatus.DidContain,
        context
      );
      MapsInitializer.updatePrivacyAgree(AMapPrivacyAgreeStatus.DidAgree, context);
      MapsInitializer.setApiKey(AMAP_HARMONY_API_KEY);
      this.mapCreatedCallback = (view?: MapView) => {
        this.logStage(`map_view_callback view=${view !== undefined}`);
        if (!view || this.mapView) return;
        this.mapView = view;
        view.onCreate();
        view.getMapAsync((map: AMap) => {
          this.logStage('amap_ready');
          this.aMap = map;
          map.setOnMarkerClickListener((marker: Marker): boolean => {
            const placeId = this.markerPlaces.get(marker.getId());
            if (placeId) this.emit({ type: 'markerSelected', placeId: placeId });
            return true;
          });
          this.renderState();
          this.emit({ type: 'ready' });
        });
      };
      MapViewManager.getInstance().registerMapViewCreatedCallback(this.mapCreatedCallback);
      this.initialized = true;
      this.logStage('sdk_initialized_mount_requested');
      this.mapMountListener?.(true);
    } catch (error) {
      const message = error instanceof Error ? error.message : '高德地图初始化失败';
      this.emit({ type: 'unavailable', reason: 'failure', message: message });
    }
  }

  private renderState(): void {
    if (!this.aMap || this.encodedState.length === 0) return;
    try {
      const state = JSON.parse(this.encodedState) as MapStatePayload;
      this.aMap.clear();
      this.markerPlaces.clear();
      state.markers.forEach((payload: MapMarkerPayload) => {
        const converted = wgs84ToGcj02(payload.latitude, payload.longitude);
        const marker = this.aMap?.addMarker(
          new MarkerOptions()
            .setPosition(new LatLng(converted[0], converted[1]))
            .setTitle(payload.title)
        );
        if (marker) this.markerPlaces.set(marker.getId(), payload.placeId);
      });
      const plannedTrackPoints = state.plannedTrackPoints ?? [];
      if (plannedTrackPoints.length >= 2) {
        const convertedPlan: LatLng[] = plannedTrackPoints.map((point: MapPointPayload) => { const converted = wgs84ToGcj02(point.latitude, point.longitude); return new LatLng(converted[0], converted[1]); });
        this.aMap.addPolyline(new PolylineOptions().add(...convertedPlan).setWidth(18).setColor(0xFF9A9A9A));
      }
      const trackPoints = state.trackPoints ?? [];
      if (trackPoints.length >= 2) {
        const convertedTrack: LatLng[] = trackPoints.map((point: MapPointPayload) => {
          const converted = wgs84ToGcj02(point.latitude, point.longitude);
          return new LatLng(converted[0], converted[1]);
        });
        this.aMap.addPolyline(
          new PolylineOptions()
            .add(...convertedTrack)
            .setWidth(12)
            .setColor(0xFFFF9F1C)
        );
      }
      if (state.showCurrentLocation && state.currentLocation) {
        const current = wgs84ToGcj02(
          state.currentLocation.latitude,
          state.currentLocation.longitude
        );
        this.aMap.addMarker(
          new MarkerOptions()
            .setPosition(new LatLng(current[0], current[1]))
            .setTitle('当前位置')
        );
      }
      if (state.camera) {
        const center = wgs84ToGcj02(state.camera.latitude, state.camera.longitude);
        this.aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
          new LatLng(center[0], center[1]),
          state.camera.zoom
        ));
      }
      this.logStage(
        `state_rendered markers=${this.markerPlaces.size} camera=${state.camera !== undefined}`
      );
    } catch (error) {
      const message = error instanceof Error ? error.message : '地图数据无法解析';
      this.emit({ type: 'unavailable', reason: 'failure', message: message });
    }
  }

  private emit(payload: MapEventPayload): void {
    if (this.eventCallback) this.eventCallback(JSON.stringify(payload));
  }

  private logStage(stage: string): void {
    hilog.info(0x0000, 'CityCapsuleMap', '%{public}s', stage);
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {
  }

  onDestroy(): void {
    if (this.mapCreatedCallback) {
      MapViewManager.getInstance().unregisterMapViewCreatedCallback(this.mapCreatedCallback);
    }
    this.mapCreatedCallback = null;
    this.hostContext = null;
    this.mapMountListener = null;
    this.sizeListener = null;
    this.eventCallback = null;
    this.aMap = null;
    this.mapView?.onDestroy();
    this.mapView = null;
    super.onDestroy();
  }

  getMessageCenterImage(): ResourceStr | null {
    return null;
  }
}

@Component
struct KRAmapComponent {
  @ObjectLink renderView: KRAmapView;
  @State mapEnabled: boolean = false;
  @State mapWidthPx: number = 0;
  @State mapHeightPx: number = 0;

  aboutToAppear(): void {
    this.renderView.setMapMountListener((enabled: boolean) => {
      this.mapEnabled = enabled;
    });
    this.renderView.setSizeListener((width: number, height: number) => {
      this.mapWidthPx = width;
      this.mapHeightPx = height;
    });
    this.renderView.attach(getContext(this));
  }

  aboutToDisappear(): void {
    this.renderView.setMapMountListener(null);
    this.renderView.setSizeListener(null);
  }

  build() {
    Stack() {
      Column()
        .width('100%')
        .height('100%')
      if (this.mapEnabled) {
        MapViewComponent()
          .width(this.mapWidthPx > 0 ? this.mapWidthPx : '100%')
          .height(this.mapHeightPx > 0 ? this.mapHeightPx : '100%')
      }
    }
    .width('100%')
    .height('100%')
  }
}

@Builder
function createAmapView(view: KuiklyRenderBaseView) {
  KRAmapComponent({ renderView: view as KRAmapView });
}

function wgs84ToGcj02(latitude: number, longitude: number): number[] {
  if (longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271) {
    return [latitude, longitude];
  }
  const axis = 6378245.0;
  const eccentricity = 0.00669342162296594323;
  let dLat = transformLatitude(longitude - 105.0, latitude - 35.0);
  let dLon = transformLongitude(longitude - 105.0, latitude - 35.0);
  const radLat = latitude / 180.0 * Math.PI;
  let magic = Math.sin(radLat);
  magic = 1 - eccentricity * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat = dLat * 180.0 / ((axis * (1 - eccentricity)) / (magic * sqrtMagic) * Math.PI);
  dLon = dLon * 180.0 / (axis / sqrtMagic * Math.cos(radLat) * Math.PI);
  return [latitude + dLat, longitude + dLon];
}

function transformLatitude(x: number, y: number): number {
  let value = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
  value += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
  value += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
  value += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
  return value;
}

function transformLongitude(x: number, y: number): number {
  let value = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
  value += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
  value += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
  value += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
  return value;
}
