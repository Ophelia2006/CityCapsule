# ADR-022：地图供应商与降级边界

- 状态：共享契约已接受；Native SDK 接入受凭据阻塞
- 日期：2026-08-04

## 决策

地图只作为 Explore 的“列表 / 地图”内部视图，不增加一级 Tab。共享层只拥有 `ExploreMapViewState`、`MapMarkerModel`、`MapCameraModel`、`MapViewEvent` 与 `NativeMapViewContract`，不得暴露供应商对象。

供应商方向采用华为 Map Kit：HarmonyOS 使用系统 Map Kit；Android 使用 HMS Map Kit。二者能覆盖本阶段的地图、Marker、相机和生命周期能力，因此业务契约等价；平台 SDK、初始化和错误码不等价，分别适配。

当前仓库没有可用 Map Kit 凭据和 AGC 配置。不得提交 Key，也不得用假底图冒充完成。Native View 仅在平台安全配置可用后启用；缺 Key、离线、SDK 不支持或初始化失败必须报告 `MapAvailability`，Explore 随即切回列表。无坐标地点始终留在列表，永不生成 Marker。

## Key 与协议

- Key 不写源码、普通 MMKV、资源明文或默认备份；Android 由本机/CI secret 注入，HarmonyOS 使用 AppGallery Connect/签名绑定配置；日志不得输出 Key。
- Marker 只传 `placeId/title/GeoPoint`，点击只回传 `placeId`；摘要卡由共享 UI 从 catalog 解析，整卡进入 typed `PlaceDetail(placeId)`。
- 相机只传中心点与 zoom。Native View 必须实现 start/stop/dispose，dispose 后回调作废。
- 当前定位可选；权限拒绝不影响地图或列表浏览。

## 外部导航

外部导航不依赖内嵌地图供应商。共享层调用 `ExternalNavigationCapability.open(latitude, longitude, placeName)`；Android 使用系统 `ACTION_VIEW + geo:`，HarmonyOS 使用系统 `startAbility + geo:`。结果映射为 `Opened / NoCompatibleApp / Unsupported / Failure`，应用不实现路径算法。

## 门禁

取得双端合法凭据并完成真机 SDK 验收前，Native 地图不能标为 DONE；当前完成的是共享契约、降级决策与外部导航 bridge。
