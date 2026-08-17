# CityCapsule 当前开发状态

> 当前代码检查点（2026-08-17）：P2-7 已由 `486bc9a` 独立提交并通过 shared 测试、Android APK 与 HarmonyOS Hvigor test/signed HAP 构建；正式一级导航仍只有“探索 / 记录 / 我的”。本轮 Place V3、上海城市包、探索城市上下文和地点内容化尚在本地检查点中，以下旧日期段落若与本检查点冲突，以本段及当前代码为准。

## 2026-08-17 地点与探索城市增量

- `Place` schema v3 增加公共 `description`、私人 `personalNote`、`contentSource`、`IMPORTED` 来源、可选坐标和 `visualRef`。v1/v2 旧 seed note 迁为公共简介，用户地点 note 迁为私人备注；业务 ID 关联不变。
- seedVersion 3 包含上海 15 个、杭州 4 个地点。上海地点均有稳定 ID、区、完整地址、WGS-84 坐标、分类、标签、简介和内容来源；没有已登记授权的真实摄影，统一使用类别 fallback。
- `ExploreCityRepository` 独立持久化当前与最近探索城市。Home/Explore/Map 使用同一选择；主动定位经独立的受支持城市判定并要求确认，不覆盖档案城市，失败不妨碍手动选择。
- 地点详情只把 `description` 作为公共介绍并展示内容来源。用户地点可从相册或相机设置托管封面；Capsule 照片不会自动成为公共封面。
- 备份协议升至 v8，包含探索城市选择及用户地点托管封面的媒体收集/路径重写。shared 自动化通过，仍待双端旧安装、媒体和城市切换真机验收。

> 账户迁移检查点：2026-07-30 当前 `main` 位于 `da99137 P1-1：Explore`，其后仍有固定操作层与文档的未提交增量。迁移或重新 clone 前必须先阅读 `MIGRATION_HANDOFF.md` 并保存工作树；不能只依赖远端 HEAD。

## 总体判断

项目处于基础版中后期：跨端宿主、统一路由、双端 MMKV、本地档案/首次引导、离线地点，以及“地点详情 → 城市碎片 → 时间轴/相册 → 回忆”已经形成代码闭环。正式“探索 / 记录 / 我的”一级导航、Home、Place Detail、Record、Explore、Profile 与 Settings/Data Management 首版代码已经接通。P2-3 双端高德真实地图、Marker、地点摘要、详情入口和外部导航代码已落地，Android 与 HarmonyOS 真机均已显示真实地图；异常降级矩阵、外部导航四态、20 次生命周期压力测试和自动化全绿仍未完成。P2-4 系统相机的双端代码与构建已完成、真机矩阵待验收；缩略图尚未完成，所以整个 App 仍不是完整基础版。

状态定义：DONE 为已形成真实闭环；PARTIAL 为可用但缺计划中的关键环节；PLACEHOLDER 为协议/骨架/开发验证；NOT_STARTED 为规划存在而无实现；BLOCKED 为有明确外部阻塞；UNKNOWN 为仅凭仓库无法确认。

## 功能状态

