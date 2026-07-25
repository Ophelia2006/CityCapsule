# CityCapsule 地点、搜索、筛选与收藏

> 协议版本：1  
> 冻结日期：2026-07-23  
> 实现更新：2026-07-25  
> 当前范围：T98～T130，协议、Repository、共享页面、双端持久化、容错与设备验收已完成。

## 1. 阶段边界

本阶段冻结一个单设备、离线、最多 500 条记录的地点目录。它不代表地图 POI
数据库，也不读取账号、网络身份、系统位置或平台文件。

包含：

- `Place v1`、地点分类、草稿、目录、校验和规范化。
- 地点目录和收藏 ID 的稳定 JSON Codec。
- 两个 typed MMKV Key 和 Repository 接口。
- 纯 shared 搜索、排序和筛选引擎。
- `PlaceList/PlaceDetail/PlaceEditor/Favorites` typed route 验证。

不包含：

- GPS、定位权限、地图 SDK、路线规划或在线 POI 搜索。
- 图片、附件、云同步、搜索历史和推荐算法。
- 地点与 Capsule 的关系或级联约束。

`MapExplore` 仍是协议占位，不得在本阶段增加入口。

## 2. Place v1

```kotlin
data class Place(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val city: String,
    val district: String?,
    val category: PlaceCategory,
    val address: String?,
    val tags: List<String>,
    val note: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
```

| 字段 | 约束 |
| --- | --- |
| `schemaVersion` | 当前只接受 `1` |
| `id` | trim 后 1～64 字符，只允许字母、数字、`_`、`-` |
| `name` | trim 后必填，最多 60 字符 |
| `city` | trim 后必填，最多 30 字符 |
| `district` | 可空，最多 30 字符 |
| `category` | `landmark/culture/food/nature/shopping/other` |
| `address` | 可空，最多 120 字符 |
| `tags` | 去空、按忽略大小写去重，最多 8 个；单项最多 16 字符 |
| `note` | 可空，最多 300 字符 |
| `createdAtEpochMs` | 非负 |
| `updatedAtEpochMs` | 不得早于创建时间 |

`PlaceDraft` 不带 ID 和时间。Repository 在创建成功时生成稳定 ID 和两个时间字段，
因此页面不能自行拼接 ID 或信任设备输入的更新时间。

地点对象不得携带：

- Android URI、HarmonyOS URI、地图 SDK 对象或平台文件路径。
- 图片/附件二进制。
- 账号、Token、远端用户 ID。
- 本阶段未使用的经纬度和远端 POI ID。

## 3. 目录和收藏 Codec

地点目录 wire 结构：

```json
{
  "schemaVersion": 1,
  "seedVersion": 1,
  "places": []
}
```

规则：

1. 目录最多 500 条。
2. ID 必须唯一；任一地点非法时整个目录解码失败，不向业务交付半合法目录。
3. 编码时按 ID 排序，保证稳定 wire 输出。
4. 解码忽略未知字段，但拒绝缺少必填字段、未知分类、重复 ID 和未来 Schema。
5. `seedVersion` 是后续初始化/升级种子数据的版本，不是平台存储迁移版本。

收藏 wire 结构：

```json
{
  "schemaVersion": 1,
  "placeIds": []
}
```

规则：

1. 最多 500 个 ID。
2. 解码时去重，编码时排序。
3. Codec 只校验 ID 格式；是否存在于目录由 Repository 在 T108～T112 校验和清理。
4. 收藏与地点目录分 Key，收藏切换不得重写地点目录。

## 4. typed Storage Key

| Kotlin 常量 | Store | wire key | type | 默认值 |
| --- | --- | --- | --- | --- |
| `AppStorageKeys.Places.CATALOG` | `cc_preferences` | `places.catalog` | `json_object` | 空目录 v1 |
| `AppStorageKeys.Favorites.PLACE_IDS` | `cc_preferences` | `favorites.place_ids` | `json_object` | 空集合 v1 |

