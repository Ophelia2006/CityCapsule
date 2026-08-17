# CityCapsule 已确认架构决策

本文只记录有代码或初始/当前文档明确依据的决策。没有依据的“为什么”不补写。

## ADR-001：不建设自有后端

状态：Accepted（初始规划明确，当前代码一致）。

结构化业务数据保存在本机；未来第三方能力通过 API/SDK 获取。当前仓库的 Node `static_server` 是 Kuikly JS 开发脚手架，不是产品后端。

## ADR-002：跨端业务 UI 使用 Kuikly Compose DSL

状态：Accepted。

初始规范将其列为固定技术方案；当前已注册业务页均位于 shared 的 Kuikly `@Page`。平台工程负责宿主、系统外观、路由与原生 bridge。

## ADR-003：共享业务不直接依赖平台路由

状态：Accepted。

commonMain 以 `AppRoute/AppNavigator` 表达跳转。HarmonyOS dispatcher 使用 HMRouter，Android dispatcher 使用 Activity/Kuikly host；业务页面未直接引用 HMRouter。理由由初始规范明确：保持共享业务平台无关和双端等价。

## ADR-004：路由传 ID，不传复杂对象

状态：Accepted。

当前 `PlaceDetail/Editor`、Capsule 协议和 native request 均传 ID/简单值，路由构造校验非空参数。这样避免平台对象和大型数据跨 bridge；详情从 Repository 重新读取。

## ADR-005：双端结构化存储使用 MMKV，并通过 typed bridge 隔离

状态：Accepted。

初始规划固定 MMKV；当前 Android/HarmonyOS 都使用 2.4.0。shared 仅依赖 `KeyValueStore`，不拿 MMKV 实例。当前协议用 store、wire key、value type、canonical string 与错误码保证双端一致。

## ADR-006：MMKV 分为 meta/preferences/cache 三个 store

状态：Accepted（当前存储文档与代码明确）。

`cc_meta` 仅供平台迁移，`cc_preferences` 保存长期非敏感设置/业务数据，`cc_cache` 保存可清理草稿。敏感 Key、媒体和无上限列表被明确排除。

## ADR-007：当前地点目录是单设备、离线、有上限的 catalog

状态：Accepted for current milestone。

`PlaceContract.MAX_CATALOG_SIZE = 500`，`places.catalog` 保存一个 JSON object，seed 缺失时初始化；不存在在线 POI、坐标或地图 SDK。当前阶段文档确认这是有边界的离线目录。为何由初始“实体独立 Key”改为 catalog 未记录，不在此补写。

## ADR-008：收藏 ID 与地点 catalog 分离

状态：Accepted。

代码注释明确目的：切换收藏不重写 catalog。Repository 在收藏前检查地点存在，读取时剔除悬空 ID，地点删除后尽力清理。

## ADR-009：本地档案不是账号

状态：Accepted。

当前档案文档明确一个安装对应一个纯本地档案，不产生网络身份，不申请相机/相册等权限。头像使用 shared 预设，支持引导草稿和本地重置。

## ADR-010：设计系统位于 shared package，而非独立 Gradle module

状态：Accepted for current stage。

当前文档明确第一阶段不拆独立模块。业务页消费语义 token，平台只处理系统外观/系统栏。复杂度稳定后是否物理拆分尚未决定。

## ADR-011：大媒体与轨迹不进入 MMKV

状态：Accepted；照片部分已实现，轨迹仍是未来约束。

初始规范明确图片、视频、导出包、缩略图和大量轨迹点保存应用沙箱，MMKV 只保存路径与元数据。当前 Android 与 HarmonyOS Photo Picker 都把选中原图复制到应用沙箱，`CityCapsule` 仅持久化 `file://` 路径；尚无缩略图、视频、导出包与轨迹。

## ADR-012：当前城市碎片使用有上限 catalog 与单草稿 Key

状态：Accepted for current milestone。

当前 `CapsuleContract` 把 catalog 上限固定为 500、单条照片上限固定为 9；已发布内容写入 `cc_preferences/capsules.catalog`，可恢复草稿写入 `cc_cache/capsules.draft`。这与现有 Place/Favorite 的有界 MMKV 架构一致，并保持草稿可清理。为何没有采用初始规划中的实体独立 Key，仓库没有进一步理由记录。

## ADR-013：城市碎片日期使用设备本地时区

状态：Accepted，取代临时 UTC 实现。

commonMain 不自行维护时区数据库。Android/HarmonyOS 的 `CCLocaleModule` 按设备本地时区把 epoch 格式为 `yyyy-MM-dd`，shared 只负责协议校验和产品文案；bridge 不可用时保留确定性 UTC 降级。这样避免跨本地午夜显示成前一天或后一天。

