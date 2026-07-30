# CityCapsule 渐进式 MVI 架构

## 状态与适用范围

状态：**PlaceList/Explore 首个代码试点已完成；双端设备 Spike 尚未完成，也不是当前全项目架构。**

从 2026-07-30 起，新建 Feature 的表现层默认采用本文定义的轻量 MVI；对现有 Feature 做页面级重构时，应把该 Feature 的 callback 型 `StateHolder` 迁移为 MVI Store。小范围缺陷修复、纯样式调整或平台适配不强制顺手迁移，避免扩大改动范围。现有 Store 按 Feature 渐进替换，不进行全项目一次性重写。

MVI 只约束表现层状态流，不取代现有 Repository、typed navigation、MMKV bridge 或平台 capability，也不等于自动引入完整 Clean Architecture。

## 当前事实与目标

当前表现层：

```text
Kuikly UI
  → 调用 StateHolder 具体方法
  → callback Repository
  → StateHolder.update(nextState)
  → onStateChanged(UiState)
  → UI mutableStateOf
```

目标表现层：

```text
Kuikly UI
  → dispatch(Intent)
Feature Store
  → Executor / Actor
  → Repository / Capability
  → Mutation
  → Pure Reducer
  → StateFlow<UiState>

Feature Store
  → Effect Flow
  → UI 执行 typed navigation、关闭页面或一次性系统交互
```

当前 9 个 `StateHolder` 不因本决策自动变成 MVI；只有完成迁移、测试和双端验证的 Feature 才能标记为 MVI。

## 项目内最小协议

项目采用自有的薄协议，不在技术 Spike 前引入第三方 MVI 框架：

```kotlin
interface MviStore<Intent, State, Effect> {
    val state: StateFlow<State>
    val effects: Flow<Effect>

    fun dispatch(intent: Intent)
    fun dispose()
}
```

各概念职责：

- `Intent`：用户操作、生命周期事件或 UI 对外部结果的回传，例如 `Load`、`QueryChanged`、`Retry`。
- `State`：可以持续重放并完整渲染 Feature 的不可变状态；Loading、Empty、Content、Error/Degraded 必须可由 State 表达。
- `Mutation`：Repository/Capability 异步执行结果产生的内部事件，只在 Store 内可见。
- `Reducer`：`(State, Mutation) -> State` 的纯函数，不访问 Repository、时钟、导航、平台 API 或可变全局状态。
- `Effect`：只消费一次的非持久行为，例如 typed navigation、关闭页面、展示一次性确认或启动需要 UI 参与的系统交互。Effect 不能代替可恢复的错误/加载状态。
- `Executor / Actor`：处理 Intent、副作用与回调适配，并把所有结果重新送入 Mutation 流；不得绕过 Reducer 直接修改 State。

首个 PlaceList 试点采用 `Channel.receiveAsFlow()` 承载 Effect；shared/Android 自动化已验证单次消费，Kuikly 双端前后台与销毁行为仍需设备 Spike，因此该选择尚未自动推广为所有 Feature 的不可变模板。

## 强制架构规则

1. 每个 Feature 一个 Store，不建立全局 Redux Store。
2. UI 只读取 State、发送 Intent、消费 Effect；迁移完成的页面不得继续调用 `store.load()`、`store.toggleFavorite()` 等业务方法。
3. Store 不依赖 `AppNavigator`、Android/ArkTS 类型、Compose `PagerState`、`LazyListState`、Context 或原始路由字符串。
4. 导航 Effect 只携带业务 ID 或 typed route 所需简单值；UI 通过 `AppNavigator` 执行。
5. 所有异步完成、失败和取消必须重新进入串行 Mutation/Reducer 流，不能在 Repository callback 中并发写 State。
6. 同一个 Feature 的 Intent 必须有确定的顺序语义；重复 Load、快速筛选和快速点击不得让旧结果覆盖新状态。
7. Store 必须有明确 owner 和 `dispose()`；Page Store 随 Page/Composition 销毁，AppShell 根 Store 随常驻根内容销毁。取消后不得继续发布 State 或 Effect。
8. `UiState` 不包含 Navigator、Repository、Capability、Compose State、平台对象或回调。
9. `PagerState`、滚动位置、动画进度、布局尺寸、焦点和手势等纯 UI 状态继续留在 Compose；`selectedRootTab`、筛选条件、草稿和业务加载状态等语义状态才进入 Store。
10. 简单单 Repository CRUD 可以由 Store 直接访问 Repository。只有出现真实跨 Repository 规则或可复用业务编排时才新增 UseCase，不创建空 UseCase/DataSource 层凑结构。
11. Repository callback API 当前可以保留；迁移时由 Executor 做受控适配。是否逐步提供 `suspend` 接口需另行评估，不与第一个 MVI Feature 强绑定。
12. 新增 MVI 基础设施应位于 `shared/src/commonMain/.../core/mvi`；Feature 的 `Intent/State/Effect/Mutation/Reducer/Store` 留在对应 `feature/*` 包内。

