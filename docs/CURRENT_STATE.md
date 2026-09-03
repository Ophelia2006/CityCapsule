# CityCapsule 当前开发状态

## 权威快照（2026-09-03）

> 本节是当前结论；后续按日期保留的段落是开发历史。历史中的“三个根 Tab”“地图/定位/相机未实现”“缩略图未实现”等描述已被当前代码取代，不再代表现状。

- `DONE（代码与自动化）`：Android/HarmonyOS 产品能力表均包含 Storage、Theme、Media、Locale、Archive、Location、External Navigation、Place Network、Track 与 Share；shared、Android、HarmonyOS 的模块名注册由 Android JVM 架构守卫共同检查，地图 Native View 也纳入双端注册门禁。
- `DONE（代码与 Android 验证）`：AppShell 的“探索 / 记录 / 漫游 / 我的”四个根内容常驻，切换使用即时 `scrollToPage`、禁止根手势、重复点击 no-op；陈旧的动画断言已修正。2026-09-03 `:androidApp:testDebugUnitTest :androidApp:assembleDebug` 通过。
- `DONE（P1 工程收敛）`：移除不存在的 H5/小程序 Gradle include、未使用 Picasso 和 HarmonyOS 示例 `KRMyModule/KRMyView` 注册；Android 增加正式应用名与矢量启动图标，HarmonyOS 清理 KuiklyDemo/example 占位名称。iOS/JS target 仍只属于 Kuikly 工程骨架，不在 Android/HarmonyOS 验收声明内。
- `DONE（业务代码）/ PARTIAL（设备验收）`：真实地点、统一城市上下文、地图/定位/外部导航、相册/相机、头像、路线拖拽、路线规划、自由/按路线漫游、轨迹、打卡、城市碎片、漫游历史、分享、缩略图和备份均已有真实代码链路；其中依赖系统权限、真实 Key、真实移动与文件选择器的异常矩阵仍需双端真机留证。
- `BLOCKED（本机 HarmonyOS 构建）`：2026-09-03 调用仓库 Hvigor wrapper 时进入配置阶段后报 SDK component missing。本轮不能据此宣称 HarmonyOS 构建通过；最近一次由当前功能源码通过的 HarmonyOS entry test 与 signed Debug HAP 证据为 2026-08-31。需补齐 DevEco SDK 组件后重跑。
- `PARTIAL（维护性）`：Kuikly/KSP 插件仍输出 multiplatform `ksp` deprecated 与 Gradle 9 compatibility 警告；本项目已明确关闭不适用的默认 hierarchy template、忽略 Windows 上禁用的 iOS target 提示，但插件升级风险需单独验证，不能在稳定化任务中强行升级。

当前可诚实描述为：**Android 代码、自动化和 Debug 包闭环完成；HarmonyOS 代码链路与历史构建闭环完成，但当前机器复建及双端真机异常矩阵尚未关闭。**

> 2026-09-02 HarmonyOS Explore 增量列表抖动修复：原实现虽然为在线地点增加了 `providerId` Compose key，但 `AppFixedHeaderScaffold` 把全部地点包在 LazyColumn 的单一 item/普通 Column 中，导致末尾节点不依赖真实视口即可组合、连续触发分页，并在追加时重新测量整段内容。现已新增真正接收 `LazyListScope` 的固定 Header Scaffold；手机本地地点和在线地点均是带命名空间稳定 key 的独立 Lazy item，分页只在真实最后可见项进入末 3 个在线地点时触发，Loading/失败/末页使用固定高度独立 Footer。Store 新增请求代次、同页幂等和分页失败显式重试；旧查询回调不能覆盖新查询，Append 保持旧前缀且按 provider ID 去重。shared 272 项测试与 Android shared 编译通过；HarmonyOS HAP 工具链当前终端不可定位，FPS、卡顿率与锚点位移仍需真机量化，因此状态为 `DONE code+shared automation/PARTIAL HarmonyOS performance acceptance`。

> 2026-08-31 照片比例修复：共享图片容器继续统一使用 `ContentScale.Crop`，即保持原比例填满容器并居中裁剪；HarmonyOS 缩略图从“强制解码为 512×512”改为“最长边不超过 512、等比缩放”。缩略图文件升为 v2，旧的可能已拉伸缓存不再复用，且在重建或删除原图时清理。HarmonyOS `entry@default test` 与 signed debug HAP 已由当前源码构建通过；待真机对竖图、横图、方图分别在时间轴方形位和漫游记忆宽封面验证中心裁剪。

