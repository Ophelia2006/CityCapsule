# CityCapsule 待办

优先级同时考虑产品闭环、数据安全和架构阻塞。标签说明来源：`[Initial Plan]`、`[Code Scan]`、`[UI/UX Redesign]`。Record Flow 的 Phase 2 首版已实现；其他 Feature 仍按用户确认的范围逐项推进。

## P0：验收 Record Flow 并建立正式产品壳

- `[Architecture Decision][PARTIAL 2026-07-30]` MVI 技术 Spike：commonMain 已显式使用 coroutines，PlaceList 自动化已验证 Intent 串行、StateFlow、Effect 单次与 dispose；仍须在 Android/HarmonyOS 设备验证重组、前后台与销毁行为，并完成 HarmonyOS 构建。
- `[Architecture Decision][UI/UX Redesign][DONE code 2026-07-30]` PlaceList/Explore 首个 MVI Feature：保留 Repository、搜索规则、路由和 storage schema，已迁移 Intent/Mutation/pure Reducer/Effect/Store 与原回归能力；设备验收见 `P1_EXPLORE_ACCEPTANCE.md`。
- `[Code Scan]` 明确 `h5App`、`miniApp` 是否属于目标；当前空 project include 不阻断 Gradle 8.7 测试，但不能作为已支持平台宣传。
- `[UI/UX Redesign][DONE 2026-07-28]` Design System v2 最小基础：暖白/中性灰/暖琥珀 token、配套深色、elevation 语义、统一 Icon 入口、类别 fallback、加载/空/错误状态及核心 Flow 组件 API；主题持久化协议未改变。
- `[UI/UX Redesign]` 在 Android/HarmonyOS 设备走查新 Light/Dark 调色、系统栏、AppIcon 字形与 elevation/shadow；若字形不一致，保持 `AppIcon` API 并替换为跨端稳定的代码矢量实现。
- `[Code Scan]` 新增真实地点摄影前，逐项完成 `docs/ASSET_ATTRIBUTION.md` 的来源、许可证、用途和署名登记；未登记素材不得进入正式 UI。
- `[UI/UX Redesign][DONE 2026-07-29]` P0-3A 单一 AppShell：唯一 Bottom Navigation、Home/Record/Profile 常驻根 Pager、点击 `animateScrollToPage`、根 `userScrollEnabled=false`、重复点击 no-op、独立滚动/页面状态保留；二级 typed route 隐藏底栏，Debug 不入壳；shared/Android/HarmonyOS 自动化与双端包构建通过。
- `[UI/UX Redesign][PARTIAL 2026-07-29]` Record Container：Timeline/Gallery 已合并为同一 RecordRootContent 的点击切换视图并共享 catalog/底栏/RecordRootView；P0-3B 再实现内部 HorizontalPager 与左右滑动，补同轴手势、状态恢复和双端手势测试；独立 Gallery route 暂留兼容。
- `[UI/UX Redesign][Code Scan]` 使用 2026-07-30 当前源码重新生成的 signed HAP 覆盖安装 HarmonyOS 真机；先确认完成首次引导后进入 `app_shell`，再检查 Tab 动画方向、快速连点、三个根滚动位置、Record 视图、Profile 编辑草稿、详情返回、系统返回键和底栏安全区。平台 guard 已有“允许 `app_shell`、拒绝退役 `home/timeline/profile` Page”的回归测试；shared 重复点击/快速选择测试与 Android 唯一底栏、无根 route action、诊断入口隔离门禁已通过。
- `[Initial Plan][UI/UX Redesign][DONE 2026-07-30]` P0-3 Home Redesign：聚合 Profile/Place/Favorite/Capsule Repository；当前档案城市优先 → 想去/未记录优先 → 类别多样化 → 稳定顺序兜底；展示 Hero、typed 分类入口、想去/同城地点、最多 3 条真实最近记忆；快速记录先选择真实地点再进入 `CapsuleEditor(placeId)`；不伪造天气、距离、摄影、AI 或网络推荐。
- `[UI/UX Redesign]` 在 Android/HarmonyOS 设备验收 P0-3 Home 的 Light/Dark、长昵称/城市名、空 catalog、部分读取失败、想去切换、3 条最近记忆、地点选择器滚动与编辑器返回栈；代码与跨端编译已通过。
- `[Code Scan][UI/UX Redesign]` 双端设备回归 Home Hero、Home“想去的地方/换一种逛法”、地点详情和搜索地点列表的想去切换：只允许心形/按钮状态变化，不得插入成功横幅、重排当前推荐、进入 Loading 或丢失搜索/筛选/滚动位置。代码与状态回归测试已于 2026-07-30 完成。
- `[Code Scan][BLOCKED]` 按 `docs/P0_RECORD_FLOW_ACCEPTANCE.md` 完成 Android/HarmonyOS 真机的选图成功、取消、复制失败、重启读取和引用清理；Android 模拟器已完成真实选图回传和沙箱复制，Android 真机未连接；HarmonyOS 的模块未注册崩溃与媒体 URI 直接按路径复制问题均已修复，Hypium 测试及 signed HAP 构建通过，仍须在 HED-AL00 覆盖安装后完成真机复验。
- `[Initial Plan]` 补相机 capability（如本阶段确认需要）和缩略图生成；正常删除/移除/丢弃的媒体引用清理已经完成，后续可评估崩溃遗留文件扫描。
- `[UI/UX Redesign][DONE code/PARTIAL device 2026-07-30]` P0-5 Record Flow 视觉完善：Editor 顶栏关闭/完成与三分支退出；Timeline 本地年月分组、大日期、地点、照片和正文摘录；Gallery 年月分组、自适应网格及 18 张分批原图保护；Detail 照片优先且管理动作下沉更多菜单；产品空态无技术说明。按 `docs/P0_RECORD_VISUAL_ACCEPTANCE.md` 完成 Android/HarmonyOS 图片渲染、返回栈、大字体与长列表真机走查后关闭设备验收项。

