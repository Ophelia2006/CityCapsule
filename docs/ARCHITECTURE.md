# CityCapsule 当前架构

> 2026-09-02 Explore 增量列表结构：`PlaceListPage` 使用 `AppFixedHeaderLazyScaffold`，本地与在线地点分别以 `local:<placeId>`、`online:<providerId>` 成为真正的 Lazy item；分页由 `LazyListState.layoutInfo.visibleItemsInfo` 中最后可见 key 进入末 3 项时触发。`PlaceListStore` 使用查询代次和已请求页集合隔离旧回调、合并重复分页请求，Reducer 只向尾部追加并按 provider ID 去重。

> 2026-08-20 增量：Profile v2 可引用 `images/avatar` 托管方形头像；Capsule v2 增加可选 `roamingSessionId`，由 typed `CapsuleEditor` route 传入并持久化。自由漫游以 500 米生成附近候选、200 米确认 GPS 到达，打卡顺序可转换为本地路线；这些平台相关能力在双端真机验收前仍为 PARTIAL。

> 2026-08-17 增量：一次性定位成功结果只进入 `CurrentLocationRuntime` 进程内状态，供 Explore、地图相机与 Home 距离排序复用，不新增持久化 Key；用户地点显式坐标仍随 Place V3 持久化。地点托管封面删除必须先经 `RepositoryPlaceMediaCleanup` 读取 Place catalog、已发布 Capsule 与草稿的全部引用，任一读取失败即停止删除。

> 2026-08-17 在线地点增量：`Explore UI → PlaceList Intent → PlaceListStore → PlaceRemoteDataSource/ReverseGeocodeCapability → CCPlaceNetworkModule → 高德 Web API`。Android/HarmonyOS 网络模块持有各自本机注入的 Web Key，共享层只收发请求参数和响应，不读取 Key。在线 POI 只有经用户明确选择后才转换为 `PlaceDraft(source=IMPORTED)` 写入 `LocalPlaceRepository`。

> 2026-08-31 在线查询缓存增量：UI 仍只依赖 `PlaceRemoteDataSource`；运行时注入 `CachingPlaceRemoteDataSource → AmapPlaceRemoteDataSource`。缓存是进程内 32 页、10 分钟 TTL 的 LRU，只保存成功的 `RemotePlace` 页，不写 MMKV、不进入备份，也不缓存失败。

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
- `app/navigation`：`AppShellPager/AppShellPage` 是唯一产品根壳；`AppRootScaffold` 只创建一个 Bottom Navigation，根 `HorizontalPager` 常驻 Home、Record、Roam、Profile 四个内容树。Roam 根内容连接活动会话、路线、想去和漫游回顾，具体会话仍是壳外二级页。
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

PlaceList/Explore 是当前首个轻量 MVI Feature。UI 只读取 State、派发 Intent，并在 UI 边界消费 typed navigation Effect；Store 不持有 Navigator 或平台对象，`DisposableEffect` 负责 `dispose()`。搜索置顶，分类为横向 chips，高级城市/区域/只看想去筛选位于 Bottom Sheet；地点整行进入详情，想去心形独立操作。默认仍按档案城市稳定优先并展示“本地点目录”；只有用户主动请求定位且成功、地点本身也有真实坐标时，才附加直线距离。

### 一次性当前位置与距离

```text
Explore CurrentLocationRequested Intent
  → LocationCapability.getCurrentLocation()
  → CCLocationModule
     ├─ Android runtime permission → GPS/Network 双 provider single update，首个结果完成（10s timeout）
     └─ HarmonyOS permission → Location Kit getCurrentLocation（10s timeout）
  → LocationResult
  → Store Event（request operation guard）
  → LocationResolved Mutation
  → pure Reducer → PlaceListUiState
  → GeoDistance.meters(current, place.geoPoint)
```

`LocationResult` 明确区分 Success、PermissionDenied、PermissionPermanentlyDenied、ServiceDisabled、Unavailable 与 Failure。位置只存在于当前 Store State，不写 MMKV；失败结果清空位置与精度，因此 UI 无法继续显示旧距离。Store dispose 后回调不能再进入事件队列，请求序号也会阻止旧请求覆盖新请求。定位不在启动时触发，也不是浏览地点或未来地图 Marker 的前置条件。

