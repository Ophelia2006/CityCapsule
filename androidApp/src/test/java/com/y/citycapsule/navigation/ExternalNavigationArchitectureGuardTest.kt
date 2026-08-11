package com.y.citycapsule.navigation

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalNavigationArchitectureGuardTest {
    @Test
    fun basePagerRegistersExternalNavigationProxyModule() {
        val source = File(sharedSourceRoot(), "base/BasePager.kt").readText()

        assertTrue(
            "BasePager must register the shared external-navigation proxy before a page acquires it.",
            source.contains(
                "externalModules[KuiklyExternalNavigationCapability.MODULE_NAME] = " +
                    "KuiklyExternalNavigationModule()"
            )
        )
    }

    private fun sharedSourceRoot(): File {
        val projectDir = File(requireNotNull(System.getProperty("user.dir")))
        val root = if (projectDir.name == "androidApp") projectDir.parentFile else projectDir
        return File(root, "shared/src/commonMain/kotlin/com/y/citycapsule")
    }
}
