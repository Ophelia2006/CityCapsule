package com.y.citycapsule

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.module.KRBridgeModule
import com.y.citycapsule.module.KRShareModule
import com.y.citycapsule.module.KRStorageModule
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

    private val pageName: String
        get() {
            val pn = intent.getStringExtra(KEY_PAGE_NAME) ?: ""
            return if (pn.isNotEmpty()) {
                return pn
            } else {
                AppRouteTable.PAGE_HOME
            }
        }

    override val routeKey: String
        get() = intent.getStringExtra(KEY_ROUTE_KEY).orEmpty().ifBlank { pageName }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidRouteStackCoordinator.shared.register(this)
        setContentView(R.layout.activity_hr)
        setupImmersiveMode()
        hrContainerView = findViewById(R.id.hr_container)
        loadingView = findViewById(R.id.hr_loading)
        errorView = findViewById(R.id.hr_error)
        kuiklyRenderViewDelegator.onAttach(hrContainerView, "", pageName, createPageData())
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
        return param
    }

    private fun argsToMap(): MutableMap<String, Any> {
        val jsonStr = intent.getStringExtra(KEY_PAGE_DATA) ?: return mutableMapOf()
        return runCatching { JSONObject(jsonStr).toMap() }
            .getOrDefault(mutableMapOf())
    }

    private fun setupImmersiveMode() {
        window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window?.statusBarColor = Color.TRANSPARENT
            window?.decorView?.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

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
