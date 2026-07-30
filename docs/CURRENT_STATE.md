# CityCapsule 当前开发状态

## 总体判断

项目处于基础版中期：跨端宿主、统一路由、双端 MMKV、本地档案/首次引导、离线地点，以及“地点详情 → 城市碎片 → 时间轴/相册 → 回忆”已经形成代码闭环。Record Flow 已进入 Phase 2 并完成首版实现，正式“探索 / 记录 / 我的”一级导航已经接通；但 Home 内容、Explore/Profile 视觉以及地图/定位仍未产品化，所以整个 App 仍不是完整基础版。

状态定义：DONE 为已形成真实闭环；PARTIAL 为可用但缺计划中的关键环节；PLACEHOLDER 为协议/骨架/开发验证；NOT_STARTED 为规划存在而无实现；BLOCKED 为有明确外部阻塞；UNKNOWN 为仅凭仓库无法确认。

## 功能状态

| 功能 | 状态 | 代码证据与边界 |
| --- | --- | --- |
| Android/HarmonyOS Kuikly 宿主 | DONE | 两端有启动、host、adapter 与平台工程；不代表所有业务双端手测完成 |
| 强类型共享路由 | DONE | `AppRoute/AppNavigator/AppRouteTable` + 双端 dispatcher/stack tests |
| 单一 AppShell / 正式一级导航 | DONE（代码）/PARTIAL（设备体验） | 一个 `AppShellPage`、一个 Bottom Navigation、三个常驻根内容；点击 Tab 以 `animateScrollToPage` 切换，根手势关闭，重复点击 no-op；shared/Android/HarmonyOS 测试与双端包构建通过，仍待设备视觉/动画/返回键走查 |
| Record 容器 | PARTIAL | Timeline/Gallery 已是同一 `RecordRootContent` 的状态视图并共享 catalog/底栏；点击切换和视图状态保留已实现，内部 HorizontalPager 与左右滑动尚未实现；独立 Gallery route 仅作兼容 |
| MMKV bridge 与主题旧值迁移 | DONE | 双端 2.4.0、typed protocol、迁移状态和测试资产 |
| Shared 主题与基础组件 | PARTIAL | 暖白/近黑/暖琥珀 Light/Dark token、统一 AppIcon 入口、PlaceCard/CapsuleCard、状态组件与 Bottom Navigation 已存在；图标仍是文本 glyph，Elevation 未完成视觉落地，缺自适应 pane |
| 本地档案与首次引导 | DONE | 启动决策、草稿恢复、保存/重置、双端 launch gate 和页面状态完整 |
| 地点本地 CRUD | PARTIAL | 新建/编辑/删除/持久化可用；没有 source，seed 地点也可删除，与“系统地点不可删”原计划不一致 |
| 地点搜索/筛选 | DONE | 纯本地字段搜索、分类/城市/区域/收藏过滤和排序有单测 |
| 收藏地点 | DONE（技术）/PARTIAL（产品） | 独立 ID 集合、容错、持久化完整；用户可见文案已改“想去”，底层保持 `Favorite*`；仍缺加入时间排序 |
| 首页 | DONE（代码）/PARTIAL（设备体验） | 已聚合 Profile/Place/Favorite/Capsule Repository，展示档案城市与头像、问候与搜索入口、可解释本地排序 Hero、分类、想去/同城地点、最多 3 条真实最近记忆和真实地点选择后的快速记录；不展示天气、距离或 AI 推荐，仍待双端视觉与交互走查 |
| 设置 | PARTIAL | 主题偏好真实可用，技术与路由验收文案已移除；缓存、存储占用、隐私、关于等规划设置未做 |
| 地点列表/详情 UI | PARTIAL | 地点详情已有“想去”、记忆计数和“在这里留下城市碎片”主 CTA；仍无地点照片、距离、地图和导航，列表视觉仍偏管理工具 |
| Profile UI | PARTIAL | 档案编辑/清除真实可用；已确认增加碎片数、关联地点数、想去数三项真实统计，但尚未接入对应 Repository 聚合 |
| 地图探索 | PLACEHOLDER | 只有 typed route，无 `@Page`、Native View 或平台地图 SDK |
| 权限原生页 | PLACEHOLDER | Harmony 可达但明确写“具体权限申请待实现”；Android launcher 未注册 |
| 文件导入原生页 | PLACEHOLDER | Harmony 可达但未调文件选择器；Android launcher 未注册 |
| 城市碎片模型/Repository/编辑器 | DONE（代码闭环） | 模型/校验/Codec、catalog/draft Key、Repository、编辑/草稿/发布/更新/退出确认和照片选择均存在；shared/Android 测试通过 |
| 时间轴/相册/碎片详情 | DONE（代码闭环）/PARTIAL（体验） | 时间倒序、地点补全、两列照片网格、详情/编辑/删除/回地点已接通；缺月份分组、平板双栏和设备手测 |
| 相册与业务文件媒体 | PARTIAL | Android/HarmonyOS 原生 Photo Picker、Pager/native 双侧模块注册、沙箱原图复制及引用保护清理已实现；Android 模拟器已完成系统 Picker 选图、回传和沙箱复制；HarmonyOS 已修复把 `file://media/...` 当普通路径传给 `copyFileSync` 的问题，现先打开受控 URI、使用 fd 复制并关闭文件；共享桥接同步异常会降级为 Failure 而不再使页面进程崩溃；最新 signed HAP 已编译，仍待真机安装和完整交互复验；无相机、缩略图 |
| 导入导出/备份 | NOT_STARTED | 无 manifest/ZIP/Repository/预览；文件页不构成实现 |
| 定位/距离/外部导航 | NOT_STARTED | Place 无坐标，平台无定位/地图能力 |
| 网络/天气/地理编码/路线 | NOT_STARTED | 无业务网络库与 RemoteDataSource |
| 路线/漫游/轨迹/打卡/扫码/成就 | NOT_STARTED | 进阶规划，无当前模型或页面 |
| AI/游记/明信片/接续/加密备份 | NOT_STARTED | 复杂版规划，无当前实现 |
| iOS/H5/小程序产品支持 | UNKNOWN | 有模板/target，缺 CityCapsule 功能验收；h5App/miniApp 目录缺失 |

