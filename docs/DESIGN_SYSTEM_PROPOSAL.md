# CityCapsule Design System Proposal

> 已确认的视觉职责基线。现有 v2 token 是实现基础；下列内容不代表已完成双端视觉验收。

## Color Tokens

| Token 职责 | 建议 |
| --- | --- |
| Background | 暖白主背景；深色模式使用近黑，不用纯黑大面积压迫 |
| Surface | 白/深灰，仅用于真实内容和浮层 |
| Text Primary/Secondary | 近黑 / 中性灰，保证层级与对比度 |
| Divider | 低对比中性线，替代多余 Card 边框 |
| Accent | 暖琥珀/夕阳橙，仅用于 Primary、选中、位置强调 |
| Success/Warning/Error | 独立语义色；必须配文字/图标，颜色不作唯一信号 |

禁止大面积蓝紫/灰紫；照片颜色是内容主色。无授权照片时只用代码生成类别 fallback。

## Typography

- Display：28–32sp，探索首页问句/城市记忆标题。
- Title Large：22–24sp，页面标题或地点名。
- Title：17–20sp，区块标题与 Card 主标题。
- Body：15–17sp，正文与表单。
- Caption：13–14sp，辅助说明。
- Metadata：11–13sp，日期、距离（有真实数据后）、标签。

字重不超过三档；正文行高约 1.45。必须在 Android/HarmonyOS 大字体下验收，不依赖固定高度截断。

## Spacing

采用 4pt 基线：4 / 8 / 12 / 16 / 20 / 24 / 32 / 40。页面边距手机 20，Card 内边距 16；相关元素 8–12，区块间 24–32。避免用 Card 代替间距。

## Radius

- Hero 图片：20–24；内容 Card：16；按钮：12–14；Chip：胶囊形；列表项：0（靠间距/分隔线）。
- Bottom Sheet 顶角 24；对话框 20。不可所有组件统一大圆角。

## Photography

- 推荐地点 Hero：4:3；地点横向 Card：4:3；紧凑列表缩略图：1:1。地点列表与想去列表必须加载对应地点图片，不以符号代替图片。
- 时间轴照片：首图 4:3，多图使用 1:1 拼贴；相册三列 1:1 Grid，间隙 2–4。
- 文字覆盖图片时使用局部底部渐变，不使用整图重色遮罩。
- 无图或单图加载失败：按地点类别生成抽象 fallback；不使用 `sample.png`，不伪造城市摄影。产品已确认在摄影能力完成前统一使用类别 fallback。当前 Place 尚无图片字段，因此真实图片方案必须先完成来源授权、资产登记、模型/迁移设计与双端加载验收。

## Icon 与 Motion

- 全部通过 `AppIcon` 统一语义入口；不混用 Emoji、PNG、文本符号和多套图标库。
- Motion 仅用于页面切换、想去状态、Bottom Sheet、图片添加/移除；150–250ms，支持减少动态效果。不得以大幅视差或循环动画替代内容。
- 根级“探索 / 记录 / 我的”使用单一 AppShell 的同一底栏；点击直接切换页面，不使用非必要位移动画，根 Pager 暂不接受手指横滑。
- Record 内部“时间轴 / 相册”使用同一页面内的 TabRow + HorizontalPager；Pager 拖动负责左右位移，底栏不参与动画并保持固定。

## 组件使用规则

- Primary Button 每页最多一个；整张地点/碎片 Card 可点，不再附“查看详情”。
- SearchField、SegmentedControl、BottomNavigation、状态组件优先复用现有 API。
- 设置行、筛选行、元数据行使用无 Card 列表；删除等危险动作进入溢出菜单或设置危险区。

## Adaptive 与 Accessibility

- `< 600dp` 使用单栏和 typed route 详情；`>= 600dp` 可使用 `AdaptivePane`。当前主 pane 420dp、pane gap 24dp、整体最大 1200dp。
- Explore 宽屏采用地点列表/地点信息；Record 宽屏采用时间轴/碎片阅读。Map/地点信息沿用同一容器，但只有真实地图能力完成后才可接入正式页面。
- 表单和长文本编辑区最大宽度 640dp；普通单栏内容最大宽度 720dp。
- 交互目标最小 48dp；固定高度仅可作为最小高度。核心按钮、图标按钮、底栏 Tab 必须有角色、名称和选中状态语义。
- 正常正文/图标对背景至少 4.5:1；大字至少 3:1。禁用态不作为可操作信息，颜色不能是状态的唯一表达。