## ADR-014：媒体删除采用“候选路径 + 全引用保护”

状态：Accepted。

正常删除碎片、移除照片、丢弃草稿和拒收超额照片只产生清理候选。shared 在每次清理前读取已发布 catalog 与当前草稿，排除仍被任一记录引用的路径；读取失败则不删除。平台再限制路径必须属于 `filesDir/images/original`。该方案优先保证不误删用户记忆，不把 MMKV 变成媒体存储，也不把任意文件删除能力暴露给业务页。

## ADR-015：有关联城市记忆的地点禁止删除

状态：Accepted for current product flow。

地点是城市碎片的业务上下文。当前产品入口在请求删除和确认执行前分别查询关联碎片；只要存在历史记忆，或关系读取失败，就阻止删除并提示先处理记忆。原有“删除地点后保留碎片并显示曾经到访的地点”只保留为旧数据/异常数据的读取降级，不再是允许的正常用户操作。

## ADR-016：基础阶段采用三个真实根页与 typed replace

状态：Superseded by ADR-019（保留为 P0-2 历史记录）。

用户明确批准基础阶段一级导航为“探索 / 记录 / 我的”，分别落到 `Home`、`Record`、`Profile` typed root；Record 当前暂由 `Timeline` route 承担。选择其他根 Tab 使用 `AppNavigator.replace`，重复点击当前 Tab 不发路由请求；详情、编辑和设置等二级页不显示底栏。该阶段复用双端现有 dispatcher 与 route stack，不为根 Tab 重写路由系统，也不提供根 Tab 左右滑动。Debug、Router Diagnostics、Image Benchmark 不提供正式产品入口。

## ADR-017：时间轴与相册合并为 Record 根容器内部视图

状态：Partially implemented。

用户确认 Timeline/Gallery 应重构为同一个 Record 根容器内的两种视图。当前 P0-3A 已让两者共享 Capsule catalog、底栏和 `RecordRootView`，点击 segmented control 不执行 `push`、`replace` 或 `back`；进入 Capsule Detail 才使用 typed route。Record 内部 `HorizontalPager`/左右滑动尚未实现。现有独立 `Gallery` route/page 作为兼容入口保留，但不再是正式产品层级。

ADR-019 已取代 ADR-016 的根级 typed replace；Record 内部的二级 Pager 仍应作为独立 Feature 验收，不能和根 Pager 手势混为一谈。

## ADR-019：单一 AppShell 与分阶段根 Pager

状态：Accepted and implemented in P0-3A。

用户明确批准现在建立单一 `AppShellPage`：只创建一个 Bottom Navigation，把 Home/Record/Profile 放入同一 `HorizontalPager`，点击底栏调用 `animateScrollToPage()`，根 Pager 暂设 `userScrollEnabled = false`。三个根内容持续组合并分别保留滚动与页面状态；重复点击当前 Tab 为 no-op。`AppRoute.Home/Timeline/Profile` 保留为 typed 入口别名，但都解析到 canonical `app_shell` route/page，并用 `initialRootTab` 指定初始页。

详情、Editor、Settings 仍使用 typed route，位于 AppShell 之外并隐藏底栏；Debug 页面不进入 AppShell。未来是否开放根 Pager 手指横滑需结合 Record 内部横向 Pager 的同轴手势冲突另行验收。

由于三个根目标共享 canonical `app_shell` route key，二级页返回指定根目标时使用 `backToRoot(AppRootTab)` 同时传递壳内目标状态与 typed backTo；缺失壳时的 replace fallback 通过 `initialRootTab` 恢复正确根页。

## ADR-018：首页内容与无摄影阶段展示规则

状态：Accepted and implemented in P0-3 Home Redesign。

探索首页以重点地点整卡作为 Primary。seed 与用户自建地点允许混合参与可解释的本地排序；由于当前模型没有 source，不伪装成能够按来源区分。Profile Overview 允许展示由当前数据精确计算的碎片数、关联地点数和想去数。地点摄影能力完成来源授权、资产登记、模型/迁移与双端验收前，所有地点内容统一使用代码生成的类别 fallback。

P0-3 将排序规则冻结为“当前档案城市优先 → 想去或尚未记录优先 → 同优先级内按类别轮转 → category enum 与 placeId 稳定兜底”。该规则只使用 Profile、Place、Favorite、Capsule 的本地真实数据，不宣称 AI、个性化、附近或实时推荐。Home 最多展示 3 条真实最近记忆；快速记录必须先选择真实地点并传递非空 `placeId`。

