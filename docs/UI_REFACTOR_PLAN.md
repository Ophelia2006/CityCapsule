# CityCapsule UI Refactor Plan（Proposal）

> 当前门禁：P0-3A 单一 AppShell 已批准并完成代码/自动化验证；Record Container 点击切换已迁入壳，内部 Pager 仍待 P0-3B。其余页面仍需一次只批准并验收一个 Feature，不得据此自动开始大规模 UI 修改。

## 1. 允许与禁止范围

当前文档同步阶段只修改 Proposal 与长期项目文档，不修改业务代码。进入实现阶段后仍需一次只验收一个 Feature；路由协议、存储 schema、业务模型或平台能力的变化必须单独说明迁移与双端验证。

## 2. 页面依赖

```text
Design tokens + App shell
  ├─ Explore Home → PlaceCard → Place Detail → Favorite
  │                                   └─ Capsule Editor
  ├─ Record root → TabRow + HorizontalPager
  │                    ├─ Timeline view
  │                    └─ Gallery view
  │                               └─ Capsule Detail
  └─ My shell → City Profile → Favorites/Settings

Map/Location/External navigation capability
  └─ Map Explore（未完成前不进入正式壳）
```

## 3. 建议开发顺序

### P0：先形成真实闭环

1. **应用壳 P0-3A（代码与自动化完成，待设备验收）**：一个 AppShell/底栏，三个常驻根内容；点击 Tab 驱动无手势根 Pager，重复点击 no-op，诊断入口不入壳。
2. **Record Container P0-3B（部分完成）**：Timeline/Gallery 已是同一 Record 根内容的点击切换视图并保留底栏/状态；下一步补内部 HorizontalPager、左右滑动和同轴手势验收；详情仍走 typed route。
3. **探索首页**：基于 seed 与用户自建地点混合 catalog，以及真实 Capsule/Favorite 本地数据和诚实空态；重点地点整卡是 Primary，本地规则必须可解释，不称 AI。
4. **地点图片数据前置**：摄影能力完成前统一类别 fallback；未来真实图片必须先确认来源与授权、登记资产，并为 Place 图片关系制定双端兼容的 schema/迁移方案。禁止直接硬编码路径或假图。
5. **地点列表与 PlaceCard**：收敛筛选/新建权重，整卡点击；摄影能力完成前使用类别 fallback。
6. **地点详情**：唯一 Primary 为记录；想去轻量化；管理动作进入溢出。
7. **Record Flow 视觉整理**：编辑器、Record Container、详情只重排，不改变草稿/媒体清理/发布语义。
8. **我的城市档案**：聚合现有档案，并展示可精确计算的碎片数、关联地点数、想去数；不新增里程、轨迹或虚构足迹。

### P1：能力完成后开放

- Location、坐标迁移、真实地图 Native View、marker、权限降级、外部导航结果通道。
- 数据管理/导入导出完成端到端验证后再进入设置。
- 平板列表/详情、地图/信息、时间轴/详情双栏。

### P2：双端品质

- 字形、阴影、系统栏、大字体、触控目标、减少动效、深浅色与窗口变化验收。
- 摄影资产只有完成 `ASSET_ATTRIBUTION.md` 登记后进入正式 UI。

## 4. 每个 Feature 的验收模板

1. 明确输入、输出、允许/禁止修改范围。
2. 列出 Loading/Empty/Content/Error/Permission/Offline/Partial Data。
3. 核对 Android/HarmonyOS 平台能力与失败、取消、不支持。
4. 运行 shared 与 Android 单测；环境允许时运行 HarmonyOS 测试/构建。
5. 双端手工检查浅色/深色、系统栏、大字体、返回、状态恢复。
6. 更新 CURRENT_STATE/TODO；未验收项保持 Partial/Blocked。

## 5. 已确认产品决定（2026-07-29）

1. 根级“探索 / 记录 / 我的”使用已实现的单一 AppShell；底栏点击 `animateScrollToPage`，根手势保持关闭。
2. Timeline/Gallery 已重构为 Record 容器内部状态视图；内部 Pager/左右滑动作为 P0-3B 单独验收。
3. 探索首页 Primary 为重点地点整卡；seed 与用户自建地点允许混合参与本地排序。
4. “我的城市档案”展示碎片数、关联地点数、想去数三项可精确计算统计。
5. 地点摄影能力完成前统一使用类别 fallback。

## 6. 仍待决定

1. 首页是否展示一条真实“最近城市记忆”（建议展示一条）。
2. 地图与导航 capability 完成前是否完全隐藏（建议继续完全隐藏）。
3. 未来真实地点图片采用“每个 Place 一个封面路径/资产 ID”，还是独立图片关系；该选择涉及 catalog schema 与双端迁移。

后续仍按单 Feature 授权进入实现，不因本轮文档确认自动修改业务代码。