两个 Key 都是新业务数据，没有 SharedPreferences/Harmony Preferences 旧来源，不加入
现有主题迁移状态机，也不修改 `CCStorageModule` 协议版本。

`AppStorageKeys` 默认值只服务 typed contract；T108 起由 Repository 区分 Missing、首次种子
初始化、临时失败和确定性损坏，不能把 Missing 无条件解释成用户主动清空。

## 5. Repository 契约

```text
PlaceRepository
  -> getCatalog / getCatalogSnapshot / getPlace
  -> createPlace / updatePlace / deletePlace

FavoriteRepository
  -> getFavoriteIds / isFavorite
  -> setFavorite / toggleFavorite
```

- 全部使用现有 `StorageResult` 和 callback，不增加第二套异步协议。
- `PlaceCatalogSnapshot` 明确标记
  `PERSISTED/INITIALIZED/MEMORY_FALLBACK/RECOVERY_READ_ONLY`。
- `PlaceIdGenerator` 和 `PlaceClock` 可注入，以保证单测可重复。
- T98～T107 只冻结接口；实际 MMKV 读写、种子初始化、串行写和容错属于 T108～T112。
- 重置本地档案和首次引导不得顺带删除地点与收藏。

## 6. 搜索与筛选

`PlaceSearchEngine` 是无平台、无存储副作用的纯函数：

```text
places + favoriteIds + query + filter
  -> PlaceSearchResult
```

搜索字段：

1. 名称。
2. 标签。
3. 城市、区域。
4. 地址。
5. 备注。

查询统一 trim、小写并折叠连续空白；多词查询要求所有词都能在任意可搜索字段中命中。
中文使用直接包含匹配，本阶段不引入拼音和分词库。

排序固定为：

1. 名称完全匹配。
2. 名称前缀。
3. 名称包含。
4. 标签包含。
5. 城市/区域/地址/备注包含。
6. 多字段组合匹配。
7. 同级按更新时间倒序，再按规范化名称和 ID 排序。

筛选规则：

- 多分类内部是 OR。
- 分类、城市、区域、仅收藏之间是 AND。
- 城市和区域执行 trim 后忽略大小写的精确匹配。
- 空查询返回筛选后的地点，并按更新时间倒序。
- 搜索和筛选状态只属于页面内存，不写入 MMKV。

## 7. 路由状态

shared 已有并继续冻结：

| AppRoute | routeKey | pageName | T130 状态 |
| --- | --- | --- | --- |
| `PlaceList` | `place_list` | `place_list` | Android/HarmonyOS 可运行 |
| `PlaceDetail(placeId)` | `place_detail` | `place_detail` | Android/HarmonyOS 可运行 |
| `PlaceEditor(placeId?)` | `place_editor` | `place_editor` | Android/HarmonyOS 可运行 |
| `Favorites` | `favorites` | `favorites` | Android/HarmonyOS 可运行 |

`PlaceDetail` 必须携带非空白 `placeId`；`PlaceEditor(null)` 表示新建，非空 `placeId`
表示编辑。

T119 已创建四个 shared `@Page`，T120 已将它们加入
`HarmonyRouteCatalog.isAvailableKuiklyPage`。Android 与 HarmonyOS Dispatcher 均只消费
typed route 解析结果，`placeId` 通过 pageData 传输，不在平台层重复解析业务模型。

## 8. 依赖关系

```text
shared 地点页面
  -> StateHolder
     -> PlaceSearchEngine（纯函数）
     -> PlaceRepository / FavoriteRepository
        -> AppStorageKeys
        -> KeyValueStore
           -> KuiklyKeyValueStore
           -> InMemoryKeyValueStore（测试）
```

平台层只负责 typed Key 的字符串持久化和路由分发，不解析 Place JSON，不复制搜索规则。

## 9. T98～T107 完成记录

