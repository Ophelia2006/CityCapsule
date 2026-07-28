# CityCapsule 待办

优先级同时考虑产品闭环、数据安全和架构阻塞。标签说明来源：`[Initial Plan]`、`[Code Scan]`、`[UI/UX Redesign]`。Record Flow 的 Phase 2 首版已实现；其他 Feature 仍按用户确认的范围逐项推进。

## P0：验收 Record Flow 并建立正式产品壳

- `[Code Scan]` 明确 `h5App`、`miniApp` 是否属于目标；当前空 project include 不阻断 Gradle 8.7 测试，但不能作为已支持平台宣传。
- `[UI/UX Redesign]` 先重构共享设计基础：暖白/中性灰/暖琥珀 token、补齐 elevation/shadow、统一 Icon 封装、摄影占位与加载/空/错误状态；保留深色和现有主题持久化协议。
- `[UI/UX Redesign]` 建立手机“探索 / 记录 / 我的”应用壳和正式导航；未实现页不得成为空壳 Tab。
- `[Initial Plan][UI/UX Redesign]` 重新设计 Home，以本地规则展示今日地点、附近/分类内容、真实最近城市记忆和一个快速记录 CTA；空数据使用诚实 empty state，不伪造推荐算法。
- `[Code Scan]` 在已安装的 2026-07-28 HAP 上完成 HarmonyOS 真机选图、取消、复制失败和重启后图片可读验收；`CCMediaModule` 注册、异常降级、native 链接和 HAP 编译已完成。另需让标准 `hvigorw` 在无既有项目缓存时也能稳定完成依赖安装，避免当前只能复用 DevEco 项目级 Hvigor 缓存。
- `[Initial Plan]` 补相机 capability（如本阶段确认需要）、缩略图生成和媒体引用清理；当前删除/移除图片会留下孤立原图。
- `[Code Scan]` 为删除地点与已有 Capsule 的关系形成产品/数据决策；当前保留 Capsule 并降级显示缺失地点。
- `[Code Scan]` 把 UTC 日期标签替换为有明确跨端协议的本地化日期格式器，补跨午夜测试。
- `[UI/UX Redesign]` 对 Record Flow 做 Android/HarmonyOS 真机验收与视觉走查，修复系统相册、图片渲染、返回栈和大字体问题。

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
