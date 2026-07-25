# CityCapsule 统一存储协议

> 协议版本：1
> 冻结日期：2026-07-23
> 实现更新：2026-07-25
> 实现范围：T17～T44 完成双端 MMKV；T73～T97 完成本地档案与首次引导；T98～T130 完成有上限的地点目录、收藏、双端持久化与容错。

## 1. 数据边界

当前可运行业务包括 Home、Settings、本地档案、首次引导、离线地点目录、搜索筛选和收藏。
地点目录最多 500 条，继续使用 MMKV 2.4.0 作为轻量键值存储，不引入数据库；
SharedPreferences 与 Harmony Preferences 只作为只读旧数据源参与一次性迁移，不再承接新业务写入。

MMKV 只用于小型、可明确键控的数据：

- `cc_meta`：Schema Version 和迁移状态，仅允许平台迁移层访问。
- `cc_preferences`：非敏感、需要长期保留的用户设置。
- `cc_cache`：可以重新生成并允许主动清理的缓存。

以下数据不得直接写入本协议：

- 图片、视频、文件内容和无明确数量上限的大型列表。
- 超过 500 条、需要关系约束、地理空间查询或并发写入的地点/胶囊主数据；这类数据必须迁移到后续数据库方案。
- Token、密码、密钥以及其他需要 Keystore/HUKS 保护的敏感信息。

## 2. 已冻结 Key 表

| Kotlin 常量 | Store | wire key | type | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `AppStorageKeys.Settings.THEME_MODE` | `cc_preferences` | `settings.theme_mode` | `string` | `system` | 主题模式：`system/light/dark` |
| `AppStorageKeys.Profile.LOCAL_PROFILE` | `cc_preferences` | `profile.local_profile` | `json_object` | 默认本地档案 | 单用户本地档案 v1 |
| `AppStorageKeys.Onboarding.COMPLETED_VERSION` | `cc_preferences` | `onboarding.completed_version` | `long` | `0` | 已完成的首次引导版本 |
| `AppStorageKeys.Onboarding.DRAFT` | `cc_cache` | `onboarding.draft` | `json_object` | 空草稿 | 可清理、可重新生成的引导中间状态 |
| `AppStorageKeys.Places.CATALOG` | `cc_preferences` | `places.catalog` | `json_object` | 空目录 v1 | 最多 500 条的离线地点目录 |
| `AppStorageKeys.Favorites.PLACE_IDS` | `cc_preferences` | `favorites.place_ids` | `json_object` | 空集合 v1 | 与地点目录分离的收藏 ID |

约束：

1. Key 只能在 `AppStorageKeys` 注册。
2. namespace 和 name 必须使用小写 snake_case。
3. 完整 Key 为 `namespace.name`，业务层不得传裸字符串。
4. 新增 Key 必须同时评审 Android/HarmonyOS 迁移影响并更新本表。

## 3. Bridge 协议

模块名固定为 `CCStorageModule`，协议版本固定为 `1`。

| 方法 | 作用 |
| --- | --- |
| `storageGet` | 读取单个 Key |
| `storagePut` | 写入单个 Key |
| `storageRemove` | 删除单个 Key |
| `storageContains` | 判断 Key 是否存在 |
| `storageGetMany` | 批量读取 |

单 Key 请求字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `protocolVersion` | number | 当前固定为 `1` |
| `store` | string | `cc_meta/cc_preferences/cc_cache` |
| `key` | string | 稳定 wire key |
| `type` | string | `string/boolean/long/double/json_object` |
| `value` | string | 仅写入时携带；始终是规范化字符串 |

统一响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | number | `0` 成功、`1` 不存在、`1001+` 错误 |
| `message` | string | 脱敏后的诊断信息 |
| `exists` | boolean | Key 是否存在 |
| `value` | string | 存在且读取成功时返回 |
| `entries` | array | 批量读取的逐项结果 |

所有值跨 Kuikly Bridge 时都使用字符串：Boolean 使用 `true/false`，Long 使用十进制字符串，Double 禁止 `NaN/Infinity`，JSON 必须是对象。这避免 ArkTS Number 对大整数的精度损失。

## 4. 错误码

