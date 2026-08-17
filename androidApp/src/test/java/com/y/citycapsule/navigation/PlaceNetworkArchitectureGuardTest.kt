package com.y.citycapsule.navigation

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceNetworkArchitectureGuardTest {
    @Test
    fun basePagerRegistersPlaceNetworkProxyModule() {
        val projectDir = File(requireNotNull(System.getProperty("user.dir")))
        val root = if (projectDir.name == "androidApp") projectDir.parentFile else projectDir
        val source = File(
            root,
            "shared/src/commonMain/kotlin/com/y/citycapsule/base/BasePager.kt"
        ).readText()

        assertTrue(
            "Every independent Pager that uses place networking must inherit its registered proxy module.",
            source.contains(
                "externalModules[AmapPlaceRemoteDataSource.MODULE_NAME] = " +
                    "KuiklyPlaceNetworkModule()"
            )
        )
    }
}