| 任务 | 完成内容 | 状态 |
| --- | --- | --- |
| T98 | 冻结离线、小目录、无定位/地图/网络的范围与隐私边界 | 完成 |
| T99 | 完成 Place v1、PlaceDraft、PlaceCatalog 和六种稳定分类 | 完成 |
| T100 | 完成字段、标签、ID、时间和目录级校验/规范化 | 完成 |
| T101 | 完成地点目录与收藏 ID Codec、稳定排序和损坏拒绝 | 完成 |
| T102 | 冻结 `places.catalog`、`favorites.place_ids` wire 结构 | 完成 |
| T103 | 注册两个 typed `cc_preferences` Key | 完成 |
| T104 | 冻结 Place/Favorite Repository、快照、时钟和 ID 生成接口 | 完成 |
| T105 | 完成纯搜索、相关性排序和组合筛选引擎 | 完成 |
| T106 | 验证四条 typed route；冻结 HarmonyOS 常量并保持未实现页面拦截 | 完成 |
| T107 | 完成 shared 单测、统一路由/存储和本规范文档 | 完成 |

## 10. 第一轮验收

自动化命令：

```powershell
.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest
```

2026-07-23 自动化结果：

- shared JVM：98 个测试，0 failure，0 error；本轮新增 15 个地点、收藏、搜索、
  Storage Key 和路由协议用例。
- Android JVM：31 个测试，0 failure，0 error。
- HarmonyOS 本地：38 个测试，0 failure，0 error；新增断言确认四个已冻结页面在
  `@Page` 落地前仍被 Guard 拦截。
- `git diff --check` 无空白错误，仅提示现有 Windows CRLF 转换策略。

本轮没有页面、业务入口或平台存储实现，因此：

- 不要求 Android 模拟器/真机或 HarmonyOS 真机人工验收。
- 不要求安装 APK/HAP。
- 不要求重新 link HarmonyOS arm64 产物；进入第二轮并需要运行页面前再执行。
- 不得手工打开四个地点 pageName；它们仍是协议占位。

## 11. T108～T120 Repository 与共享页面

| 任务 | 完成内容 | 状态 |
| --- | --- | --- |
| T108 | 提供 8 条稳定、离线、可重复写入的地点种子数据 | 完成 |
| T109 | `LocalPlaceRepository` 接入 typed `KeyValueStore`，区分 Missing、持久化、内存降级和只读恢复 | 完成 |
| T110 | 地点新建、编辑、删除、稳定 ID/时间与串行写入 | 完成 |
| T111 | `LocalFavoriteRepository` 完成幂等收藏、切换和不存在地点拒绝 | 完成 |
| T112 | 删除地点后尽力清理收藏；过期收藏在读取时隐藏并再次清理 | 完成 |
| T113 | `PlaceListStateHolder` 完成加载、搜索、筛选、收藏和空状态 | 完成 |
| T114 | `PlaceDetailStateHolder` 完成详情、收藏和删除闭环 | 完成 |
| T115 | `PlaceEditorStateHolder` 完成新建/编辑、校验、保存和未保存确认 | 完成 |
| T116 | 新增 `AppChoiceChip` 与地点摘要卡，全部使用 shared 语义令牌 | 完成 |
| T117 | `PlaceListPage` 与 `FavoritesPage` 落地 | 完成 |
| T118 | `PlaceDetailPage` 与 `PlaceEditorPage` 落地 | 完成 |
| T119 | 四个地点页面完成 `@Page` 注册与 typed `placeId` 读取 | 完成 |
| T120 | Home 增加“浏览地点/我的收藏”，HarmonyOS Guard 放行已注册页面 | 完成 |

运行时依赖关系：

```text
Home
  -> AppRoute.PlaceList / AppRoute.Favorites
  -> Android Dispatcher 或 HarmonyOS Dispatcher
  -> shared @Page
  -> StateHolder
  -> LocalPlaceRepository / LocalFavoriteRepository
  -> KuiklyKeyValueStore
  -> CCStorageModule
  -> Android MMKV / HarmonyOS MMKV
```

## 12. T121～T130 持久化、容错与交付

