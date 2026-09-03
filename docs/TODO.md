# CityCapsule 待办

## 当前 P0 / P1 收口（2026-09-03）

以下清单优先于后面的历史待办；历史条目用于追溯，不代表仍未实现。

### P0

- [x] 修正 AppShell 架构守卫，使其验证当前即时 `scrollToPage`、禁用根手势、无壳内 route replace 的真实约束。
- [x] 增加 shared → Android → HarmonyOS capability 注册一致性门禁，并覆盖双端高德地图 Native View 注册。
- [x] 运行 shared 单测；2026-09-03 通过。
- [x] 运行 Android 全量 JVM 单测并构建 Debug APK；2026-09-03 通过。
- [ ] 补齐本机 DevEco SDK 缺失组件后重跑 HarmonyOS entry test、arm64 链接与 signed Debug HAP；当前为环境阻塞，不是已确认源码失败。
- [ ] 按现有验收文档完成 Android/HarmonyOS 真机异常矩阵：权限允许/拒绝/永久拒绝、弱网/断网、定位超时、媒体取消/失败、存储不足、杀进程恢复、地图反复进入退出、旧数据升级、损坏/超大备份。

### P1

- [x] 工程只 include 真实存在的 `androidApp` 与 `shared` Gradle 模块；H5/小程序不再形成幽灵工程声明。
- [x] Android 图片栈统一为 Glide，移除无代码引用的 Picasso。
- [x] HarmonyOS 产品 host 不再注册 Kuikly 示例 `KRMyModule/KRMyView`；示例文件暂留源码，不进入产品运行表。
- [x] Android/HarmonyOS 清理 Demo 应用名、vendor、模块描述；Android 增加可用的应用图标资源。
- [x] 明确 KMP hierarchy 与 Windows 禁用 target 配置；第三方 Kuikly/KSP 和 Gradle 9 警告保留为插件升级任务。
- [x] 将 AGENTS、CURRENT_STATE、IA、用户流程和 UI 文档同步到四 Tab 与已落地的平台能力。
- [ ] 双端真机关闭 UI/UX 验收：大字体/读屏/触控区、横屏与平板、自适应双栏、长列表拖拽、图片比例与滚动性能。
- [ ] 如要形成可复现发布候选，再单独确定 targetSdk/签名/混淆/版本策略；项目明确不做真实上架时不把商店合规列为本轮阻塞。

## 历史待办与验收记录

- `[DONE code+shared automation/PARTIAL HarmonyOS performance acceptance 2026-09-02]` Explore 独立 Lazy item 改造、基于真实可见 key 的末 3 项预取、固定 Footer、请求代次、同页幂等、旧前缀保持及失败显式重试已完成；进程内 32 页/10 分钟 LRU 保持不变。按 `P1_EXPLORE_ACCEPTANCE.md` 在同一鸿蒙真机对比连续 3 页的 FPS、卡顿率、最大连续丢帧和分页锚点位移，并复验同一查询缓存命中、TTL、切城市/位置/页码不串缓存。
- `[FIXED code+automation/PARTIAL HarmonyOS device 2026-08-31]` 无真实道路结果不再绘制景点直线；自动规划失败停留在路线编辑页，不进入按路线漫游。真机复验真实道路成功后才出现折线并可出发；断网、无 Key、配额耗尽均不得显示或保存伪道路。
- `[DONE code+automation/PARTIAL dual device 2026-08-31]` 漫游根页最近记忆卡从同一漫游关联的最新带图城市碎片读取封面；复验有图、无图、文件丢失及多次漫游不串图。

## 2026-08-29 探索、地图与根导航验收

- `[Code Scan][DONE code+shared automation/PARTIAL dual device]` 应用级地图同意、在线地点 12 条增量预取、地标/古迹/全部在线结果修复、四根 Tab 与 Profile ⚙️ 菜单已实现。双端验收：首次地图同意后依次进入 Explore/路线/实时漫游/回顾不再提示；冷启动后仍保持；定位权限仍只在主动定位时出现；每类连续加载至少 3 页并验证去重、末页、断网重试、快速切分类不串页；四 Tab 状态与二级页返回正确。

## 2026-08-24 本轮回归验收