> 2026-08-31 在线搜索与漫游真实性增量：Home、Explore/Favorites 与地点详情使用共享进程内地点查询 LRU（32 页、10 分钟 TTL，约百米位置网格，只缓存成功响应）；漫游根页最近记忆卡按同一 `roamingSessionId` 展示最新带图城市碎片的真实沙箱照片。路线编辑器没有真实道路 API 折线时只显示地点 Marker，不再绘制景点间直线；“按此路线开始漫游”自动规划失败时停留在编辑页并提示重试，不再把地点顺序降级伪装为可行道路。shared Android 编译与全量单测、HarmonyOS arm64 链接、Hvigor entry test 及 signed debug HAP 构建通过；三项双端真机交互仍待验收。

> 2026-08-31 鸿蒙稳定性与路线降级修复：Explore 在线搜索增量结果以高德 `providerId` 建立 Compose 稳定身份，避免追加分页时已有原生图片节点按位置复用或重建。路线编辑器不再把地图预览及“按此路线开始漫游”绑定到道路规划成功或既有 `editingId`：新路线在名称、地点有效时即可开始；道路 API 配额/网络失败会保留真实错误并将地点顺序保存为本地路线后进入漫游。没有道路规划结果时地图只展示明确标注的地点顺序连线，不冒充真实道路。Android shared 编译与 shared 单测通过；HarmonyOS 真机滚动、地图 SDK 配额错误和降级进入漫游仍待复验。

> 2026-08-30 漫游一级页自动推荐修复：此前“下一站推送”只存在于正在进行的漫游会话页，截图所示的一级“漫游”Tab 没有读取路线、想去或漫游历史，因此只会展示固定入口。现在一级页激活时读取真实路线、想去地点、地点目录和漫游历史：优先推荐包含最多“想去”地点的可用路线（同分取最新路线），展示最多 3 个站点与已保存的真实道路距离/时长；没有路线时给出基于真实想去数量的下一步；同时展示最近一次真实漫游并直达其回顾。未创建路线或未产生历史时不伪造推荐。Android 编译与 shared 单测通过，双端界面仍待真机验收。

> 2026-08-30 下一站推送修复：上一版只是把“下一站/往期记忆”放在大地图下方的普通内容区，必须滚动才可见，不符合自动推送语义。现在按路线会话进入 ACTIVE 后自动弹出 Bottom Sheet；每次打卡导致下一站变化时再弹一次，同一地点关闭后不重复打扰。弹层显示真实路线中的首个未打卡地点、剩余地点、附近距离，并最多展示该地点两条非当前会话的已发布城市记忆；地图隐私弹层优先，避免双弹层冲突。shared 262 项测试及 HarmonyOS signed debug HAP 构建通过。

> 2026-08-30 按路线漫游收口：用户开始按路线漫游时，如果当前地点顺序没有有效的真实道路快照，`LocalRouteStore` 会自动调用步行 API、保存路线后直接进入会话，不再要求先手动规划或再次确认。实时漫游地图同时显示计划灰线和实际琥珀线；结束 ACTIVE 会话前补采一次真实位置，降低短时漫游只有一个轨迹点的情况。报告不足 2 个有效 GPS 点时明确说明无法绘制实际线，不用计划线冒充。漫游页新增“下一站”：按路线中首个未打卡地点自动推进，显示剩余地点、附近距离，并推送该地点过去真实发布的最多两条城市记忆。shared 262 项测试、Android Kotlin 编译、HarmonyOS signed debug HAP 构建通过；定位拒绝、定位失败和短时会话仍需双端真机验收。

> 2026-08-29 规划线显示修复：根因有两项——路线编辑器生成道路规划后，“按此路线开始漫游”此前未先保存内存中的规划快照；且灰色计划线宽度小于上层实际轨迹，重合时会被完全遮住。现在开始新漫游会先保存路线与真实规划结果，成功后才导航；双端计划线改为 18px 底层对照带，报告相机同时覆盖计划与实际全部点。旧漫游没有原规划快照，继续诚实降级，无法事后补造。shared 262 项测试、Android Kotlin 编译和 HarmonyOS signed debug HAP 构建通过。