## 页面清单

| 页面 | 状态 | 数据来源 | 入口 | 跳转目标 | 当前 UI 状态 |
| --- | --- | --- | --- | --- | --- |
| LaunchGate | DONE | profile/onboarding MMKV | 系统冷启动 | Home/Onboarding replace | 启动反馈页 |
| Onboarding | DONE | OnboardingRepository | LaunchGate/Settings | Home | 四步真实表单，但“默认档案”削弱引导意图，后续随产品视觉调整 |
| Home | DONE（代码）/PARTIAL（设备体验） | Profile/Place/Favorite/Capsule Repository | LaunchGate/探索 Tab | 地点列表/详情、想去、碎片详情、带 placeId 的碎片编辑器 | 已完成 P0-3 产品化内容与真实空/加载/部分失败降级；待双端视觉验收 |
| Place List | PARTIAL | Place/Favorite Repository | Home | Editor/Detail/back | 搜索筛选真实，内容层级偏管理工具 |
| Favorites | PARTIAL | 同上 | Home | Detail/back | 与地点列表复用，用户文案已为“想去”，视觉仍偏管理列表 |
| Place Detail | PARTIAL | Place/Favorite/Capsule Repository | 列表/回忆详情 | Capsule Editor/Timeline/Place Editor/back | 记录主 CTA 和记忆计数已接通；地点内容仍缺摄影、位置与导航 |
| Place Editor | DONE（当前模型范围） | Place Repository | 列表/详情 | Detail/back | 表单 CRUD 完整；不应成为核心探索视觉模板 |
| Profile | PARTIAL | Profile/Onboarding Repository | 我的 Tab | Settings/Onboarding | Profile 内容常驻 AppShell，编辑草稿在根 Tab 切换后保留；三项真实统计与想去内容尚未实现 |
| Settings | PARTIAL | SettingsRepository | Profile | Onboarding/back | 二级页无底栏；主题真实，规划设置未补齐 |
| Router Diagnostics | DEBUG | 无业务数据 | 正式产品 UI 不可达，仅非业务 pageName | Home/Settings | 明确开发诊断 |
| Image Adapter Diagnostics | DEBUG | assets/HTTP 示例 | 正式产品 UI 不可达，仅非业务 pageName | 无 | Kuikly 图片 adapter benchmark |
| Harmony Permission | PLACEHOLDER | route params | typed native route | back | 只显示骨架与参数 |
| Harmony File Import | PLACEHOLDER | route params | typed native route | back | 只显示骨架与参数 |
| Harmony Route Fallback | DONE（基础设施） | 失败参数 | guard/dispatcher | back/Home | 安全降级页，含必要诊断文本 |
| Capsule Editor | DONE（代码） | Capsule/Place Repository + PhotoPicker | Place Detail / Capsule Detail | Capsule Detail/back | 轻量日记编辑、照片/心情/标签、草稿和退出确认 |
| Capsule Detail | DONE（代码） | Capsule/Place Repository | Editor/Timeline/Gallery | Place Detail/Editor/Timeline | 回忆内容、照片、地点关联与删除确认 |
| Timeline | DONE（当前代码）/PARTIAL（目标体验） | Capsule/Place Repository | AppShell 记录 Tab/Place Detail/Capsule Detail | Capsule Detail | 已迁入 RecordRootContent 的时间轴视图；内部 Pager 动画/手势未做 |
| Gallery | DONE（当前代码）/PARTIAL（兼容清理） | 同 Timeline | AppShell Record segmented control；兼容 route | Capsule Detail/Timeline | 正式相册已是 RecordRootContent 内部视图；独立 GalleryPage/route 暂留兼容 |
| MapExplore | NOT_STARTED | 无 | 当前正式 UI 无入口 | 无 | 仅路由协议 |

