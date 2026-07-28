# CityCapsule 共享设计系统

## 1. 阶段边界

设计系统位于 `shared`，向 Android 与 HarmonyOS 的 Kuikly 页面提供同一套语义颜色、字体、尺寸、动效和组件规范。平台工程只负责系统外观检测、系统栏、启动窗口以及原生降级页面。

T45～T52 只冻结协议并建立 shared 骨架，不迁移 Home/Settings，不改变页面行为，也不接入平台主题通知。业务页面迁移属于 T59～T60，平台运行时属于 T61～T66。

第一阶段不包含：动态取色、自定义字体、复杂动画、完整图标库、外部设计系统依赖和截图基线框架。

## 2. 依赖规则

```text
core.theme.ThemeMode
       ├── core.storage（持久化与迁移）
       └── designsystem.theme（纯主题解析）

designsystem.tokens
       └── designsystem.theme.AppTheme
                  └── shared 组件和业务页面

Android/HarmonyOS system appearance
       └── app.theme coordinator（T61～T66）
                  └── designsystem.theme
```

- `designsystem` 不读取 MMKV，不调用 Android/HarmonyOS API，也不依赖路由。
- `core.storage` 不依赖 UI 组件。
- 页面只能消费语义令牌，不能依赖某个具体色值名称。
- 原始色值只允许出现在 `designsystem.tokens`、平台资源文件、测试和明确标注的调试页面。
- `ThemeMode` 的 `system/light/dark` 是存储协议 v1，修改必须提供双端迁移。
- 设计系统目前是 shared 内的 package，不创建独立 Gradle module，也不能放在 `build/generated`。

## 3. T45 视觉硬编码盘点

本轮开始时，shared 顶层页面的静态盘点如下：

| 文件 | 颜色常量 | fontSize | RoundedCornerShape | 明确 dp 间距 |
|---|---:|---:|---:|---:|
| `HomePage.kt` | 5 | 3 | 1 | 4 |
| `SettingsPage.kt` | 13 | 8 | 4 | 11 |
| `RouterPage.kt` | 4 | 2 | 0 | 4 |
| `BasicWidget.kt` | 4 | 2 | 2 | 3 |
| `ImageAdapterBenchmarks.kt` | 5 | 0 | 0 | 9 |

T59～T60 已清除 Home/Settings 的业务视觉常量。T68 又将 `RouterPage` 和 `BasicWidget` 迁移到语义令牌；`ImageAdapterBenchmarks` 因为需要用固定几何尺寸和高对比叠加色验证图片适配算法，作为唯一带理由的诊断白名单保留。

## 4. 主题协议

### ThemeMode

| 模式 | wire value | 行为 |
|---|---|---|
| 跟随系统 | `system` | 使用平台提供的系统深浅色 |
| 浅色 | `light` | 忽略系统模式，强制浅色 |
| 深色 | `dark` | 忽略系统模式，强制深色 |

降级规则：

1. 持久化缺失或损坏时由 `SettingsRepository` 回退到 `SYSTEM`。
2. `SYSTEM` 下无法获得系统外观时安全回退到浅色。
3. 强制浅色/深色不受系统外观缺失影响。
4. 平台主题同步失败不得阻断 shared 页面渲染。

### 语义颜色

页面使用以下语义，不使用 `purple500`、`gray100` 等物理色名：

```text
background / surface / surfaceVariant
primary / onPrimary / primaryContainer / onPrimaryContainer
textPrimary / textSecondary / divider
success / warning / error 及其 on/container 组合
disabledSurface / disabledContent / scrim
```

浅色与深色方案集中定义在 `AppColors.kt`。新增语义必须同时提供两套值和测试。

### 字体

| 语义 | 字号 | 行高 | 字重 |
|---|---:|---:|---|
| display | 36sp | 44sp | Bold |
| pageTitle | 30sp | 38sp | Bold |
| sectionTitle | 18sp | 26sp | Bold |
| body | 16sp | 24sp | Normal |
| bodySecondary | 14sp | 20sp | Normal |
| button | 16sp | 24sp | Bold |
| caption | 12sp | 18sp | Normal |

