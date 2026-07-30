# CityCapsule 当前架构

## 范围

本文只描述截至 2026-07-30 仓库中真实存在的架构。初始目标见 `INITIAL_PLANNING_BASELINE.md`；目标架构不会被写成当前事实。

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
  ├─ 简单页：直接调用 Repository（Settings）
  ├─ 已迁移页：Feature MVI Store → Repository
  └─ 未迁移业务页：StateHolder → Repository
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

当前没有通用 `PageStore`、UseCase、LocalDataSource 或 RemoteDataSource 层。PlaceList/Explore 与 Profile Overview/Editor 使用 Feature 级 MVI Store；其余 StateHolder 仍使用 callback 状态更新。Repository 与模型均位于 `shared/core/*`；Feature 页位于 `shared/feature/*`。这是一种 package 逻辑隔离，不是多 Gradle feature module。

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
- `designsystem`：语义颜色、Typography、Spacing/Radius/Motion token 及共享组件；`AppFixedHeaderScaffold` 提供固定 Header/独立滚动正文，`AppBottomSheet` 提供固定标题与 Footer、中间限高滚动区。旧 `AppScaffold` 的整页滚动语义保留给未迁移页面。
- `feature/onboarding`、`feature/profile`、`feature/place`、`feature/capsule`：页面及表现层状态；Profile Overview/Editor 与 PlaceList 已使用 MVI Store，其余仍主要是 callback 型 StateHolder；Capsule 包含编辑、详情、时间轴、相册和共享照片组件。
- `app/navigation`：`AppShellPager/AppShellPage` 是唯一产品根壳；`AppRootScaffold` 只创建一个 Bottom Navigation，根 `HorizontalPager` 常驻 Home、Record、Profile 三个内容树。`AppRootTab` 提供稳定 id、typed 入口别名和 Pager index。
- shared 顶层 `HomePage`、`SettingsPage`：Home 已接正式一级导航但内容仍是早期探索入口；Settings 保留真实主题偏好、首次引导与返回操作，不再承载路由验收入口。
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
  → PlaceListIntent
  → PlaceListStore（串行 Event actor）
  → PlaceListMutation
  → pure PlaceListReducer
  → StateFlow<PlaceListUiState> / one-shot PlaceListEffect
  → LocalPlaceRepository.getCatalogSnapshot()
     → places.catalog（缺失时写入 seed）
  → LocalFavoriteRepository.getFavoriteIds()
     → favorites.place_ids
     → 与 catalog 交叉验证并尽力清理悬空 ID
  → PlaceSearchEngine.search(catalog, filter, favoriteIds)
  → UI 状态 Loading / Empty / Content / Error-like notice
```

Home 的分类入口通过 typed `AppRoute.PlaceList(initialCategory)` 携带可选分类 wire value；`PlaceListPager` 解析为 `PlaceCategory` 并初始化筛选。未知或缺失值安全降级为无分类筛选。

PlaceList/Explore 是当前首个轻量 MVI Feature。UI 只读取 State、派发 Intent，并在 UI 边界消费 typed navigation Effect；Store 不持有 Navigator 或平台对象，`DisposableEffect` 负责 `dispose()`。搜索置顶，分类为横向 chips，高级城市/区域/只看想去筛选位于 Bottom Sheet；地点整行进入详情，想去心形独立操作。当前没有定位能力，默认只按档案城市稳定优先并展示“本地点目录”，不使用“附近”或距离语义。

### 探索首页聚合与本地推荐

```text
AppShellPage
  → HomeRootContent
  → HomeStateHolder
     ├─ LocalProfileRepository.getProfileSnapshot()
     ├─ PlaceRepository.getCatalogSnapshot()
     ├─ FavoriteRepository.getFavoriteIds()/toggleFavorite()
     └─ CapsuleRepository.getPublished()
  → HomeRecommendationPolicy
     → 当前档案城市优先
     → 想去或尚未记录优先
     → 同优先级内按类别轮转
     → category enum + placeId 稳定兜底