> 2026-08-29 漫游回顾完成度：详情已改为沉浸式轨迹地图、本地开始/结束时间、总时长、GPS 真实距离和单一“沿途记忆”时间轴。地点到达、照片、心情、文字和标签按发生时间串联；封面优先使用本次最早一张真实碎片照片，无照片时以轨迹地图为视觉主体。新打卡固化“到达当时是否想去”，可统计完成想去与非路线发现；旧打卡显示“旧记录未采集”。路线与漫游历史升级为 v2，只有真实道路 API 成功结果才保存计划采样折线/距离/时长并在回顾中显示计划灰线、实际琥珀线、绕路与跳过地点；v1 只降级，不推算。双端系统文本分享只使用真实漫游摘要。shared 单测、Android Kotlin 编译及 HarmonyOS signed debug HAP 构建通过；双端真机仍待验收。

> 2026-08-29 探索与根导航增量：地图 SDK 隐私同意已登记为应用级 `settings.map_privacy_accepted`，Home、Explore、路线规划、漫游实时地图与漫游回顾共用，成功同意后不再逐页重复提示（系统定位权限仍只在用户主动定位时由系统管理）。高德地点 bridge 支持 `page/pageSize`，Explore 每页 12 条并在距末尾 3 条时预取；跨页按 provider ID 去重、失败保留旧页。地标改用“景点”、古迹改用“名胜古迹”，二者不再被过严文本二次筛选，“全部”也不再因本地点已满 12 条而显示为空。AppShell 已扩展为“探索 / 记录 / 漫游 / 我的”，漫游根页连接活动会话、自由漫游、路线、想去及回顾；Profile 的档案/数据入口移至右上角 ⚙️ Sheet。shared 259 项测试、Android Kotlin 编译和 HarmonyOS signed debug HAP 构建通过；双端真机仍待验收。

> 2026-08-29 路线编辑增量：添加地点候选现限定为 Home / Explore / Map 共用的当前探索城市，UI 与 `LocalRouteStore.AddPlace` 双层校验，并在城市切换后按 `ExploreCityRuntime` 刷新。旧路线中已有的跨城地点不会被静默删除，仍可显示并由用户手动移除。真实道路“按当前顺序生成 / 生成推荐顺序”及“开始 / 继续漫游”操作已移动到“添加地点”之前。shared 单测与 Android Kotlin 编译、HarmonyOS signed debug HAP 构建通过；双端城市切换与旧路线交互仍待真机验收，状态为 `PARTIAL（code+automation+dual build DONE / device pending）`。

> 2026-08-25 道路规划运行时修复：首次实现错误地用新 `RouteNetworkModule` 类型获取已经以 `KuiklyPlaceNetworkModule` 注册的同名 bridge，导致模块获取异常被统一降级为 `Unavailable`，即使双端 Web Key 已配置也始终提示服务不可用。现已复用 BasePager 中真实注册的共享代理；共享测试、Android APK 与 HarmonyOS signed HAP 重新构建通过。

> 2026-08-25 道路规划增量：路线编辑器已通过双端 `CCPlaceNetworkModule.walkingRoute` 接入高德步行路径 API。P0 可按用户手动顺序逐段取得真实道路折线、距离与时长并交给双端原生地图渲染；P1 对 2–8 个地点请求无向道路距离矩阵，固定首个地点，以最近邻 + 2-opt 生成“推荐顺序”，用户确认采用后才改变待保存顺序。无坐标、无 Key、断网、API 失败或折线缺失均停止并保留原顺序，不回退直线。共享自动化、Android Debug APK 与 HarmonyOS signed HAP 构建通过；真实 Key、实际道路、配额和双端交互仍待真机验收，因此状态为 `PARTIAL（code+build DONE / device+live API pending）`。

> 2026-08-25 地点语义增量：`PlaceCategory` 新增地标、历史遗迹、博物馆、美术与展览、教堂、寺庙、咖啡、美食餐厅、甜品烘焙、公园、自然风景、湖泊滨水、购物、市集老街、街区漫步、演出娱乐等可选细分类及对应 Emoji。内置 seed 升至 v4 并按真实标签刷新精确 seed；高德 POI 导入按 type 映射细分类。旧 `culture/food/nature` wire 值继续可读但不再出现在新建地点选项中。Capsule 心情保留既有 wire 值并增加 Emoji，同时新增怀念与疲惫。