## 规划对照

### 已按规划完成

- Kuikly/KMP shared 主体和 Android/HarmonyOS 宿主。
- shared typed navigation + Android dispatcher + HarmonyOS HMRouter 边界。
- 双端 MMKV 与主题 legacy migration。
- 本地档案、首次引导和主题设置。
- 地点基础 CRUD、离线搜索/筛选和收藏的技术链路。
- 城市碎片的本地模型、草稿/发布/编辑/删除、时间轴、相册、详情及双端相册 capability 的代码实现。

### 部分完成

- 基础设计系统、Home、Settings、Profile、地点模块。
- 原生权限/文件路由仅 Harmony 有骨架。
- 地点协议偏离原计划，当前不具备地图/距离/图片所需字段。

### 尚未实现

- 基础版核心仍缺：地图/定位、相机、缩略图、备份、Home/Profile Redesign 与完整设置。
- 进阶版与复杂版全部产品功能。

### 后续新增（初始规划未明确为当前阶段实现）

- `LaunchGate` 独立 shared 页面与较完整的启动修复策略。
- 3-store typed storage wire protocol、type metadata、catalog/favorite 容错和 mutation queue。
- 双端自有 Kuikly route stack coordinator 与详细 guard/logger/fallback。
- iOS/JS 构建模板目标。

### 已偏离 / NEEDS_REEVALUATION

- 地点从“实体独立 Key + 索引”改为单个有上限 catalog。
- Place 去掉 source/坐标/封面，seed 与用户地点无法区分。
- 当前状态层没有 UseCase/DataSource，Page/StateHolder 直接调 Repository。
- 初始 `profile:local` 等 colon key 规划变为 `cc_preferences` store + dotted wire keys；当前协议已冻结，不能强行恢复。
- 根设置声明不存在的 `h5App/miniApp`；应判断删除声明还是补回模板，不能把它们当产品功能。

## 已知问题与临时代码