| 功能 | 状态 | 代码证据与边界 |
| --- | --- | --- |
| Android/HarmonyOS Kuikly 宿主 | DONE | 两端有启动、host、adapter 与平台工程；不代表所有业务双端手测完成 |
| 强类型共享路由 | DONE | `AppRoute/AppNavigator/AppRouteTable` + 双端 dispatcher/stack tests |
| 单一 AppShell / 正式一级导航 | DONE（代码）/PARTIAL（设备体验） | 一个 `AppShellPage`、一个 Bottom Navigation、三个常驻根内容；点击 Tab 直接 `scrollToPage`，避免非必要位移动画，根手势关闭，重复点击 no-op；仍待双端设备视觉、屏幕朗读与返回键走查 |
| MVI 表现层迁移 | PARTIAL | PlaceList/Explore、Profile Overview/Editor 与 Settings/Data Management 已迁为轻量 MVI Store，具备串行 Intent、StateFlow、Channel Effect 与 dispose；shared/Android 自动化已通过，HarmonyOS 与双端生命周期设备 Spike 尚待验收，其余 Feature 仍为 StateHolder |
| Record 容器 | PARTIAL | Timeline/Gallery 已是同一 `RecordRootContent` 的状态视图并共享 catalog/底栏；点击切换和视图状态保留已实现，内部 HorizontalPager 与左右滑动尚未实现；独立 Gallery route 仅作兼容 |
| MMKV bridge 与主题旧值迁移 | DONE | 双端 2.4.0、typed protocol、迁移状态和测试资产 |
| Shared 主题与基础组件 | PARTIAL | 暖白/近黑/暖琥珀 Light/Dark token、统一 AppIcon 入口、PlaceCard/CapsuleCard、状态组件、Bottom Navigation 与 `AdaptivePane` 已存在；核心操作具备语义和 48dp 触控基线，浅色 Primary/白字约 4.87:1；图标仍是文本 glyph，Elevation 未完成视觉落地，双端辅助技术仍待验收 |
| 本地档案与首次引导 | DONE | 启动决策、草稿恢复、保存/重置、双端 launch gate 和页面状态完整 |
| 地点本地 CRUD / Place V3 | DONE（代码与自动化）/PARTIAL（设备升级验收） | schema v3 区分公共简介、私人备注和内容来源，支持 `SEED/USER/IMPORTED`、可选坐标/封面；v1/v2 迁移保留 ID，seed 增量合并不删除用户地点 |
| 地点搜索/筛选 | DONE | 纯本地字段搜索、分类/城市/区域/收藏过滤和排序有单测 |
| 收藏地点 | DONE（技术）/PARTIAL（产品） | 独立 ID 集合、容错、持久化完整；用户可见文案已改“想去”，底层保持 `Favorite*`；仍缺加入时间排序 |
| 首页 | DONE（代码）/PARTIAL（设备体验） | 聚合 Profile/ExploreCity/Place/Favorite/Capsule；Hero 与辅助地点严格来自当前探索城市，使用可解释本地排序，不展示 AI 推荐 |
| 设置 | DONE（代码与双端构建）/PARTIAL（双端设备体验） | Settings 已迁移 MVI；主题、隐私、关于、结构化数据/照片/缓存/恢复包占用、草稿与临时文件缓存清理均有真实实现；正式 UI 无 MMKV/Push/Replace/BackTo 等开发文案 |
| 地点列表/详情 UI | PARTIAL（设备体验） | Explore 列表已完成搜索、筛选、主动定位、列表/地图切换、Marker 摘要与 typed detail route；地点详情已有记录 CTA 和有坐标地点的外部导航入口。仍无真实摄影；定位拒绝、地图异常降级及导航失败场景待双端验收 |
| Profile UI | DONE（代码与自动化）/PARTIAL（设备体验） | 根页已重构为“我的城市档案”，聚合真实 Profile/Place/Favorite/Capsule 数据，展示碎片数、去过地点数、想去数、城市足迹和最多 3 个想去地点；编辑为 typed 二级 MVI 页面，清除档案位于 Settings 危险操作区；待双端设备验收 |
| 地图探索与外部导航 | PARTIAL（双端正常路径已验收） | Explore MVI → shared map contract → Android/HarmonyOS 高德 Native View 已接通；双端真机真实地图已显示，Android Marker/摘要/详情和外部高德拉起已通过。HarmonyOS 完整 Marker 链、异常降级、导航四态与双端 20 次生命周期压力测试仍缺验收记录 |
| 权限原生页 | PLACEHOLDER | Harmony 可达但明确写“具体权限申请待实现”；Android launcher 未注册 |
| 文件导入原生页 | DEBUG/DEPRECATED | 旧 Harmony native 骨架仍保留但正式 Settings 不再使用；正式导入经跨端 `CCDataArchiveModule` 系统文件选择器完成 |
| 城市碎片模型/Repository/编辑器 | DONE（代码闭环） | 模型/校验/Codec、catalog/draft Key、Repository、编辑/草稿/发布/更新/退出确认和照片选择均存在；shared/Android 测试通过 |
| 时间轴/相册/碎片详情 | DONE（代码与视觉结构）/PARTIAL（设备体验） | 时间轴按本地年月分组并以大日期、地点、照片与正文摘录呈现；宽屏为时间轴/碎片阅读双栏，手机继续进入 typed detail；相册按年月分组、自适应 3/4 列并分批增加最多 18 张原图；仍缺缩略图和双端设备手测 |
| 相册、相机与业务文件媒体 | DONE（代码与双端构建）/PARTIAL（真机体验） | Editor 通过“拍照 / 从相册选择” Bottom Sheet 进入共享 capability；Android `TakePicture + FileProvider`、HarmonyOS `cameraPicker + saveUri/resultUri fallback` 均在拍照前创建 `images/original` 受控目标。HarmonyOS 首次真机暴露部分相机只返回 `resultUri`，现已回拷到预创建目标；取消/失败/空文件即时删除，成功只返回沙箱 `file://` 路径并复用既有 `imagePaths` 与引用保护清理。相机不可用时相册与纯文字仍可用；修复后仍待真机复验及取消/不可用/生命周期矩阵，且尚无缩略图。 |
| 导入导出/备份 | DONE（代码、双端自动化/构建）/PARTIAL（双端设备验收） | 版本化 ZIP、持久数据与已引用照片导出、选择后完整 codec 校验、内容预览、确认前内部恢复包、媒体重定位、失败时结构化数据回滚与新照片清理已实现；草稿缓存不进入备份；shared/Android 测试、Harmony Hvigor test 与 signed HAP 构建通过，仍待双端系统文件选择器与失败场景真机验收 |
| 定位/距离 | DONE（代码、自动化与双端构建）/PARTIAL（真机权限验收） | Explore 仅在用户主动点击时通过 `LocationCapability` 请求一次性前台位置；双端支持允许、拒绝、永久拒绝、服务关闭、不可用、失败/超时结果；位置不持久化，失败即隐藏距离，shared Haversine 只为具备坐标的地点计算直线距离；现有 seed 坐标仍为 null。外部导航尚未实现 |
| 网络/天气/在线地理编码 | NOT_STARTED | 无业务网络库与 RemoteDataSource；当前反向城市判定仅覆盖内置支持城市中心附近 |
| 路线/漫游/轨迹/打卡 | DONE（代码与构建）/PARTIAL（双端总验收） | `486bc9a` 已实现路线持久化、会话恢复、真实轨迹分片、打卡和总结；后台保活及双端移动/杀进程矩阵仍待验收；无“漫游”一级 Tab |
| AI/游记/明信片/接续/加密备份 | NOT_STARTED | 复杂版规划，无当前实现 |
| iOS/H5/小程序产品支持 | UNKNOWN | 有模板/target，缺 CityCapsule 功能验收；h5App/miniApp 目录缺失 |

