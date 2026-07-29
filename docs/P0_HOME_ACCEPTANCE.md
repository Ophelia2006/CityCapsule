# P0-3 Home Redesign 验收

## 代码范围

- Home 只聚合现有 Profile、Place、Favorite、Capsule Repository，不新增网络、天气、坐标、距离或图片 schema。
- 本地排序固定为：当前档案城市优先 → 想去或尚未记录优先 → 同优先级类别轮转 → category enum 与 placeId 稳定兜底。
- 快速记录必须先显示真实 catalog 地点选择器，只能进入 `CapsuleEditor(placeId = 非空地点 ID)`。

## 自动验收

1. `:shared:testDebugUnitTest`：排序优先级、类别多样化、稳定顺序及既有 shared 回归。
2. `:androidApp:testDebugUnitTest`：Android 路由/架构守卫回归。
3. Android Debug APK 构建。
4. HarmonyOS shared arm64 链接、entry test 与 signed HAP 构建（环境允许时）。

## 双端手工验收

1. 有档案城市时，首屏显示真实城市、头像和昵称；无城市时显示“未设置城市”。
2. 不出现天气、距离、“附近”、AI 推荐、猜你喜欢或网络同步文案。
3. Hero 整卡进入地点详情；想去图标切换后刷新仍保持。
4. 分类和搜索入口进入真实地点目录；想去/同城摘要进入对应地点。
5. 最近记忆只来自已发布 Capsule，按创建时间倒序最多 3 条；点击进入对应碎片详情。
6. 快速记录先打开可滚动地点选择器；选择后编辑器显示同一地点。空 catalog 时只能先新建地点。
7. 验证 Light/Dark、长昵称/城市名、空 catalog、读取部分失败、系统返回和 AppShell Tab 状态保留。
8. Hero、想去区和“换一种逛法”切换想去状态时，只更新心形/按钮状态，不重排当前可见内容、不插入成功横幅、不进入 Loading。
9. 从 Home 进入地点详情后切换想去状态，详情内容不得收缩为“正在读取地点”。
10. 搜索地点列表切换想去状态时，搜索词、筛选、结果顺序和滚动位置保持不变。

## 2026-07-30 自动验证结果

- `:shared:testDebugUnitTest`：通过。
- `:androidApp:testDebugUnitTest`：通过。
- `:androidApp:assembleDebug`：通过。
- `:shared:compileKotlinOhosArm64`：通过。
- HarmonyOS entry test、signed HAP 与 Android/HarmonyOS 设备手工验收：本轮未执行，保持待验收。
