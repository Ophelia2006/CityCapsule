# CityCapsule Low-Fidelity Wireframes（Proposal）

> 手机单栏草图，产品方向已确认；具体视觉仍需逐 Feature 验收。`[规划]` 表示当前没有真实能力，不能在实现前开放。

## 1. 探索首页

```text
┌──────────────────────────┐
│ 上海                    ◯ │
│ 下午好                     │
│ 今天想去哪里？              │
│ [ 搜索地点、分类或区域     ] │
│                            │
│ 今日值得看看                │
│ ┌──────────────────────┐  │
│ │   类别图形 fallback   │  │
│ │ 静安雕塑公园     想去 ♡ │  │
│ │ 公园 · 静安区          │  │
│ └──────────────────────┘  │
│ 附近分类*  公园 展馆 街区    │
│ 全部地点               查看 ›│
│                            │
│ 最近的城市记忆       查看记录 │
│ 7月28日 · 静安雕塑公园       │
│ 探索        记录        我的 │
└──────────────────────────┘
*当前无定位，首版文案应改为“按分类探索”
```

首屏先看问句、搜索与一个真实地点；Primary 是打开重点地点（整卡）。该地点允许从 seed 与用户自建地点混合 catalog 中按可解释本地规则产生，不宣称 AI 或个性化算法。分类与最近记忆在其后，设置不出现。**搜索框、分类项和“查看全部地点”都是探索列表入口**：分别带搜索焦点、分类筛选和无筛选状态。想去用图标；摄影能力完成前统一用类别 fallback。页面背景、标题、分类行不使用 Card。

组件树：`ExploreHomeScreen[页面] → AppScaffold[通用] → HomeTopBar[Feature] → SearchField[通用] → FeaturedPlaceCard[数据/通用变体] → CategoryRow[Feature] → CapsulePreview[数据] → AppBottomNavigation[通用]`。

## 2. 探索列表

```text
┌──────────────────────────┐
│ ‹ 地点                 ⋯  │
│ [ 列表 ]   地图*            │
│ [ 搜索地点             ]  │
│ [全部] 公园 展馆 咖啡 街区   │
│ 12 个地点          筛选     │
│ ──────────────────────── │
│ [地点图] 静安雕塑公园  ♡    │
│   公园 · 静安区             │
│ ──────────────────────── │
│ [地点图] 西岸美术馆    ♥    │
│   展馆 · 徐汇区             │
│ ──────────────────────── │
│ [地点图] 武康路             │
│   街区 · 徐汇区             │
└──────────────────────────┘
```

Primary 是打开地点行；搜索和类别辅助缩小范围。地点行必须加载该地点对应的真实图片，不使用 `▧` 等符号冒充缩略图；图片缺失、解码失败或尚未配置时才显示代码生成的类别 fallback。**地图探索的唯一产品入口是这里的“列表 / 地图”同级视图切换**，不放入底部导航，也不在探索首页另设地图按钮；当前能力未实现时隐藏“地图”。该切换只更新探索容器内的视图状态，不执行页面 push、replace、back。新建地点进入 `⋯`，筛选用 Bottom Sheet。列表项不套浮起 Card，以分隔线维持扫描效率。

组件树：`PlaceListScreen[页面] → AppScaffold → AppTopBar → SearchField → CategoryChips → ResultHeader → PlaceList → CompactPlaceCard[数据/通用] → FilterBottomSheet[Feature]`。

## 3. 地图探索（规划）

```text
┌──────────────────────────┐
│ ‹ 地图探索          [列表] │
│ [ 搜索此区域            ] │
│                            │
│       ·     ●              │
│   [真实 Native Map]   ◎    │
│          ●        ·         │
│                            │
│ ┌──────────────────────┐  │
│ │ 静安雕塑公园           │  │
│ │ 公园 · 静安区          │  │
│ └──────────────────────┘  │
└──────────────────────────┘
```

本页由探索容器顶部的“地图”状态切换显示；顶部“列表”切换恢复列表视图并保留搜索和筛选。列表/地图属于同一探索容器，不产生新的导航栈记录。视觉焦点是地图 marker，Primary 是打开选中地点摘要。定位 `◎` 为图标；权限说明用 Bottom Sheet。无 SDK/权限/离线地图时直接降级列表，不显示假底图。只有选中地点摘要使用 Card。

组件树：`MapExploreScreen[页面/规划] → MapTopBar → NativeMapViewport[平台] → MarkerLayer[数据] → SelectedPlaceCard[数据] → LocationPermissionSheet[通用状态]`。

## 4. 地点详情