> 2026-08-24 稳定性修复：恢复中的按路线漫游现以会话真实 `routeId` 为准并按路线顺序加载地点；漫游碎片选点已扩展为“路线/附近优先 + 全量地点”；地点详情可直接追加到已有漫游路线。Android crash buffer 确认反复打开地点/停留闪退为 Glide Bitmap 被回收后仍由 `KRImageView` 绘制，adapter 已复制 Bitmap 脱离 Glide 池。Harmony Kuikly host 路由项改为组件删除时注销，避免页面被覆盖时过早丢失返回栈记录。shared 测试与 Android APK 构建通过；Android 新包覆盖安装被设备端拒绝，Android/HarmonyOS 真机回归仍待执行。

> 2026-08-25 路线与推荐纠偏：Home 推荐地图在同一页面最多展示 5 个类别 Emoji + 地点选择项，不再用图片缩略图；路线编辑器已移除直线距离“推荐顺序”、直线折线和一键采用，避免把可能穿越水面/建筑的几何连线误作可漫游路线。路线仍支持真实的手动拖拽排序；道路级步行路径计算尚未实现，状态为 `NOT_STARTED`。

> 当前代码检查点（2026-08-17）：P2-7 已由 `486bc9a` 独立提交，Place V3/上海城市包/探索城市闭环已由 `8ba3147` 提交。其后本轮推荐、用户地点坐标、未支持城市空态和地点封面生命周期收口已通过 shared 测试、Android Debug APK、HarmonyOS Hvigor test/HAP 构建，尚待提交；正式一级导航仍只有“探索 / 记录 / 我的”。以下历史段落若与本检查点冲突，以本段及当前代码为准。

> 2026-08-20 P1 增量：Profile schema v2 与托管照片头像、路线 `Reorder(fromIndex,toIndex)` 长按拖动、自由漫游 500 米附近地点/200 米 GPS 到达/显式 Capsule 漫游关联/按打卡顺序保存路线已落地；真实本地搜索、分类、距离及高德单项导入经代码复核已在此前实现。shared 全量单测、Android App 编译和 HarmonyOS debug HAP 构建通过，双端真机矩阵尚未完成，统一状态为 `PARTIAL（代码与构建完成 / 设备验收未完成）`。验收见 `P1_PRODUCT_CAPABILITIES_ACCEPTANCE.md`。

> 2026-08-24 漫游闭环增量：想去地点在路线选点/自由漫游附近列表优先展示；确认到达后从想去移除并可撤销；打卡可精确创建关联 `placeId + roamingSessionId` 的城市碎片；结束会话幂等归档至 `roaming.history`，Record 根页可进入历史列表/详情并打开或补记对应碎片。同时阻止活动会话被新漫游覆盖，路线页改为“继续上次漫游”。备份结构化协议升至 v10 并包含漫游历史。共享层、Android Debug APK 与 HarmonyOS signed HAP 均已构建通过，双端真机验收待执行。状态：`PARTIAL（代码与双端构建完成 / 设备验收未完成）`。

> 2026-08-24 实时漫游地图增量：漫游页会恢复已有轨迹点，每次 15 秒成功采样在沙箱落盘后同步暖橙色折线、当前位置和地图相机；地图最多显示 500 个保首尾抽样点，文件保留全量点。“留下城市碎片”新增选点 Sheet，默认选中当前最近景点，可改选附近/已到达/路线地点，再以 typed `placeId + roamingSessionId` 进入编辑器。shared 253 项单测、Android Debug APK、HarmonyOS ArkTS 编译及 signed HAP 均通过。双端真机还未验收；当前仍是 Page 驱动的前台采样，未实现 Android 前台服务/HarmonyOS 后台长时任务，锁屏或长时离开 App 的无缺口轨迹为 `NOT_STARTED`。

> 2026-08-20 HarmonyOS 启动闪退修复：Pura 80 faultlogger 明确记录 `SIGABRT`，Kuikly `DefaultRenderNativeContextHandler::CallKotlinMethod` 断言“make sure initKuikly() has been called”。`EntryAbility` 现于 HMRouter 创建首个 Kuikly Host 前同步调用全局 `KuiklyNativeManager.internalDoLoad()`，失败时记录错误并停止加载 UI，避免进入必然 native abort。修复版 signed HAP 已构建并覆盖安装；设备当前锁屏导致 `aa start` 返回 10106102，解锁后的启动/反复冷启动验证待完成。

## 2026-08-17 地点与探索城市增量

