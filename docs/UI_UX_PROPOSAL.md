# CityCapsule UI/UX 重设计方案

> Phase 2 状态（2026-07-27）：Record Flow 首版已实现，包括地点详情记录 CTA、Capsule Editor、详情、时间轴/相册切换与照片 Grid。其余页面仍以本 Proposal 为后续基线，不得把本次局部实现误写成整套 UI 重构完成。

## 1. 当前 UI 问题报告

### 全局信息架构

当前没有稳定的一级导航。Home 用“浏览地点 / 我的收藏 / 打开本地档案 / 打开设置”大按钮充当功能目录，用户看到的是工程模块而非“今天去哪里、去过哪里”。记录与回忆核心内容尚不存在，导致产品重心被地点 CRUD 和设置取代。

页面之间虽有 typed route，但产品路径断裂：地点详情不能进入记录；Home 没有内容聚合；Profile 不能回看城市档案。路由技术闭环不等于用户闭环。

### Home

- 信息架构：内容是设计系统说明和导航按钮，没有地点、城市照片、最近记忆或个人上下文。
- 视觉层级：Card 包裹技术说明，随后五个同量级按钮，没有主次，也没有内容焦点。
- 交互：像测试控制台；`Replace Settings` 暴露路由动作。
- Demo 痕迹：AppTheme、AppRoute、AppNavigator、共享设计系统均直接出现在正式首页。

### Onboarding

- 真实状态、草稿和校验较完整，是当前最接近产品流程的页面。
- 四步流程把“本地、不上传”的边界解释清楚，但文本偏工程说明，头像/城市的情绪价值不足。
- “使用默认档案并开始”让用户可快速通过，也可能削弱档案建立；建议保留跳过能力但改成次级动作，并在 Profile 补全。

### Explore / Place List / Favorites

- 搜索和过滤能力真实，但 UI 把筛选说明、输入框、多个 chip、开关和 CRUD 操作同时堆在首屏，像数据管理工具。
- 地点 Card 主要呈现文本与操作，不以城市摄影和地点名称为焦点；详情入口/收藏行为过于按钮化。
- Favorites 只是同一列表的模式切换，仍使用“收藏”语言，缺少“未来想去”的动机表达和收藏时间信息。

### Place Detail

- 信息层级从技术说明开始，地点内容只是字段列表；没有 Hero、街区氛围、地图摘要或与个人记忆的关系。
- 收藏、编辑、删除、刷新、返回等操作同屏竞争。删除作为高风险管理动作过于显眼。
- 最重要的产品动作“在这里留下城市碎片”不存在，探索无法转入记录。

### Place Editor

状态保存和未保存确认合理，但表单视觉是纯 CRUD。它应是管理自建地点的辅助流程，不应决定整个产品的视觉语言。分类、标签和地址可更紧凑，保存作为唯一 Primary CTA。

### Timeline / Gallery / Capsule

尚无页面，不能评价现有视觉。产品最核心的“记录/回忆”因此完全缺席，也是当前完成度最大问题。

### Profile / My

当前是“本地档案”的编辑/清除页，功能真实但定位像设置子页。缺少城市碎片数、去过地点、想去、城市足迹和内容预览。危险的“清除档案”不应与主要身份内容同级。

### Settings

主题切换及失败回滚真实可用，但页面混入 MMKV 说明、路由验收、Push Settings 和 BackTo；正式产品不能出现这些内容。数据/隐私/关于尚未实现，设置内容不完整。

### Design System

优点：已经有语义色、Typography、Spacing/Radius/Motion、主题运行时和硬编码 guard。

问题：蓝紫/灰紫主视觉与城市摄影/暖记忆定位不符；缺 Icon、Shadow/Elevation、照片规格、PlaceCard/CapsuleCard/AdaptivePane；`AppCard/AppSection/AppButton` 被大量复用，形成 Everything is a Card 和大按钮菜单；Typography 的 30sp/36sp 粗标题容易在内容稀少页面显得像 Demo 展板。

## 2. 新信息架构

### 一级导航

- 探索：回答“今天可以去哪里？”
- 记录：回答“我去过哪里、留下了什么？”
- 我的：回答“我的城市档案是什么样？”

地图是探索视图，Timeline/Gallery 是记录视图。设置、数据管理和编辑页不是一级导航。

### 页面地图