| 任务 | 完成内容 | 状态 |
| --- | --- | --- |
| T121 | Android 设备用例验证 `places.catalog` 与 `favorites.place_ids` 经 MMKV 内存缓存清空和重开后保持 | 完成 |
| T122 | HarmonyOS 真机用例验证相同 Key、值类型元数据和 MMKV 重开 | 完成 |
| T123 | 双端完成态冷启动保留地点/收藏；Android 验证 LaunchGate 最终栈只留 Home | 完成 |
| T124 | 覆盖目录损坏只读恢复、收藏损坏拒绝覆盖、初始化失败内存降级和写失败保留旧目录 | 完成 |
| T125 | 覆盖列表、收藏、详情、新建、编辑、删除、未保存确认和错误状态 | 完成 |
| T126 | 覆盖 `PlaceList -> PlaceDetail -> PlaceEditor` 双端栈与 `placeId` pageData 透传 | 完成 |
| T127 | 地点业务禁止 MMKV、原始 pageName、原始 wire key 和视觉硬编码；架构守卫通过 | 完成 |
| T128 | Android APK/测试 APK、HarmonyOS arm64 shared、主 HAP/测试 HAP 构建成功 | 完成 |
| T129 | Android 模拟器 6/6、HarmonyOS 真机 6/6 设备自动化通过 | 完成 |
| T130 | 更新地点规范、统一路由、统一存储、设计系统和验收流程 | 完成 |

确定性损坏不会被种子数据覆盖：目录进入 `RECOVERY_READ_ONLY`，页面仍可显示错误说明，
但禁用会改写数据的收藏操作。临时读失败使用内存种子并显示警告；它不伪装成“已经持久化”。
写失败保持旧 MMKV 值和当前输入，页面必须显示错误，禁止误报成功。

## 13. 2026-07-25 自动化结果

- shared JVM：114/114，0 failure，0 error。
- Android JVM：34/34，0 failure，0 error。
- HarmonyOS 本地：40/40，构建退出码 0。
- Android `Pixel_10_Pro` API 17 模拟器：6/6，0 failure，0 error。
- HarmonyOS USB 真机：6/6，0 failure，0 error。
- `linkDebugSharedOhosArm64`、Android debug APK、Android androidTest APK、
  HarmonyOS debug signed HAP 与 ohosTest signed HAP 均构建成功。
- `git diff --check` 无空白错误；仅保留仓库既有的 Windows LF/CRLF 提示。

构建期间仍会显示 Kuikly KSP 版本提示、Kotlin/Native C 初始化器 warning、
HMRouter 生成代码兼容级别提示和依赖中的 ArkTS warning；本轮没有把 warning 隐藏或升级为错误。

## 14. 人工验收流程

### 14.1 验收前准备

1. Android 模拟器/真机保持在线；HarmonyOS 真机解锁、亮屏、允许 USB 调试。
2. 如果 shared Kotlin 有改动，HarmonyOS 验收前必须重新链接并复制最新产物：

```powershell
$env:DEVECO_STUDIO_HOME='D:\Software\Office\DevEcoStudio\DevEco Studio'
$env:OHOS_SDK_HOME="$env:DEVECO_STUDIO_HOME\sdk\default\openharmony"
.\gradlew.bat -c settings.ohos.gradle.kts :shared:linkDebugSharedOhosArm64

Copy-Item .\shared\build\bin\ohosArm64\debugShared\libshared.so `
  .\ohosApp\entry\libs\arm64-v8a\libshared.so -Force
Copy-Item .\shared\build\bin\ohosArm64\debugShared\libshared_api.h `
  .\ohosApp\entry\src\main\cpp\libshared_api.h -Force
```

3. Android 执行：

```powershell
.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest `
  :androidApp:connectedDebugAndroidTest :androidApp:assembleDebug
```

4. HarmonyOS 在 DevEco Studio 依次运行 `entry@default` 的本地测试、构建主 HAP，
   再运行 `entry@ohosTest`。命令行设备测试的核心命令为：

```powershell
hdc shell aa test -b com.y.citycapsule -m entry_test `
  -s unittest OpenHarmonyTestRunner -s timeout 60000
```

