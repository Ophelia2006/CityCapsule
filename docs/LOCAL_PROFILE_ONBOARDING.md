# CityCapsule 本地档案与首次引导

> 协议版本：1
> 冻结日期：2026-07-23
> 当前范围：T73～T97；已完成 shared 协议、数据与 UI 闭环、双端 LaunchGate 冷启动接入及设备测试资产。

## 1. 阶段边界

本阶段建立单用户、纯本地档案和首次引导的稳定契约。档案不代表账号，不产生网络身份，也不触发相机、相册、定位、联系人等系统权限。

包含：

- 一个应用安装对应一个本地档案。
- 本地档案、首次引导完成版本和可恢复草稿。
- shared Codec、校验、Repository、启动决策与 typed route。
- 损坏数据修复、提交中断恢复和清除顺序。
- `LaunchGatePage`、四步 `OnboardingPage`、`ProfilePage` 和 shared 状态持有者。
- Home/Settings 的强类型业务入口，以及 Android/HarmonyOS 可用页面目录。
- Android/HarmonyOS 系统无参启动统一进入 LaunchGate，再由 shared 决策进入 Home/Onboarding。

不包含：

- 登录、注册、云同步、多档案切换。
- 网络头像、自定义图片、平台文件路径或图片二进制。

T80 完成时三个页面还只是协议，T89 完成共享 UI，T97 后 Android `KuiklyHostActivity` 和 HarmonyOS `KuiklyHostPage` 的系统无参启动均默认打开 LaunchGate。显式业务跳转仍保留原目标，不会被 LaunchGate 改写。

## 2. LocalProfile v1

```kotlin
data class LocalProfile(
    val schemaVersion: Int = 1,
    val displayName: String,
    val avatarPreset: AvatarPreset,
    val homeCity: String?,
    val bio: String?
)
```

| 字段 | wire 类型 | 规则 |
| --- | --- | --- |
| `schemaVersion` | number | 当前只接受 `1` |
| `displayName` | string | trim 后必填，1～20 字符 |
| `avatarPreset` | string | `sky/forest/sunset/night` |
| `homeCity` | string，可省略 | trim 后空串规范化为 null，最多 30 字符 |
| `bio` | string，可省略 | trim 后空串规范化为 null，最多 80 字符 |

默认档案固定为：

```text
displayName = 城市漫游者
avatarPreset = sky
homeCity = null
bio = null
```

Codec 规则：

1. 保存前统一 trim 和校验。
2. 可选空白字段不写入 JSON。
3. 解码忽略未知字段，允许后续版本增加可选字段。
4. 缺少必填字段、未知头像、未知 Schema、畸形 JSON 或超长字段返回 `DECODE_FAILED`，不把半合法对象交给业务。
5. 错误消息不得包含昵称、城市或简介原文。

## 3. OnboardingDraft v1

草稿步骤固定为：

| 枚举 | wire value | 作用 |
| --- | --- | --- |
| `WELCOME` | `welcome` | 欢迎和本地隐私说明 |
| `IDENTITY` | `identity` | 昵称与预设头像 |
| `DETAILS` | `details` | 城市和简介 |
| `REVIEW` | `review` | 确认并提交 |

草稿字段与档案一致，额外包含 `currentStep`。引导早期允许 `displayName` 为空，但转换为最终 `LocalProfile` 时必须通过完整档案校验。

每个步骤是同一个 Onboarding 页面的内部状态，不增加独立 `AppRoute`，避免污染 Android Activity 栈和 HarmonyOS HMRouter 栈。

## 4. 冻结 Key

| Kotlin 常量 | Store | wire key | type | 默认值 |
| --- | --- | --- | --- | --- |
| `Profile.LOCAL_PROFILE` | `cc_preferences` | `profile.local_profile` | `json_object` | 默认本地档案 |
| `Onboarding.COMPLETED_VERSION` | `cc_preferences` | `onboarding.completed_version` | `long` | `0` |
| `Onboarding.DRAFT` | `cc_cache` | `onboarding.draft` | `json_object` | 空草稿 |

边界：

- `cc_preferences` 保存需要跨冷启动长期保留的档案和完成版本。
- `cc_cache` 保存允许重新生成的中间草稿；草稿丢失只会从欢迎步骤重新开始。
- 图片、文件内容和平台 URI 不得写入任何上述 Key。
- 三个 Key 是全新业务数据，没有 SharedPreferences/Harmony Preferences 旧值，本阶段不增加平台迁移器步骤。