- `[UI/UX Redesign][DONE code+automation+dual build/PARTIAL device 2026-08-29]` 路线编辑添加候选已限定当前探索城市，Store 同步拒绝跨城 `AddPlace`；规划及开始/继续漫游操作已前移。双端验收：分别选择杭州/上海后进入编辑页，候选只出现对应城市；页面仍在返回栈时切换城市后候选刷新；旧跨城路线地点保留且可手动移除；新建路线保存后开始漫游、活动会话继续入口均正常。

- `[Code Scan][P0]` 在 Android 设备确认 Debug APK 覆盖安装后，连续打开/返回首页“想去”地点至少 30 次并停留 5 分钟，确认 crash buffer 不再出现 `Canvas: trying to use a recycled bitmap`。
- `[Code Scan][P0]` HarmonyOS 验收 Home/Explore/Timeline → Place/Capsule Detail → 返回，确认回到来源页而不是退出应用；同时验证连续 push/back 后业务路由栈无丢项。
- `[Code Scan][P0]` 建立路线 A/B，启动 A 后从 B 入口再次进入漫游，确认标题、Marker、打卡列表仍只显示活动会话 A 的地点。
- `[UI/UX Redesign][P1]` 验收地点详情追加到路线：已有、重复、20 地点上限、无路线四种状态。
- `[UI/UX Redesign][P1]` 验收按路线/自由漫游选点：路线优先、附近优先与全量选择；当前是有序全量列表，地点数增长后再增加搜索。
- `[UI/UX Redesign][P0 device]` 双端真机验收漫游回顾：真实轨迹分片、计划/实际双折线、地图同意/拒绝、无轨迹降级、本地开始结束时间、沿途碎片、精选封面 fallback 与系统分享面板。
- `[Code Scan][P2]` 若要求分享为一张可保存图片，新增双端离屏渲染/截图和临时文件清理；当前完成的是产品内卡片预览 + 真实数据系统文本分享。
- `[Code Scan][DONE code+automation+dual build/PARTIAL live API+device 2026-08-25]` 真实步行路线规划：手动顺序逐段道路规划、距离/时长/折线、2–8 地点道路距离推荐顺序、明确采用及无直线降级已完成。使用双端真实 Web Key 验收西湖跨岸、无可步行道路、超时、断网、无 Key、连续重算、页面退出回调和高德配额后关闭。
- `[UI/UX Redesign][DONE code/PARTIAL device 2026-08-25]` Home 推荐地图同页最多展示 5 个类别 Emoji + 地点，点击同步 Marker 选择并进入详情；完成 Android/HarmonyOS 小屏、字体与 Marker 交互验收。

## 2026-08-20 P1 产品能力检查点

- `[Code Scan][P0 verification]` Pura 80 启动 SIGABRT 已按 faultlogger 根因增加 Kuikly native 预加载并成功构建/安装；设备解锁后验证首次启动、连续 20 次冷启动、前后台切换及 faultlogger 不再新增 `cppcrash-com.y.citycapsule-*`。

- `[Code Scan][DONE code+shared automation/PARTIAL device]` 用户头像：Profile v2、相册/相机、256×256 托管头像、MMKV 引用、替换/放弃清理、缺文件 fallback、备份 v9 已接通；待双端原生构建和真机矩阵。
- `[UI/UX Redesign][DONE code+shared automation/PARTIAL gesture device]` 路线排序：`Reorder(fromIndex,toIndex)`、长按 Drag Handle、滚动、保存后持久化及辅助菜单已完成；待双端长列表边缘自动滚动和辅助技术验收。
- `[Code Scan][DONE code+shared automation/PARTIAL device]` 自由漫游：500 米附近地点、200 米 GPS 到达、手动降级、typed `roamingSessionId`、显式总结关联和按打卡顺序保存路线已完成；待双端移动、定位异常、杀进程和文件失败验收。通过前继续隐藏一级入口。
- `[Code Scan][P1]` 若要从 Timeline 的历史 Capsule 打开对应漫游总结，先把单一 `RoamingSession` 存储升级为有上限的会话历史 catalog，并迁移轨迹/打卡索引；当前只显示来源标记，不用时间范围猜测或把 session ID 冒充 route ID。
- `[Code Scan][DONE code+automation/PARTIAL device]` 真实搜索/分类/附近及在线单项导入已复核存在；待无 Key、断网、弱网、重复导入和距离真机验收。
- `[Code Scan][UI/UX Redesign][P1]` 在线地点当前仍是最多 12 个的单批候选。后续为 `PlaceRemoteDataSource` 和双端网络 bridge 增加明确 page/offset 协议，在列表接近底部时加载下一页；切换城市、搜索词或探索主题时重置游标，按 provider ID 去重，并设置单次会话 60～100 个上限。在线结果不得批量写入 MMKV。
- 统一验收步骤见 `docs/P1_PRODUCT_CAPABILITIES_ACCEPTANCE.md`。

