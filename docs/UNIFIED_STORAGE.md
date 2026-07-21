# CityCapsule 统一存储协议

> 协议版本：1  
> 冻结日期：2026-07-21  
> 范围：T17～T24，仅包含 shared 协议和骨架；尚未引入双端 MMKV。

## 1. 数据边界

当前业务只有 Home、Settings 和路由诊断能力，代码中没有已落地的数据库、SharedPreferences、Harmony Preferences 或正式缓存实现。本轮只冻结现有页面能够确认的设置项，不提前创建地点、胶囊等尚未实现的数据 Key。

MMKV 只用于小型、可明确键控的数据：

- `cc_meta`：Schema Version 和迁移状态，仅允许平台迁移层访问。
- `cc_preferences`：非敏感、需要长期保留的用户设置。
- `cc_cache`：可以重新生成并允许主动清理的缓存。

以下数据不得直接写入本协议：

- 图片、视频、文件内容和大型列表。
- 地点、胶囊等需要查询和关系约束的主数据。
- Token、密码、密钥以及其他需要 Keystore/HUKS 保护的敏感信息。

## 2. 已冻结 Key 表

| Kotlin 常量 | Store | wire key | type | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `AppStorageKeys.Settings.THEME_MODE` | `cc_preferences` | `settings.theme_mode` | `string` | `system` | 主题模式：`system/light/dark` |

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
  -> SettingsRepository
  -> KeyValueStore
     -> KuiklyKeyValueStore
     -> InMemoryKeyValueStore（测试）
  -> KuiklyStorageModule
  -> CCStorageModule（T25～T34 平台实现）
  -> MMKV
```

- `StorageContract.kt`：Store、类型、Key、Result、Error 和批量结果。
- `StorageCodecs.kt`：稳定字符串编码规则。
- `AppStorageKeys.kt`：唯一 Key 注册表。
- `KeyValueStore.kt`：业务接口和内存 Fake。
- `StorageProtocol.kt`：双端必须镜像的 wire 常量和 JSON envelope。
- `KuiklyKeyValueStore.kt`：typed API 到 Kuikly Bridge 的转换。
- `SettingsRepository.kt`：首个业务 Repository，不暴露 Key 和 Bridge。

## 6. 本轮验收边界

- T17～T24 已完成：协议、Key 注册、Codec、Fake、Kuikly Module、Repository、错误码和 shared 测试。
- Android/HarmonyOS 的 `CCStorageModule` 尚未注册，业务页面本轮不得实例化正式 `KuiklyKeyValueStore` 发起原生调用。
- T25 起接入 Android MMKV；T30 起接入 HarmonyOS MMKV；随后实现迁移状态机。