## 5. 启动决策

`OnboardingStartupDecider` 是无平台依赖的纯函数，输入为三个 typed `StorageResult`，输出为 `StartupDecision`。

| 完成版本 | 档案 | 结果 | 修复 |
| --- | --- | --- | --- |
| `>= 1` | 有效 | `HOME / COMPLETED` | 清理残留草稿 |
| 缺失或 `0` | 任意可读状态 | `ONBOARDING / NOT_COMPLETED` | 保留有效档案和草稿用于预填 |
| `>= 1` | 缺失 | `ONBOARDING / PROFILE_MISSING` | 先清理完成标记 |
| `>= 1` | 损坏 | `ONBOARDING / PROFILE_INVALID` | 先清理完成标记，再清理损坏档案 |
| 任意 | 临时存储失败 | `ONBOARDING / STORAGE_UNAVAILABLE` | 不删除任何持久数据 |
| 任意 | 草稿损坏 | 不阻塞主决策 | 清理损坏草稿 |

只有 `TYPE_MISMATCH` 和 `DECODE_FAILED` 被视为确定性损坏并允许自愈删除；`NOT_INITIALIZED`、`NATIVE_FAILURE` 等临时错误只进入降级状态，不得破坏已有数据。

## 6. Repository 规则

### LocalProfileRepository

- `getProfile()`：严格读取，保留 Missing/Failure。
- `getProfileSnapshot()`：业务读取；缺失或失败时返回默认档案，并保留来源和 warning。
- `saveProfile()`：Repository 边界再次校验，非法对象返回 `INVALID_REQUEST`。
- `clearProfile()`：只删除档案 Key，不擅自改变引导状态。

### OnboardingRepository

启动读取：

```text
storage.getMany(completed_version, local_profile, draft)
  -> typed per-key result
  -> OnboardingStartupDecider
  -> 仅执行确定性损坏修复
  -> StartupDecision
```

完成提交顺序固定为：

```text
写 profile.local_profile
  -> 成功后写 onboarding.completed_version = 1
  -> 成功后删除 onboarding.draft（best effort）
```

约束：

1. 档案写失败时不得创建完成标记。
2. 档案成功、完成标记失败时保留档案和草稿，下次继续引导。
3. 完成标记成功后，草稿删除失败只作为 cleanup warning，不得回滚已完成状态。
4. 清除全部本地状态时固定按“完成标记 → 档案 → 草稿”执行。
5. 清除过程即使某一步失败，也要继续尝试后续项，最后报告第一个错误。

## 7. 路由协议

新增两个共享路由并激活既有 Profile 协议：

| AppRoute | AppRouteKey | routeKey | pageName | T97 状态 |
| --- | --- | --- | --- | --- |
| `LaunchGate` | `LAUNCH_GATE` | `launch_gate` | `launch_gate` | 可运行，双端默认冷启动入口 |
| `Onboarding` | `ONBOARDING` | `onboarding` | `onboarding` | 可运行 |
| `Profile` | `PROFILE` | `profile` | `profile` | 可运行 |

页面行为：

- `launch_gate` 判断完成后必须 `replace(Home)` 或 `replace(Onboarding)`，不得留在栈内。
- Onboarding 完成后使用 `backTo(AppRouteKey.HOME)`；栈中没有 Home 时由现有 Dispatcher 转为 replace Home。
- Onboarding 四个步骤不得创建四条业务路由。

## 8. 依赖关系

```text
feature pages（T81～T89）
  -> LocalProfileRepository / OnboardingRepository
     -> AppStorageKeys
     -> KeyValueStore
        -> KuiklyKeyValueStore
        -> InMemoryKeyValueStore（测试）
     -> CCStorageModule / MMKV v1

OnboardingRepository
  -> OnboardingStartupDecider（纯函数）
  -> LocalProfileCodec / OnboardingDraftCodec
```

平台层不得解析 LocalProfile JSON，也不得自行实现启动决策表。

## 9. T73～T80 实施记录

| 任务 | 完成内容 | 状态 |
| --- | --- | --- |
| T73 | 冻结单档案、纯本地、无账号/网络/权限的隐私边界 | 完成 |
| T74 | 冻结 LocalProfile v1、四种预设头像和字段约束 | 完成 |
| T75 | 注册档案、完成版本、草稿三个 typed Key | 完成 |
| T76 | 实现档案/草稿 Codec、规范化和确定性校验 | 完成 |
| T77 | 实现 LocalProfileRepository 的严格读取、业务快照、保存和清除 | 完成 |
| T78 | 实现启动决策、两阶段完成提交、自愈和安全清除顺序 | 完成 |
| T79 | 新增 LaunchGate/Onboarding typed route，补齐 Profile 映射 | 完成 |
| T80 | 完成 shared 单元测试和路由/存储/本规范文档 | 完成 |

