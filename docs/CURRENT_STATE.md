# CityCapsule 当前开发状态

## 总体判断

项目处于基础版中期：跨端宿主、统一路由、双端 MMKV、本地档案/首次引导、离线地点，以及“地点详情 → 城市碎片 → 时间轴/相册 → 回忆”已经形成代码闭环。Record Flow 已进入 Phase 2 并完成首版实现；但 Home、一级导航、Explore/Profile 视觉以及地图/定位仍未产品化，所以整个 App 仍不是完整基础版。

状态定义：DONE 为已形成真实闭环；PARTIAL 为可用但缺计划中的关键环节；PLACEHOLDER 为协议/骨架/开发验证；NOT_STARTED 为规划存在而无实现；BLOCKED 为有明确外部阻塞；UNKNOWN 为仅凭仓库无法确认。

## 功能状态

| 功能 | 状态 | 代码证据与边界 |
| --- | --- | --- |
| Android/HarmonyOS Kuikly 宿主 | DONE | 两端有启动、host、adapter 与平台工程；不代表所有业务双端手测完成 |
| 强类型共享路由 | DONE | `AppRoute/AppNavigator/AppRouteTable` + 双端 dispatcher/stack tests |
| MMKV bridge 与主题旧值迁移 | DONE | 双端 2.4.0、typed protocol、迁移状态和测试资产 |
| Shared 主题与基础组件 | PARTIAL | 色彩/字体/间距/圆角/动效、SegmentedControl 与多种组件存在；缺统一 Icon、Elevation/Shadow、PlaceCard 和自适应 pane，主色仍未迁移到 Proposal 的暖琥珀方案 |
| 本地档案与首次引导 | DONE | 启动决策、草稿恢复、保存/重置、双端 launch gate 和页面状态完整 |
| 地点本地 CRUD | PARTIAL | 新建/编辑/删除/持久化可用；没有 source，seed 地点也可删除，与“系统地点不可删”原计划不一致 |
| 地点搜索/筛选 | DONE | 纯本地字段搜索、分类/城市/区域/收藏过滤和排序有单测 |
| 收藏地点 | DONE（技术）/PARTIAL（产品） | 独立 ID 集合、容错、持久化完整；用户可见文案已改“想去”，底层保持 `Favorite*`；仍缺加入时间排序 |
| 首页 | PLACEHOLDER | 只有共享设计系统说明和导航大按钮，未加载档案、地点推荐、最近记忆或统计 |
| 设置 | PARTIAL/DEBUG | 主题真实可用；其余规划设置未做，并混入 Push/BackTo 路由验收文案 |
| 地点列表/详情 UI | PARTIAL | 地点详情已有“想去”、记忆计数和“在这里留下城市碎片”主 CTA；仍无地点照片、距离、地图和导航，列表视觉仍偏管理工具 |
| Profile UI | PARTIAL | 档案编辑/清除真实可用；仍是本地档案 CRUD 页，不是“我的城市档案”聚合页 |
| 地图探索 | PLACEHOLDER | 只有 typed route，无 `@Page`、Native View 或平台地图 SDK |
| 权限原生页 | PLACEHOLDER | Harmony 可达但明确写“具体权限申请待实现”；Android launcher 未注册 |
| 文件导入原生页 | PLACEHOLDER | Harmony 可达但未调文件选择器；Android launcher 未注册 |
| 城市碎片模型/Repository/编辑器 | DONE（代码闭环） | 模型/校验/Codec、catalog/draft Key、Repository、编辑/草稿/发布/更新/退出确认和照片选择均存在；shared/Android 测试通过 |
| 时间轴/相册/碎片详情 | DONE（代码闭环）/PARTIAL（体验） | 时间倒序、地点补全、两列照片网格、详情/编辑/删除/回地点已接通；缺月份分组、平板双栏和设备手测 |
| 相册与业务文件媒体 | PARTIAL | Android/HarmonyOS 原生 Photo Picker 与应用沙箱原图复制已实现；`CCMediaModule` 已同时登记到 shared Pager 与双端 native host，缺失模块会降级为失败结果而不是终止进程；shared/Android 单测、Harmony native 链接和 HAP 编译通过，最新 HAP 已安装到真机；仍待真机复验选择、取消、复制失败与重启读取，且无相机、缩略图和孤立文件清理 |
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
| Home | PLACEHOLDER | 仅主题设置 | LaunchGate/返回首页 | 地点、想去、Timeline、Profile、Settings | 开发说明 + 大按钮菜单；Timeline 入口真实但首页本身未产品化 |
| Place List | PARTIAL | Place/Favorite Repository | Home | Editor/Detail/back | 搜索筛选真实，内容层级偏管理工具 |
| Favorites | PARTIAL | 同上 | Home | Detail/back | 与地点列表复用，用户文案已为“想去”，视觉仍偏管理列表 |
| Place Detail | PARTIAL | Place/Favorite/Capsule Repository | 列表/回忆详情 | Capsule Editor/Timeline/Place Editor/back | 记录主 CTA 和记忆计数已接通；地点内容仍缺摄影、位置与导航 |
| Place Editor | DONE（当前模型范围） | Place Repository | 列表/详情 | Detail/back | 表单 CRUD 完整；不应成为核心探索视觉模板 |
| Profile | PARTIAL | Profile/Onboarding Repository | Home/Settings | Onboarding/back | 编辑与清除完整，缺统计、足迹、想去内容 |
| Settings | PARTIAL/DEBUG | SettingsRepository | Home | Profile/Onboarding/Home/Settings | 主题真实；混入技术说明和路由测试 |
| Router Diagnostics | DEBUG | 无业务数据 | 非业务 pageName | Home/Settings | 明确开发诊断 |
| Image Adapter Diagnostics | DEBUG | assets/HTTP 示例 | 非业务 pageName | 无 | Kuikly 图片 adapter benchmark |
| Harmony Permission | PLACEHOLDER | route params | typed native route | back | 只显示骨架与参数 |
| Harmony File Import | PLACEHOLDER | route params | typed native route | back | 只显示骨架与参数 |
| Harmony Route Fallback | DONE（基础设施） | 失败参数 | guard/dispatcher | back/Home | 安全降级页，含必要诊断文本 |
| Capsule Editor | DONE（代码） | Capsule/Place Repository + PhotoPicker | Place Detail / Capsule Detail | Capsule Detail/back | 轻量日记编辑、照片/心情/标签、草稿和退出确认 |
| Capsule Detail | DONE（代码） | Capsule/Place Repository | Editor/Timeline/Gallery | Place Detail/Editor/Timeline | 回忆内容、照片、地点关联与删除确认 |
| Timeline | DONE（代码）/PARTIAL（体验） | Capsule/Place Repository | Home/Place Detail/Capsule Detail | Capsule Detail/Gallery | 时间倒序内容流和完整状态；未做月份分组/大屏双栏 |
| Gallery | DONE（代码）/PARTIAL（体验） | 同 Timeline | Timeline | Capsule Detail/Timeline | 两列照片 Grid；无月份分组、缩略图生成 |
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

