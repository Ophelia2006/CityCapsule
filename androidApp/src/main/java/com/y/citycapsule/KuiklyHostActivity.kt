package com.y.citycapsule

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.tencent.kuikly.core.render.android.IKuiklyRenderExport
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.css.ktx.toMap
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator
import com.y.citycapsule.adapter.KRColorParserAdapter
import com.y.citycapsule.adapter.KRFontAdapter
import com.y.citycapsule.adapter.KRImageAdapter
import com.y.citycapsule.adapter.KRLogAdapter
import com.y.citycapsule.adapter.KRRouterAdapter
import com.y.citycapsule.adapter.KRThreadAdapter
import com.y.citycapsule.adapter.KRUncaughtExceptionHandlerAdapter
import com.y.citycapsule.designsystem.AndroidThemeHost
import com.y.citycapsule.designsystem.AndroidThemePageData
import com.y.citycapsule.module.KRBridgeModule
import com.y.citycapsule.module.KRShareModule
import com.y.citycapsule.module.KRStorageModule
import com.y.citycapsule.module.KRThemeHostModule
import com.y.citycapsule.navigation.AndroidLaunchContract
import com.y.citycapsule.navigation.AndroidRouteHost
import com.y.citycapsule.navigation.AndroidRouteRequest
import com.y.citycapsule.navigation.AndroidRouteStackCoordinator
import org.json.JSONObject

class KuiklyHostActivity :
    AppCompatActivity(),
    KuiklyRenderViewBaseDelegatorDelegate,
    AndroidRouteHost {

    private lateinit var hrContainerView: ViewGroup
    private lateinit var loadingView: View
    private lateinit var errorView: View

    private val kuiklyRenderViewDelegator = KuiklyRenderViewBaseDelegator(this)

    internal val hostedPageName: String
        get() = AndroidLaunchContract.resolvePageName(
            intent.getStringExtra(KEY_PAGE_NAME)
        )

    override val routeKey: String
        get() = AndroidLaunchContract.resolveRouteKey(
            requestedRouteKey = intent.getStringExtra(KEY_ROUTE_KEY),
            resolvedPageName = hostedPageName
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidRouteStackCoordinator.shared.register(this)
        AndroidThemeHost.applySystemBars(this, AndroidThemeHost.bootstrap(this).resolvedDark)
        setContentView(R.layout.activity_hr)
        hrContainerView = findViewById(R.id.hr_container)
        loadingView = findViewById(R.id.hr_loading)
        errorView = findViewById(R.id.hr_error)
        kuiklyRenderViewDelegator.onAttach(
            hrContainerView,
            "",
            hostedPageName,
            createPageData()
        )
    }

    override fun onDestroy() {
        AndroidRouteStackCoordinator.shared.unregister(this)
        kuiklyRenderViewDelegator.onDetach()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        kuiklyRenderViewDelegator.onPause()
    }

    override fun onResume() {
        super.onResume()
        AndroidThemeHost.applySystemBars(this, AndroidThemeHost.bootstrap(this).resolvedDark)
        kuiklyRenderViewDelegator.onResume()
    }

    override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalModule(kuiklyRenderExport)
        with(kuiklyRenderExport) {
            moduleExport(KRBridgeModule.MODULE_NAME) {
                KRBridgeModule()
            }
            moduleExport(KRShareModule.MODULE_NAME) {
                KRShareModule()
            }
            moduleExport(KRStorageModule.MODULE_NAME) {
                KRStorageModule()
            }
            moduleExport(KRThemeHostModule.MODULE_NAME) {
                KRThemeHostModule()
            }
        }
    }

    override fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalRenderView(kuiklyRenderExport)
        with(kuiklyRenderExport) {

        }
    }

    override fun finishRoute() {
        finish()
    }

    private fun createPageData(): Map<String, Any> {
        val param = argsToMap()
        param["appId"] = 1
        val theme = AndroidThemeHost.bootstrap(this)
        param.putAll(AndroidThemePageData.create(theme))
        return param
    }

    private fun argsToMap(): MutableMap<String, Any> {
        val jsonStr = intent.getStringExtra(KEY_PAGE_DATA) ?: return mutableMapOf()
        return runCatching { JSONObject(jsonStr).toMap() }
            .getOrDefault(mutableMapOf())
    }

    companion object {

        private const val KEY_PAGE_NAME = "pageName"
        private const val KEY_PAGE_DATA = "pageData"
        private const val KEY_ROUTE_KEY = "routeKey"
        private const val KEY_ROUTE_ACTION = "routeAction"

        init {
            initKuiklyAdapter()
        }

        fun start(context: Context, request: AndroidRouteRequest) {
            val starter = Intent(context, KuiklyHostActivity::class.java)
                .putExtra(KEY_PAGE_NAME, request.target)
                .putExtra(KEY_PAGE_DATA, request.pageDataJson)
                .putExtra(KEY_ROUTE_KEY, request.routeKey)
                .putExtra(KEY_ROUTE_ACTION, request.action.name)
            if (context !is Activity) {
                starter.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(starter)
        }

        private fun initKuiklyAdapter() {
            with(KuiklyRenderAdapterManager) {
                krImageAdapter = KRImageAdapter(KRApplication.application)
                krLogAdapter = KRLogAdapter
                krUncaughtExceptionHandlerAdapter = KRUncaughtExceptionHandlerAdapter
                krFontAdapter = KRFontAdapter
                krColorParseAdapter = KRColorParserAdapter(KRApplication.application)
                krRouterAdapter = KRRouterAdapter
                krThreadAdapter = KRThreadAdapter()
            }
        }
    }
}