```text
┌──────────────────────────┐
│ ‹ 地点详情          ♡  ⋯  │
│ ┌──────────────────────┐  │
│ │  摄影 / 类别 fallback │  │
│ └──────────────────────┘  │
│ 静安雕塑公园               │
│ 公园 · 上海 · 静安区        │
│ 地址                       │
│ 北京西路 500 号             │
│ #散步  #雕塑                │
│                            │
│ 我的城市记忆         2 条 › │
│                            │
│ [ 在这里留下城市碎片       ] │
└──────────────────────────┘
```

先识别地点，再阅读位置，最后看到关联记忆与唯一 Primary。想去是顶栏图标；编辑/删除在 `⋯`。地址与标签不用 Card，关联记忆整行可点。无合法照片时使用 fallback，不伪造 Hero 摄影。

组件树：`PlaceDetailScreen[页面] → AppScaffold → DetailTopBar → PlaceHero[数据] → PlaceIdentity → LocationSummary → TagRow → RelatedCapsuleRow[数据] → PrimaryRecordButton → OverflowMenu[通用]`。

## 5. 城市碎片编辑

```text
┌──────────────────────────┐
│ × 这一刻        保存草稿   │
│ 静安雕塑公园               │
│                            │
│ [ + 添加照片 ][图][图]      │
│ 说说这一刻…                 │
│ ┌──────────────────────┐  │
│ │ 风吹过树影，城市慢下来。 │  │
│ └──────────────────────┘  │
│ 心情  [平静] 开心 惊喜       │
│ 标签  #散步 #傍晚   +        │
│                            │
│ [ 保存到城市记忆          ] │
└──────────────────────────┘
```

首屏优先照片与正文，地点作为上下文。唯一 Primary 是发布；草稿是文字动作，关闭触发未保存确认。照片可滚动；心情/标签随后。表单分组靠留白，不把每组套 Card；Picker 状态用 Bottom Sheet/行内反馈。

组件树：`CapsuleEditorScreen[页面] → EditorTopBar → PlaceContext[只展示] → EditablePhotoGrid[数据] → BodyTextField[数据] → MoodSelector[Feature] → TagEditor[Feature] → PublishButton → DiscardDialog[通用]`。

## 6. 记录时间轴

```text
┌──────────────────────────┐
│ 我的城市记忆               │
│ [ 时间轴 ]   相册           │
│                            │
│ 7月 28日                    │
│ ● 静安雕塑公园              │
│   [照片] 风吹过树影…         │
│   平静 · #散步              │
│ │                          │
│ 7月 20日                    │
│ ● 武康路                    │
│   沿街慢慢走到天黑…          │
│                            │
│ 探索        记录        我的 │
└──────────────────────────┘
```

日期与地点形成阅读骨架，Primary 是打开碎片整卡。Timeline 是 Record 根容器的第 0 页；切换相册使用 TabRow + HorizontalPager，可点击也可左右滑动，只改变 PagerState，不执行页面出入栈。一级底栏由 Record 根容器持有并持续显示。时间线轴、日期标题不用 Card；碎片内容可使用低 elevation CapsuleCard。更早内容自然滚动。

组件树：`TimelineScreen[页面] → AppScaffold → RecordHeader → ViewSegmentedControl[通用] → TimelineList → DateHeader → TimelineRail → CapsuleCard[数据/通用] → AppBottomNavigation`。

## 7. 城市相册

```text
┌──────────────────────────┐
│ 我的城市记忆               │
│   时间轴   [ 相册 ]         │
│                            │
│ [图1][图2][图3]             │
│ [图4][图5][图6]             │
│ [图7][图8][图9]             │
│                            │
│ 图片按城市碎片归属，点开回忆  │
│                            │
│ 探索        记录        我的 │
└──────────────────────────┘
```

照片是全部视觉焦点，Primary 是点击照片进入所属碎片。Gallery 是同一 Record 根容器的第 1 页，不再作为最终产品中的独立二级页面；时间轴/相册切换不产生导航栈记录，一级底栏持续显示。网格继续滚动；不在每张图叠加按钮或 Card。加载失败单格显示 fallback，整体 catalog 仍可浏览。

组件树：`GalleryScreen[页面] → AppScaffold → RecordHeader → ViewSegmentedControl → PhotoGrid[数据/通用] → AppBottomNavigation`。

## 8. 城市碎片详情

```text
┌──────────────────────────┐
│ ‹ 城市碎片             ⋯  │
│ [       主照片 4:3       ] │
│ [小图] [小图]              │
│ 2026年7月28日 · 平静        │
│                            │
│ 风吹过树影，城市慢下来。     │
│ 我在长椅上坐了很久……        │
│ #散步  #傍晚                │
│ ──────────────────────── │
│ 静安雕塑公园            ›   │
└──────────────────────────┘
```