## 页面清单

| 页面 | 状态 | 数据来源 | 入口 | 跳转目标 | 当前 UI 状态 |
| --- | --- | --- | --- | --- | --- |
| LaunchGate | DONE | profile/onboarding MMKV | 系统冷启动 | Home/Onboarding replace | 启动反馈页 |
| Onboarding | DONE | OnboardingRepository | LaunchGate/Settings | Home | 四步真实表单，但“默认档案”削弱引导意图，后续随产品视觉调整 |
| Home | DONE（代码）/PARTIAL（设备体验） | Profile/Place/Favorite/Capsule Repository | LaunchGate/探索 Tab | 地点列表/详情、想去、碎片详情、带 placeId 的碎片编辑器 | 已完成 P0-3 产品化内容与真实空/加载/部分失败降级；待双端视觉验收 |
| Place List | DONE（代码与自动化）/PARTIAL（设备体验） | Profile/Place/Favorite Repository + Location Capability | Home | Editor/Detail/back | 首个 MVI 试点；定位只由用户主动触发，结果经 Mutation/Reducer 入 State；成功且地点有坐标才显示直线距离，失败保持完整目录；待双端权限场景手验 |
| Favorites | DONE（代码与自动化）/PARTIAL（设备体验） | 同上 | Home/Profile | Detail/Explore/back | 以地点内容呈现，支持搜索与即时移出想去；不伪造加入时间排序；待双端手验 |
| Place Detail | PARTIAL | Place/Favorite/Capsule Repository | 列表/回忆详情 | Capsule Editor/Timeline/Place Editor/back | 记录主 CTA 和记忆计数已接通；地点内容仍缺摄影、位置与导航 |
| Place Editor | DONE（当前模型范围） | Place Repository | 列表/详情 | Detail/back | 表单 CRUD 完整；不应成为核心探索视觉模板 |
| Profile | DONE（代码与自动化）/PARTIAL（设备体验） | Profile/Place/Favorite/Capsule Repository | 我的 Tab | Profile Edit/Want To/Place Detail/Settings | 三项统计与城市足迹均由真实数据计算；想去预览可进入详情或即时移出；待双端设备验收 |
| Profile Edit | DONE（代码与自动化）/PARTIAL（设备体验） | Profile Repository | Profile | back | typed 二级 MVI 页面；保存后刷新 Overview，未保存退出需确认 |
| Settings | DONE（代码）/PARTIAL（双端设备体验） | Settings/DataBackup Repository + DataArchive capability | Profile | 系统文件选择器/Onboarding/back | 二级 MVI 页；主题、存储、缓存、隐私、关于及带预览/恢复的导入导出完整；正式 UI 不出现底层技术或路由诊断文案 |
| Developer Tools / Router Diagnostics | DEBUG | 无业务数据 | 正式产品 UI 不可达，仅非业务 pageName | Home/Settings | 路由 Push/Replace/BackTo 等诊断集中于独立 Developer Tools，不进入 Settings |
| Image Adapter Diagnostics | DEBUG | assets/HTTP 示例 | 正式产品 UI 不可达，仅非业务 pageName | 无 | Kuikly 图片 adapter benchmark |
| Harmony Permission | PLACEHOLDER | route params | typed native route | back | 只显示骨架与参数 |
| Harmony File Import | PLACEHOLDER | route params | typed native route | back | 只显示骨架与参数 |
| Harmony Route Fallback | DONE（基础设施） | 失败参数 | guard/dispatcher | back/Home | 安全降级页，含必要诊断文本 |
| Capsule Editor | DONE（代码与视觉结构）/PARTIAL（设备体验） | Capsule/Place Repository + PhotoPicker | Place Detail / Capsule Detail | Capsule Detail/back | 顶栏关闭/完成、照片优先、自然正文、心情/地点/标签；脏草稿退出明确提供保存草稿、继续编辑、放弃修改 |
| Capsule Detail | DONE（代码与视觉结构）/PARTIAL（设备体验） | Capsule/Place Repository | Editor/Timeline/Gallery | Place Detail/Editor/Record root | 照片优先，日期/心情/正文/标签/地点形成阅读层级；编辑与删除只在更多菜单，删除后回 Record root |
| Timeline | DONE（代码与视觉结构）/PARTIAL（设备体验） | Capsule/Place Repository | AppShell 记录 Tab/Place Detail/Capsule Detail | Capsule Detail | 按本地年月分组，每条记忆为独立 lazy item，使用大日期、地点、缩略照片位与正文摘录；内部 Pager 动画/手势未做 |
| Gallery | DONE（代码与视觉结构）/PARTIAL（缩略图/兼容清理/设备体验） | 同 Timeline | AppShell Record segmented control；兼容 route | Capsule Detail/Timeline | 按本地年月分组的自适应 3/4 列网格；当前无缩略图能力，以 18 张一批限制原图同时加载；独立 GalleryPage/route 暂留兼容 |
| Explore Map View | PARTIAL | Place catalog + Explore MVI + 双端高德 Native View | Place List 的“地图”切换 | Marker 摘要/Place Detail | 双端真实地图已显示；异常与压力场景待验收 |

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
- 地点协议 v2 已补齐来源、可选坐标与可选封面引用；地图/距离/图片能力本身尚未实现。

