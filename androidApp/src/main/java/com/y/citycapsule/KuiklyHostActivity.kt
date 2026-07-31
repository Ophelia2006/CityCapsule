package com.y.citycapsule

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
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
import com.y.citycapsule.module.KRMediaModule
import com.y.citycapsule.module.KRLocaleModule
import com.y.citycapsule.module.KRStorageModule
import com.y.citycapsule.module.KRThemeHostModule
import com.y.citycapsule.module.KRDataArchiveModule
import com.y.citycapsule.module.DataArchiveFileStore
import com.y.citycapsule.navigation.AndroidLaunchContract
import com.y.citycapsule.navigation.AndroidRouteHost
import com.y.citycapsule.navigation.AndroidRouteRequest
import com.y.citycapsule.navigation.AndroidRouteStackCoordinator
import org.json.JSONObject
import android.webkit.MimeTypeMap
import java.io.File

class KuiklyHostActivity :
    AppCompatActivity(),
    KuiklyRenderViewBaseDelegatorDelegate,
    AndroidRouteHost {

    private lateinit var hrContainerView: ViewGroup
    private lateinit var loadingView: View
    private lateinit var errorView: View

    private val kuiklyRenderViewDelegator = KuiklyRenderViewBaseDelegator(this)
    private var pendingImageLimit: Int = 0
    private var pendingImageCallback: com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback? = null
    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val callback = pendingImageCallback ?: return@registerForActivityResult
        pendingImageCallback = null
        val selected = uris.take(pendingImageLimit)
        pendingImageLimit = 0
        if (selected.isEmpty()) {
            callback.invoke(KRMediaModule.response(KRMediaModule.STATUS_CANCELLED))
            return@registerForActivityResult
        }
        val createdFiles = mutableListOf<File>()
        val copied = runCatching {
            selected.mapIndexed { index, uri ->
                val mime = contentResolver.getType(uri).orEmpty()
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mime)
                    ?.takeIf(String::isNotBlank)
                    ?: "jpg"
                val directory = File(filesDir, "images/original").apply { mkdirs() }
                val target = File(
                    directory,
                    "capsule_${System.currentTimeMillis()}_${index}.$extension"
                )
                createdFiles += target
                requireNotNull(contentResolver.openInputStream(uri)).use { input ->
                    target.outputStream().use(input::copyTo)
                }
                "file://${target.absolutePath}"
            }
        }
        copied.fold(
            onSuccess = { paths ->
                callback.invoke(
                    KRMediaModule.response(KRMediaModule.STATUS_SUCCESS, paths = paths)
                )
            },
            onFailure = {
                createdFiles.forEach { file -> file.delete() }
                callback.invoke(
                    KRMediaModule.response(
                        KRMediaModule.STATUS_FAILURE,
                        "无法复制所选照片，请重试。"
                    )
                )
            }
        )
    }
    private var pendingArchiveCallback: com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback? = null
    private var pendingExportFile: File? = null
    private val archiveCreator = registerForActivityResult(
        ActivityResultContracts.CreateDocument()
    ) { uri ->
        val callback = pendingArchiveCallback ?: return@registerForActivityResult
        val file = pendingExportFile
        pendingArchiveCallback = null
        pendingExportFile = null
        if (uri == null || file == null) {
            file?.delete()
            callback.invoke(KRDataArchiveModule.response(KRDataArchiveModule.STATUS_CANCELLED))
        } else {
            DataArchiveFileStore(this).copyExport(file, uri).fold(
                onSuccess = {
                    callback.invoke(KRDataArchiveModule.response(KRDataArchiveModule.STATUS_SUCCESS) {
                        put("path", uri.toString())
                    })
                },
                onFailure = {
                    callback.invoke(KRDataArchiveModule.response(
                        KRDataArchiveModule.STATUS_FAILURE, "无法写入所选位置。"
                    ))
                }
            )
        }
    }
    private val archivePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val callback = pendingArchiveCallback ?: return@registerForActivityResult
        pendingArchiveCallback = null
        callback.invoke(
            if (uri == null) {
                KRDataArchiveModule.response(KRDataArchiveModule.STATUS_CANCELLED)
            } else {
                DataArchiveFileStore(this).stageImport(uri)
            }
        )
    }

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
            moduleExport(KRMediaModule.MODULE_NAME) {
                KRMediaModule()
            }
            moduleExport(KRLocaleModule.MODULE_NAME) {
                KRLocaleModule()
            }
            moduleExport(KRThemeHostModule.MODULE_NAME) {
                KRThemeHostModule()
            }
            moduleExport(KRDataArchiveModule.MODULE_NAME) {
                KRDataArchiveModule()
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

    internal fun pickImages(
        maxCount: Int,
        callback: com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
    ) {
        if (pendingImageCallback != null) {
            callback.invoke(
                KRMediaModule.response(
                    KRMediaModule.STATUS_FAILURE,
                    "已有照片选择操作正在进行。"
                )
            )
            return
        }
        pendingImageLimit = maxCount.coerceIn(1, 9)
        pendingImageCallback = callback
        imagePicker.launch(arrayOf("image/*"))
    }

    internal fun exportDataArchive(
        request: String,
        callback: com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
    ) {
        if (pendingArchiveCallback != null) {
            callback.invoke(KRDataArchiveModule.response(
                KRDataArchiveModule.STATUS_FAILURE, "已有文件操作正在进行。"
            ))
            return
        }
        DataArchiveFileStore(this).createExport(request).fold(
            onSuccess = { file ->
                pendingArchiveCallback = callback
                pendingExportFile = file
                archiveCreator.launch("citycapsule-backup-${System.currentTimeMillis()}.zip")
            },
            onFailure = {
                callback.invoke(KRDataArchiveModule.response(
                    KRDataArchiveModule.STATUS_FAILURE, "无法创建备份文件。"
                ))
            }
        )
    }

    internal fun selectDataArchive(
        callback: com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
    ) {
        if (pendingArchiveCallback != null) {
            callback.invoke(KRDataArchiveModule.response(
                KRDataArchiveModule.STATUS_FAILURE, "已有文件操作正在进行。"
            ))
            return
        }
        pendingArchiveCallback = callback
        archivePicker.launch(arrayOf("application/zip", "application/octet-stream"))
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
