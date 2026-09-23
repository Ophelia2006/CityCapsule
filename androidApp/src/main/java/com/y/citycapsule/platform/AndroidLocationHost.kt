package com.y.citycapsule.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.y.citycapsule.module.KRLocationModule

/** Android permission and LocationManager adapter for the shared one-shot location contract. */
internal class AndroidLocationHost(private val activity: AppCompatActivity) {
    private var pendingCallback: KuiklyRenderCallback? = null
    private var activeListener: LocationListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable {
        logWarning("location_timeout", KRLocationModule.STATUS_FAILURE)
        finish(KRLocationModule.response(KRLocationModule.STATUS_FAILURE, "定位超时，请重试。"))
    }

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            logInfo("permission_granted")
            startOneShotLocation()
        } else {
            val canExplain = activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
            val status = if (canExplain) KRLocationModule.STATUS_PERMISSION_DENIED
                else KRLocationModule.STATUS_PERMISSION_PERMANENTLY_DENIED
            logWarning("permission_rejected", status)
            finish(KRLocationModule.response(status))
        }
    }

    fun request(callback: KuiklyRenderCallback) {
        if (pendingCallback != null) {
            logWarning("request_rejected_in_flight", KRLocationModule.STATUS_FAILURE)
            callback.invoke(KRLocationModule.response(KRLocationModule.STATUS_FAILURE, "已有定位请求正在进行。"))
            return
        }
        logInfo("request_started")
        pendingCallback = callback
        val fine = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            logInfo(if (fine == PackageManager.PERMISSION_GRANTED) "permission_already_granted_fine" else "permission_already_granted_coarse")
            startOneShotLocation()
        } else {
            logInfo("permission_request_started")
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    fun dispose() {
        if (pendingCallback != null) logInfo("request_cancelled_on_destroy")
        cancelListener()
        pendingCallback = null
    }

    private fun startOneShotLocation() {
        val manager = activity.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) {
            logWarning("location_manager_unavailable", KRLocationModule.STATUS_UNAVAILABLE)
            finish(KRLocationModule.response(KRLocationModule.STATUS_UNAVAILABLE))
            return
        }
        val enabled = runCatching {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
        if (!enabled) {
            logWarning("service_disabled", KRLocationModule.STATUS_SERVICE_DISABLED)
            finish(KRLocationModule.response(KRLocationModule.STATUS_SERVICE_DISABLED))
            return
        }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) {
            logWarning("provider_unavailable", KRLocationModule.STATUS_UNAVAILABLE)
            finish(KRLocationModule.response(KRLocationModule.STATUS_UNAVAILABLE))
            return
        }
        Log.i(LOG_TAG, "stage=providers_selected providers=${providers.joinToString(",") { it.safeName() }}")
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (pendingCallback == null) return
                Log.i(LOG_TAG, "stage=location_succeeded provider=${location.provider.safeName()} hasAccuracy=${location.hasAccuracy()}")
                finish(KRLocationModule.response(
                    KRLocationModule.STATUS_SUCCESS,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy.toDouble()
                ))
            }

            override fun onProviderDisabled(provider: String) {
                Log.w(LOG_TAG, "stage=provider_disabled provider=${provider.safeName()}")
                val anyEnabled = providers.any { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
                if (!anyEnabled) {
                    logWarning("all_providers_disabled", KRLocationModule.STATUS_SERVICE_DISABLED)
                    finish(KRLocationModule.response(KRLocationModule.STATUS_SERVICE_DISABLED))
                }
            }

            override fun onProviderEnabled(provider: String) = Unit

            @Deprecated("Legacy callback")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        activeListener = listener
        runCatching {
            @Suppress("MissingPermission")
            providers.forEach { manager.requestLocationUpdates(it, 0L, 0f, listener, Looper.getMainLooper()) }
            handler.postDelayed(timeout, TIMEOUT_MS)
            logInfo("location_request_started")
        }.onFailure { error ->
            Log.e(LOG_TAG, "stage=location_request_failed errorType=${error.javaClass.simpleName} mappedStatus=${KRLocationModule.STATUS_FAILURE}")
            finish(KRLocationModule.response(KRLocationModule.STATUS_FAILURE, "无法启动定位，请重试。"))
        }
    }

    private fun finish(response: String) {
        val callback = pendingCallback ?: return
        cancelListener()
        pendingCallback = null
        logInfo("request_finished")
        callback.invoke(response)
    }

    private fun cancelListener() {
        handler.removeCallbacks(timeout)
        val listener = activeListener ?: return
        activeListener = null
        val manager = activity.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        runCatching { manager?.removeUpdates(listener) }
    }

    private fun logInfo(stage: String) = Log.i(LOG_TAG, "stage=$stage")
    private fun logWarning(stage: String, status: String) = Log.w(LOG_TAG, "stage=$stage mappedStatus=$status")

    private fun String?.safeName(): String = when (this) {
        LocationManager.GPS_PROVIDER -> "gps"
        LocationManager.NETWORK_PROVIDER -> "network"
        LocationManager.PASSIVE_PROVIDER -> "passive"
        "fused" -> "fused"
        null -> "none"
        else -> "other"
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
        const val LOG_TAG = "CityCapsuleLocation"
    }
}
