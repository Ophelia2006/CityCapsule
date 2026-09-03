# P2-7A 本地路线验收

> 历史导航说明：当前已按 ADR-036 增加“漫游”一级 Tab；本文其余路线验收步骤继续有效。

## 当前实现

- Explore 内提供“我的路线”，底栏仍只有“探索 / 记录 / 我的”。
- `LocalRoute` 只含 `id / name / orderedPlaceIds / createdAtEpochMs`。
- 支持手动添加、移除、上移、下移、保存、编辑和删除；不接在线路径规划。
- `routes.catalog` 通过 MMKV 持久化并进入 v3 备份；v1/v2 缺失路线项时迁移为空 catalog。

## 自动检查

```powershell
.\gradlew.bat :shared:testDebugUnitTest
.\gradlew.bat :androidApp:testDebugUnitTest :androidApp:assembleDebug
```

## Android / HarmonyOS 手工流程

1. 确认没有“漫游”Tab；从探索页进入“我的路线”。
2. 空状态新建；名称或地点为空时保存不可用。
3. 输入路线名，添加三个真实地点；上移/下移改变顺序，移除后重新添加，确认无重复。
4. 保存后确认列表顺序；再次编辑并保存，确认返回刷新。
5. 结束进程并重启，确认路线及顺序仍存在。
6. 删除路线，确认列表和再次重启后均不再出现。
7. 导出备份，修改后导入并确认恢复；导入 v2 备份应成功且路线为空。
8. 断网重复新建、排序、保存和删除，功能应不受影响。

## 边界检查

- 没有智能/最优路线、距离、时长、交通方式或导航线。
- 路由参数只传 `routeId`；MMKV 只保存结构化 catalog。
- 没有 GPS、轨迹、打卡或漫游入口。