| code | shared 枚举 | 语义 |
| --- | --- | --- |
| `1001` | `INVALID_REQUEST` | 请求字段或待写值非法 |
| `1002` | `NOT_INITIALIZED` | 平台 MMKV 尚未就绪 |
| `1003` | `TYPE_MISMATCH` | 已存类型与 Key 声明不一致 |
| `1004` | `DECODE_FAILED` | 已存值无法按声明类型解码 |
| `1005` | `NATIVE_FAILURE` | 平台读写失败或未返回结果 |
| `1006` | `UNKNOWN_METHOD` | 平台不支持该方法 |
| `1999` | `UNKNOWN` | 未识别错误 |

## 5. shared 依赖关系

```text
业务页面
  -> SettingsRepository / LocalProfileRepository / OnboardingRepository
     / PlaceRepository / FavoriteRepository
  -> KeyValueStore
     -> KuiklyKeyValueStore
     -> InMemoryKeyValueStore（测试）
  -> KuiklyStorageModule
  -> CCStorageModule
     -> AndroidStorageDispatcher / HarmonyStorageDispatcher
     -> AndroidMmkvStorage / HarmonyMmkvStorage
     -> MMKV 2.4.0

应用启动
  -> AndroidStorageMigrator / HarmonyStorageMigrator
     -> 读取旧 city_capsule_settings/theme_mode
     -> 幂等写入 cc_preferences/settings.theme_mode
     -> 最后提交 cc_meta/schema + completed
```

- `StorageContract.kt`：Store、类型、Key、Result、Error 和批量结果。
- `StorageCodecs.kt`：稳定字符串编码规则。
- `AppStorageKeys.kt`：唯一 Key 注册表。
- `KeyValueStore.kt`：业务接口和内存 Fake。
- `StorageProtocol.kt`：双端必须镜像的 wire 常量和 JSON envelope。
- `KuiklyKeyValueStore.kt`：typed API 到 Kuikly Bridge 的转换。
- `SettingsRepository.kt`：首个业务 Repository，不暴露 Key 和 Bridge。
- `core/profile/*`：LocalProfile v1、预设头像、校验、Codec 与 Repository。
- `core/onboarding/*`：草稿、完成版本、启动决策、提交恢复与 Repository。
- `core/place/*`：Place v1、目录 Codec、校验、Repository 契约及纯搜索筛选。
- `core/favorite/*`：收藏 ID Codec 与 Repository 契约。
- `StorageMigrationContract.kt`：双端迁移必须镜像的 Schema、状态、重试和旧 Key 契约。

## 6. 平台实现

### 6.1 Android

- `KRApplication.onCreate()` 在任何 Kuikly 页面创建前初始化 MMKV。
- `AndroidMmkvStorage` 以单进程模式打开 `cc_meta`、`cc_preferences`、`cc_cache` 三个实例；初始化失败时保留错误状态，不向业务层抛出崩溃。
- `AndroidStorageDispatcher` 校验协议版本、Store、Key 和类型，完整实现五个 Bridge 方法。
- `KRStorageModule` 以 `CCStorageModule` 注册到 `KuiklyHostActivity`，回调只返回统一 JSON envelope。

### 6.2 HarmonyOS

- `EntryAbility.onCreate()` 使用 `ApplicationContext` 初始化 MMKV。
- `HarmonyMmkvStorage` 同样以单进程模式打开三个固定实例，并向 Dispatcher 暴露就绪状态。
- `HarmonyStorageDispatcher` 与 Android 保持相同的请求校验、五个方法和错误码。
- `KRStorageModule` 在 `KuiklyViewDelegate` 中以 `CCStorageModule` 注册，回调返回统一 JSON envelope。

### 6.3 类型元数据

平台层在同一 MMKV 实例内使用保留 Key `__cc_type__.<wire-key>` 保存值类型。业务层不得声明或访问 `__cc_type__.` 前缀；读取时若已存类型与请求类型不同，返回 `TYPE_MISMATCH`，删除业务 Key 时同步删除类型元数据。

## 7. T25～T34 完成清单

| 任务 | 完成内容 | 验收状态 |
| --- | --- | --- |
| T25 | Android 引入 MMKV 2.4.0，并在 Application 初始化 | 完成 |
| T26 | Android 三 Store Provider 与字符串存取适配 | 完成 |
| T27 | Android 五方法 Dispatcher、参数校验与错误映射 | 完成 |
| T28 | Android `CCStorageModule` 注册到 Kuikly Host | 完成 |
| T29 | Android JVM 测试、仪器测试与真实模拟器持久化验证 | 完成 |
| T30 | HarmonyOS 引入 MMKV 2.4.0，并在 EntryAbility 初始化 | 完成 |
| T31 | HarmonyOS 三 Store Provider 与字符串存取适配 | 完成 |
| T32 | HarmonyOS 五方法 Dispatcher、参数校验与错误映射 | 完成 |
| T33 | HarmonyOS `CCStorageModule` 注册到 Kuikly Delegate | 完成 |
| T34 | HarmonyOS 单元测试、ohosTest 持久化测试 HAP；Settings 接入双端共享验收入口 | 完成，设备 3/3 通过 |