### 尚未实现

- 基础版核心仍缺完整验收的地图/外部导航/相机，以及尚未实现的缩略图；一次性定位、备份和完整设置已有代码，仍待设备验收。
- 进阶版与复杂版全部产品功能。

### 后续新增（初始规划未明确为当前阶段实现）

- `LaunchGate` 独立 shared 页面与较完整的启动修复策略。
- 3-store typed storage wire protocol、type metadata、catalog/favorite 容错和 mutation queue。
- 双端自有 Kuikly route stack coordinator 与详细 guard/logger/fallback。
- iOS/JS 构建模板目标。

### 已偏离 / NEEDS_REEVALUATION

- 地点从“实体独立 Key + 索引”改为单个有上限 catalog。
- Place v1 曾去掉 source/坐标/封面；2026-07-31 已通过 ADR-022 与 schema v2 修复该差异，改变原因仅能确认来自 P2-1 明确需求。
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

## 2026-08-11 P2-5 媒体维护增量

确定性缩略图、Timeline/Gallery 按需加载、双端媒体扫描/删除联动、1 小时宽限和 Settings 原图/缩略图/备份/缓存真实 bytes/count 已进入代码，未新增 MMKV key。shared Kotlin 编译通过；完整 Android 自动化受本机 `shared/build` 文件锁影响，HarmonyOS strict build 与双端真机验收待完成，详见 `P2_MEDIA_MAINTENANCE_ACCEPTANCE.md`。

