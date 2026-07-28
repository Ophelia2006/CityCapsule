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

初始规范明确图片、视频、导出包、缩略图和大量轨迹点保存应用沙箱，MMKV 只保存路径与元数据。当前 Android 与 HarmonyOS Photo Picker 都把选中原图复制到应用沙箱，`CityCapsule` 仅持久化 `file://` 路径；尚无缩略图、视频、导出包、轨迹与孤立文件清理。

## ADR-012：当前城市碎片使用有上限 catalog 与单草稿 Key

状态：Accepted for current milestone。

当前 `CapsuleContract` 把 catalog 上限固定为 500、单条照片上限固定为 9；已发布内容写入 `cc_preferences/capsules.catalog`，可恢复草稿写入 `cc_cache/capsules.draft`。这与现有 Place/Favorite 的有界 MMKV 架构一致，并保持草稿可清理。为何没有采用初始规划中的实体独立 Key，仓库没有进一步理由记录。

## ADR-013：时间轴日期当前使用确定性 UTC 标签

状态：Accepted as temporary implementation。

`CapsuleDate.kt` 的代码注释明确：commonMain 当前没有时区数据库，为避免各平台 wire 行为不一致，直接从 epoch 计算 UTC 公历日期。它不是最终的本地化日期方案；接入平台日期格式能力前，不得宣称按用户本地时区展示。

## 尚无决策依据的议题

- 为什么 Place 删除 source/坐标/封面、为何所有 seed 地点允许删除。
- 为什么当前没有 UseCase/DataSource 层。
- iOS/H5/小程序是否进入产品支持范围。
- Android 为什么同时保留 Picasso 与 Glide。
- 地图供应商、网络库、图片长期加载方案和数据层未来是否改为数据库。

这些议题应在实现相应 Feature 前形成明确 ADR，不能从类名或模板依赖推断。
