package com.y.citycapsule.architecture

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossPlatformCapabilityRegistrationGuardTest {

    @Test
    fun everySharedCapabilityIsRegisteredByAndroidAndHarmonyHosts() {
        val root = projectRoot()
        val shared = File(
            root,
            "shared/src/commonMain/kotlin/com/y/citycapsule/base/BasePager.kt"
        ).readText()
        val android = File(
            root,
            "androidApp/src/main/java/com/y/citycapsule/KuiklyHostActivity.kt"
        ).readText()
        val harmony = File(
            root,
            "ohosApp/entry/src/main/ets/kuikly/KuiklyViewDelegate.ets"
        ).readText()

        val capabilities = listOf(
            Registration("storage", "StorageProtocol.MODULE_NAME", "KRStorageModule.MODULE_NAME"),
            Registration("theme", "ThemeHostProtocol.MODULE_NAME", "KRThemeHostModule.MODULE_NAME"),
            Registration("media", "KuiklyPhotoPicker.MODULE_NAME", "KRMediaModule.MODULE_NAME"),
            Registration(
                "locale",
                "KuiklyLocalCapsuleDateFormatter.MODULE_NAME",
                "KRLocaleModule.MODULE_NAME"
            ),
            Registration(
                "archive",
                "KuiklyDataArchiveCapability.MODULE_NAME",
                "KRDataArchiveModule.MODULE_NAME"
            ),
            Registration(
                "location",
                "KuiklyLocationCapability.MODULE_NAME",
                "KRLocationModule.MODULE_NAME"
            ),
            Registration(
                "external navigation",
                "KuiklyExternalNavigationCapability.MODULE_NAME",
                "KRExternalNavigationModule.MODULE_NAME"
            ),
            Registration(
                "place network",
                "AmapPlaceRemoteDataSource.MODULE_NAME",
                "KRPlaceNetworkModule.MODULE_NAME"
            ),
            Registration("track files", "KuiklyTrackFiles.MODULE", "KRTrackModule.MODULE_NAME"),
            Registration("share", "KuiklyShareCapability.MODULE", "KRShareModule.MODULE_NAME")
        )

        capabilities.forEach { registration ->
            assertTrue(
                "Shared BasePager must register ${registration.name}",
                shared.contains(registration.sharedToken)
            )
            assertTrue(
                "Android host must register ${registration.name}",
                android.contains(registration.hostToken)
            )
            assertTrue(
                "HarmonyOS host must register ${registration.name}",
                harmony.contains(registration.hostToken)
            )
        }
    }

    @Test
    fun mapViewIsRegisteredByBothPlatformHosts() {
        val root = projectRoot()
        val android = File(
            root,
            "androidApp/src/main/java/com/y/citycapsule/KuiklyHostActivity.kt"
        ).readText()
        val harmony = File(
            root,
            "ohosApp/entry/src/main/ets/kuikly/KuiklyViewDelegate.ets"
        ).readText()

        assertTrue(android.contains("renderViewExport(KRAmapView.VIEW_NAME"))
        assertTrue(harmony.contains("map.set(KRAmapView.VIEW_NAME"))
    }

    private fun projectRoot(): File = generateSequence(
        File(System.getProperty("user.dir") ?: ".").canonicalFile,
        File::getParentFile
    ).filterNotNull().firstOrNull { File(it, "shared/src/commonMain").isDirectory }
        ?: error("Cannot locate CityCapsule project root from ${System.getProperty("user.dir")}")

    private data class Registration(
        val name: String,
        val sharedToken: String,
        val hostToken: String
    )
}