## P1：补齐基础版探索与本地数据完整性

- `[UI/UX Redesign][DONE code 2026-07-30]` 固定关键操作层：Explore 顶栏/搜索/chips、Record 标题/视图切换、Capsule Editor 关闭/完成、Place/Capsule Detail 返回/更多；Bottom Sheet 固定头尾并让中部滚动；SearchField 图标与文字对齐。仍需 Android/HarmonyOS 小屏、横屏、大字体、键盘遮挡和滚动边界真机验收。
- `[Architecture Decision]` PlaceList 试点双端验收后，后续页面级重构默认按 `MVI_ARCHITECTURE.md` 渐进迁移；建议顺序 Home → PlaceDetail → Profile → Timeline/Gallery/CapsuleDetail → 编辑器/Onboarding。小缺陷和纯样式修改不强制扩大为架构迁移。
- `[Code Scan]` 执行 UI 稳定性专项：发布/编辑/删除 Capsule 后返回时间轴和相册；长列表中段新增最新记录；时间轴/相册中段切换；Editor 选图、保存草稿、移除首张/中间照片；Profile 首次加载、保存和清除。重点检查整页 Loading、滚动归零、状态提示插入下推，以及无稳定 key 导致的缩略图闪白/错图。
- `[UI/UX Redesign][DONE 2026-07-30]` P0-4 Place Detail 产品化：类别 Hero fallback、地点/城市区域/类型、“想去”图标、关于、真实地址、最近三条城市记忆与“在这里留下城市碎片”主 CTA；编辑/删除已下沉到更多菜单，未展示尚不存在的地图、距离和导航。
- `[UI/UX Redesign][DONE code 2026-07-30]` P1-1 Explore：搜索置顶、分类横向 chips、整行 PlaceCard、高级筛选 Bottom Sheet、新建地点辅助菜单、想去内容页及档案城市/本地点目录诚实降级；真实摄影接入前继续使用类别 fallback。双端设备验收仍待完成。
- `[Initial Plan][DONE code+automation/PARTIAL device 2026-07-31]` P2-1 Place V2：ADR-022、schema v2、`SEED/USER`、可选坐标/视觉引用、v1 精确 seed ID 迁移、seed 删除保护与旧备份恢复已实现；shared 180 项测试通过。仍需 Android/HarmonyOS 使用旧安装数据覆盖升级，并分别导入真实旧/新 ZIP 验证。
- `[Initial Plan]` 实现 Location capability、坐标字段迁移、距离和权限降级。
- `[Initial Plan][UI/UX Redesign]` 接入双端真实地图 Native View、Marker、列表/地图切换和外部导航；地图不可用时降级为列表。
- `[Code Scan]` 如需“按想去时间排序”，新增有迁移方案的数据模型，不能从现有 Set 伪造加入时间；用户文案已经统一为“想去”，底层继续保留 `Favorite*`。
- `[UI/UX Redesign][DONE code 2026-07-30]` P1-2 Profile Overview：头像/昵称/城市，真实碎片数/去过地点数/想去数，按真实 Capsule→Place 关系计算城市足迹，最多 3 个想去地点预览；编辑为 typed 二级 MVI 页面，清除本地档案下沉 Settings 危险操作区且保留 Place/Favorite/Capsule。shared/Android 自动化通过；HarmonyOS 构建和双端设备验收见 `P1_PROFILE_OVERVIEW_ACCEPTANCE.md`。
- `[Initial Plan][DONE code/PARTIAL device 2026-07-30]` P1-3 Settings 与数据管理：MVI 设置页已提供真实主题、隐私、关于、结构化数据/照片/缓存/恢复包占用与带确认的缓存清理；正式 UI 已移除 MMKV、Push、Replace、BackTo 等开发文案。按 `P1_SETTINGS_DATA_ACCEPTANCE.md` 完成双端设备验收。
- `[Initial Plan][DONE code+automation/PARTIAL device 2026-07-30]` 版本化 ZIP 导出、导入 codec 校验与预览、导入前恢复包、照片恢复、失败回滚及新媒体清理已实现；shared/Android 测试、Harmony Hvigor test 与 signed HAP 构建通过。仍须在双端执行真实系统选择器、损坏包、未来版本包、取消、空间不足及写入失败验收。
- `[Code Scan]` 实现 RouteResult/requestId 结果通道；原生取消/失败/不支持必须回传，不得仅展示参数。
- `[Code Scan][PARTIAL 2026-07-30]` 路由 Push/Replace/BackTo 诊断已集中在独立 Developer Tools pageName，正式 Settings 无入口；继续清理或隔离 `KRBridgeModule/KRMyModule/KRMyView` 模板 TODO/null 分支，并增加 release 构建完全不可达门禁。
- `[Code Scan]` 统一 Android 图片加载栈，确认 Glide/Picasso 保留其一或说明不同职责。
- `[UI/UX Redesign][PARTIAL code 2026-07-31]` P1-4 大屏：`AdaptivePane`、Explore 地点列表/信息、Record 时间轴/碎片阅读区与 Editor 640dp 可读宽度已落地；真实 Map 仍因坐标、定位、Native View 和双端地图 SDK 未实现而阻塞，不能用占位页冒充完成。窗口变化、双端横屏/平板仍按 `P1_LARGE_SCREEN_ACCESSIBILITY_ACCEPTANCE.md` 验收。
- `[Code Scan][DONE code/PARTIAL device 2026-07-31]` P1-4 可访问性：核心 Button/IconButton/Bottom Navigation 已补语义与 48dp 触控基线，底栏改为最小高度适配大字体，浅色主色调整至与白字约 4.87:1，对根 Tab 取消非必要位移动画；仍须使用 Android TalkBack/HarmonyOS 屏幕朗读、大字体与系统高对比设置完成真机验收。

