# ADR-023：双端地图供应商迁移为高德地图

- 状态：已接受；Native View 与真机验收尚未完成
- 日期：2026-08-10
- 取代：ADR-022 中“采用华为 Map Kit”的供应商选择；其余共享契约与降级原则继续有效

## 决策

Android 与 HarmonyOS NEXT 统一采用高德地图 SDK。Android 使用 Maven `com.amap.api:3dmap`，HarmonyOS 使用 `@amap/amap_lbs_common` 与 `@amap/amap_lbs_map3d`。地图仍只是 Explore 的“列表 / 地图”内部视图，不增加一级 Tab。

选择迁移的已确认原因是：当前测试 Android 设备不具备可靠的 HMS Core 环境，而高德为 Android 和 HarmonyOS NEXT 提供原生 SDK，可以继续维持“共享业务契约 + 平台 Native View adapter”的边界。SDK 初始化、Key 绑定、签名标识、隐私合规、生命周期和错误码仍由各平台分别实现。

## 配置与降级

- Key 不进入 Git、MMKV、资源明文或默认备份。Android 从根目录 `local.properties` 注入；HarmonyOS 使用被 Git 忽略的 `AmapLocalConfig.ets` 注入。
- 高德 SDK 只能在用户同意地图服务隐私说明后初始化；同意前不得预初始化或发起 SDK 网络请求。
- 缺 Key、离线、SDK 不支持或初始化失败必须映射为 `MapAvailability`，Explore 自动回到列表。
- 无坐标地点始终保留在列表，不生成 Marker；不得用假底图冒充成功。
- Android 当前使用 Maven Central 可稳定解析的 `3dmap:10.0.600`。升级需重新验证隐私 API、Marker 和生命周期。

## 协议约束

- 共享层只持有 `ExploreMapViewState`、`MapMarkerModel`、`MapCameraModel`、`MapViewEvent` 与 `NativeMapViewContract`，不得暴露高德对象。
- Marker 只传 `placeId/title/GeoPoint`，点击只回传 `placeId`；共享 UI 从 catalog 解析摘要，并进入 typed `PlaceDetail(placeId)`。
- Native View 必须实现 start/stop/dispose；dispose 后回调作废。
- `GeoPoint` 的共享与持久化语义固定为 WGS-84。高德平台 adapter 只在渲染边界转换为供应商显示坐标，供应商坐标不得写回 catalog。现有 schema 2 数据没有坐标系字段，但现有种子和编辑入口尚未产生带坐标数据，因此本次通过契约澄清而非 wire schema 迁移落地；开始导入外部坐标前必须补迁移兼容测试。

## 完成门槛

双端合法凭据、隐私同意流程、Native View、Marker/摘要/详情链路和真机生命周期验收全部通过前，P2-3 地图不得标记为 DONE。依赖加入不等于地图闭环完成。
