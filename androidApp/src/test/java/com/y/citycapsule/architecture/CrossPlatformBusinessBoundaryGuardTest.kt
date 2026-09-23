package com.y.citycapsule.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossPlatformBusinessBoundaryGuardTest {
    @Test
    fun commonMainDoesNotImportPlatformSdkOrPlatformBusinessPackages() {
        val commonMain = File(projectRoot(), "shared/src/commonMain/kotlin")
        val forbiddenImports = listOf(
            "import android.",
            "import androidx.activity.",
            "import com.amap.",
            "import com.tencent.mmkv.",
            "import ohos.",
            "import @ohos.",
            "import @kit.",
            "import @hadss/hmrouter"
        )

        commonMain.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val source = file.readText()
            forbiddenImports.forEach { forbidden ->
                assertFalse(
                    "commonMain must not depend on a platform SDK: ${file.relativeTo(projectRoot())} contains $forbidden",
                    source.contains(forbidden)
                )
            }
        }
    }

    @Test
    fun androidActivityRemainsAThinHost() {
        val activity = File(
            projectRoot(),
            "androidApp/src/main/java/com/y/citycapsule/KuiklyHostActivity.kt"
        ).readText()

        listOf(
            "android.location.",
            "ActivityResultContracts.",
            "FileProvider",
            "DataArchiveFileStore",
            "core.place.",
            "core.capsule.",
            "core.roaming.",
            "feature."
        ).forEach { forbidden ->
            assertFalse("KuiklyHostActivity must delegate platform capabilities: found $forbidden", activity.contains(forbidden))
        }
        listOf("AndroidMediaHost", "AndroidLocationHost", "AndroidArchiveHost").forEach { adapter ->
            assertTrue("KuiklyHostActivity must delegate to $adapter", activity.contains(adapter))
        }
    }

    @Test
    fun roamingStoreSerializesStateThroughItsReducer() {
        val source = File(
            projectRoot(),
            "shared/src/commonMain/kotlin/com/y/citycapsule/feature/RoamingSessionState.kt"
        ).readText()
        val assignments = Regex("mutable\\.value\\s*=").findAll(source).count()

        assertTrue(source.contains("for (mutation in mutations)"))
        assertTrue(source.contains("RoamingSessionReducer.reduce"))
        assertTrue(source.contains("override fun dispose()"))
        assertTrue(
            "RoamingSessionStore state must only be assigned by its mutation reducer loop",
            assignments == 1
        )
    }

    private fun projectRoot(): File = generateSequence(
        File(System.getProperty("user.dir") ?: ".").canonicalFile,
        File::getParentFile
    ).filterNotNull().firstOrNull { File(it, "shared/src/commonMain").isDirectory }
        ?: error("Cannot locate CityCapsule project root")
}