先照片与正文，后元数据与地点。Primary 是整行打开关联地点；编辑/删除进入 `⋯`。正文、标签、地点行不套多个 Card。更多照片在首图下或横向滚动。

组件树：`CapsuleDetailScreen[页面] → AppScaffold → DetailTopBar → CapsulePhotoLayout[数据] → DateMoodMetadata → CapsuleBody → TagRow → LinkedPlaceRow[数据] → OverflowMenu`。

## 9. 我的城市档案

```text
┌──────────────────────────┐
│ 我的                  ⚙   │
│      ◯  林间漫游者          │
│         上海 · 本地档案      │
│                            │
│  18 条记忆   9 个地点   6 想去│
│                            │
│ 想去的地方              ›   │
│ 编辑档案                ›   │
│ 设置                    ›   │
│                            │
│ 探索        记录        我的 │
└──────────────────────────┘
```

首屏先显示身份，以及由现有 catalog 精确计算的碎片数、关联地点数和想去数。Primary 是编辑档案；想去/设置是列表行。清除档案不在此页。统计、导航行不用 Card，也不出现虚构里程/足迹地图。

组件树：`CityProfileScreen[页面] → AppScaffold → ProfileHeader[数据] → LocalStatsRow[数据] → ProfileNavigationList → NavigationRow[通用] → AppBottomNavigation`。

## 10. 想去页面

```text
┌──────────────────────────┐
│ ‹ 想去的地方               │
│ [ 搜索想去地点           ] │
│ 6 个地点                   │
│ ──────────────────────── │
│ [地点图] 西岸美术馆    ♥    │
│   展馆 · 徐汇区             │
│ ──────────────────────── │
│ [地点图] 武康路        ♥    │
│   街区 · 徐汇区             │
└──────────────────────────┘
```

Primary 是打开地点；心形为即时移出动作，须可恢复或明确反馈。没有加入时间，不能按“最近想去”排序。列表项不用 Card；搜索结果继续滚动。

组件树：`WantToGoScreen[页面] → AppScaffold → AppTopBar → SearchField → ResultCount → PlaceList → CompactPlaceCard[数据]`。

## 11. 核心页面状态矩阵

| 页面 | Loading | Empty | Error | Permission Denied | Offline | Partial Data |
| --- | --- | --- | --- | --- | --- | --- |
| 探索首页 | 骨架/状态组件 | 新建首个地点 | 重试 | 不适用 | 本地正常 | 地点有、记忆无则隐藏记忆区 |
| 地点列表/想去 | 列表骨架 | 清除筛选/去探索 | 重载 | 不适用 | 本地正常 | favorite 悬空 ID 自动清理 |
| 地图 | 地图占位 | 切列表 | 切列表 | 解释并切列表 | 切列表 | marker 无位置则不入地图 |
| 地点详情 | 内容骨架 | Not Found 返回列表 | 重试 | 不适用 | 本地正常 | 无图用 fallback、无地址隐藏行 |
| 碎片编辑 | 读取草稿 | 空表单是正常态 | 保留输入重试 | 保留输入并允许无图继续 | 本地正常 | 个别图片失败显示单格提示 |
| 时间轴 | 列表骨架 | 去探索地点 | 重试 | 不适用 | 本地正常 | 地点缺失仍展示碎片 |
| 相册 | 网格骨架 | 切时间轴/去记录 | 重试 | 不适用 | 本地正常 | 单图失败不阻断网格 |
| 碎片详情 | 内容骨架 | Not Found 回时间轴 | 重试 | 不适用 | 本地正常 | 地点缺失显示降级文案 |
| 我的 | 档案骨架 | 默认本地档案 + 编辑 | 重试 | 不适用 | 本地正常 | 某项统计失败显示 `—` |

## 12. 交互约定

- 返回使用 TopBar；不在内容末尾重复“返回上一页”。
- 筛选与权限解释用 Bottom Sheet；危险删除用确认 Dialog；低频管理用 Overflow Menu。
- Card 整体点击进入详情；心形、溢出等独立图标需独立触控区和语义标签。
- Bottom Navigation 仅出现在 Home、Record 根容器和 Profile Overview；详情与操作页不重复显示。Record 的 Timeline/Gallery 两种内部视图共享同一底栏。
- “探索 / 记录 / 我的”位于单一 AppShell；点击驱动 `animateScrollToPage`，重复点击当前 Tab 为 no-op，根 Pager 暂不支持手指左右滑动。
- “列表 / 地图”和“时间轴 / 相册”是各自 Feature 容器内的同级视图状态，不使用页面出入栈；其中 Record 使用 TabRow + HorizontalPager 支持点击和左右滑动。进入地点详情或碎片详情才属于层级导航。