## 技术 Spike 门禁

在第一个业务 Feature 迁移前，必须完成一个最小、可删除或可复用的技术验证：

1. 在 `shared/commonMain` 显式声明与当前 Kotlin/Kuikly 依赖图兼容的 `kotlinx-coroutines-core`，不得继续依赖传递依赖。
2. 验证 Kuikly Compose 在 Android 与 HarmonyOS 上稳定收集 `StateFlow`，重组不会重复启动 Load。
3. 验证 Effect 在重组、前后台切换和快速连续发送时不会重复消费或静默丢失。
4. 验证 `dispose()` 会取消在途任务，已销毁页面不会再导航或更新 State。
5. 验证 Intent 串行处理以及 latest-load 语义，旧异步结果不能覆盖新查询。
6. shared 单测、Android 编译/测试与 HarmonyOS HAP 构建通过；环境不能完成的设备项必须保持 Partial/Blocked。

Spike 通过前，不批量创建各 Feature 的空 Contract/Store，也不删除现有 StateHolder。

## 第一个迁移 Feature：PlaceList / Explore

PlaceList 作为试点，因为它正处于 Explore UI 重构范围，状态和用户事件清晰、平台副作用少，并已有搜索、筛选、想去与恢复只读状态测试。

建议 Contract：

```text
PlaceListIntent
├─ Load
├─ Retry
├─ QueryChanged(query)
├─ CategoryToggled(category)
├─ CityChanged(city)
├─ DistrictChanged(district)
├─ FavoritesOnlyToggled
├─ ClearFilters
├─ FavoriteToggled(placeId)
├─ PlaceClicked(placeId)
└─ CreatePlaceClicked

PlaceListEffect
├─ NavigateToDetail(placeId)
└─ NavigateToEditor
```

现有 `PlaceListUiState`、搜索规则、Repository 和 storage schema 优先保持不变。首个迁移不顺手改 Place source/坐标/封面，不引入地图、网络、数据库或全局依赖注入。

试点验收至少覆盖：

- Reducer 对每种 Mutation 的纯函数测试。
- Load success、初始化 seed、内存降级、只读恢复和 favorite 读取失败。
- 快速重复 Load/Query 时旧结果不会覆盖新状态。
- favorite busy、防重复点击、成功/失败和数据 invalidation。
- 导航 Effect 每次 Intent 只消费一次，Effect 不写入持久 State。
- 页面重组不重复 Load，销毁后不再接收结果。
- 原有 PlaceList StateHolder 测试能力等价迁移，不以删除测试换取通过。

## 后续迁移顺序

1. `PlaceList / Explore`：首个完整试点。
2. `Home`：聚合多个 Repository，替换当前 callback 嵌套和 `loadGeneration`。
3. `PlaceDetail`。
4. `Profile`。
5. `Timeline / Gallery` 与 `CapsuleDetail`。
6. `PlaceEditor`、`CapsuleEditor`、`Onboarding`：最后迁移，避免首轮同时处理草稿、未保存退出、媒体清理和多阶段提交。
7. `AppShell`：最后评估；Pager、滚动和动画继续属于 Compose UI 状态，不为追求“全 MVI”塞进 Store。

## 单个 Feature 完成标准

一个 Feature 只有同时满足以下条件，才可从 StateHolder 标记为 MVI：

- UI 只通过 `dispatch(Intent)` 驱动业务状态。
- Reducer 纯函数且有覆盖主要 Mutation 的测试。
- Repository/Capability 的成功、失败、取消都回到 Mutation。
- 一次性 Effect 不重复、不写入 State，导航继续走 typed route。
- Store 生命周期和取消行为有测试或可复现的双端验收。
- 原有业务、空态、错误态和返回栈没有回归。
- 更新 `ARCHITECTURE.md`、`CURRENT_STATE.md`、`TODO.md` 和对应 Feature 文档。

## 尚未冻结的实现细节

- `kotlinx-coroutines-core` 的具体版本，需按当前 Kotlin/Kuikly 解析结果选择。
- Effect 使用 SharedFlow 还是 Channel。
- Store Scope/Dispatcher 的跨端构造方式和 Page 生命周期绑定 API。
- Repository 是否、何时增加 suspend adapter。
- 是否需要轻量 Store factory/依赖组装；当前不引入 DI 框架。

这些问题由技术 Spike 用代码和双端构建结果决定，不从附件建议或类名推断。