## 2026-08-11 P2-6 备份兼容增量

- `DONE code + shared automation`：现有 ZIP 外层升级为 v2，带 `minReaderVersion=2`；当前 reader 接受 v1/v2并在预览前拒绝未来版本/未来 reader，旧 v1 reader 会明确拒绝新包。
- `DONE code + shared automation`：Place V1 通过当前 codec 自动迁移 source；坐标/封面缺失保持 null，恢复落盘重新编码为 Place V2。
- `DONE code + shared automation`：已发布 Capsule 引用的 Camera 原图进入备份；draft 与缩略图不进入。恢复只提交原图，Timeline/Gallery 后续按需再生成缩略图。
- `PARTIAL device`：导入前恢复包和写入失败回滚沿用现有事务编排；双端真实文件选择器、空间不足、故障注入、照片恢复、重启一致性与旧版拒绝仍须按 `P2_BACKUP_COMPATIBILITY_ACCEPTANCE.md` 验收。

## 2026-08-11 P2-3 地图实现与验收状态

- Android：`CCAmapView` 已注册到 Kuikly host，高德 `MapView`、隐私同意后初始化、WGS-84 转换、Marker、当前位置标记、Marker 点击回传和生命周期代码已实现；iQOO 真机已通过地图显示、Marker 摘要、详情进入、地图手势和外部高德拉起的正常路径验收。
- HarmonyOS：`CCAmapView` 已注册到 `KuiklyViewDelegate`，高德 `MapViewComponent`、隐私接口、Key 注入、Marker、点击回传、坐标转换和销毁代码已实现；signed HAP 已由命令行 Hvigor 构建、安装，真机真实地图显示通过。空白地图根因是 `MapViewComponent` 作为条件分支根节点时 Surface 合成不稳定；当前以明确全尺寸 `Stack` 承载默认 `MapViewComponent()`，真机复验正常。
- Explore：MVI 已增加列表/地图、隐私提示、Marker 选择、相机状态和失败回列表；摘要整卡进入 typed `PlaceDetail(placeId)`。
- 数据：seedVersion 升至 2，8 个内置地点有 WGS-84 坐标；旧 seed catalog 解码时补缺失坐标。用户自建地点编辑器仍没有坐标录入，因此这类地点继续只在列表显示。
- 外部导航：地点详情已有真实入口和四态结果处理；Android 已验证 `Opened` 并成功拉起高德，`NoCompatibleApp / Unsupported / Failure` 及 HarmonyOS 四态仍待逐项验收。
- 安全：Android/HarmonyOS 本机 Key 配置文件均由 `.gitignore` 排除，当前未发现写入 MMKV 或日志的代码；尚需完成 Git 历史扫描和导出/备份包检查后才能关闭此门槛。
- 自动化：2026-08-11 HarmonyOS signed HAP 构建成功，Android Debug APK 构建成功；同轮 Android 43 项单测有 1 项失败：`AppShellArchitectureGuardTest.appShellSwitchesRetainedRootsWithoutRouteReplace`。shared 测试因组合 Gradle 任务在 Android 失败后中止，需修复后重跑完整套件。