第一阶段统一使用系统字体。文字颜色来自 `AppColorScheme`，不固化进 `TextStyle`。

### 尺寸与动效

- 间距阶梯：0/4/8/12/16/20/24/32dp。
- 圆角阶梯：8/12/14/20dp。
- 最小点击区域：48dp。
- 页面水平边距：24dp。
- 内容最大宽度：720dp。
- 快速反馈：120ms；普通过渡：220ms；强调过渡：300ms。
- 页面转场仍由统一路由层管理，组件不得覆盖路由转场。

## 5. shared 使用方式

```kotlin
RuntimeAppTheme(themeHost = KuiklyAppThemeHost(pager)) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val dimensions = AppTheme.dimensions
    // Screen content
}
```

业务页面通过 `RuntimeAppTheme` 观察进程内的 `AppThemeRuntime`。底层 `AppTheme(themeMode, systemDark)` 只用于设计系统实现和独立测试。业务代码不应缓存 `AppTheme.colors` 到全局变量；必须在组合上下文中读取，以便主题切换触发重组。

## 6. 第一轮验收标准

- `ThemeMode` 的三个 wire value 保持不变。
- `ThemeResolver` 覆盖强制浅色、强制深色、跟随系统和系统状态缺失。
- Light/Dark 都提供完整语义颜色。
- 字体、尺寸、动效令牌拥有稳定测试。
- `AppTheme` 可以通过 CompositionLocal 暴露全部令牌。
- 现有路由、存储测试不回归。
- Home/Settings 本轮视觉保持不变，尚不以“切换后立即变色”为验收项。

## 7. T45～T52 实施记录

| 任务 | 交付结果 | 状态 |
|---|---|---|
| T45 | 完成 shared 顶层页面视觉硬编码盘点，冻结后续清理范围 | 完成 |
| T46 | 冻结 package 与依赖规则；`ThemeMode` 移至 `core.theme` 并保留 storage 类型别名 | 完成 |
| T47 | 建立间距、圆角、点击区域、内容宽度与图标尺寸令牌 | 完成 |
| T48 | 建立 Light/Dark 语义颜色方案 | 完成 |
| T49 | 建立系统字体语义层级 | 完成 |
| T50 | 建立反馈、普通、强调动效时长令牌 | 完成 |
| T51 | 建立纯函数 `ThemeResolver` 与确定性降级策略 | 完成 |
| T52 | 建立 CompositionLocal `AppTheme`、令牌测试和本规范文档 | 完成 |

2026-07-22 自动验收结果：

- shared JVM：38 个测试通过，0 failure，0 error。
- Android JVM：22 个测试通过，0 failure，0 error。
- shared `ohosArm64` 编译成功。
- shared `linkDebugSharedOhosArm64` 链接成功，新版 `libshared.so` 已复制并进入 HAP。
- Android debug APK 构建成功。
- HarmonyOS entry 本地测试：29 个通过，0 failure，0 error。
- HarmonyOS debug signed HAP 构建成功。

构建期间仍会报告 Kuikly/HMRouter 依赖、旧示例 API 和 SDK 兼容级别的既有 warning；本轮没有新增编译 warning，也没有把 warning 升级为错误。

### Windows 下的 HarmonyOS shared 构建顺序

当前 `kuikly-ohos-compile-plugin` 只在 macOS 自动执行 shared 链接与产物复制，在 Windows 上 Hvigor 构建 HAP 前必须手动执行：

```powershell
$env:DEVECO_STUDIO_HOME='D:\Software\Office\DevEcoStudio\DevEco Studio'
$env:OHOS_SDK_HOME="$env:DEVECO_STUDIO_HOME\sdk\default\openharmony"
.\gradlew.bat -c settings.ohos.gradle.kts :shared:linkDebugSharedOhosArm64

Copy-Item .\shared\build\bin\ohosArm64\debugShared\libshared.so `
  .\ohosApp\entry\libs\arm64-v8a\libshared.so -Force
Copy-Item .\shared\build\bin\ohosArm64\debugShared\libshared_api.h `
  .\ohosApp\entry\src\main\cpp\libshared_api.h -Force
```