### 探索首页聚合与本地推荐

```text
AppShellPage
  → HomeRootContent
  → HomeStateHolder
     ├─ LocalProfileRepository.getProfileSnapshot()
     ├─ ExploreCityRepository.get()
     ├─ PlaceRepository.getCatalogSnapshot()
     ├─ FavoriteRepository.getFavoriteIds()/toggleFavorite()
     └─ CapsuleRepository.getPublished()
  → HomeRecommendationPolicy
     → 只保留当前探索城市
     → 想去或尚未记录优先
     → 同优先级内按类别轮转
     → category enum + placeId 稳定兜底
```

Home 不使用网络、天气或 AI 推荐。当前探索城市由 `explore.city_selection` 独立持久化，不覆盖 `profile.homeCity`；最近记忆从已发布 Capsule 按创建时间倒序取最多 3 条，并用同次加载的 Place catalog 补充地点。快速记录先选择当前城市的真实地点再进入 `CapsuleEditor(placeId)`。

搜索覆盖名称、标签、城市、区域、地址、备注；支持类别、城市、区域和只看收藏过滤。没有在线 POI、距离、定位或推荐算法。

### 地点详情与 CRUD

地点媒体统一由 `PlaceMedia` 解析：地点自己的 `visualRef` 优先，其次读取 `places.photo_cache` 中仍有效的高德 POI 图片 URL，最后始终保留代码生成的类别 fallback。Home 与 Explore 只读取本地缓存，不为列表批量发起网络请求；地点详情在没有本地封面和有效缓存时才按需查询 `PlaceRemoteDataSource`，成功后写入最多 100 条、有效期 30 天的可清理缓存。远程图片加载失败会立即删除对应缓存项。

远程地点图片在进入平台图片 adapter 前还经过 commonMain `ImageLoadCoordinator`：首批 6 个地点使用 `VISIBLE` 优先级，其余使用 `PREFETCH`；进程内最多 3 个唯一 URL 冷请求并发，同 URL 订阅等待首个 owner 完成并复用平台缓存。`DisposableEffect` 释放离开组合的 lease；Android adapter 由 Glide 按实际布局尺寸解码并使用其内存/磁盘缓存。详细边界见 `IMAGE_LOADING_OPTIMIZATION.md`。

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

Repository 用内存 mutation queue 串行化本进程内写操作。Place catalog 当前为 schema v3：`PlaceSource` 区分 `SEED/USER/IMPORTED`；公共 `description`、私人 `personalNote`、`contentSource`、`GeoPoint` 与 `PlaceVisualRef` 均有独立字段。codec 可读取 v1/v2：旧 seed note 迁为 description，用户地点 note 迁为 personalNote；seedVersion 升级只刷新/补充精确 seed ID，不删除用户地点。当前内置上海 15 个、杭州 4 个地点；没有已授权真实摄影的 seed 使用类别 fallback。

地点类别仍使用同一 `category` wire 字段，2026-08-25 起细化为可直接呈现的产品类别并附带稳定 Emoji。新增细分类不改变 Place schema；seedVersion v4 只刷新已知 seed 的类别。旧 `culture/food/nature` 继续解码，用于兼容历史用户/导入数据，但新建地点不再提供这些宽泛选项。

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

### Settings 与数据归档

```text
SettingsPage
  → SettingsIntent
  → SettingsStore（串行事件 / StateFlow / Effect）
     ├─ SettingsRepository → settings.theme_mode
     ├─ DataBackupRepository → 六个长期 persistent keys
     └─ DataArchiveCapability → CCDataArchiveModule
        ├─ Android：Storage Access Framework + java.util.zip
        └─ HarmonyOS：DocumentViewPicker + zlib
```

归档格式为版本化 ZIP，固定包含 `data/backup.json`、`media/index.json` 与存在的 `media/images/*`。备份保存主题、本地档案、引导完成版本、地点目录、想去 ID 与城市碎片 catalog；`onboarding.draft`、`capsules.draft` 等 cache key 不进入备份。导入必须先解压到 cache staging、校验 backupVersion/完整 key 集合及每个既有 codec、显示数量预览；用户确认后才在应用沙箱创建导入前恢复 ZIP、复制导入媒体并写入数据。写入失败时恢复旧 snapshot 并清理本次创建的托管媒体；自动恢复不完整时保留内部恢复包并显示明确错误。