迁移状态机不属于 T25～T34；本轮只为后续迁移预留 `cc_meta`，不得在没有迁移方案和回滚测试时写入迁移标记。

## 8. 迁移协议

### 8.1 迁移源和目标

| 平台 | 只读旧 Store | 旧 Key | MMKV 目标 |
| --- | --- | --- | --- |
| Android | SharedPreferences `city_capsule_settings` | `theme_mode` | `cc_preferences/settings.theme_mode` |
| HarmonyOS | Preferences `city_capsule_settings` | `theme_mode` | `cc_preferences/settings.theme_mode` |

只接受字符串 `system/light/dark`，迁移前执行 trim 和小写规范化。旧值缺失或非法时不写目标值，业务使用 `system` 默认值；这属于正常完成，不进入失败重试。

### 8.2 `cc_meta` 保留项

| Key | 说明 |
| --- | --- |
| `storage.schema_version` | 当前为 `1` |
| `storage.migration_state` | `not_started/running/completed/failed` |
| `storage.migration_attempts` | 启动迁移尝试次数 |
| `storage.migration_last_error` | 固定、脱敏的失败原因码 |

迁移顺序固定为：写 `running` 和 attempts → 读取旧值 → 写目标类型和值 → 回读校验 → 清理旧 Key（best effort）→ 写 schema → 最后写 `completed`。进程在任一步退出，下次启动均可幂等重试。

### 8.3 冲突、重试和降级

- MMKV 目标已存在时始终保留目标值，只补齐类型元数据，旧值不得覆盖新值。
- 迁移写入或校验失败时记录 `failed`，下次启动重试；最多 3 次，之后返回 `retry_exhausted`，避免启动死循环。
- MMKV 或旧 Preferences 不可用时不阻塞应用启动。
- Dispatcher 对畸形 JSON 返回 `INVALID_REQUEST`，不再误报 `NATIVE_FAILURE`。
- Settings 通过 `getThemeModeSnapshot()` 读取；缺失使用默认值，存储失败或值损坏时安全回退“跟随系统”，同时保留 warning 供日志诊断。
- Settings 已移除测试态“MMKV 持久化验收”文案，正式显示为“主题偏好/切换主题偏好”。

## 9. T35～T44 完成清单

| 任务 | 完成内容 | 验收状态 |
| --- | --- | --- |
| T35 | 冻结 Schema 1、迁移状态、重试上限和旧 Key | 完成 |
| T36 | shared 迁移契约、旧主题规范化与契约测试 | 完成 |
| T37 | Android SharedPreferences 旧数据适配器 | 完成 |
| T38 | Android 幂等迁移器、冲突策略、失败记录与 3 次熔断 | 完成 |
| T39 | Android Application 启动接入与真实设备迁移测试 | 完成，设备 2/2 通过 |
| T40 | HarmonyOS Preferences 旧数据适配器（兼容 API 12） | 完成 |
| T41 | HarmonyOS 镜像迁移器、冲突策略、失败记录与 3 次熔断 | 完成 |
| T42 | HarmonyOS EntryAbility 启动接入与真实设备迁移测试 | 完成，设备 3/3 通过 |
| T43 | Repository 业务降级、Settings 正式切换、Dispatcher 畸形请求容错 | 完成 |
| T44 | 双端测试、构建产物、迁移协议与验收文档 | 完成 |

## 10. 2026-07-22 自动化验收记录

- shared JVM：29 个测试，0 failure，0 error。
- Android JVM：22 个测试，0 failure，0 error；迁移器 5 个核心分支均覆盖。
- Android `Pixel_10_Pro(AVD) - 17`：2 个设备测试通过，覆盖 MMKV 重开持久化和 SharedPreferences→MMKV 真实迁移。
- HarmonyOS 本地单元测试：29 个测试通过，迁移器 5 个核心分支均覆盖。
- HarmonyOS 设备 `4QE0225918003015`：3 个 ohosTest 通过，覆盖基础 Runner、MMKV 重开持久化和 Preferences→MMKV 真实迁移。
- Android Debug/Test APK、HarmonyOS Debug/ohosTest HAP 均重新构建并签名成功。