状态保持 `PARTIAL（正常地图显示已双端通过，完整共同门槛未通过）`。在异常降级、定位拒绝、导航四态、自动化全绿、安全审计和双端各 20 次生命周期压力验证完成前不得标记 DONE。

## 验证状态

2026-08-10：P2-3 Android Map Kit 环境接入完成：`agconnect-services.json` 保持本机且已由 Git 忽略，Huawei Maven、AG Connect Gradle 插件 `1.9.6.300`、HMS Map Kit `6.15.1.322` 与网络状态权限已配置。Gradle 已成功执行 AGC 配置处理、解析 Map Kit 运行时依赖并构建 Debug APK。此状态只证明 Android SDK/凭据配置进入构建，不等于 Native Map View、Marker 或 Explore 地图流程完成。

2026-08-04：P2-3 完成供应商核对和诚实边界落盘：新增 provider-neutral Map Contract、ADR-022、Android `ACTION_VIEW + geo:` 与 HarmonyOS `startAbility + geo:` 外部导航 bridge，结果覆盖 `Opened / NoCompatibleApp / Unsupported / Failure`。仓库没有合法 Map Kit Key/AGC 配置，因此未伪造 Native 地图；列表/地图 MVI 状态、双端 Native View、Marker/摘要尚未完成，当前状态为 PARTIAL。验收见 `P2_MAP_NAVIGATION_ACCEPTANCE.md`。

2026-07-30 完成 P1-1 Explore 代码收尾：PlaceList/Want To Go 迁为首个轻量 MVI Feature；搜索置顶、分类横向 chips、高级筛选 Bottom Sheet、整行地点点击、新建地点辅助菜单、想去内容列表及档案城市优先/本地点目录降级已落地。新增 MVI 等价回归测试，未以删除旧 StateHolder 测试换取通过。shared、Android 单测与 Android Debug 构建通过；HarmonyOS 构建和双端设备视觉/Effect/lifecycle 行为仍按 `P1_EXPLORE_ACCEPTANCE.md` 验收。

2026-07-30 完成 P1-2 Profile Overview 代码与自动化：根页由本地档案 CRUD 改为“我的城市档案”；Profile/Place/Favorite/Capsule Repository 聚合进入 Profile MVI Store。碎片数取已发布 Capsule 数，去过地点数取 Capsule 中 distinct `placeId`，想去数取 Favorite ID 数；城市足迹只使用可由当前 Place catalog 解析出的城市，历史悬空地点仍计入去过地点但不虚构城市。编辑资料成为 `profile_edit` typed 二级 MVI 页面；清除本地档案移入 Settings 危险操作区并保留 Place/Favorite/Capsule。`:shared:testDebugUnitTest`（175 tests）、`:androidApp:testDebugUnitTest`、Android Debug APK 与 `:shared:compileKotlinOhosArm64` 通过；本机普通终端没有可调用的 `hvigorw`，ArkTS test/HAP 与双端设备验收按 `P1_PROFILE_OVERVIEW_ACCEPTANCE.md` 执行。

2026-07-30 修复 Profile 想去预览的局部更新抖动：Profile 发出的 Favorite 失效事件现在携带 Place revision owner，仍通知 Explore/Want To 刷新，但 Profile 不再消费自己发出的通知并重新进入整页 Loading；数量和预览列表直接由当前 MVI State 局部更新，滚动状态保持。shared/Android 回归测试通过。

2026-07-30 完成固定操作层代码重构：Explore 固定顶栏/搜索/chips，Record 固定标题与时间轴/相册切换，Capsule Editor 固定关闭/完成，Place/Capsule Detail 固定返回与更多菜单；Bottom Sheet 改为固定标题和 Footer、中间限高滚动。SearchField 去除重复“搜索”标签与绝对叠放图标，改为同一行垂直居中的图标和单行输入。自动化通过，双端小屏/横屏/大字体/软键盘视觉仍待设备验收。

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

