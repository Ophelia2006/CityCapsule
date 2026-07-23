# CityCapsule 统一路由开发文档

> 更新日期：2026-07-23
> 代码事实源：`shared/.../core/navigation/AppRoute.kt` 与 `AppRouteTable.kt`

## 1. 统一路由表

状态说明：

- **可运行**：页面已注册，当前阶段可以完成跳转验收。
- **骨架可达**：平台页面与 Dispatcher 已接通，具体业务能力尚未实现。
- **协议占位**：共享路由协议已冻结，但页面或平台 Launcher 尚未实现。

| AppRoute | AppRouteKey | wire routeKey | 目标类型 | destination | 业务参数 | Android | HarmonyOS |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `LaunchGate` | `LAUNCH_GATE` | `launch_gate` | Kuikly | `launch_gate` | 无 | 可运行；系统无参启动默认入口 | 可运行；系统无参启动默认入口 |
| `Onboarding` | `ONBOARDING` | `onboarding` | Kuikly | `onboarding` | 无 | 可运行 | 可运行 |
| `Home` | `HOME` | `home` | Kuikly | `home` | 无 | 可运行 | 可运行 |
| `PlaceList` | `PLACE_LIST` | `place_list` | Kuikly | `place_list` | 无 | 协议占位 | 协议占位 |
| `PlaceDetail(placeId)` | `PLACE_DETAIL` | `place_detail` | Kuikly | `place_detail` | `placeId: String`，必填、非空白 | 协议占位 | 协议占位 |
| `PlaceEditor(placeId?)` | `PLACE_EDITOR` | `place_editor` | Kuikly | `place_editor` | `placeId: String?` | 协议占位 | 协议占位 |
| `MapExplore` | `MAP_EXPLORE` | `map_explore` | Kuikly | `map_explore` | 无 | 协议占位 | 协议占位 |
| `CapsuleEditor(capsuleId?, placeId?)` | `CAPSULE_EDITOR` | `capsule_editor` | Kuikly | `capsule_editor` | `capsuleId: String?`、`placeId: String?` | 协议占位 | 协议占位 |
| `CapsuleDetail(capsuleId)` | `CAPSULE_DETAIL` | `capsule_detail` | Kuikly | `capsule_detail` | `capsuleId: String`，必填、非空白 | 协议占位 | 协议占位 |
| `Timeline` | `TIMELINE` | `timeline` | Kuikly | `timeline` | 无 | 协议占位 | 协议占位 |
| `Gallery` | `GALLERY` | `gallery` | Kuikly | `gallery` | 无 | 协议占位 | 协议占位 |
| `Favorites` | `FAVORITES` | `favorites` | Kuikly | `favorites` | 无 | 协议占位 | 协议占位 |
| `Profile` | `PROFILE` | `profile` | Kuikly | `profile` | 无 | 可运行 | 可运行 |
| `Settings` | `SETTINGS` | `settings` | Kuikly | `settings` | 无 | 可运行 | 可运行 |
| `NativePermission(permissionType)` | `NATIVE_PERMISSION` | `native_permission` | Native | `/native/permission` | `permissionType: String`，必填、非空白 | 协议占位：Launcher 未注册 | 骨架可达：权限申请待实现 |
| `NativeFileImport(requestId)` | `NATIVE_FILE_IMPORT` | `native_file_import` | Native | `/native/file-import` | `requestId: String`，必填、非空白 | 协议占位：Launcher 未注册 | 骨架可达：文件选择待实现 |

### 非业务诊断页

以下页面只用于诊断和开发验收，不属于 `AppRoute`，业务代码不得使用原始字符串跳转到这些页面。

| 页面 | Kuikly pageName | 用途 |
| --- | --- | --- |
| Router Diagnostics | `router` | Kuikly RouterModule 诊断 |
| Image Adapter Diagnostics | `image_adapter` | 图片适配器诊断 |

HarmonyOS 内部 HMRouter URL 也不是共享业务路由，禁止泄漏到 `shared`：

| HMRouter URL | 作用 |
| --- | --- |
| `citycapsule/kuikly-host` | Kuikly 统一宿主页 |
| `citycapsule/native/permission` | 鸿蒙原生权限页骨架 |
| `citycapsule/native/file-import` | 鸿蒙原生文件导入页骨架 |
| `citycapsule/route-fallback` | 路由失败降级页 |

## 2. 统一导航动作

| 共享调用 | wire action | 栈语义 |
| --- | --- | --- |
| `navigator.navigate(route)` | `push` | 将目标页面压入栈顶。 |
| `navigator.replace(route)` | `replace` | 打开目标页面，并移除当前页面。 |
| `navigator.back()` | 不产生 `RouteRequest` | 仅关闭当前页面，返回上一页。 |
| `navigator.backTo(routeKey)` | `backTo` | 返回栈中最新的同 `routeKey` 页面；若目标不在栈中，则用 `replace` 恢复该目标。 |

`backTo` 的缺栈恢复用于 Home、Settings 等无参数稳定页面。带必填参数的详情页或原生页不能依赖 `backTo` 重建；需要重建时必须使用带完整参数的 `navigate(AppRoute...)` 或 `replace(AppRoute...)`。

典型行为：

```text
[home, settings, settings] -- backTo(HOME) --> [home]
[settings]                 -- backTo(HOME) --> replace --> [home]
[home, settings]           -- back()       --> [home]
```

第二条是 `Home -> replace(Settings) -> 返回首页` 的正式语义。目标 Home 因前一次 replace 不在栈中属于正常状态，不得触发路由降级页。

首次引导栈语义已经冻结，T89 页面实现遵循以下动作：

