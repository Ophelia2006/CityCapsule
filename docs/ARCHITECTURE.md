# CityCapsule 当前架构

## 范围

本文只描述 2026-07-27 仓库中真实存在的架构。初始目标见 `INITIAL_PLANNING_BASELINE.md`；目标架构不会被写成当前事实。

## 工程与模块

| 区域 | 当前职责 | 主要依赖 | 被谁使用 | 实际状态 |
| --- | --- | --- | --- | --- |
| `shared` | KMP/Kuikly 页面、领域模型、状态持有者、Repository、存储/路由协议、设计系统 | Kuikly core/compose/annotations | Android、HarmonyOS；构建配置也声明 iOS/JS | 主业务模块，真实使用 |
| `androidApp` | Android Application/Activity、Kuikly render adapters、MMKV、主题与路由 dispatcher | shared、Kuikly Android render、MMKV、Glide、Picasso | Android App | 真实宿主 |
| `ohosApp` | ArkTS UIAbility、HMNavigation/HMRouter、Kuikly host、MMKV、主题/存储/路由 bridge | Kuikly Render 2.7.0、HMRouter 1.2.4、MMKV 2.4.0 | HarmonyOS App | 真实宿主；部分原生页为骨架 |
| `iosApp` | Kuikly iOS 模板宿主 | shared framework | iOS 模板 | 存在但无 CityCapsule 业务验收证据 |
| `static_server` | Kuikly JS 本地静态服务脚手架 | Node/Koa | JS 开发工具 | 非产品后端 |
| `buildSrc` | Kuikly 版本与 Gradle 构建变量 | Gradle | 根/模块构建 | 构建基础设施 |
| `docs` | 阶段协议和当前长期文档 | 代码事实 | 开发者/AI | 文档层 |

根 `settings.gradle.kts` 声明 `:h5App` 与 `:miniApp`，但当前工作树没有对应目录；这是构建配置风险，不构成已实现模块。

## 当前真实分层

```text
Kuikly @Page
  ├─ 简单页：直接调用 Repository（Home/Settings）
  └─ 业务页：StateHolder
               → Repository
                 → KeyValueStore
                   → CCStorageModule wire protocol
                     → Android/HarmonyOS dispatcher
                       → MMKV

Capsule Editor
  → PhotoPickerCapability
    → CCMediaModule
      → Android OpenMultipleDocuments / Harmony PhotoViewPicker
        → 应用沙箱 images/original
```

当前没有通用 `PageStore`、UseCase、LocalDataSource 或 RemoteDataSource 层。StateHolder 使用 callback 状态更新，而不是初始规划中的 `StateFlow` Store。Repository 与模型均位于 `shared/core/*`；Feature 页位于 `shared/feature/*`。这是一种 package 逻辑隔离，不是多 Gradle feature module。

## Shared 包职责与依赖

- `app/theme`：运行时主题模式协调和平台 theme host。
- `core/navigation`：强类型 `AppRoute`、路由表、wire request 与 `KuiklyAppNavigator`；不依赖平台路由 API。
- `core/storage`：3 个 MMKV store、8 个 typed key、codec、错误模型与 Kuikly bridge。
- `core/profile`：本地档案模型、校验、JSON codec 和 Repository。
- `core/onboarding`：草稿、启动决策、完成/重置事务式流程。
- `core/place`：有上限地点目录、seed、搜索/筛选/排序、校验和 CRUD Repository。
- `core/favorite`：独立收藏 ID 集合和 Repository；写入前检查地点，读取时剔除悬空 ID。
- `core/capsule`：城市碎片、草稿、心情/标签/媒体路径模型，JSON codec、校验、日期标签和有上限本地 Repository。
- `core/media`：平台无关 Photo Picker 协议与 `CCMediaModule` transport；不依赖 Android/ArkTS 类型。
- `designsystem`：语义颜色、Typography、Spacing/Radius/Motion token 及共享组件。
- `feature/onboarding`、`feature/profile`、`feature/place`、`feature/capsule`：页面与 callback 型 StateHolder；Capsule 包含编辑、详情、时间轴、相册和共享照片组件。
- shared 顶层 `HomePage`、`SettingsPage`：仍为早期结构；Home 是开发菜单，Settings 混合产品设置与路由验收。
- `RouterPage`、`ImageAdapterBenchmarks`：明确的开发诊断页，不是产品功能。

