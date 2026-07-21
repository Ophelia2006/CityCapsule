package com.y.citycapsule.navigation

import android.content.Context

fun interface AndroidNativeRouteLauncher {
    fun launch(
        context: Context,
        path: String,
        pageDataJson: String
    ): Boolean
}

/**
 * Central registry for Android-native destinations such as scanner, map and file import.
 *
 * T5-T7 establishes the registry. Feature stages register concrete native launchers.
 */
class AndroidNativeRouteRegistry internal constructor() {
    private val lock = Any()
    private val launchers = mutableMapOf<String, AndroidNativeRouteLauncher>()

    fun register(path: String, launcher: AndroidNativeRouteLauncher) {
        require(path.startsWith(NATIVE_ROUTE_PREFIX)) {
            "Native route path must start with '$NATIVE_ROUTE_PREFIX'."
        }
        synchronized(lock) {
            launchers[path] = launcher
        }
    }

    fun unregister(path: String) {
        synchronized(lock) {
            launchers.remove(path)
        }
    }

    fun open(
        context: Context,
        path: String,
        pageDataJson: String
    ): Boolean {
        val launcher = synchronized(lock) { launchers[path] } ?: return false
        return launcher.launch(context, path, pageDataJson)
    }

    companion object {
        private const val NATIVE_ROUTE_PREFIX = "/native/"

        val shared: AndroidNativeRouteRegistry = AndroidNativeRouteRegistry()
    }
}