- `Place` schema v3 增加公共 `description`、私人 `personalNote`、`contentSource`、`IMPORTED` 来源、可选坐标和 `visualRef`。v1/v2 旧 seed note 迁为公共简介，用户地点 note 迁为私人备注；业务 ID 关联不变。
- seedVersion 3 包含上海 15 个、杭州 4 个地点。上海地点均有稳定 ID、区、完整地址、WGS-84 坐标、分类、标签、简介和内容来源；没有已登记授权的真实摄影，统一使用类别 fallback。
- `ExploreCityRepository` 独立持久化当前与最近探索城市。Home/Explore/Map 使用同一选择；主动定位经独立的受支持城市判定并要求确认，不覆盖档案城市，失败不妨碍手动选择。
- 地点详情只把 `description` 作为公共介绍并展示内容来源。用户地点可从相册或相机设置托管封面；Capsule 照片不会自动成为公共封面。
- 备份协议升至 v8，包含探索城市选择及用户地点托管封面的媒体收集/路径重写。shared 自动化通过，仍待双端旧安装、媒体和城市切换真机验收。
- Home 推荐已加入“想去/未去过 → 有真实封面 → 进程内当前位置距离 → 类别多样性 → 稳定 ID”的可解释排序；定位结果不写入 MMKV，重启后必须由用户再次主动定位。
- 用户地点编辑器可手填 WGS-84 坐标或主动使用当前位置。已知但未开放内容包的城市（当前为北京）可以被选择并持久化，Home/Explore 会显示诚实空态，不混入上海或杭州内容。
- 用户地点封面在替换、移除、放弃草稿和删除地点时进入跨 Place/Capsule/草稿的引用保护清理；引用读取失败时停止删除，避免存储故障造成媒体丢失。
- 该 Place V3 检查点完成时，在线 POI 与在线逆地理编码尚未实现；其后的真实状态以紧接着的“高德在线地点增量”为准。内置地点仍没有已登记授权的摄影，继续使用类别 fallback。

## 2026-08-17 高德在线地点增量

- Android/HarmonyOS 新增 `CCPlaceNetworkModule`，Web Key 只来自 Git 忽略的本机配置；共享层通过 `PlaceRemoteDataSource` 使用网络能力，Page 不直接访问 HTTP。
- Explore MVI 支持按当前探索城市进行高德关键词搜索；查询为空且已经主动定位时可搜索附近地点。在线结果展示高德 `photos` 首图，空结果、断网、Key 不可用和服务错误均不覆盖本地目录。
- 用户明确点击后才把单个 POI 写入本地 Catalog，来源为 `IMPORTED`，`contentSource` 保存高德 POI ID 用于去重；不会把整座城市结果批量写入 MMKV。
- 高德国内查询坐标在网络边界完成 WGS-84/GCJ-02 转换，持久层继续只保存 WGS-84。真实 Web Key 已以“上海/外滩”最小请求验证 `status=1` 且返回照片。
- 在线逆地理编码已接入并以原离线城市半径判定兜底。当前城市持久协议仍只接受 `CityRegistry` 已登记城市，因此任意新城市的长期选择仍需后续扩展城市目录协议；本轮没有虚构“全国城市已经完整支持”。
- 地点详情优先展示地点自己的托管封面；没有本地封面时先读本地远程图片缓存，未命中才通过 `PlaceRemoteDataSource` 按名称与城市补充高德 POI 首图，并独立标注图片来源。Home、Explore 列表和详情共用 `PlaceMedia`，统一按“本地封面 → 30 天高德 URL 缓存 → 类别 fallback”展示；列表不批量联网，缓存最多 100 条、不进入备份，图片加载失败会删除失效项。地点文字仍保留原 `contentSource`。
- 2026-08-19 为远程地点图片增加共享加载协调器：首批 6 个地点优先、最大 3 个唯一 URL 并发、相同 URL 冷加载去重、组合销毁释放 lease，并提供进程内成功/失败/取消/去重指标。Android 继续使用 Glide 的目标尺寸解码与缓存；HarmonyOS 实际缓存命中和滚动性能尚待真机量化。
- `CCPlaceNetworkModule` 的 shared proxy 已在所有业务 Pager 的共同基类 `BasePager` 注册；此前只完成平台宿主注册、遗漏 Pager 内模块表会使独立地点详情首次查询时触发 `acquireModule` 致命异常，该缺口已由架构守卫覆盖。

