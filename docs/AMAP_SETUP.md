# 高德地图本机配置

以下控制台、签名和真机步骤必须由项目所有者执行。任何 Key 都不要提交到 Git，也不要发到聊天或截图中。

## Android

1. 在高德开放平台创建 Android 应用，包名填写 `com.y.citycapsule`。
2. 在项目根目录运行 `./gradlew :androidApp:signingReport`，复制 `debug` 变体的 SHA1（不是 SHA-256）。
3. 创建 Android Key，绑定上述包名和 SHA-1。
4. 在根目录被忽略的 `local.properties` 增加：

   ```properties
   AMAP_ANDROID_API_KEY=你的AndroidKey
   ```

5. Gradle Sync 后安装到真机。iQOO Z9 Turbo 可以测试高德 Android SDK，不要求 HMS Core。

发布版必须使用发布证书 SHA-1 配置独立 Key。

## HarmonyOS NEXT

1. 使用最终 `bundleName` 和实际签名构建/安装应用。
2. 按高德文档取得已签名应用的完整 `appId`。本地真机与云真机可能不同，应分别创建 Key。
3. 创建 HarmonyOS NEXT Key，并绑定对应 appId。
4. 复制 `ohosApp/entry/src/main/ets/config/AmapLocalConfig.example.ets` 为同目录 `AmapLocalConfig.ets`，填入 Key；该目标文件已被 Git 忽略。
5. 在 `ohosApp` 执行 `ohpm install`，用 DevEco Studio 真机运行并验收。

HarmonyOS 高德地图不要求 AppGallery Connect Map Kit；但 HarmonyOS 应用签名、bundleName 和 appId 仍是系统构建及高德 Key 绑定的必要信息。

## 隐私与验收

首次进入地图视图时，先展示地图服务说明并取得明确同意；拒绝时保持列表。配置完成后逐项执行 `docs/P2_MAP_NAVIGATION_ACCEPTANCE.md`，分别记录 Android 与 HarmonyOS 真机结果。