复制后再通过 DevEco Studio 构建 HAP。只执行 `compileKotlinOhosArm64` 能证明源码可编译，但不能证明最新 shared 已进入最终 HAP。

## 8. T53～T60 共享组件与页面迁移

| 任务 | 交付结果 | 状态 |
|---|---|---|
| T53 | 建立滚动、安全区和最大内容宽度统一的 `AppScaffold` | 完成 |
| T54 | 建立标题、正文、辅助文字等语义文本组件 | 完成 |
| T55 | 建立 primary/secondary/text/danger 按钮及 disabled/loading 状态 | 完成 |
| T56 | 建立 `AppCard`、`AppSection` 和 `AppSettingsRow` | 完成 |
| T57 | 建立状态提示组件及 neutral/success/warning/error 语义 | 完成 |
| T58 | 建立 SYSTEM/LIGHT/DARK 三段式主题选择器 | 完成 |
| T59 | Home 清除业务视觉硬编码并迁移到共享组件 | 完成 |
| T60 | Settings 清除业务视觉硬编码，接入即时预览、保存状态和失败回滚 | 完成 |

Home/Settings 只组合共享组件并使用 `AppNavigator`、`SettingsRepository`；组件层不依赖路由和 MMKV。`ComponentPaletteTest` 覆盖按钮、状态提示和主题选择器的语义映射。

## 9. T61～T66 双端主题运行时

### 冻结协议

页面启动参数协议版本为 1，Android 与 HarmonyOS 必须在创建 Kuikly 页面前注入：

| key | 类型 | 说明 |
|---|---|---|
| `themeProtocolVersion` | number | 当前固定为 1 |
| `themeMode` | string | 持久化偏好：`system/light/dark` |
| `systemDark` | boolean | 平台当前是否为深色外观 |
| `resolvedDark` | boolean | native 启动阶段的最终解析结果 |
| `isNightMode` | boolean | Kuikly 内建主题回调兼容字段 |

shared 向 native 同步系统栏使用 `CCThemeHostModule.applyAppearance`，请求字段为 `protocolVersion/themeMode/isDark`。HarmonyOS 系统外观变化通过 Kuikly `themeDidChanged` 事件回传，事件数据使用 `isNightMode`。

### 运行链路

```text
MMKV theme_mode + platform system appearance
                 │
                 ▼
AndroidThemeHost / HarmonyThemeHost
                 │ pageData bootstrap
                 ▼
BasePager ──► AppThemeRuntime ──► RuntimeAppTheme ──► shared semantic tokens
                 ▲                       │
                 │ Settings preview      │ resolved isDark
                 │ / persist / rollback  ▼
                 └──────────── CCThemeHostModule ──► native system bars
```

- `AppThemeRuntime` 是所有活动 Kuikly 页面共享的运行时状态源；Settings 选择后 Home 与 Settings 使用同一状态。
- 保存前先即时预览；MMKV 写入失败时恢复前一个模式，成功时再次确认目标模式。
- `SYSTEM` 响应平台深浅色变化；`LIGHT/DARK` 只更新系统状态快照，不覆盖用户强制偏好。
- Android 在 Activity 创建和恢复时重新读取 MMKV 并设置状态栏/导航栏，系统配置变化由 Activity 重建重新注入。
- HarmonyOS 在 Ability 创建时绑定主窗口，在 `onConfigurationUpdate` 中通知活动 Kuikly 页面，并同步窗口系统栏。

| 任务 | 交付结果 | 状态 |
|---|---|---|
| T61 | 冻结 pageData、主题事件和 native module 协议；建立 `AppThemeRuntime` | 完成 |
| T62 | Android 启动前读取 MMKV 与系统外观并注入 Kuikly pageData | 完成 |
| T63 | Android 注册主题 host module，运行中同步状态栏和导航栏 | 完成 |
| T64 | HarmonyOS 启动前读取 MMKV 与系统外观并注入 Kuikly pageData | 完成 |
| T65 | HarmonyOS 注册主题 host module，处理配置变化并同步系统栏 | 完成 |
| T66 | 完成跨页面一致性、失败回滚、协议测试和双端产物验收 | 完成 |