> 账户迁移检查点：2026-07-30 当前 `main` 位于 `da99137 P1-1：Explore`，其后仍有固定操作层与文档的未提交增量。迁移或重新 clone 前必须先阅读 `MIGRATION_HANDOFF.md` 并保存工作树；不能只依赖远端 HEAD。

## 总体判断

项目处于基础版中后期：跨端宿主、统一路由、双端 MMKV、本地档案/首次引导、离线地点，以及“地点详情 → 城市碎片 → 时间轴/相册 → 回忆”已经形成代码闭环。正式“探索 / 记录 / 我的”一级导航、Home、Place Detail、Record、Explore、Profile 与 Settings/Data Management 首版代码已经接通。P2-3 双端高德真实地图、Marker、地点摘要、详情入口和外部导航代码已落地，Android 与 HarmonyOS 真机均已显示真实地图；异常降级矩阵、外部导航四态、20 次生命周期压力测试和自动化全绿仍未完成。P2-4 系统相机的双端代码与构建已完成、真机矩阵待验收；缩略图尚未完成，所以整个 App 仍不是完整基础版。

状态定义：DONE 为已形成真实闭环；PARTIAL 为可用但缺计划中的关键环节；PLACEHOLDER 为协议/骨架/开发验证；NOT_STARTED 为规划存在而无实现；BLOCKED 为有明确外部阻塞；UNKNOWN 为仅凭仓库无法确认。

## 功能状态

| 功能 | 状态 | 代码证据与边界 |
| --- | --- | --- |
| Android/HarmonyOS Kuikly 宿主 | DONE | 两端有启动、host、adapter 与平台工程；不代表所有业务双端手测完成 |
| 强类型共享路由 | DONE | `AppRoute/AppNavigator/AppRouteTable` + 双端 dispatcher/stack tests |
| 单一 AppShell / 正式一级导航 | DONE（代码）/PARTIAL（设备体验） | 一个 `AppShellPage`、一个 Bottom Navigation、四个常驻根内容；点击 Tab 直接 `scrollToPage`，避免非必要位移动画，根手势关闭，重复点击 no-op；仍待双端设备视觉、屏幕朗读与返回键走查 |
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

## HarmonyOS 启动稳定性（2026-08-20）

- `DONE`：修复 Pura 80 冷启动 `SIGABRT`。故障 HAP 混用了旧版 `libshared.so` 与 Kuikly Render 2.23.3，导致渲染器在首帧触发 `make sure initKuikly() has been called` 断言。
- `DONE`：Kuikly 依赖统一固定为 2.23.2：通用/Android 使用 `2.23.2-2.1.21`，HarmonyOS KMP 使用已发布的 `2.23.2-2.0.21-ohos`，ArkTS Render 使用 `2.23.2`。2.23.3 未发布对应的 `ohosArm64` Maven variant，当前不得单独升级 Render。
- `DONE`：从当前源码重新链接 `libshared.so`、构建 signed Debug HAP，并在 HUAWEI Pura 80 上连续完成 3 次冷启动；每次 5 秒后进程均存活，未产生新的 C++ crash。
- `PARTIAL`：本轮只证明应用启动链稳定；地图反复进入退出、定位权限与地图 Marker 等平台能力仍按各自验收清单执行。

## 路线排序、头像与探索城市修正（2026-08-20）

