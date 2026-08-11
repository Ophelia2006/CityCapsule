# P2-3 地图与外部导航验收

最后更新：2026-08-11

当前状态：`PARTIAL`。双端真实高德地图正常显示已通过，但共同验收门槛尚未全部完成，不得标记 `DONE`。

## 当前验收矩阵

| 门槛 | 状态 | 证据与剩余工作 |
| --- | --- | --- |
| Android 和 HarmonyOS 真机显示真实高德地图 | PASS | Android iQOO 与 HarmonyOS 真机均已显示真实地图 |
| 隐私同意先于 SDK 初始化 | PASS（代码）/PENDING（冷启动记录） | 双端代码顺序正确；待各保留一次清数据/重装记录 |
| Key 不进入 Git、MMKV、日志或备份 | PARTIAL | 两个本机配置文件已被 `.gitignore` 排除，未发现写 MMKV/日志的代码；待 Git 历史、导出包、恢复包和默认备份检查 |
| Marker → 摘要 → `PlaceDetail(placeId)` | PARTIAL | Android 真机通过；HarmonyOS 待完整复验 |
| 定位拒绝不破坏地图和列表 | PENDING | 代码有拒绝分支，待双端真机场景验证 |
| 无 Key、断网、初始化失败自动回列表 | PENDING | 代码有失败降级，待双端逐项验证 |
| 外部导航四种结果 | PARTIAL | Android `Opened` 已通过；其余三态及 HarmonyOS 四态待验证 |
| 双端构建与自动化测试 | FAIL | 2026-08-11 HarmonyOS signed HAP 与 Android Debug APK 构建成功；Android 43 项单测中 1 项失败：`AppShellArchitectureGuardTest.appShellSwitchesRetainedRootsWithoutRouteReplace` |
| 双端各 20 次生命周期压力验证 | PENDING | 尚无完整计数记录 |
| 更新状态、TODO 和验收记录 | PASS | 2026-08-11 已同步 `CURRENT_STATE.md`、`TODO.md` 与本文件 |

## 已验证的正常路径

- Android：真实地图、Marker 摘要、摘要进入 typed PlaceDetail、地图手势和外部高德拉起通过。
- HarmonyOS：命令行 Hvigor 构建 signed HAP、安装及真实地图显示通过。
- HarmonyOS 空白地图修复：使用默认 `MapViewComponent()`，并由具有明确宽高的全尺寸 `Stack` 承载。直接把条件分支作为根节点或显式使用 `type: 'texture'` 均未通过真机显示验证。

## DONE 前剩余步骤

1. HarmonyOS 完成 Marker → 摘要 → PlaceDetail。
2. 双端验证定位拒绝、无 Key、断网和初始化失败。
3. 双端验证外部导航四态及特殊字符地点名编码。
4. 双端各执行 20 次进入/退出和前后台生命周期压力验证。
5. 修复 Android 架构守卫测试，重跑 shared、Android、HarmonyOS 全套测试与构建。
6. 完成 Key 的 Git 历史、日志、导出/恢复包及默认备份审计。

## 原始验收步骤

1. 从“探索”进入地点目录，确认只有“列表 / 地图”切换且 Bottom Navigation 未新增地图。
2. 无 Key、断网、SDK 初始化失败时选择地图：提示真实原因并自动回列表；无坐标地点仍可打开详情。
3. 配置合法 Key 后：只显示有坐标地点 Marker；点击 Marker 出现对应摘要；点击摘要整卡进入 `PlaceDetail(placeId)`。
4. 拒绝定位权限时地图和列表继续可用；允许后可选显示当前位置。
5. 前后台切换、窗口变化、反复进入退出 20 次，确认无重复 Marker、销毁后回调或明显泄漏。
6. 有坐标地点触发外部导航：安装兼容 App 时返回 `Opened` 并拉起；无处理 App、平台不支持、系统异常分别得到 `NoCompatibleApp`、`Unsupported`、`Failure`。
7. 用含中文、空格、`&`、括号的地点名重复外部导航，确认编码和坐标正确。

自动化至少运行 shared 单测、Android 单测/构建与 HarmonyOS 类型检查/构建。Native Map 真机步骤需要合法双端 Map Kit 配置；缺凭据时只能验收降级，不得宣称地图闭环完成。
