package com.y.citycapsule.base

import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.*
import com.y.citycapsule.app.theme.AppThemeRuntime
import com.y.citycapsule.app.theme.KuiklyThemeHostModule
import com.y.citycapsule.core.storage.KuiklyStorageModule
import com.y.citycapsule.core.storage.StorageProtocol
import com.y.citycapsule.core.theme.ThemeBootstrapContract
import com.y.citycapsule.core.theme.ThemeHostProtocol
import com.y.citycapsule.core.theme.ThemeMode

internal abstract class BasePager : ComposeContainer() {
    private var nightModel: Boolean? by observable(null)

    override fun createExternalModules(): Map<String, Module>? {
        val externalModules = hashMapOf<String, Module>()
        externalModules[BridgeModule.MODULE_NAME] = BridgeModule()
        externalModules[StorageProtocol.MODULE_NAME] = KuiklyStorageModule()
        externalModules[ThemeHostProtocol.MODULE_NAME] = KuiklyThemeHostModule()
        return externalModules
    }

    override fun willInit() {
        super.willInit()
        val params = pageData.params
        val themeMode = ThemeMode.fromWireValue(
            params.optString(ThemeBootstrapContract.KEY_THEME_MODE)
        )
        val systemDark = when {
            params.has(ThemeBootstrapContract.KEY_SYSTEM_DARK) ->
                params.optBoolean(ThemeBootstrapContract.KEY_SYSTEM_DARK)
            params.has(ThemeBootstrapContract.KEY_KUIKLY_NIGHT_MODE) ->
                params.optBoolean(ThemeBootstrapContract.KEY_KUIKLY_NIGHT_MODE)
            else -> null
        }
        AppThemeRuntime.bootstrap(themeMode, systemDark)
    }

    override fun created() {
        super.created()
        isNightMode()
    }

    override fun themeDidChanged(data: JSONObject) {
        super.themeDidChanged(data)
        if (data.has(IS_NIGHT_MODE_KEY)) {
            nightModel = data.optBoolean(IS_NIGHT_MODE_KEY)
            AppThemeRuntime.updateSystemAppearance(nightModel!!)
        }
    }

    // 是否为夜间模式
    override fun isNightMode(): Boolean {
        if (nightModel == null) {
            nightModel = when {
                pageData.params.has(ThemeBootstrapContract.KEY_SYSTEM_DARK) ->
                    pageData.params.optBoolean(ThemeBootstrapContract.KEY_SYSTEM_DARK)
                else -> pageData.params.optBoolean(IS_NIGHT_MODE_KEY)
            }
        }
        return nightModel!!
    }

    // 不开启调试UI模式
    override fun debugUIInspector(): Boolean {
        return false
    }

    companion object {
        const val IS_NIGHT_MODE_KEY = "isNightMode"
    }

}
