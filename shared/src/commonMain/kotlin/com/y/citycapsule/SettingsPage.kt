package com.y.citycapsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteKey
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.core.storage.SettingsRepository
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.core.storage.ThemeMode
import com.y.citycapsule.core.storage.ThemeModeSnapshot
import com.y.citycapsule.core.storage.ThemeModeSource

@Page(AppRouteTable.PAGE_SETTINGS, supportInLocal = true)
internal class SettingsPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val settingsRepository = SettingsRepository(KuiklyKeyValueStore(this))
        setContent {
            SettingsScreen(navigator, settingsRepository)
        }
    }
}

@Composable
private fun SettingsScreen(
    navigator: AppNavigator,
    settingsRepository: SettingsRepository
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var storageStatus by remember { mutableStateOf("正在读取主题偏好…") }

    fun applySnapshot(snapshot: ThemeModeSnapshot) {
        themeMode = snapshot.mode
        storageStatus = when (snapshot.source) {
            ThemeModeSource.PERSISTED -> "当前偏好：${snapshot.mode.displayName()}"
            ThemeModeSource.DEFAULT_MISSING -> "当前偏好：跟随系统"
            ThemeModeSource.DEFAULT_RECOVERY -> "存储暂不可用，已安全回退为跟随系统"
        }
    }

    LaunchedEffect(settingsRepository) {
        settingsRepository.getThemeModeSnapshot(::applySnapshot)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(
                start = 24.dp,
                top = (statusBarHeight + 40).dp,
                end = 24.dp,
                bottom = 24.dp
            )
    ) {
        Text(
            text = "设置",
            color = Color(0xFF1F2430),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Settings 使用同一个 AppNavigator 返回平台路由栈。",
            color = Color(0xFF687083),
            fontSize = 16.sp
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "主题偏好",
            color = Color(0xFF1F2430),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = storageStatus,
            color = Color(0xFF687083),
            fontSize = 14.sp
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFE7F4EA))
                .clickable {
                    val nextMode = themeMode.next()
                    storageStatus = "正在保存 ${nextMode.displayName()}…"
                    settingsRepository.setThemeMode(nextMode) { writeResult ->
                        when (writeResult) {
                            is StorageResult.Success -> {
                                settingsRepository.getThemeModeSnapshot(::applySnapshot)
                            }
                            StorageResult.Missing -> {
                                storageStatus = "保存结果未确认，请稍后重试"
                            }
                            is StorageResult.Failure -> {
                                storageStatus = "暂时无法保存，仍使用 ${themeMode.displayName()}"
                            }
                        }
                    }
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "切换主题偏好",
                color = Color(0xFF26703B),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFE9ECF8))
                .clickable { navigator.backTo(AppRouteKey.HOME) }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "返回首页",
                color = Color(0xFF3F4BA8),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFE9ECF8))
                .clickable { navigator.navigate(AppRoute.Settings) }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Push another Settings",
                color = Color(0xFF3F4BA8),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFE9ECF8))
                .clickable(onClick = navigator::back)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "返回上一页",
                color = Color(0xFF3F4BA8),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun ThemeMode.next(): ThemeMode = when (this) {
    ThemeMode.SYSTEM -> ThemeMode.LIGHT
    ThemeMode.LIGHT -> ThemeMode.DARK
    ThemeMode.DARK -> ThemeMode.SYSTEM
}

private fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}
