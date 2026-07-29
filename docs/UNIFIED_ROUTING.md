# CityCapsule 统一路由开发文档

> 更新日期：2026-07-30
> 代码事实源：`shared/.../core/navigation/AppRoute.kt` 与 `AppRouteTable.kt`
> 当前一级导航事实：Home、Timeline、Profile 是 typed 入口别名，统一解析到 canonical `app_shell`；壳内 Tab 切换不产生 wire action。


## 1. 统一路由表

状态说明：

- **可运行**：页面已注册，当前阶段可以完成跳转验收。
- **骨架可达**：平台页面与 Dispatcher 已接通，具体业务能力尚未实现。
- **协议占位**：共享路由协议已冻结，但页面或平台 Launcher 尚未实现。

| AppRoute | AppRouteKey | wire routeKey | 目标类型 | destination | 业务参数 | Android | HarmonyOS |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `LaunchGate` | `LAUNCH_GATE` | `launch_gate` | Kuikly | `launch_gate` | 无 | 可运行；系统无参启动默认入口 | 可运行；系统无参启动默认入口 |
| `Onboarding` | `ONBOARDING` | `onboarding` | Kuikly | `onboarding` | 无 | 可运行 | 可运行 |
| `Home` | `HOME` | `app_shell` | Kuikly | `app_shell` | `initialRootTab=home` | 可运行 | 可运行 |
| `PlaceList` | `PLACE_LIST` | `place_list` | Kuikly | `place_list` | 无 | 可运行 | 可运行 |
| `PlaceDetail(placeId)` | `PLACE_DETAIL` | `place_detail` | Kuikly | `place_detail` | `placeId: String`，必填、非空白 | 可运行 | 可运行 |
| `PlaceEditor(placeId?)` | `PLACE_EDITOR` | `place_editor` | Kuikly | `place_editor` | `placeId: String?` | 可运行 | 可运行 |
| `MapExplore` | `MAP_EXPLORE` | `map_explore` | Kuikly | `map_explore` | 无 | 协议占位 | 协议占位 |
| `CapsuleEditor(capsuleId?, placeId?)` | `CAPSULE_EDITOR` | `capsule_editor` | Kuikly | `capsule_editor` | `capsuleId: String?`、`placeId: String?` | 可运行 | 可运行 |
| `CapsuleDetail(capsuleId)` | `CAPSULE_DETAIL` | `capsule_detail` | Kuikly | `capsule_detail` | `capsuleId: String`，必填、非空白 | 可运行 | 可运行 |
| `Timeline` | `TIMELINE` | `app_shell` | Kuikly | `app_shell` | `initialRootTab=timeline` | 可运行 | 可运行 |
| `Gallery` | `GALLERY` | `gallery` | Kuikly | `gallery` | 无 | 可运行 | 可运行；兼容入口 |
| `Favorites` | `FAVORITES` | `favorites` | Kuikly | `favorites` | 无 | 可运行 | 可运行 |
| `Profile` | `PROFILE` | `app_shell` | Kuikly | `app_shell` | `initialRootTab=profile` | 可运行 | 可运行 |
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

二级页返回指定根目标必须调用 `backToRoot(AppRootTab)`：先把目标 Tab 交给仍存活的 AppShell，再 typed `backTo` canonical `app_shell`；若壳缺失，fallback `replace` 携带 `initialRootTab` 恢复正确根页。带必填参数的详情页或原生页不能依赖 `backTo` 重建。

典型行为：

```text
[app_shell(initial=home), settings] -- back() --> [app_shell(selected=home)]
[app_shell(selected=profile), capsule_detail]
    -- backToRoot(RECORD) --> [app_shell(selected=timeline)]
[settings] -- backToRoot(EXPLORE)
    --> replace missing target --> [app_shell(initial=home)]
```

首次引导仍使用 typed replace，但最终根目标是 AppShell：

