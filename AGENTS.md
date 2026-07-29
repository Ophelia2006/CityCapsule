# CityCapsule AI 协作规则

## 事实来源优先级

1. 当前实际代码、测试结果与运行行为。
2. 当前配置、数据结构和真实调用关系。
3. 已确认且与代码一致的当前项目文档。
4. 三份初始规划文件及其摘要 `docs/INITIAL_PLANNING_BASELINE.md`。
5. 其他历史文档。
6. 聊天记录。

初始规划不是“已实现”证明，但仍是未来功能开发的重要基线。发现规划、文档与代码不一致时，记录“原计划 / 当前实现 / 差异 / 可确认原因”；找不到原因时不得补写理由。

## 产品边界

CityCapsule / 城市胶囊是“城市探索 + 个人城市记录工具”，核心体验是“发现 → 探索 → 记录 → 回忆”。它不是社交媒体、攻略社区、导航 App、照片管理器或后台管理系统。不得在没有真实实现时宣传 AI 推荐、云同步、社区、实时后台或智能算法。

基础阶段产品一级导航采用“探索 / 记录 / 我的”。用户界面的“收藏”展示为“想去”，底层 `Favorite*` 命名与既有存储协议无需为文案而重构。地图在基础阶段属于探索方式，不提前成为一级 Tab。

## 当前技术与模块边界

- 跨端业务 UI：Kuikly Compose DSL，主要位于 `shared/src/commonMain`。
- 共享逻辑：Kotlin Multiplatform；当前只有 `shared` 一个主要业务 Gradle 模块，以 package 做逻辑隔离。
- 本地结构化存储：Android 与 HarmonyOS 均为 MMKV 2.4.0，经 `KeyValueStore` 和 `CCStorageModule` bridge 访问。
- 路由：共享层只依赖 `AppRoute` / `AppNavigator`；Android 使用 `AndroidRouteDispatcher`，HarmonyOS 使用 `HarmonyRouteDispatcher` 再进入 HMRouter。
- 主题：shared 语义 token + 双端系统外观 host。
- 当前没有数据库、网络业务数据源、MQ、Redis、RPC、地图 SDK、定位或相机实现。城市碎片已经接入双端系统相册选择；原图复制到应用沙箱，MMKV 只保存路径和结构化元数据。删除碎片、移除照片或丢弃草稿后，必须通过引用保护的媒体清理能力删除不再被 catalog/草稿引用的托管原图。
- iOS/H5/小程序来自 Kuikly 工程骨架，不属于当前已验收的 CityCapsule 产品目标；不得仅因目录或 target 存在就宣称支持。

业务页面不得直接依赖 Android SDK、ArkTS、HMRouter、MMKV 实例或原始路由字符串。平台 API 只能位于平台宿主/adapter/capability 边界。网络能力未来必须经 `RemoteDataSource` / Repository 接入，不能写入 Page、StateHolder 或领域模型。

## 数据与路由规则

- 新持久化 Key 只能登记在 `AppStorageKeys`，并同步审查 Android/HarmonyOS 协议、迁移和文档。
- 不修改既有 wire key、类型、store 或 schema 含义而不提供双端迁移。
- MMKV 只保存有明确上限的小型结构化数据。图片、视频、导出包、缩略图和轨迹点保存到应用沙箱文件系统，MMKV 只保存路径、索引和元数据。
- API Key 不能硬编码、写普通 MMKV 或进入默认备份；未来应使用平台安全存储。
- 路由优先传业务 ID，不传完整业务对象、Bitmap、PixelMap、Context 或回调。
- 新路由必须同时登记共享路由表、平台可用目录、dispatcher/原生注册和测试。
- 原生能力必须有失败、取消、拒绝权限和不支持状态；不得用模拟成功掩盖错误。

## 正式一级导航规则

