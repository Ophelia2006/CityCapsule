# CityCapsule 待办

优先级同时考虑产品闭环、数据安全和架构阻塞。标签说明来源：`[Initial Plan]`、`[Code Scan]`、`[UI/UX Redesign]`。Record Flow 的 Phase 2 首版已实现；其他 Feature 仍按用户确认的范围逐项推进。

## P0：验收 Record Flow 并建立正式产品壳

- `[Code Scan]` 明确 `h5App`、`miniApp` 是否属于目标；当前空 project include 不阻断 Gradle 8.7 测试，但不能作为已支持平台宣传。
- `[UI/UX Redesign][DONE 2026-07-28]` Design System v2 最小基础：暖白/中性灰/暖琥珀 token、配套深色、elevation 语义、统一 Icon 入口、类别 fallback、加载/空/错误状态及核心 Flow 组件 API；主题持久化协议未改变。
- `[UI/UX Redesign]` 在 Android/HarmonyOS 设备走查新 Light/Dark 调色、系统栏、AppIcon 字形与 elevation/shadow；若字形不一致，保持 `AppIcon` API 并替换为跨端稳定的代码矢量实现。
- `[Code Scan]` 新增真实地点摄影前，逐项完成 `docs/ASSET_ATTRIBUTION.md` 的来源、许可证、用途和署名登记；未登记素材不得进入正式 UI。
- `[UI/UX Redesign]` 建立手机“探索 / 记录 / 我的”应用壳和正式导航；未实现页不得成为空壳 Tab。
- `[Initial Plan][UI/UX Redesign]` 重新设计 Home，以本地规则展示今日地点、附近/分类内容、真实最近城市记忆和一个快速记录 CTA；空数据使用诚实 empty state，不伪造推荐算法。
- `[Code Scan][BLOCKED]` 按 `docs/P0_RECORD_FLOW_ACCEPTANCE.md` 完成 Android/HarmonyOS 真机的选图成功、取消、复制失败、重启读取和引用清理；Android 模拟器已完成真实选图回传和沙箱复制，Android 真机未连接；HarmonyOS 的模块未注册崩溃与媒体 URI 直接按路径复制问题均已修复，Hypium 测试及 signed HAP 构建通过，仍须在 HED-AL00 覆盖安装后完成真机复验。
- `[Initial Plan]` 补相机 capability（如本阶段确认需要）和缩略图生成；正常删除/移除/丢弃的媒体引用清理已经完成，后续可评估崩溃遗留文件扫描。
- `[UI/UX Redesign]` 真机可用后继续 Record Flow 视觉走查，检查图片渲染、返回栈和大字体问题。

## P1：补齐基础版探索与本地数据完整性

- `[UI/UX Redesign]` 重构 Explore/PlaceCard/PlaceDetail：摄影内容、分类、搜索、整卡点击、“想去”、位置摘要和“在这里留下城市碎片”主 CTA。
- `[Initial Plan]` 决定 Place source/seed 不可删除规则并迁移现有 catalog；避免无依据直接改 wire schema。
- `[Initial Plan]` 实现 Location capability、坐标字段迁移、距离和权限降级。
- `[Initial Plan][UI/UX Redesign]` 接入双端真实地图 Native View、Marker、列表/地图切换和外部导航；地图不可用时降级为列表。
- `[Code Scan]` 如需“按想去时间排序”，新增有迁移方案的数据模型，不能从现有 Set 伪造加入时间；用户文案已经统一为“想去”，底层继续保留 `Favorite*`。
- `[UI/UX Redesign]` 将 Profile 重构为“我的城市档案”：头像/昵称/城市、真实统计、城市足迹、想去内容、数据与设置入口；编辑与危险操作下沉。
- `[Initial Plan]` 完成设置中的缓存清理、存储占用、隐私/关于与二次确认重置。
- `[Initial Plan]` 实现备份导出、导入验证/预览、导入前备份和媒体恢复；补齐 Android launcher 与 Harmony 文件选择器。
- `[Code Scan]` 实现 RouteResult/requestId 结果通道；原生取消/失败/不支持必须回传，不得仅展示参数。
- `[Code Scan]` 清理或隔离 `KRBridgeModule/KRMyModule/KRMyView` 模板 TODO/null 分支；诊断页只在开发入口可达。
- `[Code Scan]` 统一 Android 图片加载栈，确认 Glide/Picasso 保留其一或说明不同职责。
- `[UI/UX Redesign]` 增加平板列表/详情、地图/信息、时间轴/详情双栏 `AdaptivePane`，验证窗口变化不丢状态。
- `[Code Scan]` 做可访问性检查：大字体、语义、触控目标、颜色非唯一提示、减少动效。

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