## 2026-08-17 当前 P0 检查点

- `[Code Scan][DONE code+shared automation]` Place V3、v1/v2 迁移、`IMPORTED`、公共简介/私人备注/内容来源、备份 v8 与旧备份恢复。
- `[Code Scan][DONE code+shared automation]` 上海城市包扩展至 15 个地点；全部具备坐标、地址、分类、标签、简介和内容来源。真实摄影未引入，继续使用明确类别 fallback。
- `[Code Scan][DONE code+shared automation/PARTIAL device]` 独立探索城市、最近城市、手动选择、主动定位确认、Home/Explore/Map 同一城市范围及重启恢复；仍需双端定位拒绝/关闭/超时和城市切换设备验收。
- `[UI/UX Redesign][DONE code/PARTIAL device]` 地点详情公共简介/来源、私人备注隔离、用户地点相册/相机封面、备份媒体重定位；仍需双端选择、拍摄、替换、导出导入与缺文件降级验收。
- `[Code Scan][PENDING]` 引入上海真实摄影前必须逐项完成 `ASSET_ATTRIBUTION.md`，否则保持类别 fallback。
- `[Code Scan][DONE code+automation/PARTIAL device]` Home 推荐已使用当前城市、想去/到访、真实封面、进程内定位距离、类别多样性和稳定 ID；没有定位时不伪造距离。
- `[Code Scan][DONE code+automation/PARTIAL device]` 用户地点可手填 WGS-84 坐标或主动使用当前位置；需补双端键盘、非法输入、权限拒绝和重启读取真机验收。
- `[Code Scan][DONE code+automation/PARTIAL device]` 地点封面替换、移除、放弃和删除已使用 Place/Capsule/草稿引用保护清理；需补双端文件缺失、删除失败与导入恢复真机验收。
- `[Code Scan][DONE code+automation/PARTIAL device]` 已知未支持城市可被选择并持久化，Home/Explore 显示诚实空态且不串入其他城市地点；当前北京仅用于验证此状态，不代表已有北京内容包。
- `[Initial Plan][DONE code+automation/PARTIAL device]` 高德在线 POI 搜索、照片预览、单个选择性导入和在线逆地理编码已接入双端网络桥；Web Key 只存在本机忽略配置，断网/空结果/服务错误降级到本地点。仍需双端真机验证弱网、图片加载失败、重复导入和请求生命周期。
- `[Initial Plan][PARTIAL code+automation/PARTIAL device 2026-08-24]` Home 已支持输入任意城市名，并用真实 POI 响应确认城市名称和中心坐标后保存动态城市；定位也可产生动态城市。仍缺正式全国城市/行政区编码目录、城市联想与重名城市消歧，因此不能宣称完整全国城市选择已完成。
- `[Code Scan][DONE code+automation/PARTIAL device]` Home、Explore 与地点详情已统一按“本地托管封面 → 高德 POI 有效缓存 → 类别 fallback”展示；详情按需查询，列表不批量联网。URL 只进入 `cc_cache/places.photo_cache`（100 条/30 天），不写入地点模型或备份，加载失败删除。仍需双端真机验证成功、慢加载、失败、离线和页面反复进入退出。
- `[Code Scan][DONE code+automation/PARTIAL profiling 2026-08-19]` 地点图片已增加首批 6 张优先、最大并发 3、URL 去重、lease 释放和进程内调度指标。下一步在 Android/HarmonyOS 真机记录首图/首屏时间、缓存命中、峰值内存和滚动丢帧；地点规模扩大前将 Explore 单一 lazy item 改为 viewport 驱动的独立 lazy items。
- `[Code Scan][DONE code+Android log verification]` `CCPlaceNetworkModule` 已在 `BasePager` 的 shared proxy 模块表统一注册，并增加架构守卫；地点详情因漏注册导致的 Android `acquireModule` 闪退已定位并修复，HarmonyOS 仍需真机复验同一路径。

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
- `[Initial Plan][DONE code+build/PARTIAL device 2026-08-11]` P2-4 系统相机 capability：Editor 来源 Bottom Sheet、双端拍照前受控目标、取消/失败/空文件清理、沙箱路径校验边界、既有 `imagePaths` 与引用保护清理均已接通；shared 与相关 Android 单测、Android APK、HarmonyOS signed HAP 构建通过。按 `P2_CAMERA_ACCEPTANCE.md` 完成双端真机矩阵后关闭；缩略图生成与崩溃遗留文件扫描仍未实现。
- `[UI/UX Redesign][DONE code/PARTIAL device 2026-07-30]` P0-5 Record Flow 视觉完善：Editor 顶栏关闭/完成与三分支退出；Timeline 本地年月分组、大日期、地点、照片和正文摘录；Gallery 年月分组、自适应网格及 18 张分批原图保护；Detail 照片优先且管理动作下沉更多菜单；产品空态无技术说明。按 `docs/P0_RECORD_VISUAL_ACCEPTANCE.md` 完成 Android/HarmonyOS 图片渲染、返回栈、大字体与长列表真机走查后关闭设备验收项。

