# CityCapsule 地点图片加载性能优化

## 1. 目标与范围

本次优化针对 Home、Explore、地点详情共用的地点图片渲染链。目标不是在页面打开时无限并发下载全部原图，而是在保持统一图片体验的同时控制首屏压力：

```text
类别 fallback 立即渲染
→ 首批 6 个地点标记为 VISIBLE
→ 全局最多 3 个冷请求并发
→ 其余图片按 PREFETCH 排队
→ 相同 URL 共享一个冷加载 owner
→ 页面销毁时释放排队或活动 lease
→ 成功后由平台内存/磁盘缓存复用
```

适用范围是地点封面与高德 POI 图片。Capsule 相册仍沿用既有分批组合策略，不在本次改造成地点图片管线。

## 2. 技术栈

| 层级 | 技术 | 本次职责 |
| --- | --- | --- |
| 跨端 UI | Kotlin Multiplatform + Kuikly Compose DSL | `PlaceMedia` 统一 fallback、请求准入和成功/失败反馈 |
| 状态与生命周期 | Compose `remember` + `DisposableEffect` | 维护单个媒体节点的加载许可；离开组合时释放 lease |
| 调度 | commonMain `ImageLoadCoordinator` | 并发上限 3、VISIBLE/PREFETCH 优先级、URL 去重、取消和指标 |
| Android 图片实现 | Kuikly `IKRImageAdapter` + Glide 4.12 | HTTP/file/assets 图片下载、内存/磁盘缓存、按目标尺寸 `override`、`centerCrop/fitCenter` |
| HarmonyOS 图片实现 | Kuikly HarmonyOS Render 图片适配链 | 消费同一个共享准入结果并执行平台图片加载；具体缓存由当前 Render adapter 管理 |
| URL 元数据 | MMKV 2.4.0，`cc_cache/places.photo_cache` | 最多 100 条、30 天有效；不保存图片字节、不进入备份 |
| 自动化 | Kotlin Test + Gradle | 调度顺序、并发上限、去重、取消、失败和跨端编译验证 |

## 3. 核心实现

### 3.1 统一媒体入口

`PlaceMedia` 的资源优先级保持：

```text
Place.visualRef（本地托管封面）
→ 有效的高德 URL 缓存
→ 类别 fallback
```

fallback 始终先占据固定媒体区域，所以图片排队、慢加载或失败都不会让 Card 高度变化，也不会出现破图空白。

### 3.2 有限并发与分级加载

`PlaceImageLoadRuntime` 使用一个进程内协调器：

- 最大冷请求并发：3；
- Explore 排名前 6 的地点：`VISIBLE`；
- 后续地点：`PREFETCH`；
- 高优先级任务总是在低优先级队列前启动；
- 本地 `file://` 封面不经过网络并发门禁。

这避免十几张网络图片在同一帧开始下载和解码。它是有界的分级预热，不是启动时下载整座城市。

### 3.3 URL 请求去重

协调器以规范化 URL 为 key。第一个订阅者成为冷加载 owner；同 URL 的其他媒体节点等待 owner 完成。成功后等待者再进入平台加载器，此时可以命中 Glide/Render adapter 的缓存，而不会形成多个并行冷下载。

进程内 warmed URL LRU 最多保留 128 条，避免运行时间增长导致集合无界。

### 3.4 生命周期释放

每个远程 `PlaceMedia` 通过 `DisposableEffect` 持有 `ImageLoadLease`：

- 尚在队列：离开组合时直接移除订阅；
- 正在活动且没有其他订阅者：释放并发槽，让下一项开始；
- 同 URL 还有订阅者：把 owner 转移给仍可见的节点。

实际网络请求的底层取消能力仍取决于 Kuikly 平台图片 adapter；共享协调器保证已经离开组合的节点不继续占用调度名额。

### 3.5 尺寸感知与缓存边界

Kuikly 把媒体布局尺寸传给 Android `KRImageAdapter`。Glide 在 `needResize` 时调用 `override(requestWidth, requestHeight)`，再应用 `centerCrop` 或 `fitCenter`，避免列表缩略图完整解码超大原图。

缓存分为两类：

1. MMKV 只保存有界 URL 元数据，不保存 Bitmap 或图片字节；
2. 图片字节、内存 LRU 与磁盘缓存交给平台图片加载器。

因此备份不会携带临时网络图片，缓存清除或离线也不会破坏地点数据。

## 4. 可观测指标

`ImageLoadCoordinator.metrics()` 提供：

- `uniqueRequestsStarted`：实际进入冷加载的唯一 URL 数；
- `deduplicatedSubscribers`：被合并的重复订阅数；
- `warmedCacheSubscribers`：已预热 URL 的复用次数；
- `successfulRequests` / `failedRequests`；
- `cancelledRequests`；
- 当前 active / queued 数量。

这些指标目前是开发期进程内快照，不进入正式产品 UI，也不持久化用户行为。

## 5. 自动化验收

`ImageLoadCoordinatorTest` 覆盖：

- 并发不超过配置上限；
- VISIBLE 优先于 PREFETCH；
- 同 URL 冷请求去重；
- lease 释放后并发槽转交；
- owner 失败时不触发重复冷请求。

同时需要执行：

```powershell
./gradlew.bat :shared:testDebugUnitTest
./gradlew.bat :androidApp:assembleDebug
./gradlew.bat -c settings.ohos.gradle.kts :shared:linkDebugSharedOhosArm64
```

## 6. 面试表述

> CityCapsule 的跨端页面使用 Kuikly Compose。地点列表原本会在一次组合中触发多张远程图片，我在 commonMain 建立了与平台加载器解耦的图片准入层：固定 fallback 保证布局稳定，首批内容优先，冷请求全局并发限制为 3，相同 URL 只保留一个 owner，组件销毁时释放 lease。Android 继续利用 Glide 的内存/磁盘缓存和目标尺寸解码，MMKV 只保存有上限、可过期且不进备份的 URL 元数据。这样既控制首屏网络与解码峰值，也保持 Android/HarmonyOS 一致的失败降级。

## 7. 已知边界与后续量化

- 当前 `AppFixedHeaderScaffold` 把 Explore 正文作为单个 lazy item，首批 6 张依据结果顺序近似首屏，而不是读取精确 viewport；若地点规模显著扩大，应把结果列表改为独立 `LazyListScope.items` 并按可见索引动态调整优先级。
- HarmonyOS Render adapter 的内存/磁盘缓存命中率需要真机 profiling，不能仅由共享层断言。
- 下一轮可增加首图时间、首屏完成时间、P95 解码耗时、峰值内存和滚动丢帧的设备基准；当前指标聚焦调度正确性。