- `DONE code`：路线拖拽不再用拖动位移反向推动整页；排序项以 `placeId` 作为稳定身份，拖拽把手与卡片一起排序，选中卡片使用语义强调色，并在持续拖向上/下边界时循环滚动与重排。保留辅助菜单，仍然只在点击保存后写 Repository。
- `DONE code`：资料编辑取消“拍摄头像”入口及对应 Intent/相机依赖，只保留系统相册选择；Capsule Editor 的拍照能力不受影响。
- `DONE code`：探索城市协议升级为 schema v2，可保存定位检测到但不在本地 registry 的城市定义；确认西安等动态城市后 Store 会立即更新并持久化，schema v1 已有选择可迁移。
- `DONE code`：城市选择入口移到 Explore 顶栏左上标题区；当前选中城市用品牌色、加粗和勾选表示，不再显示“当前”字样。不支持内容的动态城市明确显示“暂无内容”，不伪造地点。
- `DONE automation`：Android 共享编译、`:shared:testDebugUnitTest` 和 HarmonyOS `:shared:compileKotlinOhosArm64` 通过；新增动态西安持久化及 schema v1 迁移测试。
- `PARTIAL device`：拖拽视觉、边界自动滚动速度、长地点名稳定布局、动态城市确认和头像选图仍需 Android/HarmonyOS 真机验收。
- `FIXED code 2026-08-20`：首轮拖拽修正仍以可变 `index` 作为 `pointerInput` key，重排后会重启手势，导致强调色和自动滚动一起终止。现改为稳定 `placeId` key，并由 `AppFixedHeaderScaffold` 回传真实滚动视口边界，指针进入顶/底 72dp 区域时持续滚动与重排。
- `FIXED code+automation 2026-08-20`：Home 原先把 `selectedCityId` 再从固定 Registry 反查，动态西安因查不到而回退上海。现直接消费 `ExploreCitySelection.selectedCity`；Home 左上城市标题可直接打开城市选择器。
- `DONE code+automation/PARTIAL network+device 2026-08-20`：当所选城市没有本地 catalog 地点时，Home 与 Explore 使用已有高德 `PlaceRemoteDataSource` 按城市中心请求“景点”候选。Home 只展示最多 4 个在线候选；Explore 允许用户确认后保存到本地，不自动持久化整批 POI。新增动态西安上下文与在线推荐回归测试；真实结果仍取决于双端高德 Key、网络及 POI 服务响应。
- `DONE code+automation/PARTIAL device 2026-08-20`：在线地点不再只存在于 Bottom Sheet；Explore 主列表直接显示最多 12 个在线候选及“保存到本地”操作。Home/Explore 在线候选共用真实 `photoUrl` 媒体组件，经 `PlaceImageLoadRuntime` 执行 URL 去重、首屏优先和最多 3 个冷请求并发；无图或加载失败保留类别 fallback。
- `FIXED code+automation 2026-08-20`：高德返回的“西安市”与产品城市“西安”原本被精确比较，导入成功后立即被当前筛选隐藏。现在网络解析、新导入和 Home/Explore 城市匹配共用行政后缀规范化；已存储的“西安市”旧数据也可直接显示。导入成功后同时将该 POI 的真实图片 URL 写入有界照片缓存，当前列表立即复用。
- `FIXED code+automation 2026-08-20`：单一“景点”查询在西安只返回 8 个可解析 POI，尽管双端原生请求 `offset=20`。城市推荐现按“景点 → 博物馆 → 公园 → 咖啡”顺序补充，以 provider ID 去重，Explore 达到 12 个、Home 达到 4 个后立即停止后续请求。服务端所有分类合计仍不足时诚实显示实际数量，不伪造补齐。
- `FIXED code+automation 2026-08-20`：Explore 原先仅在当前城市地点数为 0 时请求在线候选，历史测试保存的 8 个地点会阻止自动加载。默认无搜索、无筛选的列表现在会在本地可见地点不足 12 个时请求在线推荐，排除已经按 provider ID 保存的 POI，并只补足剩余名额；例如 8 个本地点会补充最多 4 个在线候选。搜索和筛选结果不自动混入推荐。
- `DONE code+automation/PARTIAL device 2026-08-21`：Explore 顶部粗分类 chips 已改为不进入持久化 schema 的探索主题：城市地标、咖啡、餐厅、博物馆、展览、公园、自然景点、商场街区。主题分别过滤本地名称/标签并触发对应在线关键词查询；高德类型解析优先识别公园、餐饮、文化和购物细类，避免被通用“风景名胜”提前归入城市地标。shared 单测与 HarmonyOS arm64 共享编译通过，双端真实 POI 命中质量与横向滚动仍待真机验收。
- `FIXED code+automation/PARTIAL device 2026-08-24`：无本地地点的城市此前只有紧凑在线候选，导致西安 Home 缐少主推荐视觉。现在首个真实在线候选使用 Hero 地点卡，其余候选保持紧凑卡；不把在线候选伪装成本地已保存地点。
- `DONE code+automation/PARTIAL device 2026-08-24`：Home 左上城市选择器支持输入任意城市名。系统通过现有 `PlaceRemoteDataSource` 查询真实 POI，只有返回城市与输入匹配时才以该 POI 坐标建立并持久化动态城市，随后统一刷新 Home/Explore/Map；无网络、无 Key 或无匹配结果时明确报错，不创建虚构城市。shared 单测和 HarmonyOS arm64 native 链接通过，待鸿蒙真机验证输入法、弹窗状态及真实服务响应。
