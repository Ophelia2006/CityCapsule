# Initial Planning Baseline

## 来源与用途

本基线来自用户确认的三份初始文件：通用架构与 AI Coding 规范、文档包 README、三阶段开发文档合并版。它描述“项目最开始准备去哪里”，不描述当前完成度；当前事实见 `ARCHITECTURE.md` 与 `CURRENT_STATE.md`。

## 初始产品规划

CityCapsule 被定义为个人城市探索与创作工具，不建设多用户社区、聊天、云同步或全局排行榜。

- 基础版 Local Journal：本地档案、首页、预置/自建地点、收藏、地图、地点详情、城市碎片、相机/相册、时间轴/相册、设置、导入导出。
- 进阶版 Offline Explorer：路线、漫游会话、后台轨迹、定位/扫码打卡、成就、天气/地理编码/路线 API、离线资料、通知和桌面组件。
- 复杂版 Creative Companion：AI 场景/文案、本地模板降级、游记、明信片、轨迹动画、本地全文搜索、高级统计、实况状态、接续、加密备份、路线分享和大屏适配。

三个阶段递进且保留旧功能与数据；每个阶段结束时都应成为可独立运行、核心链路闭环的产品。

## 初始核心流程

基础版计划流程：首次启动 → 本地档案 → 首页地点 → 地图/详情 → 收藏 → 拍照/选图 → 创建城市碎片 → 时间轴 → 导出备份。

进阶版计划流程：地点 → 路线 → 离线准备 → 漫游 → 轨迹 → 定位/扫码打卡 → 总结/成就 → 城市碎片。

复杂版计划流程：漫游结果 → 照片/轨迹 → AI 或本地模板游记 → 明信片 → 导出/分享 → 实况与接续。

## 初始目标架构

```text
Kuikly Page
  → PageStore / StateHolder
  → UseCase
  → Repository
  → LocalDataSource / RemoteDataSource
  → MMKV + 文件系统 / 第三方 API
  → Platform Capability / Kuikly Module / Native View
```

初始约束包括：commonMain 不依赖平台 API；业务 UI 不直接访问存储、HTTP 或 HMRouter；平台能力通过接口隔离；不自建后端；无网络/拒绝权限/API 不可用时必须降级。

## 初始技术规划

- Kuikly Compose DSL + Kotlin Multiplatform。
- Android 与 HarmonyOS 为主要运行平台；HarmonyOS 使用 HMRouter，Android 提供等价 dispatcher。
- 双端 MMKV 存结构化数据；媒体、导出包和轨迹分片存应用沙箱。
- Android 可使用 OkHttp、地图 SDK、Photo Picker/CameraX、Fused Location、Keystore；HarmonyOS 可使用 NetStack、Map/Camera/Location Kit、PhotoAccessHelper、安全存储。以上属于可选实现方向，不等于当前依赖。
- Native View 用于地图/相机等复杂原生画布，业务控制区仍用 Kuikly。
- 第三方 API 仅通过 RemoteDataSource；用户配置 Key，Key 进入安全存储。

## 初始数据规划

基础规划中的 `Place` 包含来源、坐标、封面和时间字段；`CityCapsule` 包含文字、心情、地点、坐标、最多 9 张图片、草稿/发布状态。初始规范倾向每个实体独立 Key + 索引，禁止无限增长的单 JSON 数组，并规划 schema migration 和固定文件目录。

## 与当前实现的主要差异

| 主题 | 原计划 | 当前代码 | 可确认原因 |
| --- | --- | --- | --- |
| 分层 | Store → UseCase → Repository → DataSource | Page → StateHolder/Repository → KeyValueStore；无 UseCase/DataSource 层 | 未发现设计原因记录 |
| 地点存储 | 实体独立 Key + 索引 | `places.catalog` 单 JSON，硬上限 500 | 当前专题文档明确选择“有上限的离线目录”；为何改变存储形态未记录 |
| Place 字段 | source、坐标、封面等 | 无 source、坐标、封面；有 city/district/tags/note | `Place.kt` 注释明确当前协议刻意排除平台 URI、地图对象、坐标和远端 ID；更改动机未完整记录 |
| 本地档案 | id、nickname、avatarPath、city、时间戳 | displayName、预设头像、可选 homeCity/bio；无账号/文件头像 | 当前阶段文档明确本地单用户且不申请相册权限 |
| 地图/定位/媒体 | 基础版真实实现 | 仅路由协议或完全不存在 | 尚未开发 |
| 城市碎片/时间轴 | 基础版核心闭环 | 只有 typed route 协议，无页面/模型/Repository | 尚未开发 |
| 导入导出 | 基础版 ZIP 闭环 | Harmony 文件导入只有骨架，Android launcher 未注册 | 尚未开发 |
| 平台范围 | Android + HarmonyOS | 两端有实质宿主；另有 iOS/JS 模板 | iOS/JS 来自工程模板，未发现产品支持决策 |

不能从现有资料确认差异原因的条目只保留事实，不作推断。