## P2：基础版稳定后再评估的扩展

- `[Initial Plan]` 路线、漫游会话、后台轨迹、定位/扫码打卡、成就、天气/地理编码/路线 API、离线资料、通知与 Widget。
- `[Initial Plan]` 网络 capability、RemoteDataSource、缓存和安全存储；Provider/Key 由用户配置，失败时本地降级。
- `[UI/UX Redesign]` 只有路线/GPS/打卡形成真实闭环后，才评估一级导航升级为“探索 / 漫游 / 记录 / 我的”。
- `[Initial Plan]` AI 场景/文案、本地模板、游记、明信片、全文搜索、统计、实况、接续、加密备份和路线分享。
- `[Code Scan]` 重新评估 KMP target 范围：iOS/H5/小程序是产品目标、演示目标还是应移除的模板。
- `[Code Scan]` 处理构建维护警告：迁移 target-specific KSP configuration、梳理 KMP hierarchy template，并在升级前消除 Gradle 9 不兼容用法。
- `[Code Scan]` 当 500 条 catalog、关系约束或查询复杂度不再合适时，单独评估数据库；不得未经 ADR 直接把 MMKV 换成 Room/SQLite/relationalStore。

## 完成标准提醒

任务只有在真实数据、失败/空/加载状态、双端能力（或明确降级）、自动测试和必要手工验收都成立时才可从 TODO 移入 DONE。仅有 route、接口、静态页面、mock 或“已接入”文案不算完成。