```text
[launch_gate] -- replace(Onboarding) --> [onboarding]
[launch_gate] -- replace(Home)       --> [app_shell(initial=home)]
[onboarding] -- backToRoot(EXPLORE)  --> [app_shell(selected=home)]
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

## 5. AppShell 与 Settings 当前语义

- Bottom Navigation 只由 `AppShellPage` 创建一次，Home/Record/Profile 根内容常驻同一无手势 HorizontalPager。
- 点击其他根 Tab 只调用 `animateScrollToPage`；重复点击当前 Tab 是 no-op，不发送 push、replace 或 back。
- Place/Capsule Detail、Editor、Settings 位于 AppShell 外，不显示底栏；普通 `back()` 返回同一壳并保留原 Tab 状态。
- 二级页需要定向回根时使用 `backToRoot(AppRootTab)`，不得直接 `backTo(HOME/TIMELINE/PROFILE)`。
- Settings 只保留正式产品入口；`Replace Settings`、多实例栈按钮和路由验收文案已经移除。
- Router Diagnostics 与 Image Adapter Diagnostics 不属于 `AppRoute`，正式 UI 不得进入。

验收以 `P0_APP_SHELL_ACCEPTANCE.md` 为准，重点验证重复点击、快速切换、三个根状态保留、二级页底栏隐藏和系统返回不回退旧 Tab host。

Android/HarmonyOS 均把 `LaunchGate` 设为系统无参启动根页。全新安装最终进入 Onboarding；存在当前完成版本和有效档案时最终进入 `app_shell(initial=home)`。

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

## 8. T98～T107 地点路由冻结状态

`PlaceList`、`PlaceDetail(placeId)`、`PlaceEditor(placeId?)` 和 `Favorites` 的 shared
routeKey、pageName、参数及 `AppRouteKey` 映射已经通过协议测试，统一路由表状态仍为
“协议占位”，因为本轮没有创建 `@Page`。

HarmonyOS 已镜像：

```text
PLACE_LIST_ROUTE_KEY / PLACE_LIST_PAGE_NAME
PLACE_DETAIL_ROUTE_KEY / PLACE_DETAIL_PAGE_NAME
PLACE_EDITOR_ROUTE_KEY / PLACE_EDITOR_PAGE_NAME
FAVORITES_ROUTE_KEY / FAVORITES_PAGE_NAME
```

该段是 T107 的历史冻结状态。T119 已创建四个 shared `@Page`，T120 已将目标加入
`HarmonyRouteCatalog.isAvailableKuiklyPage`。Android 已通过本轮编译与测试；HarmonyOS 代码已完成注册，
但尚未完成本轮 HAP 复编，因此不能标为已验证可运行。Guard 现在要求四个已注册页面必须放行，未知 Kuikly
pageName 仍必须拦截。

参数约束：

- `PlaceDetail(placeId)`：`placeId` 必填且不能是空白字符串。
- `PlaceEditor(null)`：新建模式，不发送 `placeId`。
- `PlaceEditor(placeId)`：编辑模式，发送非空白 `placeId`。
- `PlaceList/Favorites`：无业务参数。
- 带参数详情页不得依赖缺栈 `backTo` 重建；必须使用完整 typed route。

T121～T130 的地点栈回归已覆盖：

```text
[app_shell(initial=home)] -> push(PlaceList)
       -> push(PlaceDetail(placeId))
       -> push(PlaceEditor(placeId))
       -> back() -> PlaceDetail
       -> backTo(PLACE_LIST) -> PlaceList
```

Android 与 HarmonyOS Request Decoder 都验证 `placeId` 原样进入 pageData；业务页面只构造
`AppRoute.PlaceDetail/PlaceEditor`，不直接传 `place_detail`、`place_editor` 或平台 URL。

## 15. 2026-07-27 Record Flow 路由接入

Capsule Editor、Capsule Detail 与兼容 Gallery route/page 已注册为真实 Kuikly 页面；Timeline 的正式入口已迁入 `app_shell` Record 根内容。共享路由仍只传 `placeId` / `capsuleId`，不传 Capsule 或图片对象。

当前主链路是：AppShell/Place Detail `push` Editor → 发布后 `replace` Capsule Detail → Detail 调用 `backToRoot(RECORD)` 返回同一 AppShell；Timeline/Gallery 在 Record 根内容内只切 Compose 状态，不产生路由动作；内容卡再 `push` Capsule Detail。删除成功同样定向返回 Record 根。

独立 `Gallery` route/page 暂作兼容入口，不是正式产品层级。双端最新构建与设备验收状态以 `CURRENT_STATE.md` 和 `P0_APP_SHELL_ACCEPTANCE.md` 为准。