正式 Settings 只调用共享 capability，不使用旧 `NativeFileImport` 骨架路由。路由 Push/Replace/BackTo 等诊断集中在非业务 `router` pageName（页面标题为 Developer Tools），正式产品 UI 不提供入口。

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

业务页面没有直接调用 HMRouter。路由参数传 `placeId`、`capsuleId`、`requestId` 等 ID。AppShell、CapsuleEditor、CapsuleDetail、Gallery、MapExplore、RoamingSession 与 RoamingHistory 均有真实 `@Page`；Timeline/Profile/Home/Roam 是 AppShell 内部根内容。

### 正式一级导航

```text
AppRoute.Home / Timeline / Profile + AppRootTab.ROAM
  → 同一 routeKey/pageName: app_shell
  → initialRootTab: home / timeline / roam / profile
  → AppShellPage
     ├─ HorizontalPager(userScrollEnabled = false, beyondViewportPageCount = 3)
     │  ├─ HomeRootContent + 独立 LazyListState
     │  ├─ RecordRootContent + 独立 LazyListState + RecordRootView
     │  ├─ RoamingRootContent + 独立 LazyListState
     │  └─ ProfileRootContent + 独立 LazyListState
     └─ 唯一 AppBottomNavigation
         → 点击其他 Tab：scrollToPage(index)
        → 重复点击当前 Tab：no-op

二级页面 typed push → 独立 Page/AppScaffold（无底栏）→ back 返回同一 AppShell 实例
```

根 Tab 切换不再调用平台 dispatcher，因此不会产生四个原生根 host 或独立 Tab 路由栈。Home/Timeline/Profile typed 根 route 仍可用于冷启动、外部入口和兼容调用；Roam 当前是壳内根目标，通过 `AppShellRuntime` 选择。Android/HarmonyOS 测试覆盖根 Tab 切换不增加原生栈项，以及 push 详情后 back 保留同一 AppShell host。

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

### 当前自适应页面结构

- `AppRootScaffold` 将根内容限制在 1200dp；普通单栏页仍为 720dp，Editor 使用 640dp 可读宽度。
- 共享 `AdaptivePane` 以 600dp 为断点：紧凑窗口只渲染主 pane，并由 typed route 承担详情；宽窗口渲染 420dp 主 pane、24dp 间距和自适应详情 pane。
- Explore 当前宽屏为“地点目录 / 地点信息”，Record 当前宽屏为“时间轴 / 城市碎片阅读”；右 pane 的“打开详情”继续进入既有完整详情页。
- Map 已作为 Explore 内部“列表 / 地图”视图实现，不是独立一级 Tab；双端 Native View 与 Marker 已接入。`AdaptivePane` 仍可供后续大屏“地图 / 地点信息”布局复用，当前设备验收重点仍是手机单栏流程。

## 数据与缓存

当前没有关系数据库、Entity/DTO 映射层、Redis、MQ 或 RPC。领域模型直接通过自定义 JSON codec 写入 MMKV；没有远端 DTO。

| Store | Key | 类型 | 谁写 | 谁读/何时删除 |
| --- | --- | --- | --- | --- |
| `cc_preferences` | `settings.theme_mode` | string | SettingsRepository / 平台旧值迁移 | 启动与 Settings；重置主题时删除 |
| `cc_preferences` | `profile.local_profile` | JSON object | Profile/Onboarding Repository | 启动、档案页；重置本地状态时删除 |
| `cc_preferences` | `onboarding.completed_version` | long | OnboardingRepository | LaunchGate；重置时删除 |
| `cc_cache` | `onboarding.draft` | JSON object | OnboardingStateHolder 经 Repository | 引导恢复；完成或重置时删除 |
| `cc_preferences` | `places.catalog` | JSON object，最多 500 地点 | LocalPlaceRepository | 地点全流程；暂无整体清除 UI |
| `cc_preferences` | `explore.city_selection` | JSON object，当前城市 + 最近城市 | LocalExploreCityRepository | Home/Explore/Map 共享浏览范围；用户确认切换时更新 |
| `cc_preferences` | `favorites.place_ids` | JSON object，最多 500 ID | LocalFavoriteRepository | 列表/详情；取消、地点删除或悬空修复时更新 |
| `cc_preferences` | `capsules.catalog` | JSON object，最多 500 条碎片 | LocalCapsuleRepository | 详情/时间轴/相册/地点记忆计数；发布、更新、删除时整体重写 |
| `cc_cache` | `capsules.draft` | JSON object，单个可恢复草稿 | CapsuleEditor 经 Repository | 同一新建/编辑上下文恢复；匹配的发布或明确放弃时删除 |
| `cc_cache` | `places.photo_cache` | JSON object，最多 100 条地点远程图片引用 | PlaceDetail 写；Home/Explore/Detail 读 | 30 天过期、加载失败删除、不进入备份 |