2026-07-22 第三轮自动验收结果：

- shared JVM：47 个测试通过，0 failure，0 error。
- Android JVM：22 个测试通过，0 failure，0 error；debug APK 构建成功。
- shared `linkDebugSharedOhosArm64` 成功，最新版 `libshared.so` 已复制到 entry。
- HarmonyOS 本地测试：32 个通过，0 failure，0 error。
- HarmonyOS debug signed HAP 构建成功。

### 模拟器/真机验收

1. 清除应用数据后启动，确认默认 `SYSTEM` 与系统外观一致，首帧无先亮后暗闪烁。
2. Home 打开 Settings，依次选择浅色、深色、跟随系统；确认页面背景、文字、卡片、按钮、状态栏和导航栏同时变化。
3. 每种模式下返回 Home，再次进入 Settings，确认 Home 与 Settings 一致且选择状态正确。
4. 选择深色或浅色后杀进程重启，确认强制偏好恢复；切换系统深浅色时强制模式不变化。
5. 选择跟随系统后在应用运行、后台和恢复三种场景切换系统深浅色，确认 shared 页面与系统栏同步。
6. 连续执行 `Home -> Settings -> Push Settings -> 返回上一页 -> 返回首页`，确认整个原生栈没有旧主题页面。
7. 验证主题保存成功提示；如需验证失败回滚，可临时让存储 dispatcher 返回失败，确认界面恢复到选择前模式并显示错误状态。

## 10. T67～T72 硬编码清理、双端测试与交付

### 最终硬编码规则

shared 业务页面、公共组件和路由诊断页不得直接声明物理视觉值，必须从 `AppTheme.colors`、`AppTheme.typography` 和 `AppTheme.dimensions` 读取。自动守卫会扫描 `shared/src/commonMain/kotlin/com/y/citycapsule` 并拒绝以下新增内容：

- `Color(0x...)` 形式的原始 ARGB 色值；
- `#RRGGBB` / `#AARRGGBB` 形式的 CSS 色值；
- `Color.Black`、`Color.White`、`Color.Red` 等物理命名色；
- 非零的裸 `.dp` / `.sp` 常量。

以下内容不属于业务视觉硬编码：令牌定义自身、`0.dp` 这类无量纲默认值、`Color.Transparent` 语义透明值，以及 `statusBarHeight.dp` 这类平台运行时数值转换。

允许清单被冻结为下表；新增文件不得静默加入，必须同时补充理由、测试和本文档：

| 文件 | 保留理由 |
|---|---|
| `designsystem/tokens/AppColors.kt` | Light/Dark 语义颜色的唯一色值来源 |
| `designsystem/tokens/AppDimensions.kt` | 间距、圆角和尺寸令牌的唯一来源 |
| `designsystem/tokens/AppTypography.kt` | 字号、行高和字重令牌的唯一来源 |
| `ImageAdapterBenchmarks.kt` | 固定画布尺寸和红/蓝/绿叠加层是图片适配基准的断言信号，不是业务 UI |

`DesignSystemHardcodingGuardTest` 同时冻结扫描规则和上述四项允许清单。`RouterPage` 已使用 `RuntimeAppTheme`、`AppScaffold`、`AppTopBar`、`AppSection` 和语义按钮；`BasicWidget` 的光标、导航栏字号、颜色和间距也已改为主题令牌。

### 任务记录

| 任务 | 交付结果 | 状态 |
|---|---|---|
| T67 | 重新扫描 shared 视觉常量并冻结硬编码判定规则 | 完成 |
| T68 | 清理 RouterPage/BasicWidget，冻结唯一诊断白名单 | 完成 |
| T69 | 增加源码硬编码守卫并完成 shared/Android JVM 回归 | 完成 |
| T70 | 冻结 Android pageData 映射与 native module 协议测试，构建 debug APK | 完成 |
| T71 | 冻结 HarmonyOS pageData/native module/event 协议测试，重链 shared 并构建 signed HAP | 完成 |
| T72 | 更新设计系统规则、测试结果和双端人工验收流程 | 完成 |

2026-07-23 最终自动验收结果：