输出必须包含 `Tests run: 6, Failure: 0, Error: 0, Pass: 6`。

### 14.2 首次初始化、浏览和搜索

1. 清除应用数据后启动；完成首次引导进入 Home。
2. 点击“浏览地点”。首次进入应显示“已准备 8 个离线示例地点”，列表共 8 条。
3. 搜索“博物馆”，应命中“上海博物馆”和“中国茶叶博物馆”。
4. 城市填“上海”、区域填“徐汇区”，应只显示该组合下的地点。
5. 切换“文化”分类、城市/区域和“只看收藏”，确认不同维度之间是 AND；
   点击“清除筛选”恢复全部地点。
6. 输入不存在的关键字，应显示无匹配状态，不应白屏或崩溃。

### 14.3 收藏与详情

1. 在“上海博物馆”卡片点击收藏，提示“已加入收藏”。
2. 回 Home 打开“我的收藏”，只应显示已收藏地点。
3. 在收藏页取消收藏，该卡片应立即从当前列表消失。
4. 回地点列表点击“查看详情”，城市、区域、地址、标签、备注均应正确。
5. 详情页切换收藏后返回收藏页，状态必须一致。

### 14.4 新建、编辑、删除与路由栈

1. 地点列表点击“新建地点”，直接保存空表单；应显示必填/校验错误且不写入数据。
2. 填写名称“验收咖啡馆”、城市“上海”、分类“美食”、标签“咖啡，安静”并保存；
   应进入新地点详情。
3. 点击“编辑地点”，修改名称或备注并保存；返回详情后应自动刷新并立即显示新值。
4. 编辑任意字段后点击“放弃并返回”，应出现未保存确认；确认后不落盘。
5. 在详情页删除地点并确认，应返回地点列表；再次搜索该名称无结果，收藏中也不得残留。
6. 连续执行：

```text
Home -> PlaceList -> PlaceDetail -> PlaceEditor
```

依次使用返回键，应逐层回退，不得跳转到错误页面；列表、详情和编辑页不得进入路由降级页。

### 14.5 进程重启与冷启动

1. 新建一个地点并收藏，记住名称。
2. 从系统任务列表划掉应用，或在 IDE 中 Stop 后重新运行；不要清除应用数据。
3. 冷启动应先由 LaunchGate 决策并最终进入 Home，不应停留在 LaunchGate。
4. 再次打开地点列表和收藏页：新建地点、编辑值和收藏状态必须全部保留。
5. 切换主题后重复一次冷启动，确认地点数据与主题互不覆盖。
6. 最后清除应用数据并重启：自建地点与收藏应被清除，首次进入地点列表重新初始化 8 条种子。

### 14.6 容错专项

正常产品验收不应直接修改 MMKV。需要做专项恢复验证时，使用测试构建注入以下状态：

| 注入 | 预期 |
| --- | --- |
| `places.catalog` 为非法 JSON/错误类型 | 显示只读恢复错误，不用种子覆盖坏值，不允许收藏改写 |
| 地点目录临时读取失败 | 显示内存示例与警告，不宣称已持久化 |
| 地点写入失败 | 旧目录保留，当前表单保留，显示保存失败 |
| `favorites.place_ids` 损坏 | 地点仍可浏览，收藏状态显示不可用，坏值不被覆盖 |
| 收藏包含已删除地点 ID | UI 隐藏过期项，并尽力清理；清理失败也不得重新显示 |

自动化已覆盖以上状态；人工只需在需要验证错误文案或禁用态视觉时执行。

### 14.7 通过标准

- 双端设备自动化均为 6/6，主包安装成功且无启动崩溃。
- 地点 CRUD、搜索、筛选和收藏在 Android/HarmonyOS 行为一致。
- 杀进程重启后数据保留；清除应用数据后重新初始化。
- 任何失败都不得误报成功、覆盖确定性坏数据或产生业务层字符串跳转。
- Light/Dark 下文字、按钮、筛选 Chip、错误/空状态可读，长文本可滚动，触控区域可用。