`cc_meta` 只供平台迁移层使用，保存 schema/migration 状态与 type metadata。业务原图位于双端应用沙箱 `images/original`，MMKV 只保存 `file://` 路径；正常删除、移除和丢弃流程已有全引用保护清理。当前没有全目录垃圾扫描、生成式缩略图或导出包；地点远程图片仅缓存 URL 引用，实际图片字节仍由现有图片加载栈管理。

## 平台与网络能力

- Android：Kuikly host 注册媒体、一次性前台定位、外部导航与 `CCAmapView`；地图由高德 Android `MapView` 实现。相册图片复制到 `filesDir/images/original`；仍没有相机能力。
- HarmonyOS：注册媒体、一次性前台定位、外部导航与 `CCAmapView`；地图由高德 `MapViewComponent` 实现，并必须位于明确全尺寸的 `Stack` 宿主中以保证 Surface 与 Kuikly 稳定合成。Permission/FileImport 页仍是骨架；`KRMyView/KRMyModule` 示例文件不再注册到产品 host，`KRBridgeModule` 仍承载兼容诊断能力。
- 图片 adapter 的 HTTP 示例只存在于诊断页，不是业务网络层。
- 没有 OkHttp/Ktor/Retrofit/NetStack 业务封装、RemoteDataSource、天气、地理编码、路线或 AI 调用。

## Explore 地图与外部导航（P2-3 当前实现）

```text
PlaceListPage
  → PlaceListIntent（列表/地图、隐私同意、MapViewEvent）
  → PlaceListStore / pure reducer
  → ExploreMapViewState
  → Kuikly CCAmapView
     ├─ Android KRAmapView → 高德 Android MapView
     └─ HarmonyOS KRAmapView → 高德 MapViewComponent
  → MarkerSelected(placeId)
  → shared catalog 解析摘要
  → typed PlaceDetail(placeId)
```

地图仍是 Explore 内部视图，不是一级 Tab。首次选择地图先由共享 UI 获取明确同意，Native View 只在同意后调用高德隐私接口和 SDK 初始化。缺 Key 或初始化失败通过 `MapAvailability` 回到列表；这些降级分支已有代码但尚未完成双端场景验收。seed catalog v2 为 8 个内置地点补充 WGS-84 坐标；旧 seed 数据解码时只为缺坐标的精确 seed ID 补坐标，不覆盖用户地点或已有坐标。Android 使用高德 `CoordinateConverter`，HarmonyOS adapter 在渲染边界执行 WGS-84 → GCJ-02；供应商坐标不写回 catalog。

地点详情只在 `geoPoint != null` 时显示外部导航操作，通过 `ExternalNavigationCapability` 唤起系统/已安装地图应用，不实现路线算法。

## Target Architecture（已批准目标，非当前全量现状）

## P2-5 媒体派生与维护

`images/original/<managed-id>` 是 catalog/draft 持久化的唯一媒体引用；`images/thumbnail/<managed-id>.jpg` 是不写 MMKV 的可再生文件。Timeline/Gallery 经 `MediaMaintenanceCapability` 按需生成并优先加载缩略图，失败回退原图；Detail 使用原图。

无引用清理由 `RepositoryMediaMaintenance` 先完整读取 published catalog 与 draft，再把保护集合交给平台。任一读取失败即停止。平台只删除受控目录内且超过一小时宽限期的文件，并联动删除缩略图。

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

## 相机与托管媒体调用链