## 主要业务调用链

### 冷启动与首次引导

```text
Android KRApplication / Harmony EntryAbility
  → 初始化 MMKV + 执行 theme legacy migration
  → 无显式业务目标时打开 LaunchGate
  → LaunchGatePage
  → OnboardingRepository.getStartupDecision()
     → onboarding.completed_version + profile.local_profile + onboarding.draft
  → replace(Home | Onboarding)
```

引导提交由 `OnboardingStateHolder` 调用 `OnboardingRepository.complete()`：保存 profile → 写完成版本 → 删除草稿；中途失败保留可恢复状态。清除档案按完成标记、草稿、档案的顺序执行。

### 地点列表、搜索与想去

```text
PlaceListPage / FavoritesPager
  → PlaceListStateHolder.load()
  → LocalPlaceRepository.getCatalogSnapshot()
     → places.catalog（缺失时写入 seed）
  → LocalFavoriteRepository.getFavoriteIds()
     → favorites.place_ids
     → 与 catalog 交叉验证并尽力清理悬空 ID
  → PlaceSearchEngine.search(catalog, filter, favoriteIds)
  → UI 状态 Loading / Empty / Content / Error-like notice
```

搜索覆盖名称、标签、城市、区域、地址、备注；支持类别、城市、区域和只看收藏过滤。没有在线 POI、距离、定位或推荐算法。

### 地点详情与 CRUD

```text
PlaceDetailPage(placeId)
  → PlaceDetailStateHolder
  → PlaceRepository.getPlace + FavoriteRepository.isFavorite
  → toggleFavorite / deletePlace

PlaceEditorPage(placeId?)
  → PlaceEditorStateHolder
  → PlaceValidator
  → LocalPlaceRepository.create/update
  → places.catalog 整体重写
```

Repository 用内存 mutation queue 串行化本进程内写操作。删除地点后会尽力从收藏集合清理 ID。当前所有 seed 地点与用户新增地点使用同一模型，代码没有 `PlaceSource`，所以没有实现“系统地点不可删除”。

### 城市碎片记录与回忆

```text
PlaceDetailPage(placeId)
  → CapsuleEditorPage(placeId | capsuleId)
  → CapsuleEditorStateHolder
     ├─ CapsuleRepository.get/saveDraft/publish
     │   → capsules.draft / capsules.catalog
     ├─ PlaceRepository.getPlace
     └─ PhotoPickerCapability.pickImages
         → 双端系统相册 → 应用沙箱文件 URI
  → replace CapsuleDetailPage(capsuleId)
  → TimelinePage / GalleryPage
  → CapsuleDetailPage(capsuleId)
  → PlaceDetailPage(placeId)
```

`CityCapsule` 保存正文、可选心情、标签、地点 ID、最多 9 个图片路径和创建/更新时间。编辑器支持同上下文草稿恢复、保存、未保存退出确认、发布与更新；新建草稿和既有碎片草稿不会互相误恢复。`LocalCapsuleRepository` 整体重写最多 500 条的 catalog，并串行化 publish/delete；更新一个已不存在的 ID 返回 `Missing`，不会静默新建。

时间轴按创建时间倒序读取 catalog，再按 `placeId` 补充地点信息；地点缺失时仍保留碎片。相册是同一数据集的照片网格，不建立第二份媒体索引。当前日期标签使用 UTC 的确定性换算，尚未接平台本地时区格式器。

### 主题设置

```text
SettingsPage
  → SettingsRepository
  → settings.theme_mode
  → AppThemeRuntime
  → AndroidThemeHost / HarmonyThemeHost
```

