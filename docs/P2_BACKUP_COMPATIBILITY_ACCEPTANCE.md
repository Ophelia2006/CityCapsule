# P2-6 备份兼容与双端真机验收

## 实现边界

- 沿用现有 ZIP：`data/backup.json`、`media/index.json`、`media/images/*`，未重做归档链路。
- 当前导出外层版本为 `backupVersion=2`、`schemaVersion=2`、`minReaderVersion=2`。
- 当前版本可读取 v1、v2；高于 v2 或声明 `minReaderVersion>2` 的包在预览阶段拒绝，且不写 MMKV、不提交媒体。
- 旧版客户端只接受 `backupVersion=1`，因此会明确拒绝当前 v2 导出，不会按旧语义静默导入。
- Place catalog v1 由当前 Place codec 解码：精确命中内置 seed ID 的地点迁移为 `SEED`，其余为 `USER`；缺失坐标、封面迁移为 `null`。恢复写入时以 v2 重新编码，ID 及 Favorite/Capsule 关系不变。
- 备份媒体清单只来自已发布 Capsule 的 `imagePaths`。Camera 与相册产生的已引用托管原图均进入清单；draft 和 `images/thumbnail` 不进入备份。恢复只写 `images/original`，Timeline/Gallery 首次显示时按需重新生成缩略图。
- 导入顺序保持：临时解压 → shared codec 校验/预览 → 用户确认 → 当前数据恢复包 → 导入原图 → 结构化写入。写入失败恢复旧 snapshot，并删除本轮已创建原图。

## 测试材料

每个平台各准备一台真机，并保存测试前应用数据副本。准备以下文件：

1. 当前版本生成的 v2 正常包，包含一个 Camera 拍摄且已发布的 Capsule。
2. 将正常 ZIP 截断或替换 `data/backup.json` 为非法 JSON 的损坏包。
3. 将 `backupVersion` 改为 `99` 的未来版本包。
4. 保持 `backupVersion=2`，将 `minReaderVersion` 改为 `99` 的未来 reader 包。
5. v1 包：Place 项没有 `source`、`geoPoint`、`visualRef`，同时包含一个已知 seed ID 和一个自建 ID。
6. 可安装的旧版（只接受 backupVersion 1）和当前版安装包。

每项分别记录 Android / HarmonyOS 的设备、系统版本、应用 commit、结果、截图或日志。失败日志不得包含完整照片 URI 或用户正文。

## A. 导出与取消

1. 新建地点记录，使用系统相机拍照并发布；确认 Timeline/详情可见原图。
2. 设置 → 数据管理 → 导出，选择有效目标。
3. 解压副本检查：`backup.json` 三个版本字段均为 2；`media/index.json` 包含已发布 Camera 原图；对应文件位于 `media/images`；没有 `thumbnail` entry，也没有 draft-only 图片。
4. 再次导出，在系统保存选择器中取消。

通过标准：有效 ZIP 可被当前版预览；取消显示中性提示，不留下宣称成功的文件或改变业务数据；原应用数据不变。

## B. 导入取消、损坏包与未来版本

1. 选择正常包，到数量预览后点击取消；重新进入 Timeline/想去/档案确认无变化。
2. 分别选择损坏包、未来版本包、未来 reader 包。

通过标准：取消会丢弃 staging session；损坏包提示文件损坏/格式不支持；两类未来包提示版本不支持或需要更高版本；均不得出现确认写入步骤，不得改变 MMKV、照片目录或当前主题。

## C. 空间不足

1. 用大照片备份，将设备可用空间压到小于“临时解压 + 恢复包 + 导入原图”所需空间。
2. 尝试选择并确认导入；分别覆盖 staging 解压不足、创建恢复包不足、复制媒体不足（条件允许时）。

通过标准：任一步失败都显示失败而非成功；创建恢复包失败时不开始提交媒体/数据；媒体提交失败时不写结构化数据，并清理已经复制的本轮媒体；重启后原数据仍可读。释放空间后应用可继续正常导出/导入。

## D. 注入写入失败与回滚

真机 debug 验收包使用一次性故障注入，让某个 persistent key 在导入写入阶段返回 Failure；不得把故障开关提交到正式产品入口。

1. 导入一份内容明显不同且带照片的包。
2. 在恢复包创建和媒体提交完成后触发结构化写入失败。
3. 重启应用，逐项检查主题、档案、地点、想去、Capsule 和原照片。

通过标准：提示“原数据已恢复，导入照片已清理”；所有旧 persistent key 恢复；本轮 `import_*` 原图不存在；`files/backups/recovery` 保留导入前 ZIP。若任一步恢复不完整，必须提示停止继续修改数据，不能宣称回滚成功。

## E. 带照片恢复与缩略图再生成

1. 在源设备发布 Camera 照片 Capsule 并导出。
2. 在目标设备确认导入；完成后进入 Timeline、Gallery、Capsule Detail。
3. 检查恢复后的 Capsule 路径已改为目标设备 `images/original/import_*`；详情显示原图。
4. 导入结束立即检查 `images/thumbnail`：ZIP 中没有复制来的缩略图。打开 Timeline/Gallery 后再次检查。

通过标准：原图恢复且不引用源设备绝对路径；详情可读原图；缩略图由目标设备按需重新生成，即使生成失败也以原图降级，不丢失记录。

## F. Place V1 → V2

1. 当前设备先保留一套不同数据，再选择 v1 包并查看预览数量。
2. 确认导入，重启后检查两个地点、关联的想去和 Capsule。
3. 再从当前版导出并检查 Place catalog wire value。

通过标准：已知 seed ID 为 `source=seed`，自建 ID 为 `source=user`；二者缺失坐标和封面时均为 null/不写字段；ID、Favorite placeId、Capsule placeId 不变；重新导出的 Place catalog 为 schema v2。

## G. 新包对旧版门禁

1. 当前版导出 v2 包。
2. 在隔离测试设备安装旧版并选择该包。

通过标准：旧版在预览前明确提示“备份版本不受支持”，业务数据与媒体目录不变。若旧版进入确认或写入步骤，本项失败，必须停止发布 v2。

## 自动化基线

- `DataBackupRepositoryTest` 覆盖 v2/minReader 门禁、v1 Place 恢复后 v2 重编码、未来版本无写入、Camera 已发布原图清单及 draft/thumbnail 排除。
- Android/HarmonyOS 平台归档实现均只解析各自 `filesDir/images/original` 的直接子文件；导入媒体也只写该目录。
- 自动化通过不替代系统文件选择器、真实空间不足、真实相机文件和进程重启测试。