## 11. 第三轮验收流程

### 11.1 自动化门禁

1. 执行 shared 与 Android JVM 测试：
   `gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest`
2. 期望：shared `29/29`、Android `22/22`，failure/error 均为 0。
3. Android 设备在线后执行：
   `gradlew.bat :androidApp:connectedDebugAndroidTest`
4. 期望：`Starting 2 tests`，最终 `Finished 2 tests` 且 `BUILD SUCCESSFUL`。
5. DevEco Studio 执行 entry 本地测试，或使用 Hvigor `entry@default test`。
6. 期望：`Tests run: 29, Failure: 0, Error: 0, Pass: 29`。
7. 在 DevEco Studio 运行 `entry > ohosTest`；或安装主 HAP、ohosTest HAP 后执行 `aa test`。
8. 期望：`Tests run: 3, Failure: 0, Error: 0, Pass: 3`。

### 11.2 迁移规则白盒验收

逐项核对 Android/HarmonyOS 的迁移单测：

1. 旧值 `" DARK "` 迁移成 `dark`，写入类型元数据并清理旧 Key。
2. MMKV 已有 `light`、旧存储为 `dark` 时最终仍为 `light`。
3. 旧值 `amoled` 时迁移完成但不写目标，业务回落 `system`。
4. 首次目标写失败时状态为 `failed`、attempt 为 1；恢复后第二次成功、attempt 为 2。
5. attempts 已为 3 且状态为 `failed` 时返回 `retry_exhausted`，不再读取旧源。
6. 传入畸形 Bridge JSON，响应必须为 `INVALID_REQUEST(1001)`。

### 11.3 双端业务黑盒验收

1. 全新安装后启动应用，进入 Settings。
2. “主题偏好”应显示“当前偏好：跟随系统”，不得出现 MMKV、Bridge 或错误码等技术文案。
3. 点击“切换主题偏好”，应依次显示“浅色 → 深色 → 跟随系统”。
4. 保存“深色”，从系统任务列表彻底结束应用，再重新启动并进入 Settings。
5. 应继续显示“当前偏好：深色”；仅返回上一页再进入不算冷启动验收。
6. 快速连续进入/退出 Settings，确认无闪退、无卡死、无主题被旧值覆盖。
7. Android Logcat 或 HarmonyOS Hilog 过滤 `CityCapsuleStorage`：首次正常启动应出现 `outcome=completed`，后续启动应出现 `already_completed`。
8. 全流程不得出现 `NOT_INITIALIZED`、`TYPE_MISMATCH`、`NATIVE_FAILURE`，也不得因旧 Preferences 异常阻塞首屏。

## 12. T73～T80 本地档案与首次引导数据骨架

本轮没有修改 `CCStorageModule`、Bridge 版本或平台 Dispatcher。新 Key 使用现有 `json_object/long/getMany` 能力，Android 与 HarmonyOS 不需要增加协议分支。

完成规则：

1. `LocalProfile` 只保存昵称、预设头像、可选城市和简介，不保存账号、Token、图片、文件路径。
2. 启动快照通过一次 `getMany` 读取完成版本、档案和草稿。
3. 引导提交必须先写档案，最后写完成版本；完成后草稿清理为 best effort。
4. 清除状态必须先删完成版本，再删档案和草稿。
5. 只有确定性损坏允许自动删除；临时 MMKV 错误不得破坏持久数据。
6. 三个新 Key 没有旧平台来源，不加入现有主题迁移器。

详细模型、决策矩阵、任务记录和第一轮验收见 `docs/LOCAL_PROFILE_ONBOARDING.md`。

## 13. T81～T89 共享业务闭环

共享页面只通过 Repository 使用既有存储能力：

```text
OnboardingPage -> OnboardingStateHolder -> OnboardingRepository
ProfilePage    -> ProfileStateHolder    -> LocalProfileRepository
                                      -> KuiklyKeyValueStore
                                      -> CCStorageModule v1
                                      -> Android/HarmonyOS MMKV
```

