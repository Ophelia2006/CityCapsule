# P1-2 Profile Overview 验收

## 验收范围

验证“我的城市档案”真实数据聚合、Profile MVI 生命周期、二级编辑页、想去联动，以及 Settings 危险操作。不得用 mock 数据或视觉占位冒充通过。

统计口径：

- 城市碎片数：`CapsuleRepository.getPublished()` 返回的已发布 Capsule 数。
- 去过地点数：已发布 Capsule 中 distinct `placeId` 数；已被删除的历史地点 ID 仍计数。
- 想去数：`FavoriteRepository.getFavoriteIds()` 的有效 ID 数。
- 城市足迹：只把 Capsule 的 `placeId` 与当前 Place catalog 匹配后按 `Place.city` 聚合；不能解析的历史地点不虚构城市。

## 自动化

在仓库根目录执行：

```powershell
& 'C:\Users\Ophelia\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7\bin\gradle.bat' :shared:testDebugUnitTest :androidApp:testDebugUnitTest --no-daemon
```

预期：

- `BUILD SUCCESSFUL`。
- Profile Store 测试覆盖真实聚合、distinct 地点、悬空历史地点、想去移出、typed Effect、资料保存/校验和未保存退出。
- AppRoute 测试确认 `profile_edit` 的 route、pageName、wire key 和目标类型。

HarmonyOS 在已配置 DevEco/hvigor 的终端执行：

```text
hvigorw --mode module -p product=default -p module=entry@default -p buildMode=debug test --no-daemon
hvigorw --mode module -p product=default -p module=entry@default -p buildMode=debug assembleHap --no-daemon
```

预期 ArkTS route guard 测试通过，Debug HAP 生成成功。本轮 `:shared:compileKotlinOhosArm64` 已通过，但当前机器的普通终端无法找到 `hvigorw`，所以不能把 ArkTS test/HAP 标为已通过。

## 准备真实数据

1. 在资料编辑页设置昵称和城市，例如“Ophelia / 上海”。
2. 将上海博物馆加入“想去”，再任选另一个地点加入“想去”。
3. 在上海博物馆发布两条城市碎片。
4. 在另一个城市的地点发布一条城市碎片。
5. 返回“我的”根 Tab。

## 主流程验收

1. 身份区显示刚保存的头像预设、昵称和城市；不出现 Repository、MMKV、路由或 Debug 文案。
2. 三项统计应为：城市碎片 `3`、去过地点 `2`、想去 `2`。
3. “我的城市足迹”按真实城市显示；上海应为 `1 个地点 / 2 条记忆`，另一城市应为 `1 个地点 / 1 条记忆`。
4. “想去的地方”最多预览 3 个真实 Place；点击整张地点内容进入 Place Detail，点击心形只移出想去，不误触详情。
5. 移出后想去数立即减一、对应预览消失；切换到 Explore/Want To 后状态一致。
6. 点击“查看全部”进入 Want To 页面；返回后仍位于“我的”，不创建第二个 Bottom Navigation。

## 编辑资料验收

1. 点击身份区或“编辑资料”，进入无底栏的 `profile_edit` 二级页。
2. 修改昵称、头像、城市、简介并保存；返回 Overview 后身份区立即刷新，统计不受影响。
3. 输入非法空昵称或超长字段，必须停留页面并显示校验反馈，不写入坏数据。
4. 修改后直接返回，应出现丢弃确认；取消后保留编辑内容，确认后返回且不保存。
5. 连续快速点击保存只允许产生一次有效保存/返回，不重复 push/pop。

## 危险操作验收

1. Profile 根页不出现“清除本地档案”按钮。
2. 进入“数据与设置”，在底部危险操作区找到“清除本地档案”。
3. 确认文案必须明确：清除昵称/头像/城市/简介和首次引导状态，但保留地点、想去和城市记忆。
4. 取消确认时任何数据不变。
5. 确认后进入 Onboarding；重新完成资料后，原有 Capsule、想去和 Place 仍存在，Profile 统计恢复为原值。

## 状态与设备验收

Android 与 HarmonyOS 各执行一次：

- Light/Dark、系统字体 1.0x/1.3x、短屏和横屏；无截断、重叠、操作被底栏遮挡。
- Profile 首次进入只出现合理 Loading；切换根 Tab 再回来不闪回默认档案。
- 编辑保存、想去移出后不整页抖动或滚动归零。
- Repository 部分读取失败时未知统计显示 `—` 并给降级说明，不能显示伪造的 `0`。
- 快速切换 Tab、反复进入/退出编辑页、App 前后台切换后，无重复 Effect、幽灵跳转或销毁后状态写入。

全部通过后，才可把 P1-2 从“DONE（代码与自动化）/PARTIAL（设备体验）”改为完整 DONE。