```text
CapsuleEditorPage
  → 添加照片 Bottom Sheet
  → CameraCapability / PhotoPickerCapability
  → CCMediaModule
  → Android TakePicture + FileProvider
    或 HarmonyOS cameraPicker + saveUri
  → filesDir/images/original/<managed file>
  → CapsuleDraft.imagePaths
  → Capsule Repository / MMKV（仅保存 file:// 路径）
```

系统相机启动前由平台宿主创建受控目标；取消、失败或空结果由平台立即清理。成功路径与相册复制路径进入相同的 `imagePaths` 协议，移除照片、丢弃草稿和删除碎片继续经 `RepositoryCapsuleMediaCleanup` 做 catalog/草稿全引用保护，再由双端 `CCMediaModule.deleteImages` 限定删除 `images/original` 直属文件。

## 本地路线 7A

`Explore → typed AppRoute → LocalRoute Page → LocalRouteStore (MVI) → DefaultLocalRouteRepository → routes.catalog / MMKV`。Repository 在写入前通过 PlaceRepository 校验有序地点 ID；道路规划与漫游运行时见后续 7B 和路线规划链路。

路线编辑器还通过 `ExploreCityRepository` 读取 Home / Explore / Map 共用的当前探索城市。UI 只展示 `Place.city` 与当前城市匹配的可添加地点，Store 在处理 `AddPlace` 时执行相同校验，避免绕过 UI 添加跨城地点；比较时仅统一首尾空白和可选“市”后缀，不做模糊城市推断。旧路线已经保存的跨城地点继续显示并允许用户手动移除，不在读取时静默改写持久数据。`ExploreCityRuntime.revision` 变化会触发路线页重新加载城市上下文。

## 漫游会话 7B

`LocalRoute Page → typed RoamingSession route → RoamingSessionStore → LocalRoamingSessionRepository → roaming.session / MMKV`。Repository 串行执行状态转换；绑定路线开始前校验 `routeId`。该链路不访问 Location Capability。

## 前台轨迹 7C

`RoamingSessionPage 前台循环 → LocationCapability → TrackRepository → CCTrackModule → files/tracks/*/chunk_N.json`；Repository 只把分片路径索引和状态写入 `roaming.track`。定位失败进入 `INTERRUPTED`，不结束漫游。

## 打卡与总结 7D

定位采样用 `GeoDistance` 判断 150 米附近，只产生“确认到达”候选；用户确认后才写 `roaming.check_ins`。定位中断只允许 `MANUAL` 标记。总结按需读取 Route、RoamingSession、CheckIn、轨迹分片和时间范围内 CityCapsule，不保存伪造或重复的 summary snapshot。

## 漫游回顾与想去联动（2026-08-24）

`RoamingRecord` v2 与 `CityCapsule.roamingSessionId` 是报告事实来源。路线编辑器只在高德步行 API 成功后保存最多 500 点的 `PlannedRouteSnapshot`；漫游结束时固化计划快照、实际 Track 文件路径/距离、到达快照和“到达当时是否想去”。`buildRoamingReport` 只做纯派生：按时间合并到达与碎片，并计算心情/标签摘要、完成想去、临时发现、跳过地点与绕路距离。原始 GPS 点仍在轨迹文件中。

地图契约同时携带计划点和实际点，双端原生高德 View 分别绘制灰色计划线和暖琥珀实际线。共享 `ShareCapability` 进入 Android ACTION_SEND / HarmonyOS Share Kit；当前是产品内卡片预览和系统文本分享，不伪装成尚未生成的图片卡片。

2026-08-30 起，按路线开始漫游以“可用的真实道路快照”为前置条件：已有且顺序一致则直接复用；没有则由 `LocalRouteStore → RoutePlanningRemoteDataSource` 自动规划，成功保存后导航，失败停留并显示真实错误。会话页的下一站只按 `orderedPlaceIds - checkedInPlaceIds` 取首项；往期记忆只读取该地点已发布且不属于当前会话的 Capsule。结束 ACTIVE 会话前尝试一次最终定位采样，但权限/服务失败时仍允许结束，报告按实际点数降级。

