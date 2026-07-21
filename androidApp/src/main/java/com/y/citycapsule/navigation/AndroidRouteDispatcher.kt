package com.y.citycapsule.navigation

import android.app.Activity
import android.content.Context
import android.util.Log
import com.y.citycapsule.KuiklyHostActivity
import org.json.JSONObject

/**
 * Android equivalent of HarmonyRouteDispatcher.
 *
 * This is the only Android class allowed to choose between a Kuikly host and a native route.
 */
class AndroidRouteDispatcher internal constructor(
    private val stackCoordinator: AndroidRouteStackCoordinator,
    private val nativeRouteRegistry: AndroidNativeRouteRegistry
) {

    fun openPage(
        context: Context,
        pageName: String,
        pageData: JSONObject
    ): Boolean = runCatching {
        dispatch(
            context = context,
            request = AndroidRouteRequestDecoder.decode(pageName, pageData)
        )
    }.onFailure { error ->
        Log.e(TAG, "Failed to open route '$pageName'.", error)
    }.getOrDefault(false)

    fun back(context: Context): Boolean {
        val activity = context as? Activity
        if (activity == null) {
            Log.e(TAG, "Cannot go back because router context is not an Activity.")
            return false
        }
        activity.finish()
        return true
    }

    internal fun dispatch(
        context: Context,
        request: AndroidRouteRequest
    ): Boolean {
        val effectiveRequest = if (request.action == AndroidRouteAction.BACK_TO) {
            val closedCount = stackCoordinator.backTo(request.routeKey)
            when (AndroidBackToPolicy.decide(closedCount)) {
                AndroidBackToDecision.COMPLETE -> return true
                AndroidBackToDecision.REPLACE_TARGET -> {
                    Log.w(
                        TAG,
                        "backTo target '${request.routeKey}' is not in the Android route stack; " +
                            "replacing the current route with '${request.target}'."
                    )
                    request.copy(action = AndroidRouteAction.REPLACE)
                }
            }
        } else {
            request
        }

        val opened = when (effectiveRequest.targetType) {
            AndroidRouteTargetType.KUIKLY -> {
                KuiklyHostActivity.start(context, effectiveRequest)
                true
            }

            AndroidRouteTargetType.NATIVE -> {
                nativeRouteRegistry.open(
                    context = context,
                    path = effectiveRequest.target,
                    pageDataJson = effectiveRequest.pageDataJson
                ).also { handled ->
                    if (!handled) {
                        Log.e(TAG, "Native route '${effectiveRequest.target}' is not registered.")
                    }
                }
            }
        }

        if (opened && effectiveRequest.action == AndroidRouteAction.REPLACE) {
            (context as? Activity)?.finish()
                ?: Log.w(TAG, "replace opened the target but could not finish a non-Activity context.")
        }
        return opened
    }

    companion object {
        private const val TAG = "AndroidRoute"

        val shared: AndroidRouteDispatcher = AndroidRouteDispatcher(
            stackCoordinator = AndroidRouteStackCoordinator.shared,
            nativeRouteRegistry = AndroidNativeRouteRegistry.shared
        )
    }
}