保存时先预览，失败回滚。Android/HarmonyOS 都有旧主题偏好到 MMKV 的有界重试迁移。

## 路由架构

```text
Feature Page
  → AppNavigator(AppRoute)
  → AppRouteTable.resolve()
  → Kuikly router module wire envelope
     ├─ Android: KRRouterAdapter → AndroidRouteDispatcher
     │            ├─ KuiklyHostActivity
     │            └─ AndroidNativeRouteRegistry（当前无业务 launcher）
     └─ Harmony: RouterAdapter → HarmonyRouteDispatcher
                  → HMRouter → KuiklyHostPage / 原生骨架页
```

业务页面没有直接调用 HMRouter。路由参数传 `placeId`、`capsuleId`、`requestId` 等 ID。CapsuleEditor、CapsuleDetail、Timeline、Gallery 已有真实 `@Page` 并加入 Harmony 可用目录；MapExplore 仍只有协议，没有页面实现。

## 数据与缓存

当前没有关系数据库、Entity/DTO 映射层、Redis、MQ 或 RPC。领域模型直接通过自定义 JSON codec 写入 MMKV；没有远端 DTO。

| Store | Key | 类型 | 谁写 | 谁读/何时删除 |
| --- | --- | --- | --- | --- |
| `cc_preferences` | `settings.theme_mode` | string | SettingsRepository / 平台旧值迁移 | 启动与 Settings；重置主题时删除 |
| `cc_preferences` | `profile.local_profile` | JSON object | Profile/Onboarding Repository | 启动、档案页；重置本地状态时删除 |
| `cc_preferences` | `onboarding.completed_version` | long | OnboardingRepository | LaunchGate；重置时删除 |
| `cc_cache` | `onboarding.draft` | JSON object | OnboardingStateHolder 经 Repository | 引导恢复；完成或重置时删除 |
| `cc_preferences` | `places.catalog` | JSON object，最多 500 地点 | LocalPlaceRepository | 地点全流程；暂无整体清除 UI |
| `cc_preferences` | `favorites.place_ids` | JSON object，最多 500 ID | LocalFavoriteRepository | 列表/详情；取消、地点删除或悬空修复时更新 |
| `cc_preferences` | `capsules.catalog` | JSON object，最多 500 条碎片 | LocalCapsuleRepository | 详情/时间轴/相册/地点记忆计数；发布、更新、删除时整体重写 |
| `cc_cache` | `capsules.draft` | JSON object，单个可恢复草稿 | CapsuleEditor 经 Repository | 同一新建/编辑上下文恢复；匹配的发布或明确放弃时删除 |

`cc_meta` 只供平台迁移层使用，保存 schema/migration 状态与 type metadata。业务原图位于双端应用沙箱 `images/original`，MMKV 只保存 `file://` 路径；当前没有缩略图、媒体引用清理、导出包或网络缓存。

## 平台与网络能力

- Android：在 Kuikly host 注册 `CCMediaModule`，使用系统多选文档契约选择图片并复制到 `filesDir/images/original`；取消、并发请求、复制失败均回传显式状态。没有相机、定位、地图或通用文件导入 launcher。
- HarmonyOS：注册 `CCMediaModule`，使用 `PhotoViewPicker` 选择图片、`fileIo` 复制到 `filesDir/images/original`，结果用同一 JSON 状态协议回传。Permission/FileImport 页仍是骨架；`KRMyView/KRMyModule/KRBridgeModule` 保留模板/临时代码。
- 图片 adapter 的 HTTP 示例只存在于诊断页，不是业务网络层。
- 没有 OkHttp/Ktor/Retrofit/NetStack 业务封装、RemoteDataSource、天气、地理编码、路线或 AI 调用。

## Target Architecture（未来，非现状）

核心业务扩大时再引入 `StateHolder/Store → UseCase → Repository → Local/RemoteDataSource → Capability`。不要为了形式先创建空层；在地图、城市碎片或网络功能落地时，以可测试的业务规则为边界逐步补齐。
