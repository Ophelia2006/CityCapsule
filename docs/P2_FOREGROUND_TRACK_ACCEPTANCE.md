# P2-7C 前台轨迹验收

## 实现边界

- 漫游进行中且漫游页位于前台时，每 15 秒请求一次定位；也可点击“记录当前位置”。
- 成功点分片写入应用沙箱 `tracks/<sessionStartedAt>/chunk_N.json`。
- MMKV `roaming.track` 只保存分片索引、点数、状态、中断原因和时间，不保存坐标。
- 定位或文件失败只把轨迹标记为 `INTERRUPTED`；漫游仍保持 `ACTIVE` 并允许重试。
- 暂停或离开页面停止采样。没有后台定位、保活或耗电策略。

## 自动检查

```powershell
.\gradlew.bat :shared:testDebugUnitTest
.\gradlew.bat :androidApp:testDebugUnitTest :androidApp:assembleDebug
```

## Android / HarmonyOS 真机流程

1. 自由漫游 → 开始，允许前台定位权限。
2. 保持页面前台至少 35 秒，确认点数至少增长两次；手动记录再增长一次。
3. 暂停后等待 20 秒，点数不增长；继续后恢复。
4. 离开页面等待 20 秒，不采样；重新进入进行中会话后恢复。
5. 关闭定位或拒绝权限，确认轨迹显示中断，但漫游仍进行中且可暂停/结束。
6. 恢复定位并手动记录，确认中断恢复、点数增长。
7. 结束并重启，确认轨迹元数据和索引仍在。
8. 检查沙箱：坐标只存在 `files/tracks/.../*.json`，MMKV 中无 latitude/longitude。

不验收后台采样、系统保活、功耗策略、轨迹线、距离统计、到达判断或打卡。