- 2026-07-30：P0-5 Record Flow 视觉结构完成：Editor 只保留顶栏“完成”主动作并将三种草稿退出选择收进离开流程；Timeline 改为本地年月分组的 lazy 记忆条目；Gallery 改为年月分组、自适应网格和 18 张一批的原图加载保护；Detail 改为照片优先且编辑/删除只存在于更多菜单；空态不再解释技术实现。shared/Android 单测、Android Debug APK 与 HarmonyOS Kotlin/Native arm64 编译通过。当前环境因未配置 `OHOS_SDK_HOME`/`DEVECO_STUDIO_HOME` 无法完成 HarmonyOS native link/HAP；双端视觉、返回栈、大字体与媒体真机行为仍须按 `P0_RECORD_VISUAL_ACCEPTANCE.md` 验收。

- 2026-07-30：修复 P0-5 多图与编辑回归：详情/Editor 照片改用无孤立尾格的均衡行布局，2/3/5 张不再留下固定三列空洞；照片路径作为 Compose stable key，减少增删时 painter slot 错位与鸿蒙闪动；照片删除控件保持 48dp 触控区但视觉容器统一为 32dp；新建发布仍 replace 到详情，编辑发布改为 back 到原详情并由 `CapsuleFeatureRuntime.revision` 触发重载，避免每次编辑叠加一个旧详情页。布局与导航策略新增 shared 回归测试，Android 单测/APK 与 HarmonyOS arm64 共享编译通过，仍需双端真机视觉确认。

- 2026-07-30：鸿蒙复验发现仅在分行 `Row` 内增加 stable key 仍不能阻止跨行照片销毁。均衡网格现改为单一自定义 `Layout` 父节点：全部照片以路径 key 作为直接子项，增删后只重新测量和放置，避免未删除 painter 因跨父节点迁移而闪白。紧凑删除按钮的叉号改为 Canvas 双线绘制，不再依赖两端字体基线。shared/Android 单测与 HarmonyOS arm64 共享编译通过，需用最新 APK/HAP 复验视觉结果。

- 2026-07-30：修正详情页恰好 2 张照片时的尺寸不一致：双图现在使用同一行等宽、等高的正方形布局；1 张仍为 Hero，3 张及以上仍为 Hero + 均衡网格。该调整只影响展示容器，不修改原图或媒体存储。

## P2-7A 本地路线（2026-08-12）

- `DONE`：Explore 内路线入口、手动选点排序、保存/编辑/删除、MVI Store、`routes.catalog` 和备份 v3 已落地。
- `NOT_STARTED`：在线路径规划、漫游、轨迹、打卡；没有新增“漫游”Tab。
- 双端真机验收见 `P2_LOCAL_ROUTE_ACCEPTANCE.md`。

## P2-7B 漫游会话（2026-08-12）

- `DONE`：可选路线/自由漫游的开始、暂停、继续、结束状态机，MVI 页面、`roaming.session` 持久恢复及备份 v4 已落地。
- `NOT_STARTED`：定位、轨迹采样、到达判断和打卡；没有新增“漫游”一级 Tab。
- 双端手工验收见 `P2_ROAMING_SESSION_ACCEPTANCE.md`。

## P2-7C 前台轨迹（2026-08-12）

- `DONE`：前台 15 秒/手动采样、双端沙箱分片、MMKV 元数据索引和中断恢复已落地。
- `NOT_STARTED`：后台定位、保活、耗电策略、轨迹线、距离统计和打卡。

## P2-7D 打卡与总结（2026-08-12）

- `DONE`：150 米附近提示后确认 GPS 打卡、定位中断时明确手动打卡、打卡后城市碎片入口，以及从路线/会话/轨迹文件/打卡/碎片实时生成总结。
- `NOT_STARTED`：扫码、自动打卡、后台持续定位；“漫游”一级 Tab 仍未增加。
- P2-7 双端总验收见 `P2_7_END_TO_END_ACCEPTANCE.md`。
