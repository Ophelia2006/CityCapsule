package com.y.citycapsule

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import com.y.citycapsule.module.KRTrackModule
import com.y.citycapsule.module.KRLocaleModule
import com.y.citycapsule.module.KRLocationModule
import com.y.citycapsule.module.KRPlaceNetworkModule
import com.y.citycapsule.module.KRExternalNavigationModule
import com.y.citycapsule.module.KRStorageModule
import com.y.citycapsule.module.KRThemeHostModule
import com.y.citycapsule.module.KRDataArchiveModule
import com.y.citycapsule.module.DataArchiveFileStore
import com.y.citycapsule.map.KRAmapView
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
    private var pendingLocationCallback: com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback? = null
    private var activeLocationListener: LocationListener? = null
    private val locationHandler = Handler(Looper.getMainLooper())
    private val locationTimeout = Runnable {
        logLocationWarning("location_timeout", "failure")
        finishLocation(KRLocationModule.response(KRLocationModule.STATUS_FAILURE, "定位超时，请重试。"))
    }
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            logLocationInfo("permission_granted")
            startOneShotLocation()
        }
        else {
            val canExplain = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
            logLocationWarning(
                "permission_rejected",
                if (canExplain) KRLocationModule.STATUS_PERMISSION_DENIED
                else KRLocationModule.STATUS_PERMISSION_PERMANENTLY_DENIED
            )
            finishLocation(KRLocationModule.response(
                if (canExplain) KRLocationModule.STATUS_PERMISSION_DENIED
                else KRLocationModule.STATUS_PERMISSION_PERMANENTLY_DENIED
            ))
        }
    }
    private var pendingImageCallback: com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback? = null
    private var pendingCameraCallback: com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback? = null
    private var pendingCameraFile: File? = null
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
    private val cameraCapture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { captured ->
        val callback = pendingCameraCallback ?: return@registerForActivityResult
        val target = pendingCameraFile
        pendingCameraCallback = null
        pendingCameraFile = null
        if (!captured || target == null || !target.isFile || target.length() <= 0L) {
            target?.delete()
            callback.invoke(KRMediaModule.response(KRMediaModule.STATUS_CANCELLED))
            return@registerForActivityResult
        }
        callback.invoke(
            KRMediaModule.response(
                KRMediaModule.STATUS_SUCCESS,
                paths = listOf("file://${target.absolutePath}")
            )
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
        cancelLocationRequest()
        pendingCameraFile?.delete()
        pendingCameraFile = null
        pendingCameraCallback = null
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
            moduleExport(KRTrackModule.MODULE_NAME) { KRTrackModule() }
            moduleExport(KRLocaleModule.MODULE_NAME) {
                KRLocaleModule()
            }
            moduleExport(KRThemeHostModule.MODULE_NAME) {
                KRThemeHostModule()
            }
            moduleExport(KRDataArchiveModule.MODULE_NAME) {
                KRDataArchiveModule()
            }
            moduleExport(KRLocationModule.MODULE_NAME) { KRLocationModule() }
            moduleExport(KRPlaceNetworkModule.MODULE_NAME) { KRPlaceNetworkModule() }
            moduleExport(KRExternalNavigationModule.MODULE_NAME) { KRExternalNavigationModule() }
        }
    }

    override fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalRenderView(kuiklyRenderExport)
        with(kuiklyRenderExport) {
            renderViewExport(KRAmapView.VIEW_NAME, ::KRAmapView, null)
        }
    }

    override fun finishRoute() {
        finish()
    }

    internal fun pickImages(
        maxCount: Int,
        callback: com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
    ) {
        if (pendingImageCallback != null || pendingCameraCallback != null) {
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

    internal fun captureImage(
        callback: com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
    ) {
        if (pendingImageCallback != null || pendingCameraCallback != null) {
            callback.invoke(KRMediaModule.response(
                KRMediaModule.STATUS_FAILURE,
                "已有照片操作正在进行。"
            ))
            return
        }
        val target = runCatching {
            val directory = File(filesDir, "images/original").apply {
                check(mkdirs() || isDirectory)
            }
            File.createTempFile("camera_", ".jpg", directory)
        }.getOrElse {
            callback.invoke(KRMediaModule.response(
                KRMediaModule.STATUS_FAILURE,
                "无法创建拍照目标文件。"
            ))
            return
        }
        val uri = runCatching {
            FileProvider.getUriForFile(this, "$packageName.fileprovider", target)
        }.getOrElse {
            target.delete()
            callback.invoke(KRMediaModule.response(
                KRMediaModule.STATUS_FAILURE,
                "无法准备系统相机。"
            ))
            return
        }
        pendingCameraFile = target
        pendingCameraCallback = callback
        try {
            cameraCapture.launch(uri)
        } catch (_: ActivityNotFoundException) {
            finishCameraLaunchFailure(KRMediaModule.STATUS_UNSUPPORTED, "当前设备没有可用的系统相机。")
        } catch (_: Throwable) {
            finishCameraLaunchFailure(KRMediaModule.STATUS_FAILURE, "无法打开系统相机。")
        }
    }

    private fun finishCameraLaunchFailure(status: String, message: String) {
        val callback = pendingCameraCallback
        pendingCameraCallback = null
        pendingCameraFile?.delete()
        pendingCameraFile = null
        callback?.invoke(KRMediaModule.response(status, message))
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

    internal fun requestCurrentLocation(
        callback: com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
    ) {
        if (pendingLocationCallback != null) {
            logLocationWarning("request_rejected_in_flight", KRLocationModule.STATUS_FAILURE)
            callback.invoke(KRLocationModule.response(KRLocationModule.STATUS_FAILURE, "已有定位请求正在进行。"))
            return
        }
        logLocationInfo("request_started")
        pendingLocationCallback = callback
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            logLocationInfo(
                if (fine == PackageManager.PERMISSION_GRANTED) "permission_already_granted_fine"
                else "permission_already_granted_coarse"
            )
            startOneShotLocation()
        } else {
            logLocationInfo("permission_request_started")
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun startOneShotLocation() {
        val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) {
            logLocationWarning("location_manager_unavailable", KRLocationModule.STATUS_UNAVAILABLE)
            finishLocation(KRLocationModule.response(KRLocationModule.STATUS_UNAVAILABLE))
            return
        }
        val enabled = runCatching {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
        if (!enabled) {
            logLocationWarning("service_disabled", KRLocationModule.STATUS_SERVICE_DISABLED)
            finishLocation(KRLocationModule.response(KRLocationModule.STATUS_SERVICE_DISABLED))
            return
        }
        logLocationInfo("service_enabled")
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        if (providers.isEmpty()) {
            logLocationWarning("provider_unavailable", KRLocationModule.STATUS_UNAVAILABLE)
            finishLocation(KRLocationModule.response(KRLocationModule.STATUS_UNAVAILABLE))
            return
        }
        Log.i(
            LOCATION_LOG_TAG,
            "stage=providers_selected providers=${providers.joinToString(",") { it.safeProviderName() }}"
        )
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (pendingLocationCallback == null) return
                Log.i(
                    LOCATION_LOG_TAG,
                    "stage=location_succeeded provider=${location.provider.safeProviderName()} " +
                        "hasAccuracy=${location.hasAccuracy()}"
                )
                finishLocation(KRLocationModule.response(
                    KRLocationModule.STATUS_SUCCESS,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy.toDouble()
                ))
            }
            override fun onProviderDisabled(provider: String) {
                Log.w(
                    LOCATION_LOG_TAG,
                    "stage=provider_disabled provider=${provider.safeProviderName()}"
                )
                val hasEnabledProvider = providers.any { candidate ->
                    runCatching { manager.isProviderEnabled(candidate) }.getOrDefault(false)
                }
                if (!hasEnabledProvider) {
                    logLocationWarning("all_providers_disabled", KRLocationModule.STATUS_SERVICE_DISABLED)
                    finishLocation(KRLocationModule.response(KRLocationModule.STATUS_SERVICE_DISABLED))
                }
            }
            override fun onProviderEnabled(provider: String) = Unit
            @Deprecated("Legacy callback")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        activeLocationListener = listener
        runCatching {
            @Suppress("MissingPermission")
            providers.forEach { provider ->
                manager.requestLocationUpdates(
                    provider,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
            locationHandler.postDelayed(locationTimeout, LOCATION_TIMEOUT_MS)
            logLocationInfo("location_request_started")
        }.onFailure { error ->
            Log.e(
                LOCATION_LOG_TAG,
                "stage=location_request_failed errorType=${error.javaClass.simpleName} " +
                    "mappedStatus=${KRLocationModule.STATUS_FAILURE}"
            )
            finishLocation(KRLocationModule.response(KRLocationModule.STATUS_FAILURE, "无法启动定位，请重试。"))
        }
    }

    private fun finishLocation(response: String) {
        val callback = pendingLocationCallback ?: return
        cancelLocationListener()
        pendingLocationCallback = null
        logLocationInfo("request_finished")
        callback.invoke(response)
    }

    private fun cancelLocationListener() {
        locationHandler.removeCallbacks(locationTimeout)
        val listener = activeLocationListener ?: return
        activeLocationListener = null
        val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        runCatching { manager?.removeUpdates(listener) }
    }

    private fun cancelLocationRequest() {
        if (pendingLocationCallback != null) logLocationInfo("request_cancelled_on_destroy")
        cancelLocationListener()
        pendingLocationCallback = null
    }

    private fun logLocationInfo(stage: String) {
        Log.i(LOCATION_LOG_TAG, "stage=$stage")
    }

    private fun logLocationWarning(stage: String, mappedStatus: String) {
        Log.w(LOCATION_LOG_TAG, "stage=$stage mappedStatus=$mappedStatus")
    }

    private fun String?.safeProviderName(): String = when (this) {
        LocationManager.GPS_PROVIDER -> "gps"
        LocationManager.NETWORK_PROVIDER -> "network"
        LocationManager.PASSIVE_PROVIDER -> "passive"
        "fused" -> "fused"
        null -> "none"
        else -> "other"
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

        private const val LOCATION_TIMEOUT_MS = 10_000L
        private const val LOCATION_LOG_TAG = "CityCapsuleLocation"

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
