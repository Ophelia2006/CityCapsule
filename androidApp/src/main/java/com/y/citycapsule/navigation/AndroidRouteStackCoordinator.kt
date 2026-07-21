package com.y.citycapsule.navigation

import java.lang.ref.WeakReference

/**
 * Contract implemented by Android pages that participate in the app route stack.
 */
interface AndroidRouteHost {
    val routeKey: String

    fun finishRoute()
}

/**
 * Tracks route keys independently from Android component class names.
 *
 * Every Kuikly page uses the same Activity class, so FLAG_ACTIVITY_CLEAR_TOP cannot implement
 * AppNavigator.backTo(routeKey). This coordinator finds the latest matching route instance and
 * closes only the hosts above it.
 */
class AndroidRouteStackCoordinator internal constructor() {
    private val lock = Any()
    private val entries = mutableListOf<RouteEntry>()

    fun register(host: AndroidRouteHost) {
        require(host.routeKey.isNotBlank()) { "Android route host key must not be blank." }
        synchronized(lock) {
            pruneLocked()
            entries.removeAll { it.host.get() === host }
            entries += RouteEntry(
                routeKey = host.routeKey,
                host = WeakReference(host)
            )
        }
    }

    fun unregister(host: AndroidRouteHost) {
        synchronized(lock) {
            entries.removeAll {
                val current = it.host.get()
                current == null || current === host
            }
        }
    }

    /**
     * @return number of hosts closed, or null when the target is not in the current stack.
     */
    fun backTo(routeKey: String): Int? {
        require(routeKey.isNotBlank()) { "backTo routeKey must not be blank." }

        val hostsToFinish = synchronized(lock) {
            pruneLocked()
            val targetIndex = entries.indexOfLast { it.routeKey == routeKey }
            if (targetIndex < 0) {
                return null
            }

            val entriesAbove = entries.subList(targetIndex + 1, entries.size).toList()
            entriesAbove.forEach(entries::remove)
            entriesAbove.mapNotNull { it.host.get() }.asReversed()
        }

        hostsToFinish.forEach(AndroidRouteHost::finishRoute)
        return hostsToFinish.size
    }

    internal fun snapshotRouteKeys(): List<String> = synchronized(lock) {
        pruneLocked()
        entries.map(RouteEntry::routeKey)
    }

    internal fun clear() {
        synchronized(lock) {
            entries.clear()
        }
    }

    private fun pruneLocked() {
        entries.removeAll { it.host.get() == null }
    }

    private data class RouteEntry(
        val routeKey: String,
        val host: WeakReference<AndroidRouteHost>
    )

    companion object {
        val shared: AndroidRouteStackCoordinator = AndroidRouteStackCoordinator()
    }
}