```text
[launch_gate] -- replace(Onboarding) --> [onboarding]
[launch_gate] -- replace(Home)       --> [home]
[onboarding]  -- backTo(HOME)        --> replace missing target --> [home]
[home, onboarding] -- backTo(HOME)   --> [home]
```

Onboarding 的 Welcome、Identity、Details、Review 是单页内部步骤，不得注册成四条路由。

## 3. 传输协议

共享层通过 Kuikly `RouterModule.openPage(target, pageData)` 发送路由请求。以下 `__cc_` 字段为保留字段，业务参数不得覆盖：

| 字段 | 值 |
| --- | --- |
| `__cc_route_action` | `push`、`replace`、`backTo` |
| `__cc_route_key` | 表中的稳定 wire routeKey |
| `__cc_target_type` | `kuikly` 或 `native` |

目标规则：

- Kuikly 目标使用稳定 `pageName`。
- Native 目标必须以 `/native/` 开头。
- Android 直接复用 Kotlin `RouteProtocol`。
- HarmonyOS 无法导入 Kotlin 常量，因此 `HarmonyRouteProtocol` 必须逐项镜像相同值。

## 4. 双端分发关系

```text
共享业务页面
  -> AppNavigator / AppRoute
  -> AppRouteTable.resolve()
  -> Kuikly RouterModule
     -> AndroidRouteDispatcher
        -> KuiklyHostActivity 或 AndroidNativeRouteRegistry
     -> HarmonyRouteDispatcher
        -> HMRouter KuiklyHostPage 或 HarmonyNativeRouteRegistry
```

双端 Dispatcher 的共同约束：

1. 业务层只传 `AppRoute` 和 `AppRouteKey`，不得传 Android Activity、Intent、HMRouter URL 或页面字符串。
2. 栈协调器只负责记录和查找 `routeKey`；缺失目标是否恢复由 Dispatcher 的 BackToPolicy 决定。
3. `backTo` 命中栈时直接完成回退；未命中时转换为同目标的 `replace`。
4. 未注册 Native 路由、未知 Kuikly 页面、协议非法或 HMRouter 未初始化仍属于真实失败，必须记录日志并执行既定降级。

## 5. Settings 当前验收入口

| 按钮 | 调用 | 预期结果 |
| --- | --- | --- |
| 打开本地档案 | `navigate(AppRoute.Profile)` | 打开共享 Profile 页面。 |
| 重新查看首次引导 | `navigate(AppRoute.Onboarding)` | 打开共享四步 Onboarding 页面，不改变冷启动根页。 |
| 返回首页 | `backTo(AppRouteKey.HOME)` | 一次回到 Home；Home 不在栈中时 replace 恢复 Home。 |
| Push another Settings | `navigate(AppRoute.Settings)` | 再压入一个 Settings，用于验证多实例栈。 |
| 返回上一页 | `back()` | 只关闭当前 Settings。 |

验收链路：

1. 冷启动进入 Home，点击 `Replace Settings`，再点击“返回首页”：直接显示 Home，不出现降级页。
2. Home 依次 Push Settings、Push another Settings，再点击“返回首页”：跨过中间页面直接回到 Home。
3. Settings 点击“返回上一页”：只回退一层。
4. Home 的“打开本地档案”和 Settings 的两个档案入口只能使用 typed route，不得出现 `"profile"`、`"onboarding"` 原始字符串跳转。
5. Onboarding 完成后 `backTo(HOME)`；若当前栈没有 Home，由双端 Dispatcher 转为 replace Home。

Android/HarmonyOS 已把 `LaunchGate` 设为系统无参启动根页。全新安装最终进入 Onboarding；存在当前完成版本和有效档案时最终进入 Home。Settings 手工进入 Onboarding 不会清除完成标记，只有 Profile 的确认清除操作会重置下次冷启动结果。

## 6. 新增或变更路由的同步清单

1. 在 `AppRoute.kt` 增加或修改强类型路由及 `AppRouteKey`。
2. 在 `AppRouteTable.kt` 同步 routeKey、destination、参数与 `resolveBackTo` 映射。
3. Kuikly 页面使用 `@Page(AppRouteTable.PAGE_...)` 注册；Native 目标使用 `/native/...`。
4. Android Native 路由在 `AndroidNativeRouteRegistry` 注册 Launcher。
5. HarmonyOS 同步 `HarmonyRoutes.ets` 页面目录，并在 `HarmonyNativeRouteRegistry` 或 Kuikly 可用目录注册。
6. 增加共享协议测试、Android Dispatcher/栈测试、HarmonyOS Guard/Dispatcher/栈测试。
7. 更新本文档中的统一路由表和双端状态，不得只改某一端字符串常量。

## 7. 双端冷启动契约

| 场景 | Android | HarmonyOS | 最终页面 |
| --- | --- | --- | --- |
| 桌面/系统无参启动 | `AndroidLaunchContract` 默认 `launch_gate` | `HarmonyLaunchContract` 默认 `launch_gate` | 由 shared 决策 |
| 显式打开 Profile | 保留 `profile/profile` | 保留 `profile/profile` | Profile |
| 首次安装或完成标记缺失 | LaunchGate replace | LaunchGate replace | Onboarding |
| 完成版本与档案有效 | LaunchGate replace | LaunchGate replace | Home |
| 已完成但档案缺失/损坏 | shared 清理无效完成状态后 replace | 同左 | Onboarding |
| 存储临时失败 | 不破坏持久数据，安全 replace | 同左 | Onboarding |

启动完成后平台业务栈只能保留最终页面：

```text
[launch_gate] -- replace(Onboarding) --> [onboarding]
[launch_gate] -- replace(Home)       --> [home]
```

平台 Launcher 不得直接读取 `profile.local_profile` 或 `onboarding.completed_version`；启动目的地只能由 shared `OnboardingStartupDecider` 产生。