```

Home 不使用网络、天气、坐标或距离。最近记忆从已发布 Capsule 按创建时间倒序取最多 3 条，并用同次加载的 Place catalog 补充地点。快速记录先在 Home 内的可滚动选择器选择真实地点，再进入 `CapsuleEditor(placeId)`；空 catalog 时只提供新建地点。

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
     ├─ PhotoPickerCapability.pickImages
     │   → 双端系统相册 → 应用沙箱文件 URI
     └─ CapsuleMediaCleanup
         → catalog + draft 引用过滤 → CCMediaModule.deleteImages
  → replace CapsuleDetailPage(capsuleId)
  → TimelinePage / GalleryPage
  → CapsuleDetailPage(capsuleId)
  → PlaceDetailPage(placeId)
```

`BasePager.createExternalModules()` 必须登记 `CCMediaModule` 与 `CCLocaleModule` 的 shared `Module` 代理；Android/HarmonyOS host 的 render module 注册只提供 native 实现，不能替代 Pager 侧注册。缺少任一侧都会使 `Pager.acquireModule` 在用户点击时失败。

`CityCapsule` 保存正文、可选心情、标签、地点 ID、最多 9 个图片路径和创建/更新时间。编辑器支持同上下文草稿恢复、保存、未保存退出确认、发布与更新；新建草稿和既有碎片草稿不会互相误恢复。`LocalCapsuleRepository` 整体重写最多 500 条的 catalog，并串行化 publish/delete；更新一个已不存在的 ID 返回 `Missing`，不会静默新建。

时间轴按创建时间倒序读取 catalog，再按 `placeId` 补充地点信息；渲染层按设备本地 `yyyy-MM` 分组，并让每条记忆保持独立 lazy item。相册是同一数据集的照片网格，不建立第二份媒体索引；同样按本地年月分组。当前没有缩略图数据或缓存，Gallery 只以 18 张为一批逐步增加可组合的原图，不能把该保护描述为已实现缩略图。日期通过 `CCLocaleModule` 使用设备本地时区生成稳定 `yyyy-MM-dd`，shared 再转换为产品文案。

删除碎片、移除照片、丢弃草稿或拒收超额选择时，shared 先读取 catalog 与草稿构造保护集合，只把未引用候选路径交给平台删除；任一引用读取失败即延后清理。平台只允许删除 `filesDir/images/original` 直属托管文件。该机制覆盖正常业务操作，不是全目录垃圾扫描；进程在“复制完成、元数据尚未落盘”之间崩溃仍可能留下待后续维护处理的文件。

产品层禁止删除仍有关联城市碎片的地点，并在确认删除时再次查询关系；无关联地点才进入 `PlaceRepository.deletePlace`。底层 Repository 仍保留独立能力，跨 Repository 检查不是数据库事务。

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

业务页面没有直接调用 HMRouter。路由参数传 `placeId`、`capsuleId`、`requestId` 等 ID。AppShell、CapsuleEditor、CapsuleDetail、Gallery 已有真实 `@Page`；Timeline/Profile/Home 已迁为 AppShell 内部内容。MapExplore 仍只有协议，没有页面实现。

### 正式一级导航

```text
AppRoute.Home / Timeline / Profile
  → 同一 routeKey/pageName: app_shell
  → initialRootTab: home / timeline / profile
  → AppShellPage
     ├─ HorizontalPager(userScrollEnabled = false, beyondViewportPageCount = 2)
     │  ├─ HomeRootContent + 独立 LazyListState
     │  ├─ RecordRootContent + 独立 LazyListState + RecordRootView
     │  └─ ProfileRootContent + 独立 LazyListState
     └─ 唯一 AppBottomNavigation
        → 点击其他 Tab：animateScrollToPage(index)
        → 重复点击当前 Tab：no-op

二级页面 typed push → 独立 Page/AppScaffold（无底栏）→ back 返回同一 AppShell 实例
```