## 10. 第一轮自动化验收

执行：

```powershell
gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest
```

2026-07-23 结果：

- shared JVM：72 个测试，0 failure，0 error。
- Android JVM：27 个测试，0 failure，0 error。
- 本轮未修改平台存储协议、默认启动页、APK UI 或 HAP UI，因此不要求设备验收。

进入第二轮前的历史边界：

1. Android/HarmonyOS 冷启动仍直接进入 Home。
2. `launch_gate/onboarding/profile` 不应被当作已经可运行的页面。
3. 新页面只能依赖 Repository、typed route 和 shared 设计系统，不得直接访问 MMKV 或原始字符串 Key。

## 11. T81～T89 共享 UI 与业务闭环

### 11.1 组件与页面依赖

```text
Home / Settings
  -> AppNavigator + AppRoute.Profile / AppRoute.Onboarding
     -> ProfilePage / OnboardingPage / LaunchGatePage
        -> ProfileStateHolder / OnboardingStateHolder
           -> LocalProfileRepository / OnboardingRepository
              -> KuiklyKeyValueStore -> 双端 CCStorageModule -> MMKV

业务页面
  -> AppTextField / AppAvatarPicker / AppStepIndicator / AppConfirmDialog
     -> AppTheme 语义令牌
```

页面不直接读取 MMKV、不拼接原始 Key、不解析 JSON，也不包含 Android 或 HMRouter 平台字符串。

### 11.2 任务记录

| 任务 | 完成内容 | 状态 |
| --- | --- | --- |
| T81 | 增加共享输入框及细描边尺寸令牌，支持标签、占位、错误、字数和多行输入 | 完成 |
| T82 | 增加预设头像选择器、步骤指示器和确认对话框 | 完成 |
| T83 | 实现 LaunchGate 页面和加载态，根据共享启动决策 replace Home/Onboarding | 完成 |
| T84 | 实现 Onboarding 欢迎步骤、本地隐私说明和默认档案快速入口 | 完成 |
| T85 | 实现昵称、头像、城市和简介编辑及字段校验 | 完成 |
| T86 | 实现四步状态流转、草稿自动保存和存储失败内存降级 | 完成 |
| T87 | 实现预览提交、两阶段持久化和完成后返回 Home | 完成 |
| T88 | 实现 Profile 查看、编辑、保存、取消及清除本地状态确认 | 完成 |
| T89 | 接入 Home/Settings 强类型入口、HarmonyOS 页面目录、状态测试和交付文档 | 完成 |

### 11.3 自动化验收

2026-07-23 结果：

- shared JVM：80 个测试，0 failure，0 error。
- Android JVM：27 个测试，0 failure，0 error。
- HarmonyOS 本地单元测试：34 个测试，0 failure，0 error。
- 新增状态测试覆盖草稿恢复、字段拦截、提交成功、存储失败降级、档案保存/取消和安全清除。
- Android debug APK 构建成功。
- HarmonyOS arm64 shared 重链成功，`.so`/头文件已同步，debug signed HAP 构建成功。

### 11.4 本轮人工验收

T81～T89 结束时默认启动页仍是 Home，因此当时通过现有业务入口验收：

1. Home 点击“打开本地档案”，缺少档案时应显示默认档案快照；点击编辑，修改昵称/头像/城市/简介后保存。
2. 返回 Home 再次进入档案页，数据应保持；点击取消时，本次未保存修改应全部撤销。
3. Settings 点击“重新查看首次引导”，依次完成欢迎、身份、详情、确认四步；返回上一步时内容保持。
4. 在昵称为空时点击下一步，应停留当前步骤并显示业务错误；可选城市和简介允许留空。
5. 在任一步离开 Onboarding 后重新进入，应从最近保存的草稿继续。
6. 完成引导或使用默认档案后，应一次回到 Home；不得返回前一个引导步骤，也不得进入降级页。
7. Profile 点击“清除档案与引导状态”，先显示确认对话框；确认成功后进入 Onboarding，取消则不删除数据。
8. Android 与 HarmonyOS 各执行一次杀进程重启后再进入 Profile，确认 MMKV 数据仍存在。