- shared JVM：47 个测试通过，0 failure，0 error。
- Android JVM：27 个测试通过，0 failure，0 error；其中包含 pageData 完整映射、未知系统外观降级、native module 协议和源码硬编码守卫。
- HarmonyOS 本地测试：33 个通过，0 failure，0 error；包含主题 wire contract 对齐测试。
- shared `linkDebugSharedOhosArm64` 成功，最新版 `libshared.so` 与头文件已复制到 entry。
- Android debug APK 与 HarmonyOS debug signed HAP 均构建成功。

### T67～T72 双端人工验收

1. 安装全新 APK/HAP 或清除应用数据；分别在系统浅色、深色下首次启动，确认首屏、状态栏和导航栏颜色一致且无闪烁。
2. 从 Home 进入 Settings，依次选择浅色、深色、跟随系统；检查页面背景、标题、正文、卡片、选择器、按钮及系统栏同步变化。
3. 每次切换后执行 Settings 返回 Home、重新进入 Settings，并检查选中态和 Home 外观一致；不得出现旧主题残留页。
4. 打开 RouterPage，逐项执行 push、replace、back 和 backTo；确认诊断页在 Light/Dark 下可读，路由栈和降级行为不回归。
5. 在强制浅色/深色下切换系统外观，应用主题不得变化；在跟随系统下切换系统外观，前后台恢复后 shared 页面与系统栏必须同步。
6. 杀进程后重启，确认最后保存的主题模式恢复；卸载重装或清除数据后应回到 `SYSTEM`。
7. Android 检查 `androidApp-debug.apk`，HarmonyOS 检查 `entry-default-signed.hap`；Windows 上 HarmonyOS 验收前仍须按第 7 节顺序重链并复制最新 shared 产物。

## 11. T81～T89 本地档案共享组件

本轮继续复用既有设计系统，没有引入平台专属业务 UI。新增组件如下：

| 组件 | 作用 | 主要令牌 |
| --- | --- | --- |
| `AppTextField` | 单行/多行输入、标签、占位、错误、字数和禁用状态 | colors、typography、dimensions |
| `AppProfileAvatar` | 渲染四种纯本地预设头像 | primaryContainer、title、avatarSize |
| `AppAvatarPicker` | 选择预设头像并展示选中态 | primary/surfaceVariant、strokeThin |
| `AppStepIndicator` | 展示四步引导进度 | primary/outline、indicatorSize |
| `AppConfirmDialog` | 清除档案前的共享确认浮层 | scrim/surface、dialogWidth |

`AppDimensions.strokeThin` 是本轮新增的细描边语义令牌。组件和 `LaunchGatePage`、`OnboardingPage`、`ProfilePage` 均通过 `AppTheme` 获取颜色、字号、间距、圆角和尺寸；源码硬编码守卫已经覆盖这些新增文件。

页面分工：

- LaunchGate 只负责展示加载态并消费启动决策，不展示或编辑业务数据。
- Onboarding 组合输入框、头像选择和步骤指示器，四个步骤保留在同一页面。
- Profile 复用相同的输入与头像组件，清除操作统一使用确认对话框。
- Home/Settings 只提供 typed route 入口，不复制档案 UI。

2026-07-23 自动化门禁结果：shared 80/80、Android 27/27、HarmonyOS 34/34；`DesignSystemHardcodingGuardTest` 通过，未增加视觉白名单。Android debug APK 构建成功；HarmonyOS arm64 shared 重链并同步后，debug signed HAP 构建成功。

T90～T97 把 `LaunchGatePage` 接入双端真实冷启动根页，没有新增视觉常量或平台专属加载 UI。LaunchGate 继续由 `RuntimeAppTheme` 和语义令牌渲染，冷启动主题页数据仍由既有 Android/HarmonyOS ThemeHost 注入。

## 12. T108～T130 地点共享页面

地点业务继续使用 shared 设计系统，没有新增 Android View 或 ArkUI 业务页面。