- 基础阶段只有三个真实根目标：探索 → `AppRoute.Home`，记录 → `AppRoute.Timeline`，我的 → `AppRoute.Profile`；三个 typed route 都进入同一个 `AppShellPage`，只通过 `initialRootTab` 选择初始页。
- `AppShellPage` 只创建一个 Bottom Navigation，并以根 `HorizontalPager` 常驻 Home / Record / Profile 三个内容树。点击底栏调用 `pagerState.animateScrollToPage()`；重复点击当前 Tab 必须是 no-op。根 Pager 当前 `userScrollEnabled = false`，不得提前开放手指横滑。
- 三个根内容分别持有自己的滚动与页面状态；切换 Tab 不得重建平台页面。Record 内部“时间轴 / 相册”使用 Compose 状态切换并共享 catalog，内部切换不执行 `push`、`replace` 或 `back`；内部左右滑动 Pager 属于后续 Feature。
- Place/Capsule Detail、Editor、Settings 等二级页面继续使用 typed route 并位于 AppShell 之外，因此不显示底栏。现有独立 Gallery route/page 只作兼容入口，不是正式产品层级。
- 二级页需要返回指定根 Tab 时必须调用共享 `backToRoot(AppRootTab)`；不要直接 `backTo(HOME/TIMELINE/PROFILE)`，否则 canonical `app_shell` 虽能返回，但常驻壳无法得知应选中的 Tab。
- Debug、Router Diagnostics、Image Benchmark 可以保留为开发诊断页，但不得从正式产品 UI 进入。

## UI/UX 规则

- 正式页面不得出现 AppTheme、Repository、MMKV、Kuikly、HMRouter、路由验收、Replace、Debug 状态或“已接入”等开发信息。
- 一个页面优先只有一个 Primary CTA；整张内容 Card 可点击，避免连续的大按钮菜单和后台 CRUD 风格。
- 地点、城市碎片等真实内容单元才使用 Card，禁止 Everything is a Card。
- 视觉方向：70% 现代极简、20% 城市摄影、10% 旅行记忆；基础色为暖白、近黑、中性灰，Accent 使用克制的暖琥珀/夕阳橙。
- 视觉色彩主要来自真实城市照片；不得用假照片、假推荐或假业务数据冒充完成状态。
- 产品照片、插画和第三方图形必须先登记到 `docs/ASSET_ATTRIBUTION.md`；缺少可核验来源与授权时只能使用代码生成的类别 fallback，诊断页 `sample.png` 永远不得作为地点摄影素材。
- Feature 页面消费 design-system token，避免批量硬编码颜色、字号、间距和圆角。
- 手机使用单栏和底部导航；中大窗口采用有业务意义的列表/详情或地图/信息双栏，不能只拉宽手机布局。
- 未实现的功能不得提前放入主导航或正式首页；诊断入口保留在开发页。

## 修改前与交付要求

每次修改 Feature 前依次阅读：

1. 本文件；
2. `docs/ARCHITECTURE.md`、`docs/DECISIONS.md`、`docs/CURRENT_STATE.md`、`docs/TODO.md`；
3. UI Feature 还需阅读 `docs/INFORMATION_ARCHITECTURE.md`、`docs/USER_FLOWS.md`、`docs/WIREFRAMES.md`、`docs/DESIGN_SYSTEM_PROPOSAL.md`、`docs/UI_REFACTOR_PLAN.md` 中相关部分；
4. `docs/INITIAL_PLANNING_BASELINE.md` 中与 Feature 对应的部分；
5. 该 Feature 当前实际代码和测试。

一次只处理一个可验收 Feature。先明确允许/禁止修改范围、输入输出、状态、平台依赖、失败降级和验收步骤，再实现。不得顺手重构无关模块，不得覆盖用户已有改动。完成后运行相应 shared 单测、Android 单测、HarmonyOS 测试/构建（环境允许时）和必要的双端手工验收，并更新状态文档。Record Flow 已获准进入 Phase 2；其他大规模 UI 重构仍需按用户确认的 Feature 范围推进。