根 Tab 切换不再调用平台 dispatcher，因此不会产生三个原生根 host 或独立 Tab 路由栈。三个 typed 根 route 仍可用于冷启动、外部入口和兼容调用，但统一解析到 `app_shell` 并携带初始 Tab。Android/HarmonyOS 测试覆盖根 Tab 切换不增加原生栈项，以及 push 详情后 back 保留同一 AppShell host。

二级页若要回到指定根目标，调用 `backToRoot(AppRootTab)`：`AppShellRuntime` 先把目标 Tab 交给仍存活的壳，再执行 typed `backTo`；若壳已不在原生栈中，`resolveBackTo` 的 fallback request 也携带对应 `initialRootTab`。直接对 HOME/TIMELINE/PROFILE 调用普通 `backTo` 会丢失“返回后选中哪个 Tab”的语义，业务代码不得这样使用。

### Record Container 当前实现

```text
AppShellPage / RecordRootContent
  → AppSegmentedControl + RecordRootView
    ├─ TIMELINE: Timeline content
    └─ GALLERY: Gallery content
  → 两种视图共享 Capsule catalog 加载结果
  → CapsuleDetail 仍通过 AppNavigator.navigate(typed route)
```

时间轴/相册点击切换已在同一 Record 内容树内实现，切换后底栏和 Record 视图状态保留。Timeline 的月份标题和记忆条目直接作为 lazy items；Gallery 的月份与自适应网格也位于同一根滚动容器，并以显式“继续查看”扩大照片批次。Record 内部 `HorizontalPager` 与手指左右滑动尚未实现；独立 `GalleryPage` / `AppRoute.Gallery` 仍作为兼容入口存在，但正式一级 UI 不进入它。

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

`cc_meta` 只供平台迁移层使用，保存 schema/migration 状态与 type metadata。业务原图位于双端应用沙箱 `images/original`，MMKV 只保存 `file://` 路径；正常删除、移除和丢弃流程已有全引用保护清理，当前没有全目录垃圾扫描、缩略图、导出包或网络缓存。

## 平台与网络能力

- Android：在 Kuikly host 注册 `CCMediaModule`，使用系统多选文档契约选择图片并复制到 `filesDir/images/original`；取消、并发请求、复制失败均回传显式状态。没有相机、定位、地图或通用文件导入 launcher。
- HarmonyOS：注册 `CCMediaModule`，使用 `PhotoViewPicker` 选择图片、`fileIo` 复制到 `filesDir/images/original`，结果用同一 JSON 状态协议回传。Permission/FileImport 页仍是骨架；`KRMyView/KRMyModule/KRBridgeModule` 保留模板/临时代码。
- 图片 adapter 的 HTTP 示例只存在于诊断页，不是业务网络层。
- 没有 OkHttp/Ktor/Retrofit/NetStack 业务封装、RemoteDataSource、天气、地理编码、路线或 AI 调用。

## Target Architecture（已批准目标，非当前全量现状）

后续表现层按 Feature 渐进迁移为轻量 MVI：

```text
Kuikly UI
  → dispatch(Intent)
Feature Store / Executor
  → Repository / Capability
  → Mutation
  → pure Reducer
  → StateFlow<UiState>

Feature Store
  → Effect Flow
  → UI 执行 typed navigation / 一次性行为
```

PlaceList/Explore 与 Profile Overview/Editor 已完成 Store 代码迁移；其余 callback 型 StateHolder 仍是实际架构，不能据此宣称全项目已迁移。显式 coroutines、Reducer/Store/Effect/dispose 的 shared 与 Android 自动化已通过；Kuikly StateFlow 收集、前后台 Effect 与销毁行为仍待 Android/HarmonyOS 设备验收。详细契约与迁移门禁见 `MVI_ARCHITECTURE.md`。

MVI 只解决表现层状态流。简单 CRUD Store 可以直接调用 Repository；存在真实跨 Repository 业务规则时才引入 UseCase，出现本地/远端来源时再引入 Local/RemoteDataSource。不要为了形式先创建空层，也不建立全局 Redux Store。
