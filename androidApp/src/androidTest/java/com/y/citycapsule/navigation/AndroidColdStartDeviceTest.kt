package com.y.citycapsule.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.y.citycapsule.KuiklyHostActivity
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.onboarding.OnboardingContract
import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.profile.LocalProfileCodec
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.StorageKey
import com.y.citycapsule.core.storage.StorageProtocol
import com.y.citycapsule.storage.AndroidMmkvStorage
import com.y.citycapsule.storage.AndroidStorageDispatcher
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidColdStartDeviceTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val dispatcher = AndroidStorageDispatcher()
    private var originalState: Map<StorageKey<*>, RawValue> = emptyMap()

    @Before
    fun setUp() {
        assertTrue(AndroidMmkvStorage.initialize(context).success)
        originalState = STARTUP_KEYS.associateWith(::snapshot)
        finishRouteActivities()
        AndroidRouteStackCoordinator.shared.clear()
        clearStartupState()
    }

    @After
    fun tearDown() {
        finishRouteActivities()
        AndroidRouteStackCoordinator.shared.clear()
        STARTUP_KEYS.forEach { key ->
            remove(key)
            originalState[key]?.let { restore(key, it) }
        }
    }

    @Test
    fun freshColdStartReplacesLaunchGateWithOnboarding() {
        launchAndAssert(AppRouteTable.ROUTE_ONBOARDING)
    }

    @Test
    fun completedProfileColdStartReplacesLaunchGateWithHome() {
        put(
            AppStorageKeys.Profile.LOCAL_PROFILE,
            LocalProfileCodec.encode(LocalProfile.DEFAULT)
        )
        put(
            AppStorageKeys.Onboarding.COMPLETED_VERSION,
            OnboardingContract.CURRENT_COMPLETED_VERSION.toString()
        )

        launchAndAssert(AppRouteTable.ROUTE_HOME)
    }

    private fun launchAndAssert(expectedDestinationRouteKey: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        assertNotNull("AndroidManifest must expose a launcher Activity.", launchIntent)
        assertEquals(
            KuiklyHostActivity::class.java.name,
            requireNotNull(launchIntent).component?.className
        )
        assertEquals(
            AppRouteTable.PAGE_LAUNCH_GATE,
            AndroidLaunchContract.resolvePageName(
                launchIntent.getStringExtra("pageName")
            )
        )
        assertEquals(
            AppRouteTable.ROUTE_LAUNCH_GATE,
            AndroidLaunchContract.resolveRouteKey(
                requestedRouteKey = launchIntent.getStringExtra("routeKey"),
                resolvedPageName = AndroidLaunchContract.resolvePageName(
                    launchIntent.getStringExtra("pageName")
                )
            )
        )
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)

        val scenario = ActivityScenario.launch<KuiklyHostActivity>(launchIntent)
        assertEventually("Expected cold start destination '$expectedDestinationRouteKey'.") {
            AndroidRouteStackCoordinator.shared.snapshotRouteKeys() ==
                listOf(expectedDestinationRouteKey)
        }
        scenario.close()
    }

    private fun clearStartupState() {
        STARTUP_KEYS.forEach(::remove)
    }

    private fun put(key: StorageKey<*>, encodedValue: String) {
        val response = dispatcher.dispatch(
            StorageProtocol.METHOD_PUT,
            request(key).put(StorageProtocol.FIELD_VALUE, encodedValue).toString()
        )
        assertEquals(StorageProtocol.CODE_SUCCESS, response.getInt(StorageProtocol.FIELD_CODE))
    }

    private fun remove(key: StorageKey<*>) {
        val response = dispatcher.dispatch(
            StorageProtocol.METHOD_REMOVE,
            request(key).toString()
        )
        assertEquals(StorageProtocol.CODE_SUCCESS, response.getInt(StorageProtocol.FIELD_CODE))
    }

    private fun request(key: StorageKey<*>): JSONObject = JSONObject()
        .put(StorageProtocol.FIELD_PROTOCOL_VERSION, StorageProtocol.VERSION)
        .put(StorageProtocol.FIELD_STORE, key.store.wireValue)
        .put(StorageProtocol.FIELD_KEY, key.wireKey)
        .put(StorageProtocol.FIELD_TYPE, key.codec.valueType.wireValue)

    private fun snapshot(key: StorageKey<*>): RawValue {
        val store = requireNotNull(AndroidMmkvStorage.store(key.store.wireValue))
        return RawValue(
            value = store.read(key.wireKey),
            valueType = store.read(TYPE_KEY_PREFIX + key.wireKey)
        )
    }

    private fun restore(key: StorageKey<*>, rawValue: RawValue) {
        val store = requireNotNull(AndroidMmkvStorage.store(key.store.wireValue))
        rawValue.value?.let { store.write(key.wireKey, it) }
        rawValue.valueType?.let { store.write(TYPE_KEY_PREFIX + key.wireKey, it) }
    }

    private fun assertEventually(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + ROUTE_TIMEOUT_MS
        do {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            if (condition()) {
                return
            }
            SystemClock.sleep(POLL_INTERVAL_MS)
        } while (SystemClock.uptimeMillis() < deadline)
        throw AssertionError(
            "$message Actual stack=${AndroidRouteStackCoordinator.shared.snapshotRouteKeys()}"
        )
    }

    private fun finishRouteActivities() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            val activities = Stage.values()
                .flatMap { stage -> monitor.getActivitiesInStage(stage) }
                .filterIsInstance<KuiklyHostActivity>()
                .distinct()
            activities.forEach(Activity::finish)
        }
        instrumentation.waitForIdleSync()
    }

    private data class RawValue(
        val value: String?,
        val valueType: String?
    )

    private companion object {
        const val TYPE_KEY_PREFIX = "__cc_type__."
        const val ROUTE_TIMEOUT_MS = 15_000L
        const val POLL_INTERVAL_MS = 100L

        val STARTUP_KEYS: List<StorageKey<*>> = listOf(
            AppStorageKeys.Onboarding.COMPLETED_VERSION,
            AppStorageKeys.Profile.LOCAL_PROFILE,
            AppStorageKeys.Onboarding.DRAFT
        )
    }
}