## P1：补齐基础版探索与本地数据完整性

- `[Code Scan][DONE code+shared automation+dual build/PARTIAL device 2026-08-24]` 漫游×想去×城市碎片×回顾闭环：想去优先选点、到达移除/撤销、碎片精确关联、有界历史归档、Record 回顾入口、会话覆盖保护和备份 v10 已实现。Android APK 与 HarmonyOS signed HAP 已重建，按 `P2_7_END_TO_END_ACCEPTANCE.md` 完成双端证据矩阵。
- `[Code Scan][DONE code+automation+dual build/PARTIAL device 2026-08-24]` 漫游创建碎片前选点（默认最近景点）、全量轨迹文件、当前位置和双端高德实时 Polyline 已实现；完成双端实际移动、长轨迹、地图拒绝/无 Key/断网/生命周期验收。
- `[Initial Plan][NOT_STARTED]` 若要在锁屏、长时切到外部导航/相机或系统回收后仍无缺口记录，单独实现 Android 前台定位服务与 HarmonyOS 长时任务/后台定位，并补权限、通知、耗电和异常恢复决策。
- `[Code Scan][DONE code+automation/PARTIAL device 2026-08-20]` 路线拖拽修正：稳定 `placeId` 身份、把手随卡片移动、整卡选中色、取消反向页面滚动、持续边界自动滚动已实现；在双端使用超过一屏的路线验收向上/向下拖动、长名称不换行回归、边界滚动及保存后顺序。
- `[Code Scan][DONE code+automation/PARTIAL device 2026-08-20]` 探索城市修正：动态定位城市可确认、持久化并迁移，Explore 左上标题区打开选择器，选中项使用勾选/品牌色/加粗。在双端验收西安确认后标题即时更新、重启恢复，以及“暂无内容”降级。
- `[Code Scan][DONE code+automation/PARTIAL device 2026-08-20]` Home 与 Explore 共享动态城市上下文；Home 左上可切换城市，无本地内容时两页按需请求高德 POI 在线候选。双端验收西安切换、Home 不再回退上海、弱网/无 Key 降级、Explore 确认导入及重启后本地显示。
- `[Code Scan][DONE code+automation/PARTIAL device 2026-08-20]` Home 最多 4 个、Explore 主列表最多 12 个在线候选已接入受控真实图片加载；验收两端 HTTPS 图片、无图 fallback、快速滚动、重复 URL 去重、失败恢复与导入后从在线候选移除。
- `[Code Scan][DONE code+automation/PARTIAL device 2026-08-24]` 无本地 catalog 时，Home 第一个真实在线候选使用 Hero 卡，修复西安首页缺少主推荐卡；验收有图、无图、图片失败以及在线空结果四种状态。
- `[Code Scan][DONE code+automation/PARTIAL device 2026-08-24]` Home 城市弹窗支持按名称查找其他城市并持久化动态城市；验收成都/广州等有效城市、错别字、同名行政区、断网、无 Key、切换后 Home/Explore/Map 同步及重启恢复。
- `[Code Scan][DONE code/PARTIAL device 2026-08-20]` 头像编辑只保留相册选择；双端验收选择、取消、文件缺失回退和更换时旧文件清理。