- Home 已完成首版产品化聚合与本地推荐；分类入口以 typed 可选参数初始化地点列表筛选，搜索入口进入现有真实搜索页。Settings 的完整产品内容仍待后续 Feature 重构。
- `KRBridgeModule.ets` 有 close/copy/toast/date TODO；`KRMyModule/KRMyView` 有模板式 null 返回。
- Harmony 原生 placeholder 直接显示 JSON 参数；只适合开发阶段。
- Android 同时依赖 Picasso 与 Glide；实际图片 adapter 使用情况需要在媒体阶段统一，避免双栈长期存在。
- `settings.gradle.kts` 声明没有源码目录的 `h5App/miniApp`；本轮 Gradle 8.7 配置与测试仍成功，但两者是否保留为目标尚未确认。
- 构建输出提示 KMP 的通用 `ksp` 配置已弃用、显式 iOS source-set `dependsOn` 阻止默认 hierarchy template、部分 Gradle 特性不兼容未来 Gradle 9；这些不是本轮测试失败，但需要构建维护。
- Design System v2 已建立统一图标入口、照片资产登记门禁和真实 empty/error/loading 状态组件；双端字形与阴影仍待设备视觉验收。
- 产品入口已禁止删除有关联 Capsule 的地点；“曾经到访的地点”只作为旧数据或异常关系的读取降级。检查与删除跨两个 Repository，不具备数据库事务原子性。
- 正常删除/移除/丢弃流程已有引用保护清理；进程若在原图复制后、草稿或记录落盘前崩溃，仍可能留下无法由候选清理发现的文件。
- 时间轴、相册和详情日期已接双端设备本地时区格式器；bridge 不可用时才使用 UTC 降级。
- Record 页面仍依赖单栏 `AppScaffold`，平板列表/详情双栏尚未实现。
- 当前 repository mutation queue 只保证单实例/单进程顺序，不能等同数据库事务或跨页面全局并发控制。
- Home、地点列表与地点详情的想去切换已避免成功提示插入、当前内容重排和页面自身 revision 重载；Record/Profile/Editor 的加载替换、共享滚动状态和照片位置组合仍是待设备验证的抖动高风险区。

## 验证状态

2026-07-28 完成 `:shared:testDebugUnitTest`、Android APK 和 OHOS Kotlin/Native/signed HAP 重建。Android 闪退有完整堆栈证明模拟器运行的是未包含 Pager 媒体模块注册的旧 APK；覆盖安装最新包后，已实测 `DocumentsUI PickActivity` 打开、选择真实图片、回到编辑器并复制到 `files/images/original`，无 `AndroidRuntime` 崩溃。共享媒体 capability 另增加同步桥接异常保护和回归测试，宿主版本错配或模块遗漏时返回 Failure，不再让异常逃逸到 Kuikly 线程。

HarmonyOS 首轮闪退日志已明确为 `CCMediaModule` 未注册，注册与共享 bridge 异常降级随后完成。第二轮真机日志显示 Picker 已成功返回 `file://media/Photo/...`，但旧实现直接执行 `copyFileSync(sourceUri, target)`，在 `copy_file.cpp:IsAllPath` 以错误码 2 失败。当前实现已改为 `openSync(sourceUri)` 获取 Picker 授予读取权限的 `File`，再以 `sourceFile.fd` 复制到沙箱，并在 `finally` 关闭文件；复制前登记目标以便失败时清理半文件，错误日志只记录阶段、错误码和消息，不记录完整照片 URI。HarmonyOS 本地 Hypium 测试、ArkTS 编译、HAP 打包与签名已通过；成功/取消/失败恢复/重启读取仍须按 `P0_RECORD_FLOW_ACCEPTANCE.md` 在 HED-AL00 真机复验，因此 P0-0 不能标记 DONE。

P0-2 正式一级导航于 2026-07-28 完成代码与自动化验证：`:shared:testDebugUnitTest`、`:androidApp:testDebugUnitTest` 和 HarmonyOS `entry@default test` 均通过；Android Debug APK、HarmonyOS arm64 `libshared.so` 与 signed Debug HAP 已使用本轮源码重建成功。测试覆盖三个真实根路由映射、typed replace、重复点击当前 Tab no-op、跨根页替换后旧 Tab 不留在返回栈，以及根页进入详情再返回时只保留当前根页。双端设备上的底栏安全区、深浅色、字形和根页返回键行为仍需按本次验收流程手工确认。

P0-3A 于 2026-07-29 取代 P0-2 的根 Tab replace：Home/Timeline/Profile typed route 统一进入 canonical `app_shell`，底栏只创建一次，三个根内容位于同一无手势 HorizontalPager；底栏点击执行 `animateScrollToPage`，三个独立 LazyListState、根页面 remember 状态与 RecordRootView 在 Tab 切换间保留。详情、Editor、Settings 继续 typed route；Debug 不进入壳。`:shared:testDebugUnitTest`、`:androidApp:testDebugUnitTest`、Android Debug APK、HarmonyOS Kotlin/Native arm64、ArkTS entry test 与 signed Debug HAP 均通过。首次 HarmonyOS 真机验收发现平台 `HarmonyRouteCatalog` 仍是 P0-2 白名单，导致完成引导后的 `app_shell` 被 guard 拒绝；现已补登记 `app_shell`、撤销已不存在的 standalone `home/timeline/profile` Page，并将 `recoverHome()` 指向 canonical AppShell。HarmonyOS 单测、ArkTS 编译和 signed HAP 已重新通过，等待用新 HAP 覆盖安装后复验动画、滚动恢复、安全区与返回键。