## ADR-020：后续表现层采用按 Feature 渐进迁移的轻量 MVI

状态：Accepted target；技术 Spike 与首个 Feature 迁移尚未完成。

用户明确要求后续代码尽量采用 MVI。当前代码已有不可变 `UiState`、集中状态更新、可替换 Repository 和 StateHolder 单测，但仍是 callback 型 StateHolder，并没有统一 Intent、Mutation、纯 Reducer、Effect、StateFlow 或 Store 生命周期协议。`shared/commonMain` 也没有显式声明 coroutines 依赖。因此该决策不能写成“项目当前已经使用 MVI”。

项目采用自有薄 MVI Contract，并按 Feature 渐进迁移；不引入全局 Redux Store、不一次性重写 9 个 StateHolder，也不在验证前引入第三方 MVI 框架。首个试点为 PlaceList/Explore，之后依次考虑 Home、PlaceDetail、Profile、Record 只读页，最后再处理编辑器、Onboarding 和 AppShell。

固定边界是：UI 只发送 Intent、读取 StateFlow State、消费一次性 Effect；异步结果经 Executor 转为 Mutation，由纯 Reducer 产生新 State。导航仍由 UI 使用 typed `AppNavigator` 执行，Store 不依赖平台或 Compose UI 状态。UseCase/DataSource 只在出现真实业务边界时增加，MVI 不等同于为 Clean Architecture 补空层。

在业务迁移前先完成 Android/HarmonyOS 技术 Spike，验证显式 coroutines 依赖、StateFlow 收集、Effect 单次消费、Intent 顺序与 `dispose()`。Effect 的具体 Flow 实现、Scope/Dispatcher 和 callback/suspend 适配方式由 Spike 结果决定。详细规则见 `MVI_ARCHITECTURE.md`。

## ADR-021：数据备份采用版本化 ZIP，并在确认导入前创建恢复包

状态：Accepted and implemented（shared/Android 已自动化，HarmonyOS signed HAP 已构建，双端设备验收待完成）。

初始三阶段规划明确要求备份同时携带结构化数据与媒体，导入顺序必须是“选择 → 临时解压 → manifest/JSON 校验 → 预览 → 确认 → 生成导入前备份 → 写入”，且验证失败不得覆盖当前数据。当前媒体真实保存在应用沙箱、MMKV 只保存路径，因此只导出 MMKV 字符串不能形成可恢复备份。

当前实现使用版本化 ZIP：`data/backup.json` 保存六个长期 persistent key 的 wire value 与存在性，`media/index.json` 保存旧沙箱路径到归档 entry 的映射，`media/images/*` 保存仍被已发布 Capsule 引用的托管原图。两个 cache draft key 与可再生成缩略图不进入备份。导入先在 cache staging 解压，由共享层用当前 codec 校验完整 key 集合并生成数量预览；用户确认后平台先在 `filesDir/backups/recovery` 创建当前数据恢复 ZIP，再复制导入媒体并写入结构化数据。写入失败时恢复旧 snapshot 并删除本次创建的媒体；恢复不完整时保留恢复 ZIP 并停止宣称成功。

P2-6 将外层归档版本提升为 v2，并写入 `minReaderVersion=2`：当前 reader 继续接受 v1/v2，未来版本或要求更高 reader 的包在预览前拒绝；旧 v1 reader 因不接受 `backupVersion=2` 而明确失败，不能静默按旧语义导入。ZIP 目录结构未改变。Place V1 → V2 继续复用 StorageKey codec，恢复落盘时重新编码为 v2。

Settings 只依赖共享 `DataArchiveCapability`；Android 以系统 Storage Access Framework 与 `java.util.zip` 实现，HarmonyOS 以 `DocumentViewPicker` 与 `zlib` 实现。旧 `NativeFileImport` 骨架不再属于正式调用链。

## 尚无决策依据的议题

- 当前是否、何时为具体跨 Repository 规则引入 UseCase/DataSource 层。
- iOS/H5/小程序是否进入产品支持范围。
- Android 为什么同时保留 Picasso 与 Glide。
- 地图供应商、网络库、图片长期加载方案和数据层未来是否改为数据库。

这些议题应在实现相应 Feature 前形成明确 ADR，不能从类名或模板依赖推断。

## ADR-022：Place catalog v2 显式记录来源、可选坐标与可选视觉引用

状态：Accepted。