- `[UI/UX Redesign][DONE code 2026-07-30]` 固定关键操作层：Explore 顶栏/搜索/chips、Record 标题/视图切换、Capsule Editor 关闭/完成、Place/Capsule Detail 返回/更多；Bottom Sheet 固定头尾并让中部滚动；SearchField 图标与文字对齐。仍需 Android/HarmonyOS 小屏、横屏、大字体、键盘遮挡和滚动边界真机验收。
- `[Architecture Decision]` PlaceList 试点双端验收后，后续页面级重构默认按 `MVI_ARCHITECTURE.md` 渐进迁移；建议顺序 Home → PlaceDetail → Profile → Timeline/Gallery/CapsuleDetail → 编辑器/Onboarding。小缺陷和纯样式修改不强制扩大为架构迁移。
- `[Code Scan]` 执行 UI 稳定性专项：发布/编辑/删除 Capsule 后返回时间轴和相册；长列表中段新增最新记录；时间轴/相册中段切换；Editor 选图、保存草稿、移除首张/中间照片；Profile 首次加载、保存和清除。重点检查整页 Loading、滚动归零、状态提示插入下推，以及无稳定 key 导致的缩略图闪白/错图。
- `[UI/UX Redesign][DONE 2026-07-30]` P0-4 Place Detail 产品化：类别 Hero fallback、地点/城市区域/类型、“想去”图标、关于、真实地址、最近三条城市记忆与“在这里留下城市碎片”主 CTA；编辑/删除已下沉到更多菜单，未展示尚不存在的地图、距离和导航。
- `[UI/UX Redesign][DONE code 2026-07-30]` P1-1 Explore：搜索置顶、分类横向 chips、整行 PlaceCard、高级筛选 Bottom Sheet、新建地点辅助菜单、想去内容页及档案城市/本地点目录诚实降级；真实摄影接入前继续使用类别 fallback。双端设备验收仍待完成。
- `[Initial Plan][DONE code+automation/PARTIAL device 2026-07-31]` P2-1 Place V2：ADR-022、schema v2、`SEED/USER`、可选坐标/视觉引用、v1 精确 seed ID 迁移、seed 删除保护与旧备份恢复已实现；shared 180 项测试通过。仍需 Android/HarmonyOS 使用旧安装数据覆盖升级，并分别导入真实旧/新 ZIP 验证。
- `[Initial Plan][DONE code+automation/PARTIAL device 2026-07-31]` P2-2 Location Capability：Explore 主动 Intent → 双端一次性前台定位 → Result → Mutation → State 已实现；结果覆盖允许、拒绝、永久拒绝、服务关闭、不可用和失败/超时；位置不持久化，失败隐藏距离，shared 纯函数仅对有坐标地点计算。Android/shared 测试与双端构建通过；按 `P2_LOCATION_ACCEPTANCE.md` 完成真机权限、系统服务、前后台与销毁验收。
- `[Initial Plan][UI/UX Redesign][PARTIAL normal path 2026-08-11]` P2-3：双端高德真实地图代码与真机显示已通过；Explore MVI 列表/地图、隐私门禁、共享 `CCAmapView`、8 个 seed 坐标、Marker 摘要 → typed PlaceDetail、外部导航入口和失败回列表已实现。Android 已验证 Marker/摘要/详情、手势及 `Opened` 拉起高德；HarmonyOS 以全尺寸 `Stack` 承载默认 `MapViewComponent()` 后解决 Surface 空白并通过显示验收。剩余：HarmonyOS Marker 完整链、双端定位拒绝、无 Key/断网/初始化失败回列表、外部导航其余三态与 HarmonyOS 四态、Key Git 历史/备份审计、双端各 20 次生命周期压力验证；修复当前 Android 架构守卫测试 1 项失败并重跑 shared/Android/HarmonyOS 全套构建测试，最后才能标记 DONE。
- `[Code Scan]` 如需“按想去时间排序”，新增有迁移方案的数据模型，不能从现有 Set 伪造加入时间；用户文案已经统一为“想去”，底层继续保留 `Favorite*`。
- `[UI/UX Redesign][DONE code 2026-07-30]` P1-2 Profile Overview：头像/昵称/城市，真实碎片数/去过地点数/想去数，按真实 Capsule→Place 关系计算城市足迹，最多 3 个想去地点预览；编辑为 typed 二级 MVI 页面，清除本地档案下沉 Settings 危险操作区且保留 Place/Favorite/Capsule。shared/Android 自动化通过；HarmonyOS 构建和双端设备验收见 `P1_PROFILE_OVERVIEW_ACCEPTANCE.md`。
- `[Initial Plan][DONE code/PARTIAL device 2026-07-30]` P1-3 Settings 与数据管理：MVI 设置页已提供真实主题、隐私、关于、结构化数据/照片/缓存/恢复包占用与带确认的缓存清理；正式 UI 已移除 MMKV、Push、Replace、BackTo 等开发文案。按 `P1_SETTINGS_DATA_ACCEPTANCE.md` 完成双端设备验收。
- `[Initial Plan][DONE code+automation/PARTIAL device 2026-07-30]` 版本化 ZIP 导出、导入 codec 校验与预览、导入前恢复包、照片恢复、失败回滚及新媒体清理已实现；shared/Android 测试、Harmony Hvigor test 与 signed HAP 构建通过。仍须在双端执行真实系统选择器、损坏包、未来版本包、取消、空间不足及写入失败验收。
- `[Initial Plan][DONE code+shared automation/PARTIAL device 2026-08-11]` P2-6 备份兼容：外层 v2 + minReader 门禁、v1/v2 reader、Place V1→V2 恢复、Camera 已发布原图纳入、draft/缩略图排除与恢复后按需再生均已落实；按 `P2_BACKUP_COMPATIBILITY_ACCEPTANCE.md` 完成 Android/HarmonyOS 的导出、取消、损坏/未来包、空间不足、故障注入回滚、带照片恢复和旧版 reader 拒绝后关闭。
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

