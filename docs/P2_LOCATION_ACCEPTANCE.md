# P2-2 Location Capability 验收

## 已实现边界

- 只获取一次前台当前位置；不后台定位、不持续监听、不持久化精确位置。
- 只有 Explore 的“获取当前位置/重新定位”触发权限；启动、进入 AppShell、加载地点目录均不申请。
- 成功后仅给具有真实 `geoPoint` 的地点显示 shared Haversine 直线距离；当前无坐标的 seed 不显示距离。
- 失败、拒绝、服务关闭或不可用时清空当前位置并隐藏全部距离，列表、搜索、筛选和详情入口保持可用。
- 地图 Native View、Marker 与外部导航不属于本任务。

## 双端真机验收流程

每个场景开始前记录平台、系统版本、App 版本和是否为全新安装。Android 与 HarmonyOS 各执行一遍。

1. 全新安装并启动，完成引导后停留 Explore；确认系统没有弹定位权限，地点列表完整且没有距离。
2. 点击“获取当前位置”，选择允许（精确或大致均各测一次）；确认按钮先显示“定位中…”，随后显示成功说明。只有带真实坐标的测试地点出现距离，无坐标地点不出现占位值、`0 m` 或随机距离。
3. 清除 App 权限/数据后重试，首次弹窗选择拒绝；确认显示拒绝说明，所有距离隐藏，地点列表仍可搜索、筛选、收藏和进入详情。
4. 再次请求并选择“不再询问”，或在系统设置把定位权限设为禁止且不允许再次询问；确认进入 `PermissionPermanentlyDenied` 文案，页面不循环弹窗。到系统设置恢复权限后返回，再点“重新定位”应可成功。
5. 保留 App 权限但关闭系统定位服务，再主动请求；确认返回 `ServiceDisabled`，不显示距离。开启系统定位服务后再次请求应恢复。
6. 在室内、模拟器或屏蔽定位源使请求超过 10 秒；确认返回失败/超时文案，按钮恢复可点击，目录不进入整页错误态。
7. 请求定位后立即切到后台，等待结果，再回前台；若页面仍是同一 Store，可显示该次真实结果且不得重复申请。再次请求期间连续点击按钮必须只产生一个请求。
8. 请求定位后立即返回销毁 Place List，再等待超过 10 秒；重新进入页面时不得出现旧请求的成功/失败提示或距离。重复“进入 → 请求 → 退出”三次，确认无崩溃和旧 Store 回写。
9. 成功定位后再次请求，并在第二次请求失败；确认旧距离立即消失且不会复用上一次精确位置。
10. 关闭网络但保持系统定位可用后重试；定位结果由平台能力决定，无论成功或失败，地点目录都必须完整可用，且不能伪造成功。

## 自动化与构建证据

- `:shared:testDebugUnitTest`：覆盖距离纯函数、Success/失败状态、dispose 后迟到回调不写 Store。
- `:androidApp:compileDebugKotlin`：Android 权限、LocationManager 单次请求与 10 秒超时通过编译。
- HarmonyOS `assembleHap`：权限声明、Location Kit 和 Kuikly module 通过 ArkTS/HAP 构建。

真机完成以上矩阵前，本 Feature 状态保持 `PARTIAL（真机权限验收）`。

## 双端诊断日志

Android 与 HarmonyOS Location module 均使用固定 tag `CityCapsuleLocation` 输出脱敏阶段日志，不记录经纬度、权限 token 或设备标识。Android 只额外记录归一化的 provider 类型、是否具有精度，以及启动异常的类名；HarmonyOS 记录原始 `BusinessError.code` 和映射后的 status，不记录系统错误正文。

在 DevEco Log 中按 `CityCapsuleLocation` 过滤。正常顺序应为：

```text
request_started
permission_granted
service_enabled
location_request_started
location_request_succeeded
request_finished
```

错误码映射：`201 → permission_denied`、`401 → failure/参数错误`、`801 → unavailable`、`3301000 → unavailable/服务不可用`、`3301100 → service_disabled`、`3301200 → failure/未能取得位置`；未知错误保留原始数字 code，并按发生阶段安全降级。

Android 正常阶段为 `request_started → permission_* → service_enabled → providers_selected → location_request_started → location_succeeded → request_finished`。实现同时向已启用的 GPS/Network provider 请求单次位置，以首个有效结果完成并统一注销监听；不再依赖模拟器可能返回但不响应 `geo fix` 的 fused best provider。模拟器未在请求后的 10 秒内注入新坐标时记录 `location_timeout`；页面销毁取消未完成请求时记录 `request_cancelled_on_destroy`。

HarmonyOS 首次拒绝后，同一页面再次主动请求改走 `requestPermissionOnSetting`，日志出现 `permission_settings_request_started`。这是系统允许的设置授权入口，不保证再次显示与首次完全相同的权限弹窗；再次拒绝或系统不允许变更时返回 `PermissionPermanentlyDenied`，由全宽状态提示引导用户前往系统设置。