- `PlaceContract.SCHEMA_VERSION` 从 1 提升为 2；catalog 与其中每个 `Place` 使用相同 schema 版本。
- `Place.source` 是必填枚举，稳定 wire 值为 `seed` / `user`。
- `Place.geoPoint` 是可选对象，只包含 `latitude` / `longitude`；`Place.visualRef` 是可选对象，只包含 `type`（`bundled_asset` / `managed_file`）与 `value`。
- v1 catalog 只在读取时兼容：Place ID 位于当前 `PlaceSeedData` 内置 ID 集合时迁移为 `SEED`，其余迁移为 `USER`；坐标和视觉引用均迁移为 `null`。不得使用 ID 前缀猜测来源。
- 迁移不改 Place ID，也不改 Favorite 的 Place ID 或 Capsule 的 `placeId`。当前 Key 不变；旧备份中的 v1 Place catalog 通过同一 codec 解码，恢复写入时重新编码为 v2。P2-6 后新导出包的外层版本为 v2，但 ZIP 路径布局不变。
- `SEED` 地点由 Repository 拒绝删除；`USER` 地点沿用既有“先确认无 Capsule 关联，再删除并清理 Favorite”流程。
- 本 ADR 只冻结本地数据协议和删除边界，不接入地图 SDK、网络 POI、在线图片或媒体下载。

依据：P2-1 明确需求；当前 Place、Favorite、Capsule 均以稳定 Place ID 建立关系，备份导入也复用各 `StorageKey` codec。

## ADR-023：第一版定位只提供用户主动触发的一次性前台位置

状态：Accepted；双端代码与构建完成，真机权限矩阵待验收。

- 启动和普通地点目录加载不申请权限；只响应 Explore 的主动 Intent，未来打开真实地图时也可复用同一 capability。
- shared 只依赖 `LocationCapability` 与六类 `LocationResult`，平台权限、系统定位开关和 provider/Location Kit 留在 Android/HarmonyOS host。
- 精确位置只保留在当前 Feature Store State，不进入 MMKV、备份或日志；失败结果清空位置，不复用旧距离。
- 距离使用 shared 纯 Haversine 函数，只在当前位置成功且地点存在真实 `GeoPoint` 时展示。定位失败不阻止浏览地点，未来地图 Marker 也不得依赖用户定位成功。
- Store 将 callback 结果送回串行 Event → Mutation → Reducer；请求序号丢弃旧结果，`dispose()` 后禁止回写已销毁 Store。

依据：P2-2 明确需求，以及现有 Explore MVI 的串行事件与生命周期边界。

## ADR-024：第一版拍照委托系统相机并直写受控沙箱文件

状态：Accepted；双端代码与构建完成，真机验收待完成。

- 用户明确要求第一版不自建 CameraX / Camera Kit 预览页，不实现滤镜、裁剪、美颜或自定义取景器。
- shared 只依赖 `CameraCapability` 与显式 Success / Cancelled / Failure / Unsupported 结果；业务页面不接触 Android、ArkTS 或相机对象。
- Android 使用系统 `TakePicture` contract 与私有 `FileProvider`；HarmonyOS 使用系统 `cameraPicker` 与 `PickerProfile.saveUri`。两端都在打开相机前于 `filesDir/images/original` 创建目标文件。
- 取消、启动失败或没有有效内容时立即删除预创建文件；成功只返回该目录内的 `file://` 路径。
- 拍摄路径继续写入既有 `CapsuleDraft.imagePaths`，不修改 Capsule wire/schema；后续移除、丢弃和删除继续走 ADR-014 的全引用保护清理。
- 相机不可用不是编辑器阻断条件：相册与纯文字记录继续可用。

## ADR-022：7A 使用手动排序的本地路线

用户明确要求四字段 Route、手动选点排序、不接在线路径规划且入口位于 Explore。因此采用有界 schema v1 `routes.catalog`、Feature-owned MVI Store 和备份 v3；不创建“漫游”Tab，不预建轨迹与打卡空壳。

## ADR-023：7B 只实现可恢复的本地会话状态机

用户明确把 7B 限定为可选路线以及开始、暂停、继续、结束。因此会话采用单条 `roaming.session` 持久记录和严格状态转换，并进入备份 v4；定位、轨迹与打卡继续留给后续步骤。

## ADR-024：轨迹点进文件，MMKV 只保存索引

7C 要求分片文件存点、MMKV 保存元数据与索引，并推迟后台能力。因此采用双端专用 `CCTrackModule` 限制沙箱写入；前台页面驱动采样，失败只更新轨迹中断状态。

## ADR-025：打卡必须区分 GPS 确认与手动记录

7D 明确禁止伪造 GPS 到达。因此附近判定本身不自动打卡，用户确认后记录 `GPS_CONFIRMED`；定位不可用时只能记录 `MANUAL`。总结从真实源数据实时计算，数据不足时显示不可计算。