上述 11.4 是 T81～T89 的历史验收范围；T90～T97 已把冷启动自动分流纳入正式验收。

## 12. T90～T97 双端冷启动与设备测试

### 12.1 冷启动链路

```text
Android Launcher
  -> KuiklyHostActivity（无 pageName/routeKey）
  -> AndroidLaunchContract -> launch_gate

HarmonyOS EntryAbility
  -> Index / HMNavigation homePageUrl
  -> KuiklyHostPage（无业务参数）
  -> HarmonyLaunchContract -> launch_gate

launch_gate
  -> OnboardingRepository.getStartupDecision()
     -> 未完成/状态缺失/确定性损坏/临时存储失败 -> replace(Onboarding)
     -> 完成版本有效且档案有效                  -> replace(Home)
```

两端只负责把“系统无参启动”映射为 LaunchGate，不读取业务 Key、不解析档案 JSON，也不复制 shared 决策表。带 `pageName/routeKey` 的显式 Dispatcher 请求保持原目标。

### 12.2 任务记录

| 任务 | 完成内容 | 状态 |
| --- | --- | --- |
| T90 | 冻结无参系统启动进入 LaunchGate、显式路由保持不变的双端契约 | 完成 |
| T91 | Android `KuiklyHostActivity` 接入 `AndroidLaunchContract`，默认页从 Home 改为 LaunchGate | 完成 |
| T92 | Android 增加启动契约单测和真实 Launcher 冷启动设备测试 | 完成，模拟器 2/2 |
| T93 | HarmonyOS `KuiklyHostPage` 接入 `HarmonyLaunchContract`，HMNavigation 首页进入 LaunchGate | 完成 |
| T94 | HarmonyOS 增加启动契约单测、ohosTest 冷启动边界和完成标记重开测试 | 代码与测试 HAP 完成；真机执行需设备在线 |
| T95 | shared 增加首次安装、完成后重启、清除后重启三段生命周期测试 | 完成 |
| T96 | 完成 shared/Android/HarmonyOS 回归、arm64 重链及 APK/HAP/ohosTest HAP 构建 | 完成 |
| T97 | 更新统一路由、存储、本地档案规范与双端人工验收流程 | 完成 |

### 12.3 自动化结果

2026-07-23：

- shared JVM：83 个测试，0 failure，0 error。
- Android JVM：31 个测试，0 failure，0 error。
- Android `Pixel_10_Pro(AVD) - 17`：4 个设备测试通过；其中 2 个直接启动 Launcher 并等待业务路由栈稳定，另 2 个覆盖 MMKV 重开和旧数据迁移。
- HarmonyOS 本地单元测试：38 个测试，0 failure，0 error。
- HarmonyOS ohosTest：5 个设备用例已编译进测试 HAP；当前设备离线时不能申报真机运行通过。

### 12.4 双端人工验收流程

每个平台都按以下顺序完整执行，不能只通过页面内返回模拟冷启动：

1. 卸载应用后重新安装，或在系统设置中清除全部应用数据。
2. 从桌面图标启动；允许短暂看到 LaunchGate 加载态，最终必须进入 Onboarding Welcome，不能先显示 Home。
3. 完成 Identity 并进入 Details，让草稿成功保存；从任务列表结束应用进程后重新启动，应继续 Onboarding，并恢复已经保存的昵称和头像。
4. 完成四步引导；应一次进入 Home，系统返回不得重新露出 LaunchGate 或 Onboarding。
5. 从任务列表结束进程，再从桌面启动；LaunchGate 判断后必须进入 Home。
6. 打开 Profile，确认昵称、头像、城市和简介与提交内容一致。
7. 在 Profile 执行“清除档案与引导状态”，确认后进入 Onboarding；再次结束进程并冷启动，仍应进入 Onboarding。
8. 在浅色、深色和跟随系统下各冷启动一次，确认 LaunchGate、Onboarding/Home 与系统栏主题一致，无明显先亮后暗。
9. 全流程不得出现 HMRouter 降级页、Android 空白 Activity、`backTo target home` 拦截失败、重复 Home 或重复 Onboarding。

HarmonyOS 真机执行 ohosTest 前，设备需在 Device Manager/`hdc list targets -v` 中显示 Online；安装主 HAP 与 ohosTest HAP 后运行 `entry > ohosTest`，期望 `Tests run: 5, Failure: 0, Error: 0, Pass: 5`。