- Onboarding 切换步骤时保存 `onboarding.draft`；保存失败时保留当前内存状态并显示警告，不阻断继续填写。
- 最终提交仍严格执行“档案 → 完成版本 → best-effort 草稿清理”，UI 不自行改变顺序。
- Profile 保存前执行相同的 shared 校验和规范化；取消编辑不写存储。
- 清除确认后执行“完成版本 → 档案 → 草稿”，任一步失败都保留错误状态，不误报清除成功。
- Android 与 HarmonyOS 没有新增 Bridge 方法或平台协议分支。

2026-07-23 第二轮自动化结果：shared 80/80、Android 27/27、HarmonyOS 34/34。状态测试覆盖存储成功、损坏/缺失快照、保存失败内存降级和安全清除。

T90～T97 已把三个 Key 接入双端实际冷启动，但没有增加 Bridge 方法或平台业务判断：Android/HarmonyOS 都先打开 LaunchGate，再由 shared 一次 `getMany` 读取并决策。第三轮回归结果为 shared 83/83、Android JVM 31/31、Android 设备 4/4、HarmonyOS 本地 38/38；HarmonyOS 设备测试 HAP 包含 5 个用例，设备在线后执行。

## 14. T98～T107 地点与收藏数据骨架

本轮新增 `places.catalog` 和 `favorites.place_ids` 两个 `cc_preferences` Key，继续使用
`json_object`、`storageGet/storagePut/storageRemove` 和当前 Bridge 协议，不修改 Android
或 HarmonyOS Dispatcher。

边界：

1. `places.catalog` 只承载最多 500 条的离线小目录，不承载图片、地图对象或关系查询。
2. `favorites.place_ids` 与目录分离，收藏切换不得重写地点目录。
3. 两个 Key 没有旧平台来源，不加入主题迁移器。
4. Codec 拒绝未来 Schema、未知分类、重复地点 ID 和非法字段，但忽略未知可选字段。
5. 平台层不得解析 Place JSON；搜索、筛选和业务校验只在 shared 执行。
6. T107 只冻结 Repository 接口；T108～T130 已完成种子初始化、MMKV 读写、共享页面、容错和设备验证。

完整字段、搜索排序和筛选规则见 `docs/PLACES_SEARCH_FAVORITES.md`。

## 15. T108～T130 地点持久化与恢复语义

地点与收藏没有旧平台数据源，不执行“首次迁移”；它们从 Missing 状态开始，由 shared
Repository 使用现有 Bridge 协议初始化：

```text
LocalPlaceRepository / LocalFavoriteRepository
  -> KuiklyKeyValueStore
  -> storageGet / storagePut / storageRemove
  -> AndroidStorageDispatcher / HarmonyStorageDispatcher
  -> cc_preferences
     -> places.catalog
     -> __cc_type__.places.catalog = json_object
     -> favorites.place_ids
     -> __cc_type__.favorites.place_ids = json_object
```

恢复规则：

1. `places.catalog` Missing：写入稳定的 8 条种子；写成功才标记 `INITIALIZED`。
2. 初始化写失败：返回内存种子和 `MEMORY_FALLBACK`，不伪造持久化成功。
3. 类型不匹配或 JSON 解码失败：返回空的 `RECOVERY_READ_ONLY` 快照，不覆盖坏值。
4. 临时 native 读取失败：返回内存种子并带 warning，允许稍后重新加载。
5. 地点 CRUD 只在完整目录成功写入后回调成功；写失败保持旧目录。
6. 收藏写入与目录写入分离；不存在的地点不能加入收藏。
7. 删除地点后收藏清理是尽力执行；读取收藏时会隐藏并再次清理过期 ID。
8. 确定性损坏、临时失败和 Missing 是三种不同状态，不得互相降格。

2026-07-25 回归结果：

- shared JVM 114/114、Android JVM 34/34。
- Android 模拟器设备测试 6/6；`places.catalog`、`favorites.place_ids`、类型元数据、
  完成态冷启动和主题旧数据迁移均通过。
- HarmonyOS USB 真机测试 6/6；相同 MMKV 重开、完成态标记、地点/收藏和迁移均通过。
- Android APK/androidTest APK 与 HarmonyOS 主 HAP/ohosTest HAP 构建成功。

设备测试会在写入前保存原始值并在 `finally/catch` 路径恢复，避免验收用例污染开发设备数据。
完整人工流程与容错注入矩阵见 `docs/PLACES_SEARCH_FAVORITES.md` 第 14 节。