```text
想去 / 路线编辑
  → LocalRouteStore（想去地点优先）
  → RoamingSessionStore
     ├─ 到达确认 → CheckInRepository
     ├─ 若原为想去 → FavoriteRepository.remove（可撤销）
     ├─ 留下城市碎片 → CapsuleEditor(placeId, roamingSessionId)
     └─ 结束 → Track.complete → RoamingHistoryRepository.archive
  → 记录 / 漫游回顾 → RoamingHistoryPage
     → 地点到达记录 → 已有 Capsule 详情 / 补记 Capsule
```

`roaming.history` 保存最多 100 条已结束漫游快照，每条最多 20 个到达地点。快照保存会话类型、路线名与有序地点快照、开始/结束时间、真实轨迹距离、到达方式及轨迹分片路径。快照 ID 继续使用会话 `startedAtEpochMs` 字符串，与现有 Capsule `roamingSessionId` 关联协议一致。结束后或重启读到 `ENDED` 会话时均执行幂等归档，防止“会话已结束、历史未写入”的崩溃窗口。轨迹坐标仍只存文件，MMKV 不保存点集。

2026-08-24 增量：`RoamingSessionStore` 在读取轨迹索引后通过 `TrackFileCapability.readChunks()` 恢复已有点，每次成功采样在文件落盘后再更新 UI State。`ExploreMapViewState.trackPoints` 把 WGS-84 轨迹交给既有 `CCAmapView`；Android 使用 `PolylineOptions`，HarmonyOS 使用等价 `PolylineOptions`，两端都在渲染边界转为高德坐标。文件保留全量点，跨端地图 State 最多等距取 500 个显示点并保留首尾，不改动 Track schema。

漫游页的“留下城市碎片”允许选择当前地点 catalog 中的任意地点：按路线漫游优先排列本次路线地点与附近地点，自由漫游优先排列附近地点，其后补齐已到达地点和全量 catalog。有定位时默认选中最近地点，无附近结果时按路线顺序选择首个地点；确认后只通过 typed `CapsuleEditor(placeId, roamingSessionId)` 进入编辑器。恢复 ACTIVE/PAUSED 会话时，只以会话持久化的 `routeId` 作为当前路线，不能继续使用页面请求携带的另一条 `routeId`。

地点详情可读取本地路线 catalog，并把当前地点 ID 追加到用户选定的已有路线；重复地点不追加，空路线 catalog 引导进入路线编辑页。

当前采样仍由漫游 Kuikly Page 的会话协程驱动，能覆盖页面保持活动时的开始到结束与页内导航。Android 前台服务、HarmonyOS 长时任务/后台定位尚未实现；锁屏、系统回收或长时切到其他 App 时不得宣称无缺口后台轨迹。

2026-08-24 回顾呈现增量：`RoamingHistoryStore → TrackFileCapability.readChunks(trackChunkPaths)` 读取归档记录引用的真实轨迹点，详情页在用户确认地图 SDK 后绘制轨迹，同时聚合 `roamingSessionId == record.id` 的已发布 Capsule，展示时间、时长、距离、到达地点数、碎片数以及带首张照片的碎片卡。轨迹点继续只存在沙箱分片文件中，不复制进 MMKV 历史模型。

本地路线编辑器只持久化用户最终确认的地点顺序，并通过长按拖拽和辅助菜单调整；任何可视路线都必须来自真实道路规划结果，绝不把地点坐标直接连线。

2026-08-25 起路线规划调用链为 `LocalRoute UI → LocalRouteStore → RoutePlanningRemoteDataSource → CCPlaceNetworkModule.walkingRoute → 高德 Web 步行 API → PlannedWalkingRoute → ExploreMapViewState → 双端原生 CCAmapView`。P0 按手动顺序顺序请求相邻地点；P1 仅对 2–8 个地点建立真实道路距离矩阵，固定首点并执行最近邻 + 2-opt。该结果称为“推荐顺序”，不宣称全局最优。规划结果当前只存在 Store State，不写 MMKV 或备份；路线持久层仍只保存用户最终确认的地点 ID 顺序。

Home 探索推荐增加地图入口，最多标记当前本地推荐中的 5 个有坐标地点；地图下方以类别 Emoji + 地点名称展示同页选择项，点击后选中对应 Marker，并可进入地点详情。该入口不再依赖地点多图或缩略图模型。