### P2-5 验收状态（2026-08-11）

- `[Initial Plan][DONE code/PARTIAL build+device]` 确定性缩略图、Timeline/Gallery 按需加载与原图降级、删除联动、1 小时宽限扫描、引用读取失败停删、Settings 真实 bytes/count 与安全清理已实现；未新增 MMKV key。shared Kotlin 编译通过；完整 Android 测试被本机 `shared/build` 文件锁阻断，HarmonyOS strict build 与双端真机矩阵待按 `P2_MEDIA_MAINTENANCE_ACCEPTANCE.md` 完成。

任务只有在真实数据、失败/空/加载状态、双端能力（或明确降级）、自动测试和必要手工验收都成立时才可从 TODO 移入 DONE。仅有 route、接口、静态页面、mock 或“已接入”文案不算完成。

## P2-7 分步状态（2026-08-12）

- [x] 7A 本地路线：手动选点、排序和持久化。
- [x] 7B 漫游会话：开始、暂停、继续、结束与重启恢复。
- [x] 7C 前台 GPS 轨迹：沙箱分片和 MMKV 索引。
- [x] 7D 手动打卡、城市碎片入口与真实总结。
- [ ] Android/HarmonyOS 移动、杀进程恢复与总验收；通过前不增加“漫游”一级 Tab。

## P2-7B 状态（2026-08-12）

- [x] 漫游会话：开始、暂停、继续、结束及进程重启恢复。
- [ ] 7C GPS 轨迹。
- [ ] 7D 打卡。

## P2-7C 状态（2026-08-12）

- [x] 前台轨迹：沙箱分片、MMKV 索引、定位失败不中止漫游。
- [ ] 后台定位、耗电策略和系统保活（独立平台 Feature）。
- [ ] 7D 打卡。

## P2-7D 状态（2026-08-12）

- [x] 附近确认、显式手动打卡、城市碎片入口和真实总结。
- [ ] Android/HarmonyOS 完成 P2-7 总验收并留存证据。
- [ ] 仅在总验收通过后评估“漫游”一级 Tab，不默认增加。