2026-07-30 按 R1/R2/R3/R6 重新核对并验证 P0-3A：根 Tab 继续使用单一 AppShell Pager，不恢复已废弃的 typed replace。新增 shared 重复点击/快速选择状态测试，以及 Android JVM 的唯一 Bottom Navigation、壳内无根 route action、诊断页正式入口隔离门禁；`:shared:testDebugUnitTest` 与 `:androidApp:testDebugUnitTest` 通过，Android Debug APK、HarmonyOS arm64 `libshared.so`、ArkTS `entry@default test` 与 signed Debug HAP 均由当前源码重建成功。R5 真机验收已由用户确认通过，不修改 Pager 动画实现；HarmonyOS HAP 仍需覆盖安装执行完整设备清单。

2026-07-30 完成 P0-3 Home Redesign 代码：AppShell 注入并复用 Profile/Place/Favorite/Capsule Repository，Home 在探索 Tab 激活及 Place/Capsule revision 变化时重载；本地推荐规则、分类 typed 预筛选、最多 3 条最近记忆和先选地点再记录均已实现。`:shared:testDebugUnitTest`、`:androidApp:testDebugUnitTest`、Android Debug APK 与 `:shared:compileKotlinOhosArm64` 通过；双端设备视觉、Bottom Sheet 滚动和交互验收仍按 `P0_HOME_ACCEPTANCE.md` 执行。

2026-07-30 修复想去切换抖动：Home 加载时建立 Hero/辅助地点的稳定展示快照，切换想去只更新状态；地点列表和地点详情为 revision 失效事件增加页面 owner，跳过自己发出的重载；三处成功操作均不再在内容上方插入状态横幅。`:shared:testDebugUnitTest`（160 tests）、`:androidApp:testDebugUnitTest` 与 `:shared:compileKotlinOhosArm64` 通过。`linkDebugSharedOhosArm64` 因本机缺少 `OHOS_SDK_HOME`/默认 OpenHarmony SDK 而未完成，需在配置 SDK 的环境重跑；设备抖动验收仍待执行。

## 2026-07-28 Design System v2 状态

- `DONE`：暖白/近黑/中性灰/暖灰/琥珀浅色方案及配套深色方案；主题持久化 key、wire value 和协议版本保持不变。
- `DONE`：最小组件 API 已建立：统一图标入口、底部导航、两种 PlaceCard、两种 CapsuleCard、PhotoGrid、SearchField、Empty/Loading/Error、Overflow Menu、Bottom Sheet、Elevation token。
- `DONE`：地点列表接入 Compact PlaceCard 与类别 fallback；时间轴接入 CapsuleCard 和产品状态；相册接入共享 PhotoGrid。
- `DONE`：新增 `ASSET_ATTRIBUTION.md` 资产门禁；当前无产品可用地点摄影，诊断 `sample.png` 被明确排除。
- `DONE`：AppBottomNavigation 只存在于单一 AppShell，点击驱动无手势根 Pager 动画；重复点击 no-op，根切换不写入原生返回栈。
- `PARTIAL`：SearchField、Overflow Menu、Bottom Sheet 尚未接入正式 Home/详情页；组件存在不等于对应 Feature 完成。
- `PARTIAL`：Elevation 尚未完成双端阴影视觉走查；AppIcon 首版需要 Android/HarmonyOS 字形一致性验收。

- 2026-07-30：P0-4 Place Detail 已产品化：真实地点内容 → 想去探索行为 → 最近城市记忆 → 记录 CTA 的层级已落地；类别 Hero fallback、地址降级、更多菜单与最近三条已发布 Capsule 已接通。shared 161 tests 通过；双端设备视觉与交互仍需按验收流程确认。