- 基础版核心仍缺：地图/定位、相机、媒体清理/缩略图、备份、正式一级导航、Home/Profile 与完整设置。
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

- 正式 Home/Settings 泄露 AppTheme、AppRoute、AppNavigator、MMKV、Replace、Push 等开发信息。
- `KRBridgeModule.ets` 有 close/copy/toast/date TODO；`KRMyModule/KRMyView` 有模板式 null 返回。
- Harmony 原生 placeholder 直接显示 JSON 参数；只适合开发阶段。
- Android 同时依赖 Picasso 与 Glide；实际图片 adapter 使用情况需要在媒体阶段统一，避免双栈长期存在。
- `settings.gradle.kts` 声明没有源码目录的 `h5App/miniApp`；本轮 Gradle 8.7 配置与测试仍成功，但两者是否保留为目标尚未确认。
- 构建输出提示 KMP 的通用 `ksp` 配置已弃用、显式 iOS source-set `dependsOn` 阻止默认 hierarchy template、部分 Gradle 特性不兼容未来 Gradle 9；这些不是本轮测试失败，但需要构建维护。
- 当前无图标系统、照片资产策略、真实 empty/error/loading 产品状态组件组合。
- 地点删除不会级联删除 Capsule；时间轴会保留碎片并以“曾经到访的地点”降级。是否允许删除有记忆的地点仍需产品决策。
- 删除碎片或从编辑器移除照片不会删除沙箱原图，可能产生孤立文件；需要媒体引用清理策略。
- 时间轴日期当前按 UTC 计算，跨本地午夜时可能与用户本地日期不一致。
- Record 页面仍依赖单栏 `AppScaffold`，平板列表/详情双栏尚未实现。
- 当前 repository mutation queue 只保证单实例/单进程顺序，不能等同数据库事务或跨页面全局并发控制。

## 验证状态

2026-07-28 完成 `:shared:testDebugUnitTest` 与 `:androidApp:testDebugUnitTest`，Gradle 8.7 `BUILD SUCCESSFUL`；测试覆盖 `CCMediaModule` wire 名称和缺失模块异常降级。使用实际 DevEco SDK 完成 `:shared:linkDebugSharedOhosArm64`，源与 entry 中 `libshared.so` 的 SHA-256 均为 `21A98C8915E7AE1DF2A78075C19FB4848171A403D44F8AC5A4EEC4D6E0B3A5E8`。Hvigor 完成 ArkTS 编译、native strip、签名 HAP 构建，HAP SHA-256 为 `B608FCD58AB2C672B75C694383F8E90C5404D25BD6C62A73FC23BC319F1EAD59`，并于 10:57 覆盖安装到 HarmonyOS 真机。尚未在新包上执行 GUI 选图/取消/失败/重启读取验收；标准 `hvigorw` 的重复 `pnpm install` 仍会失败，本次构建复用了 DevEco 已安装成功的项目级 Hvigor 缓存。