```text
App
├─ 启动
│  ├─ Launch Gate
│  └─ Onboarding
├─ 探索
│  ├─ Home
│  ├─ Search / Filter
│  ├─ Place List
│  ├─ Map Explore
│  └─ Place Detail
│     ├─ External Navigation
│     └─ Capsule Editor(placeId)
├─ 记录
│  ├─ Timeline
│  ├─ Gallery
│  ├─ Capsule Detail
│  │  └─ Place Detail
│  └─ Capsule Editor
└─ 我的
   ├─ Profile Overview
   ├─ Profile Edit
   ├─ Want To Go
   ├─ City Archive / Statistics
   └─ Data & Settings
      ├─ Theme / Privacy / About
      └─ Import / Export / Reset
```

当前尚未实现的页面只出现在方案中，Phase 2 不得先建空壳入口。

## 3. 核心用户 Flow

### 发现

Home 当前城市/问候 → 今日推荐 Hero（本地规则）→ Place Detail → 想去状态反馈 → 返回后卡片即时更新。

首页推荐明确使用本地 seed + 距离（有权限时）+ 类别多样性 + 未去过/未想去等简单规则。无定位时按当前城市、seed 稳定顺序和类别轮换；不出现“AI 为你推荐”。

### 探索

Explore 搜索/分类 → 列表/地图切换 → Marker/PlaceCard → Place Detail → 外部导航。地图不可用或权限拒绝时保持列表可用并隐藏距离，不阻断详情。

### 记录

Place Detail → “在这里留下城市碎片” → Capsule Editor → 拍照/选图或纯文字 → 心情/标签/文字 → 保存到城市记忆 → Capsule Detail/Timeline。权限拒绝、复制失败和未保存退出都有明确恢复路径。

### 回忆

记录 Tab → Timeline（默认）/Gallery → Capsule Detail → 关联 Place Detail。编辑/删除放详情菜单；Timeline 首层优先照片、日期、地点和记忆文本。

## 4. Design System Proposal

### Color

- Canvas：Warm White `#F8F6F1`，Dark `#12110F`。
- Surface：`#FFFFFF` / `#1B1916`；Subtle Surface `#F0EDE6`。
- Text Primary：`#1D1B18`；Secondary：`#6F6A62`；Divider：`#E3DED4`。
- Accent Amber：建议 `#C97824`（浅色）与更亮的暗色等价色；只用于主 CTA、选中、位置和关键反馈。
- Success/Warning/Error 保持语义独立；不能用 Accent 代替所有状态。
- 城市照片承担主要彩度，UI 不大面积铺琥珀色。

具体色值应在实现前用 Android/HarmonyOS 真机和深色对比度验证；以上是提案，不是已冻结 token。

### Typography

- Display：32/38，Semibold，用于 Home 一次性问候或城市名。
- Title Large：26/32，Semibold，页面标题。
- Title：20/26，Semibold，内容组标题。
- Body：16/24，Regular，主要阅读。
- Caption：14/20，Regular，辅助说明。
- Metadata：12/16，Medium，距离/类别/日期。

避免整页 Bold；地点名和记忆正文比技术说明更突出。中英文均优先系统字体以保证跨端一致和可访问性。

### Spacing / Radius / Elevation

- 4pt 基础网格：4/8/12/16/24/32/40。
- 手机水平边距 20；内容块间 24–32；卡片内部 12–16。
- Hero 16–20 圆角；内容 Card 12–16；Button 10–12；Chip pill；普通列表不包 Card。
- 仅浮层、Hero/照片 Card、Bottom Sheet 使用低层级阴影；普通分组依靠留白和 divider。

### Components

优先补齐：AppTopBar、BottomNavigation、Primary/SecondaryButton、IconButton、PlaceCard（compact/hero）、CapsuleCard（timeline/gallery）、TagChip、SearchField、SegmentedControl、Empty/Error/Loading、ConfirmDialog、BottomSheet、PhotoGrid、AdaptivePane。

危险操作使用菜单 → 确认；整张 Place/Capsule Card 可点击，不再放“查看详情”按钮。

### Icon

建立一个跨端统一封装与同一视觉家族。基础集合：Home/Explore、Timeline、Profile、Search、Map、Location、Heart、Camera、Gallery、More、Back、Share、Navigation、Add。Emoji 和文本符号不作为正式图标混用。

### Photography

