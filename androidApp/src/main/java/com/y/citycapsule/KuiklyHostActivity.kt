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
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.y.citycapsule.adapter.KRColorParserAdapter
import com.y.citycapsule.adapter.KRFontAdapter
import com.y.citycapsule.adapter.KRImageAdapter
import com.y.citycapsule.adapter.KRLogAdapter
import com.y.citycapsule.adapter.KRRouterAdapter
import com.y.citycapsule.adapter.KRThreadAdapter
import com.y.citycapsule.adapter.KRUncaughtExceptionHandlerAdapter
import com.y.citycapsule.designsystem.AndroidThemeHost
import com.y.citycapsule.designsystem.AndroidThemePageData
import com.y.citycapsule.map.KRAmapView
import com.y.citycapsule.module.KRBridgeModule
import com.y.citycapsule.module.KRDataArchiveModule
import com.y.citycapsule.module.KRExternalNavigationModule
import com.y.citycapsule.module.KRLocaleModule
import com.y.citycapsule.module.KRLocationModule
import com.y.citycapsule.module.KRMediaModule
import com.y.citycapsule.module.KRPlaceNetworkModule
import com.y.citycapsule.module.KRShareModule
import com.y.citycapsule.module.KRStorageModule
import com.y.citycapsule.module.KRThemeHostModule
import com.y.citycapsule.module.KRTrackModule
import com.y.citycapsule.navigation.AndroidLaunchContract
import com.y.citycapsule.navigation.AndroidRouteHost
import com.y.citycapsule.navigation.AndroidRouteRequest
import com.y.citycapsule.navigation.AndroidRouteStackCoordinator
import com.y.citycapsule.platform.AndroidArchiveHost
import com.y.citycapsule.platform.AndroidLocationHost
import com.y.citycapsule.platform.AndroidMediaHost
import org.json.JSONObject

/**
 * Thin Android host for Kuikly pages.
 *
 * Product UI and business decisions live in commonMain. This Activity owns only Android lifecycle,
 * renderer registration and delegation to Android system-capability adapters.
 */
class KuiklyHostActivity :
    AppCompatActivity(),
    KuiklyRenderViewBaseDelegatorDelegate,
    AndroidRouteHost {

    private lateinit var hrContainerView: ViewGroup
    private lateinit var loadingView: View
    private lateinit var errorView: View
    private val kuiklyRenderViewDelegator = KuiklyRenderViewBaseDelegator(this)
    private val mediaHost = AndroidMediaHost(this)
    private val archiveHost = AndroidArchiveHost(this)
    private val locationHost = AndroidLocationHost(this)

    internal val hostedPageName: String
        get() = AndroidLaunchContract.resolvePageName(intent.getStringExtra(KEY_PAGE_NAME))

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
        kuiklyRenderViewDelegator.onAttach(hrContainerView, "", hostedPageName, createPageData())
    }

    override fun onDestroy() {
        locationHost.dispose()
        mediaHost.dispose()
        archiveHost.dispose()
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
            moduleExport(KRBridgeModule.MODULE_NAME, ::KRBridgeModule)
            moduleExport(KRShareModule.MODULE_NAME, ::KRShareModule)
            moduleExport(KRStorageModule.MODULE_NAME, ::KRStorageModule)
            moduleExport(KRMediaModule.MODULE_NAME, ::KRMediaModule)
            moduleExport(KRTrackModule.MODULE_NAME, ::KRTrackModule)
            moduleExport(KRLocaleModule.MODULE_NAME, ::KRLocaleModule)
            moduleExport(KRThemeHostModule.MODULE_NAME, ::KRThemeHostModule)
            moduleExport(KRDataArchiveModule.MODULE_NAME, ::KRDataArchiveModule)
            moduleExport(KRLocationModule.MODULE_NAME, ::KRLocationModule)
            moduleExport(KRPlaceNetworkModule.MODULE_NAME, ::KRPlaceNetworkModule)
            moduleExport(KRExternalNavigationModule.MODULE_NAME, ::KRExternalNavigationModule)
        }
    }

    override fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalRenderView(kuiklyRenderExport)
        kuiklyRenderExport.renderViewExport(KRAmapView.VIEW_NAME, ::KRAmapView, null)
    }

    override fun finishRoute() = finish()

    internal fun pickImages(maxCount: Int, callback: KuiklyRenderCallback) =
        mediaHost.pickImages(maxCount, callback)

    internal fun captureImage(callback: KuiklyRenderCallback) = mediaHost.captureImage(callback)

    internal fun exportDataArchive(request: String, callback: KuiklyRenderCallback) =
        archiveHost.export(request, callback)

    internal fun selectDataArchive(callback: KuiklyRenderCallback) = archiveHost.select(callback)

    internal fun requestCurrentLocation(callback: KuiklyRenderCallback) = locationHost.request(callback)

    private fun createPageData(): Map<String, Any> {
        val param = argsToMap()
        param["appId"] = 1
        param.putAll(AndroidThemePageData.create(AndroidThemeHost.bootstrap(this)))
        return param
    }

    private fun argsToMap(): MutableMap<String, Any> {
        val json = intent.getStringExtra(KEY_PAGE_DATA) ?: return mutableMapOf()
        return runCatching { JSONObject(json).toMap() }.getOrDefault(mutableMapOf())
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
            if (context !is Activity) starter.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