| 组件/页面 | 作用 | 设计约束 |
| --- | --- | --- |
| `AppChoiceChip` | 分类、城市和收藏筛选的可切换选项 | 语义颜色、圆角、间距和最小触控区域 |
| `PlaceSummaryCard` | 列表/收藏共用地点摘要与收藏动作 | `AppCard`、共享文字和按钮组件 |
| `PlaceListPage` / `FavoritesPage` | 搜索、筛选、结果、空状态和错误状态 | `RuntimeAppTheme` + `AppScaffold` |
| `PlaceDetailPage` | 详情、收藏、编辑、删除 | 语义 danger/success/error 状态 |
| `PlaceEditorPage` | 新建/编辑、字段校验、分类、未保存确认 | `AppTextField`、`AppChoiceChip`、`AppConfirmDialog` |

`PlaceArchitectureGuardTest` 禁止地点 feature 直接引用 MMKV、`openPage()`、原始 pageName
和 `places.catalog/favorites.place_ids` wire key。`DesignSystemHardcodingGuardTest` 继续禁止业务
页面直接声明物理颜色、非零裸 `.dp/.sp`；本轮没有新增白名单。

2026-07-25 门禁结果：shared 114/114、Android JVM 34/34、HarmonyOS 本地 40/40；
Android 模拟器与 HarmonyOS 真机设备测试均 6/6。Light/Dark 下的 Chip、卡片、输入、
确认弹窗、空状态、只读恢复和错误提示仍需按地点验收文档进行最终视觉走查。

## 13. 2026-07-27 Record Flow 组件

本轮新增 `AppSegmentedControl`，用于“时间轴 / 相册”两种同级内容视图切换；选中态、容器、圆角、字体和最小触控区域全部来自语义 token。`CapsulePhoto`/`CapsulePhotoList` 位于 Capsule feature，只负责业务照片的加载、裁切、失败降级与可选移除，不承担平台选图或文件存储。

`AppDimensions` 新增 `mediaPreviewHeight` 与 `mediaThumbnailSize`，分别用于碎片详情/时间轴大图和相册网格。Gallery 使用两列真实照片 Grid，Editor 的心情选项使用两列布局；Record 页面未引入裸色值、字号、间距或圆角，Android hardcoding guard 已通过。

本轮没有修改全局颜色方案。暖白/中性灰/暖琥珀视觉迁移仍是独立 Design System 任务，不能因 Record 页面使用现有 token 就标记为完成。

## 14. 2026-07-28 Design System v2 最小产品化

浅色方案迁移到暖白画布 `#F8F6F1`、近黑正文 `#1D1B18`、中性灰辅助文字 `#6F6A62`、暖灰分隔线 `#E3DED4` 与琥珀 Accent `#C97824`。深色方案同步为暖中性色与低饱和琥珀，`ThemeMode` 的 `system/light/dark` wire value、`settings.theme_mode` key 和 Theme Protocol v1 均未修改。Android/HarmonyOS 系统栏颜色与新画布同步。

本轮只建立 Home/Explore/Record 后续会消费的最小组件：`AppIcon/AppIconButton`、`AppBottomNavigation`、Hero/Compact `PlaceCard`、Timeline/Recent `CapsuleCard`、`PhotoGrid`、`SearchField`、`EmptyState/LoadingState/ErrorState`、`AppOverflowMenu`、`AppBottomSheet` 与 `AppElevation`。组件保持纯展示/回调边界，不依赖 Repository、路由、MMKV 或平台 API。地点列表已消费 Compact PlaceCard；时间轴已消费 CapsuleCard 和三种产品状态；相册已消费 PhotoGrid。

当前地点 schema 没有照片字段，仓库也没有获准用于产品的地点摄影。因此 PlaceCard 默认展示克制的 `PlaceMediaFallback` 类别图形；`sample.png` 继续只属于图片适配诊断页。所有未来产品资产必须先登记到 `ASSET_ATTRIBUTION.md`。

`AppElevation` 目前冻结 flat/raised/overlay 语义值；Kuikly 双端阴影渲染效果仍以设备视觉验收为准，组件不得依赖阴影作为唯一层级信号。`AppIcon` 建立统一调用入口和有限图标词汇，业务页面不得混用 emoji、Material 图标、PNG 与文本符号；当前首版图形仍需进行 Android/HarmonyOS 字体渲染走查。