- Hero：16:10 或 4:3，展示街道/建筑/公园/咖啡/夜景等城市环境。
- Place Card：4:3；Capsule timeline 可用 3:2/方形组合；Gallery 1:1。
- 使用真实本地 seed 授权照片或明确占位；加载时使用中性 skeleton/fade-in，失败时用克制的类别图形，不伪造照片。
- 照片上文字只保留必要的标题/渐变保护，避免模板式贴纸、胶带、邮戳堆叠。

### Motion

120ms 状态反馈、220ms 页面/卡片过渡、300ms 强调完成反馈。使用照片 fade-in、想去微反馈、Card→Detail、Bottom Sheet 和发布成功；尊重减少动态效果，不加入技术展示型动画。

## 5. 页面级重构方案

| 页面 | 页面目标 | 核心内容与层级 | Primary CTA | 次级操作 |
| --- | --- | --- | --- | --- |
| Home | 同时回答“去哪”和“最近留下什么” | 城市/天气占位与头像 → 问候/搜索 → 今日 Hero → 附近/分类 → 最近 1–3 条记忆 | 快速记录（有地点上下文时带入） | 想去、查看全部、切换城市 |
| Explore | 让地点成为内容 | 搜索 → 类别 chips → 附近横向卡 → 值得绕路 Hero → 列表/地图切换 | 无固定大按钮；点击内容就是主行为 | 筛选、想去、地图 |
| Place Detail | 地点信息 → 探索行为 → 个人记录 | Hero → 名称/街区/标签/想去 → 关于 → 地图/距离/导航 → 我的记忆 | 在这里留下城市碎片 | 想去、导航、编辑自建地点；删除进菜单 |
| Capsule Editor | 轻量城市日记 | 顶栏关闭/完成 → 照片 → “写下这一刻” → 心情 → 地点 → 标签 | 保存到城市记忆 | 草稿、移除照片、关闭；无后台字段表格感 |
| Timeline | 回忆去过哪里 | 月份 → 日期/地点 → 照片 → 摘录，按时间倒序 | 空状态时“留下第一条城市碎片” | Timeline/Gallery 切换、筛选 |
| Gallery | 从照片进入记忆 | 年月分组的真实照片 Grid | 点击照片进入 Capsule Detail | 年月筛选、Timeline 切换 |
| Profile | 展示个人城市档案 | 头像/昵称/城市 → 碎片/去过/想去真实统计 → 城市足迹 → 想去内容 → 数据与设置 | 无常驻大 CTA；内容点击进入对应页 | 编辑资料、数据与设置 |

Home 的天气只有真实 API/缓存实现后才展示；当前可以展示当前城市，不使用假天气。距离同理，无定位时隐藏或使用“当前城市”信息，不能写虚假公里数。

## 6. 状态与大屏

所有核心页至少设计 Loading、Empty、Content、Error/Degraded。错误要保留可用内容：地点持久化失败时可显示只读 seed；定位拒绝时用列表；照片失败时允许纯文字；网络未来失败时使用本地内容。

- Compact：单栏 + Bottom Navigation，详情 push。
- Medium/Expanded：Explore 为地点列表 | 详情，地图为地图 | 地点信息，记录为 Timeline | Capsule Detail。
- Editor 在大屏仍保持可读最大宽度；未来明信片才使用画布/属性双栏。

## 7. UI 重构开发顺序

### P0：Record Flow first

1. 设计 token/Icon/内容组件最小升级。
2. 冻结 Capsule/媒体数据与 capability。
3. Capsule Editor → Timeline → Capsule Detail → Gallery。
4. Place Detail 接入“留下城市碎片”。
5. Home 使用真实地点 + 最近记忆重构。

这会先完成“记录/回忆”产品心脏，而不是同时重写所有静态页面。

### P1：Explore and My

1. Explore/PlaceCard/PlaceDetail 视觉与交互。
2. 想去语言与内容页。
3. Profile 聚合为“我的城市档案”。
4. 地图/定位/外部导航和大屏双栏。

### P2：Data completeness and future

备份/数据设置、可访问性与性能收尾；基础版闭环稳定后，再进入路线/漫游等进阶版。没有真实实现前不建立 AI、漫游、实况等入口。

## 8. Phase 1 停止点

本方案只用于评审。确认 IA、四条 Flow、视觉方向、页面结构和 P0 顺序之前，不修改 Home/Explore 等业务 UI，也不开始大规模重构。
